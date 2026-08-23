import test from 'node:test'
import assert from 'node:assert/strict'
import { access, readFile } from 'node:fs/promises'
import * as live2dMiku from './live2dMiku.js'

test('Miku 模型配置引用的运行资源全部存在', async () => {
  const modelUrl = new URL('../../public/live2d/miku/miku.model3.json', import.meta.url)
  const modelConfig = JSON.parse(await readFile(modelUrl, 'utf8'))
  const references = modelConfig.FileReferences
  const assetPaths = [
    references.Moc,
    ...(references.Textures || []),
    references.Physics,
    references.DisplayInfo
  ].filter(Boolean)

  await Promise.all(assetPaths.map(assetPath => access(new URL(assetPath, modelUrl))))
})

test('模型创建失败时销毁临时 Pixi 应用，后续可重新初始化', async () => {
  assert.equal(typeof live2dMiku.createLive2dSession, 'function')

  const applications = []
  class FakeApplication {
    constructor() {
      this.ticker = {}
      this.stage = { addChild() {} }
      this.destroyed = false
      applications.push(this)
    }

    destroy() {
      this.destroyed = true
    }
  }

  const loadError = new Error('texture_05.png 加载失败')
  const failingModel = { from: async () => { throw loadError } }

  await assert.rejects(
    live2dMiku.createLive2dSession({
      canvas: { parentElement: { clientWidth: 280, clientHeight: 280 } },
      Application: FakeApplication,
      Live2DModel: failingModel
    }),
    loadError
  )
  assert.equal(applications[0].destroyed, true)

  const loadedModel = { internalModel: {}, destroy() {} }
  const successfulModel = { from: async () => loadedModel }
  const session = await live2dMiku.createLive2dSession({
    canvas: { parentElement: { clientWidth: 280, clientHeight: 280 } },
    Application: FakeApplication,
    Live2DModel: successfulModel
  })

  assert.equal(session.model, loadedModel)
  assert.equal(applications[1].destroyed, false)
})

test('Live2D 交互统一由 DOM 拖动层处理，Pixi 不应参与模型命中和光标管理', async () => {
  let applicationOptions = null
  class FakeApplication {
    constructor(options) {
      applicationOptions = options
      this.ticker = {}
      this.stage = { addChild() {} }
      this.renderer = { events: { cursorStyles: {} } }
    }
  }

  const loadedModel = {
    internalModel: {},
    eventMode: 'static',
    interactiveChildren: true,
    destroy() {}
  }
  await live2dMiku.createLive2dSession({
    canvas: { parentElement: { clientWidth: 280, clientHeight: 280 } },
    Application: FakeApplication,
    Live2DModel: {
      from: async (_source, options) => {
        assert.equal(options.autoInteract, undefined)
        assert.equal(options.autoHitTest, false)
        assert.equal(options.autoFocus, false)
        return loadedModel
      }
    }
  })

  assert.deepEqual(applicationOptions.eventFeatures, {
    move: false,
    globalMove: false,
    click: false,
    wheel: false
  })
  assert.equal(loadedModel.eventMode, 'none')
  assert.equal(loadedModel.interactiveChildren, false)
})

test('Miku 自动启动延迟不超过 1 秒', () => {
  assert.equal(typeof live2dMiku.LIVE2D_AUTO_START_DELAY_MS, 'number')
  assert.ok(live2dMiku.LIVE2D_AUTO_START_DELAY_MS <= 1000)
})

test('Miku 适配画布时完整居中显示并保留安全边距', () => {
  assert.equal(typeof live2dMiku.fitLive2dModel, 'function')
  assert.equal(typeof live2dMiku.LIVE2D_FIT_PADDING_PX, 'number')

  const scaleWrites = []
  const anchorWrites = []
  const model = {
    // 模拟已经缩放后的 Pixi 尺寸；适配算法不能再次使用它们作为原始尺寸。
    width: 198,
    height: 396,
    internalModel: { width: 1000, height: 2000 },
    scale: { set(value) { scaleWrites.push(value) } },
    anchor: { set(x, y) { anchorWrites.push([x, y]) } },
    x: 0,
    y: 0
  }
  const screen = { width: 280, height: 420 }

  live2dMiku.fitLive2dModel(model, screen)
  live2dMiku.fitLive2dModel(model, screen)

  const expectedScale = Math.min(
    (screen.width - live2dMiku.LIVE2D_FIT_PADDING_PX * 2) / model.internalModel.width,
    (screen.height - live2dMiku.LIVE2D_FIT_PADDING_PX * 2) / model.internalModel.height
  )
  assert.deepEqual(scaleWrites, [expectedScale, expectedScale], '重复适配不应因当前缩放尺寸而漂移')
  assert.deepEqual(anchorWrites, [[0.5, 0.5], [0.5, 0.5]])
  assert.equal(model.x, screen.width / 2)
  assert.equal(model.y, screen.height / 2)
  assert.ok(model.internalModel.height * expectedScale <= screen.height - live2dMiku.LIVE2D_FIT_PADDING_PX * 2)
})

test('Miku 适配使用 drawable 真实边界，避免右侧头发被声明画布裁切', () => {
  const scaleWrites = []
  const anchorWrites = []
  const model = {
    internalModel: {
      width: 1000,
      height: 2000,
      coreModel: {
        getDrawableCount() { return 3 },
        getDrawableDynamicFlagIsVisible(index) { return index !== 2 },
        getDrawableOpacity(index) { return index === 2 ? 0 : 1 }
      },
      getDrawableVertices(index) {
        if (index === 0) {
          return new Float32Array([-80, 100, 520, 100, 520, 1900, -80, 1900])
        }
        if (index === 1) {
          return new Float32Array([480, 400, 1120, 400, 1120, 1800, 480, 1800])
        }
        return new Float32Array([-600, -400, 1800, -400, 1800, 2400, -600, 2400])
      }
    },
    scale: { set(value) { scaleWrites.push(value) } },
    anchor: { set(x, y) { anchorWrites.push([x, y]) } },
    x: 0,
    y: 0
  }
  const screen = { width: 280, height: 420 }

  live2dMiku.fitLive2dModel(model, screen)

  const drawableWidth = 1200
  const drawableHeight = 1800
  const expectedScale = Math.min(
    (screen.width - live2dMiku.LIVE2D_FIT_PADDING_PX * 2) / drawableWidth,
    (screen.height - live2dMiku.LIVE2D_FIT_PADDING_PX * 2) / drawableHeight
  )

  assert.deepEqual(scaleWrites, [expectedScale])
  assert.deepEqual(anchorWrites, [[0, 0]])
  assert.equal(
    model.x + 1120 * expectedScale,
    screen.width - live2dMiku.LIVE2D_FIT_PADDING_PX,
    '最右侧头发网格应落在画布安全边距内'
  )
  assert.equal(
    model.y + 100 * expectedScale,
    (screen.height - drawableHeight * expectedScale) / 2,
    '真实 drawable 边界应在画布内垂直居中'
  )
})

test('Miku 加载后持续应用官方水印隐藏参数，并可解除监听', () => {
  assert.equal(typeof live2dMiku.keepMikuWatermarkHidden, 'function')

  const listeners = new Map()
  const writes = []
  const internalModel = {
    coreModel: {
      setParameterValueById(id, value) {
        writes.push([id, value])
      }
    },
    on(event, handler) {
      listeners.set(event, handler)
    },
    off(event, handler) {
      if (listeners.get(event) === handler) listeners.delete(event)
    }
  }

  const stop = live2dMiku.keepMikuWatermarkHidden(internalModel)

  assert.deepEqual(writes, [['Param137', 1]], '初始化时应立即隐藏水印')
  assert.equal(typeof listeners.get('beforeModelUpdate'), 'function')

  listeners.get('beforeModelUpdate')()
  assert.deepEqual(writes, [['Param137', 1], ['Param137', 1]], '每帧更新前应维持隐藏状态')

  stop()
  assert.equal(listeners.has('beforeModelUpdate'), false, '销毁模型时应解除监听')
})

test('Miku 适配会同步更新真实边界命中区，确保溢出声明画布的右侧仍可点击', () => {
  const model = {
    internalModel: {
      width: 3500,
      height: 8888,
      coreModel: {
        getDrawableCount() { return 1 },
        getDrawableDynamicFlagIsVisible() { return true },
        getDrawableOpacity() { return 1 }
      },
      getDrawableVertices() {
        return new Float32Array([743, 0, 5158, 0, 5158, 8979, 743, 8979])
      }
    },
    scale: { set() {} },
    anchor: { set() {} },
    x: 0,
    y: 0
  }

  live2dMiku.fitLive2dModel(model, { width: 280, height: 420 })

  assert.equal(typeof model.hitArea?.contains, 'function', '应为模型设置覆盖真实 drawable 边界的命中区')
  assert.equal(model.hitArea.contains(5157, 4500), true, '右侧溢出声明画布的网格应属于命中区')
  assert.equal(model.hitArea.contains(700, 4500), false, '真实边界外不应触发命中')
})

test('Miku 点击入口可由外部拖动层调用，避免依赖 Pixi 声明画布命中区', () => {
  assert.equal(typeof live2dMiku.handleLive2dTap, 'function')
})

test('Miku 首帧更新后重新适配，避免初始化时 drawable 状态尚未就绪导致回退到声明画布', () => {
  assert.equal(typeof live2dMiku.scheduleLive2dFit, 'function')

  const callbacks = new Set()
  const ticker = {
    add(callback) { callbacks.add(callback) },
    remove(callback) { callbacks.delete(callback) }
  }
  const model = {
    internalModel: {
      width: 3500,
      height: 8888,
      coreModel: {
        getDrawableCount() { return 1 },
        getDrawableDynamicFlagIsVisible() { return true },
        getDrawableOpacity() { return 1 }
      },
      getDrawableVertices() {
        return new Float32Array([743, 0, 5158, 0, 5158, 8979, 743, 8979])
      }
    },
    scale: { set() {} },
    anchor: { set() {} },
    x: 0,
    y: 0
  }
  const app = { ticker, screen: { width: 280, height: 420 } }

  live2dMiku.scheduleLive2dFit(model, app, 2)
  assert.equal(callbacks.size, 1)
  callbacks.forEach(callback => callback())
  assert.equal(model.hitArea.contains(5157, 4500), true)
  assert.equal(callbacks.size, 1, '至少等待两次 ticker 更新，确保首帧 drawable 已完成')
  callbacks.forEach(callback => callback())
  assert.equal(callbacks.size, 0)
})

test('运行时把贴图指向降采样副本，且副本文件真实存在', async () => {
  // 模型自带 6 张 4096 贴图共 25.4MB，但看板娘最大只显示 270x430 CSS 像素。
  // 运行时改用 2048 副本（6.4MB），原始 miku.4096/ 必须原样保留。
  const modelUrl = new URL('../../public/live2d/miku/miku.model3.json', import.meta.url)
  const modelConfig = JSON.parse(await readFile(modelUrl, 'utf8'))

  // 磁盘上的 model3.json 不能被改写，仍应指向原始 4096 目录
  assert.ok(
    modelConfig.FileReferences.Textures.every(p => p.startsWith('miku.4096/')),
    '磁盘 model3.json 必须保持指向原始贴图，改写只发生在内存中'
  )

  const rewritten = live2dMiku.useDownscaledTextures(structuredClone(modelConfig))
  const textures = rewritten.FileReferences.Textures
  assert.equal(textures.length, 6)
  assert.ok(textures.every(p => p.startsWith('miku.2048/')), '运行时贴图应指向 2048 副本')

  // 降采样副本必须真实存在，否则模型会加载失败
  await Promise.all(textures.map(p => access(new URL(p, modelUrl))))

  // 原始贴图同时保留（授权约束：不可二改）
  await Promise.all(modelConfig.FileReferences.Textures.map(p => access(new URL(p, modelUrl))))
})

test('支持 WebP 的浏览器把贴图指向 WebP 副本，且副本文件真实存在', async () => {
  const modelUrl = new URL('../../public/live2d/miku/miku.model3.json', import.meta.url)
  const modelConfig = JSON.parse(await readFile(modelUrl, 'utf8'))

  const rewritten = live2dMiku.useDownscaledTextures(structuredClone(modelConfig), { webp: true })
  const textures = rewritten.FileReferences.Textures
  assert.equal(textures.length, 6)
  assert.ok(
    textures.every(p => /^miku\.2048webp\/texture_\d+\.webp$/.test(p)),
    'WebP 模式应指向 miku.2048webp/ 下的 .webp 文件'
  )

  // WebP 副本必须真实存在（由 downscale 脚本 --format webp 生成）
  await Promise.all(textures.map(p => access(new URL(p, modelUrl))))
})

test('不支持 WebP 的环境回退 PNG 副本，探测函数在非浏览器环境不抛错', () => {
  assert.equal(
    live2dMiku.supportsWebpTextures(),
    false,
    '单测环境没有 document，应判定为不支持并走 PNG 回退'
  )

  const config = { FileReferences: { Textures: ['miku.4096/texture_00.png'] } }
  assert.deepEqual(
    live2dMiku.useDownscaledTextures(structuredClone(config), { webp: false }).FileReferences.Textures,
    ['miku.2048/texture_00.png']
  )
  // 非 png 后缀的未知路径不做 WebP 改写，交由 PNG 分支原样/常规处理
  const mixed = live2dMiku.useDownscaledTextures(
    { FileReferences: { Textures: ['miku.4096/texture_00.webp'] } },
    { webp: true }
  )
  assert.deepEqual(mixed.FileReferences.Textures, ['miku.2048/texture_00.webp'])
})

test('贴图路径改写对未知结构保持原样，避免模型换版后静默失败', () => {
  const untouched = live2dMiku.useDownscaledTextures({ FileReferences: { Textures: ['other/tex.png'] } })
  assert.deepEqual(untouched.FileReferences.Textures, ['other/tex.png'])

  // 缺字段时不应抛错
  assert.doesNotThrow(() => live2dMiku.useDownscaledTextures({}))
  assert.doesNotThrow(() => live2dMiku.useDownscaledTextures(null))
})
