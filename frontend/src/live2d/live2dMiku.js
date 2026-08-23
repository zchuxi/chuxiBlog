// 初曦的窝 —— Live2D 看板娘加载器（Cubism 4 / 初音未来 miku）
// 使用 pixi-live2d-display 加载 .moc3 模型，需先加载 Cubism 4 核心 live2dcubismcore.min.js。
//
// 交互：
//   - 单击看板娘：随机切换一个表情（7 个非水印表情），并短暂显示表情名
//   - 双击看板娘：恢复默认表情
//   - 点击时有轻微的向上弹跳反馈
//   - 看板娘整体可拖拽（由 live2dWidgetDrag.js 负责，位置持久化到 localStorage）
//
// 说明：
//   1. miku 模型（.moc3/.model3.json）为 Cubism 4 格式，旧版 2.1 核心（live2d.min.js）无法加载。
//   2. 表情/动作通过在内存中改写模型配置注册（不修改磁盘上的 model3.json，规避「不可二改」）。
//   3. pixi.js / pixi-live2d-display 体积较大，用动态 import 按需加载，避免进入首屏主包。
//   4. 适配画布时按 drawable 真实边界计算（避免右侧头发被声明画布裁切），保留安全边距并居中。

const CORE_URL = '/live2d/live2dcubismcore.min.js'
const MODEL_URL = '/live2d/miku/miku.model3.json'

// 模型自带 6 张 4096 贴图（共 25.4MB），但看板娘最大只显示 270x430 CSS 像素，
// DPR 3 下也只需 810x1290。4096 会带来三重开销：下载 25.4MB、主线程解码
// 约 208ms/张、解码后显存 384MB。运行时改用 2048 副本（6.4MB / 96MB 显存），
// 原始 miku.4096/ 完整保留在磁盘上不做修改。
const TEXTURE_DIR_ORIGINAL = 'miku.4096/'
const TEXTURE_DIR_RUNTIME = 'miku.2048/'

// 首屏稳定后自动初始化看板娘的延迟（毫秒）；测试约束 ≤ 1000
export const LIVE2D_AUTO_START_DELAY_MS = 600

// 看板娘适配画布时四周保留的安全边距（像素）
export const LIVE2D_FIT_PADDING_PX = 12

// 可点击切换的表情（模型自带 8 个 exp3 中排除「水印」，另排除「QQ人」）
const EXPRESSIONS = ['圈圈', '脸红', '前倾', '唱歌', '葱', '比心']

let corePromise = null
let app = null
let model = null
let ready = false
let watermarkStop = null
let currentExpression = null
let tapTimer = null
let lastTapAt = 0
let toastEl = null
let fitStop = null

function loadCore() {
  if (corePromise) return corePromise
  corePromise = new Promise((resolve, reject) => {
    if (window.Live2DCubismCore) { resolve(); return }
    const s = document.createElement('script')
    s.src = CORE_URL
    s.async = true
    s.onload = () => resolve()
    s.onerror = () => reject(new Error('Cubism 4 核心加载失败: ' + CORE_URL))
    document.body.appendChild(s)
  })
  return corePromise
}

/* ---------- 贴图路径改写（降采样副本） ---------- */

/**
 * 把 model3.json 里的贴图路径指向降采样副本。
 * 只改内存中的配置对象，不触碰磁盘上的 model3.json 与原始贴图。
 * 找不到预期路径时原样返回，避免模型换版后静默加载失败。
 */
export function useDownscaledTextures(settingsJson) {
  const textures = settingsJson?.FileReferences?.Textures
  if (!Array.isArray(textures)) return settingsJson
  settingsJson.FileReferences.Textures = textures.map(path =>
    typeof path === 'string' && path.startsWith(TEXTURE_DIR_ORIGINAL)
      ? TEXTURE_DIR_RUNTIME + path.slice(TEXTURE_DIR_ORIGINAL.length)
      : path
  )
  return settingsJson
}

/* ---------- 画布适配 ---------- */

function getDrawableBounds(internalModel) {
  const coreModel = internalModel?.coreModel
  if (!coreModel || typeof coreModel.getDrawableCount !== 'function' || typeof internalModel.getDrawableVertices !== 'function') return null
  const count = coreModel.getDrawableCount()
  let minX = Infinity
  let minY = Infinity
  let maxX = -Infinity
  let maxY = -Infinity
  for (let i = 0; i < count; i++) {
    const visible = typeof coreModel.getDrawableDynamicFlagIsVisible === 'function'
      ? coreModel.getDrawableDynamicFlagIsVisible(i)
      : true
    const opacity = typeof coreModel.getDrawableOpacity === 'function'
      ? coreModel.getDrawableOpacity(i)
      : 1
    if (!visible || opacity <= 0) continue
    const vertices = internalModel.getDrawableVertices(i)
    if (!vertices) continue
    for (let j = 0; j < vertices.length; j += 2) {
      const x = vertices[j]
      const y = vertices[j + 1]
      if (x < minX) minX = x
      if (x > maxX) maxX = x
      if (y < minY) minY = y
      if (y > maxY) maxY = y
    }
  }
  if (!Number.isFinite(minX) || !Number.isFinite(maxX) || !Number.isFinite(minY) || !Number.isFinite(maxY)) return null
  if (maxX <= minX || maxY <= minY) return null
  return { minX, minY, maxX, maxY, width: maxX - minX, height: maxY - minY }
}

function unionBounds(previous, current) {
  if (!previous) return current
  const minX = Math.min(previous.minX, current.minX)
  const minY = Math.min(previous.minY, current.minY)
  const maxX = Math.max(previous.maxX, current.maxX)
  const maxY = Math.max(previous.maxY, current.maxY)
  return { minX, minY, maxX, maxY, width: maxX - minX, height: maxY - minY }
}

function createHitArea(bounds) {
  return {
    contains(x, y) {
      return x >= bounds.minX && x <= bounds.maxX && y >= bounds.minY && y <= bounds.maxY
    }
  }
}

export function fitLive2dModel(model, screen) {
  if (!model || !screen) return
  const W = screen.width || 0
  const H = screen.height || 0
  if (!W || !H) return
  const P = LIVE2D_FIT_PADDING_PX

  const currentBounds = getDrawableBounds(model.internalModel)
  if (currentBounds) {
    const bounds = unionBounds(model.__mikuFitBounds, currentBounds)
    model.__mikuFitBounds = bounds
    // 使用 drawable 真实边界：模型左上角为锚点，右侧贴安全边距、内容垂直居中
    const scale = Math.min((W - P * 2) / bounds.width, (H - P * 2) / bounds.height)
    model.scale.set(scale)
    model.anchor.set(0, 0)
    model.hitArea = createHitArea(bounds)
    model.x = W - P - bounds.maxX * scale
    model.y = (H - bounds.height * scale) / 2 - bounds.minY * scale
    return
  }

  // 兜底：使用声明画布尺寸，居中显示
  const internalModel = model.internalModel
  const iw = internalModel?.width || model.width || 1
  const ih = internalModel?.height || model.height || 1
  const scale = Math.min((W - P * 2) / iw, (H - P * 2) / ih)
  model.scale.set(scale)
  model.anchor.set(0.5, 0.5)
  model.x = W / 2
  model.y = H / 2
}

export function scheduleLive2dFit(targetModel, targetApp, frames = 2) {
  if (!targetModel || !targetApp?.ticker || !targetApp.screen) return () => {}
  let remaining = Math.max(1, Math.floor(frames))
  const onTick = () => {
    fitLive2dModel(targetModel, targetApp.screen)
    remaining -= 1
    if (remaining <= 0) targetApp.ticker.remove(onTick)
  }
  targetApp.ticker.add(onTick)
  return () => targetApp.ticker.remove(onTick)
}

function restartLive2dFit(frames) {
  fitStop?.()
  fitStop = model && app ? scheduleLive2dFit(model, app, frames) : null
}

function onResize() {
  if (app && model) fitLive2dModel(model, app.screen)
}

/* ---------- 会话创建（可注入依赖，便于测试） ---------- */

export async function createLive2dSession({ canvas, Application, Live2DModel, source = MODEL_URL }) {
  if (!canvas) throw new Error('未找到 live2d 画布元素')
  const stage = canvas.parentElement
  const w = stage?.clientWidth || 280
  const h = stage?.clientHeight || 280
  const resolution = typeof window !== 'undefined' && window.devicePixelRatio
    ? Math.min(window.devicePixelRatio, 2)
    : 1
  const app = new Application({
    view: canvas,
    width: w,
    height: h,
    resizeTo: stage || undefined,
    transparent: true,
    antialias: true,
    autoDensity: true,
    resolution,
    backgroundAlpha: 0,
    // 点击与拖动由外层 DOM 统一处理，避免 Pixi 对透明像素做 hit-test 后反复改写 cursor。
    eventFeatures: {
      move: false,
      globalMove: false,
      click: false,
      wheel: false
    }
  })
  try {
    const model = await Live2DModel.from(source, {
      ticker: app.ticker,
      autoHitTest: false,
      autoFocus: false
    })
    model.eventMode = 'none'
    model.interactiveChildren = false
    app.stage.addChild(model)
    return { app, model }
  } catch (err) {
    try { app.destroy(false) } catch { /* noop */ }
    throw err
  }
}

/* ---------- 水印隐藏（官方水印参数 Param137） ---------- */

export function keepMikuWatermarkHidden(internalModel) {
  const hide = () => internalModel.coreModel.setParameterValueById('Param137', 1)
  hide()
  internalModel.on('beforeModelUpdate', hide)
  return () => internalModel.off('beforeModelUpdate', hide)
}

/* ---------- 点击交互：表情切换 ---------- */

function getExpressionManager() {
  return model?.internalModel?.motionManager?.expressionManager || null
}

function showToast(text) {
  const widget = document.querySelector('.live2d-widget')
  if (!widget) return
  if (!toastEl) {
    toastEl = document.createElement('div')
    Object.assign(toastEl.style, {
      position: 'absolute',
      left: '50%',
      top: '4px',
      zIndex: 6,
      padding: '5px 12px',
      borderRadius: '999px',
      background: 'var(--popover-bg, rgba(255,255,255,.94))',
      border: '1px solid var(--topbar-border, rgba(0,0,0,.08))',
      boxShadow: '0 10px 22px rgba(0,0,0,.14)',
      color: 'var(--text-color, #333)',
      fontSize: '13px',
      fontWeight: 600,
      lineHeight: 1.4,
      whiteSpace: 'nowrap',
      pointerEvents: 'none',
      opacity: 0,
      transform: 'translateX(-50%) translateY(-5px)',
      transition: 'opacity .25s ease, transform .25s ease'
    })
    widget.appendChild(toastEl)
  }
  toastEl.textContent = text
  toastEl.style.opacity = '1'
  toastEl.style.transform = 'translateX(-50%) translateY(0)'
  clearTimeout(showToast._timer)
  showToast._timer = setTimeout(() => {
    if (toastEl) {
      toastEl.style.opacity = '0'
      toastEl.style.transform = 'translateX(-50%) translateY(-5px)'
    }
  }, 1300)
}

function bounce(strength) {
  if (!model || !app) return
  const baseY = model.y
  const duration = 300
  const start = performance.now()
  const onTick = () => {
    const t = Math.min(1, (performance.now() - start) / duration)
    const k = Math.sin(Math.PI * t)
    model.y = baseY - strength * 8 * k
    if (t >= 1) {
      app.ticker.remove(onTick)
      model.y = baseY
    }
  }
  app.ticker.add(onTick)
}

function playRandomExpression() {
  if (!model) return
  const list = EXPRESSIONS.length > 1 ? EXPRESSIONS.filter(n => n !== currentExpression) : EXPRESSIONS
  if (!list.length) return
  const name = list[Math.floor(Math.random() * list.length)]
  currentExpression = name
  model.expression(name).then(ok => {
    if (ok) {
      showToast(name)
      restartLive2dFit(45)
    }
    else currentExpression = null
  }).catch(() => { currentExpression = null })
}

function resetExpression() {
  if (!model) return
  const em = getExpressionManager()
  if (em) {
    em.resetExpression()
    currentExpression = null
    showToast('恢复默认')
    restartLive2dFit(45)
  }
}

function onTap() {
  if (!ready) return
  const now = Date.now()
  if (now - lastTapAt < 300) {
    // 双击：恢复默认表情
    clearTimeout(tapTimer)
    tapTimer = null
    lastTapAt = 0
    resetExpression()
    bounce(1.5)
    return
  }
  lastTapAt = now
  clearTimeout(tapTimer)
  tapTimer = setTimeout(() => {
    tapTimer = null
    lastTapAt = 0
    // 单击：随机表情
    playRandomExpression()
    bounce(1)
  }, 280)
}

export function handleLive2dTap() {
  onTap()
}

/* ---------- 生命周期 ---------- */

export function isLive2dReady() {
  return ready
}

export async function initLive2d(canvas) {
  if (app) return
  if (!canvas) throw new Error('未找到 live2d 画布元素')
  await loadCore()

  const [{ Application }, { Live2DModel }] = await Promise.all([
    import('pixi.js'),
    import('pixi-live2d-display-lipsyncpatch/cubism4'),
  ])

  // 读取模型配置并在内存中注册表情/动作（不动磁盘上的 model3.json）
  const res = await fetch(MODEL_URL)
  if (!res.ok) throw new Error('模型配置加载失败: ' + MODEL_URL)
  const settingsJson = await res.json()
  settingsJson.url = MODEL_URL
  useDownscaledTextures(settingsJson)
  settingsJson.FileReferences.Expressions = EXPRESSIONS.map(name => ({
    Name: name,
    File: encodeURIComponent(name + '.exp3.json')
  }))

  const session = await createLive2dSession({
    canvas,
    Application,
    Live2DModel,
    source: settingsJson
  })
  app = session.app
  model = session.model

  fitLive2dModel(model, app.screen)
  restartLive2dFit(3)
  window.addEventListener('resize', onResize)

  // 持续隐藏官方水印参数（Param137）
  watermarkStop = keepMikuWatermarkHidden(model.internalModel)

  ready = true
}

export function destroyLive2d() {
  ready = false
  clearTimeout(tapTimer)
  tapTimer = null
  clearTimeout(showToast._timer)
  fitStop?.()
  fitStop = null
  if (watermarkStop) {
    try { watermarkStop() } catch { /* noop */ }
    watermarkStop = null
  }
  if (model) {
    try { model.destroy() } catch { /* noop */ }
    model = null
  }
  if (toastEl) {
    toastEl.remove()
    toastEl = null
  }
  window.removeEventListener('resize', onResize)
  try { app?.destroy(false) } catch { /* noop */ }
  app = null
}
