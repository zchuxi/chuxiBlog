<template>
  <main ref="pageRef" class="parallax-page" :style="pageStyle">
    <section class="parallax-scroll-section">
      <div class="parallax-sticky-frame">
        <div class="parallax-stage-hud">
          <div class="parallax-stage-toolbar">
            <div class="parallax-stage-progress">
              <span class="parallax-stage-progress-line"></span>
              <span class="parallax-stage-progress-fill"></span>
            </div>
          </div>
        </div>
        <div ref="viewportsWindowRef" class="parallax-viewports-window">
          <div class="parallax-viewports-track" :style="trackStyle">
            <!-- 开场屏 -->
            <section class="parallax-viewport parallax-viewport-hero" :style="viewportStyle(0)">
              <div class="parallax-viewport-background">
                <img class="parallax-viewport-image" :src="parallaxConfig?.introBg || DEFAULT_PARALLAX.introBg" alt="" />
              </div>
              <div v-if="!isMobile" class="parallax-viewport-curtain">
                <span class="parallax-viewport-curtain-top"></span>
                <span class="parallax-viewport-curtain-bottom"></span>
              </div>
              <div class="parallax-viewport-atmosphere">
                <span v-if="!isMobile" class="parallax-viewport-beam"></span>
                <span class="parallax-viewport-orb parallax-viewport-orb-primary"></span>
                <span class="parallax-viewport-orb parallax-viewport-orb-secondary"></span>
                <span v-if="!isMobile" class="parallax-viewport-grid"></span>
              </div>
              <div v-if="!isMobile" class="parallax-viewport-frame">
                <span class="parallax-viewport-frame-line parallax-viewport-frame-line-top"></span>
                <span class="parallax-viewport-frame-line parallax-viewport-frame-line-bottom"></span>
                <span class="parallax-viewport-frame-highlight"></span>
              </div>
              <div class="parallax-viewport-overlay"></div>
              <div class="parallax-viewport-content">
                <div class="parallax-hero-section" :style="heroStyle">
                  <span class="parallax-hero-section-glow parallax-hero-section-glow-primary"></span>
                  <span class="parallax-hero-section-glow parallax-hero-section-glow-secondary"></span>
                  <div class="parallax-hero-section-panel">
                    <p class="parallax-hero-section-intro">{{ parallaxConfig?.introTitle || DEFAULT_PARALLAX.introTitle }}</p>
                    <div class="parallax-hero-section-main">
                      <span class="parallax-hero-section-badge">Parallax Story</span>
                      <h1 class="parallax-hero-section-title">{{ parallaxConfig?.introSubtitle || DEFAULT_PARALLAX.introSubtitle }}</h1>
                    </div>
                    <div class="parallax-hero-section-footer">
                      <span class="parallax-hero-section-line"></span>
                      <p class="parallax-hero-section-caption">向下滚动，进入每一屏的风景与片段。</p>
                    </div>
                  </div>
                </div>
              </div>
            </section>

            <!-- 故事屏 -->
            <section
              v-for="(s, i) in stories"
              :key="s.id"
              class="parallax-viewport parallax-viewport-story"
              :style="viewportStyle(i + 1)"
            >
              <div class="parallax-viewport-background">
                <img class="parallax-viewport-image" :src="s.imageUrl" alt="" />
              </div>
              <div v-if="!isMobile" class="parallax-viewport-curtain">
                <span class="parallax-viewport-curtain-top"></span>
                <span class="parallax-viewport-curtain-bottom"></span>
              </div>
              <div class="parallax-viewport-atmosphere">
                <span v-if="!isMobile" class="parallax-viewport-beam"></span>
                <span class="parallax-viewport-orb parallax-viewport-orb-primary"></span>
                <span class="parallax-viewport-orb parallax-viewport-orb-secondary"></span>
                <span v-if="!isMobile" class="parallax-viewport-grid"></span>
              </div>
              <div v-if="!isMobile" class="parallax-viewport-frame">
                <span class="parallax-viewport-frame-line parallax-viewport-frame-line-top"></span>
                <span class="parallax-viewport-frame-line parallax-viewport-frame-line-bottom"></span>
                <span class="parallax-viewport-frame-highlight"></span>
              </div>
              <div class="parallax-viewport-overlay"></div>
              <div class="parallax-viewport-content">
                <div
                  class="parallax-story-section"
                  :class="`parallax-story-section-${s.align}`"
                  :style="storyStyle(i + 1)"
                >
                  <article class="parallax-story-section-card">
                    <span class="parallax-story-section-trapezoid"></span>
                    <span class="parallax-story-section-accent-line"></span>
                    <span class="parallax-story-section-index">{{ String(s.id).padStart(2, '0') }}</span>
                    <h2 class="parallax-story-section-title">{{ s.title }}</h2>
                    <p class="parallax-story-section-description">{{ s.description }}</p>
                    <p class="parallax-story-section-note">{{ s.note }}</p>
                  </article>
                </div>
              </div>
            </section>

            <!-- 告别屏 -->
            <section class="parallax-viewport parallax-viewport-farewell" :style="viewportStyle(viewportCount - 1)">
              <div class="parallax-viewport-background">
                <img class="parallax-viewport-image" :src="parallaxConfig?.outroBg || DEFAULT_PARALLAX.outroBg" alt="" />
              </div>
              <div v-if="!isMobile" class="parallax-viewport-curtain">
                <span class="parallax-viewport-curtain-top"></span>
                <span class="parallax-viewport-curtain-bottom"></span>
              </div>
              <div class="parallax-viewport-atmosphere">
                <span v-if="!isMobile" class="parallax-viewport-beam"></span>
                <span class="parallax-viewport-orb parallax-viewport-orb-primary"></span>
                <span class="parallax-viewport-orb parallax-viewport-orb-secondary"></span>
                <span v-if="!isMobile" class="parallax-viewport-grid"></span>
              </div>
              <div v-if="!isMobile" class="parallax-viewport-frame">
                <span class="parallax-viewport-frame-line parallax-viewport-frame-line-top"></span>
                <span class="parallax-viewport-frame-line parallax-viewport-frame-line-bottom"></span>
                <span class="parallax-viewport-frame-highlight"></span>
              </div>
              <div class="parallax-viewport-overlay"></div>
              <div class="parallax-viewport-content">
                <div class="parallax-farewell-section">
                  <div class="parallax-farewell-section-panel">
                    <span class="parallax-farewell-section-tag">{{ parallaxConfig?.outroTitle || DEFAULT_PARALLAX.outroTitle }}</span>
                    <h2 class="parallax-farewell-section-title">{{ parallaxConfig?.outroSubtitle || DEFAULT_PARALLAX.outroSubtitle }}</h2>
                  </div>
                </div>
              </div>
            </section>
          </div>
        </div>
      </div>
    </section>
  </main>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { api } from '../api'
import '../assets/css/parallax.css'

const DEFAULT_PARALLAX = {
  introTitle: 'A Quiet Opening',
  introSubtitle: '在光与影的缝隙间，慢慢展开一段无声的故事。',
  introBg: '/image/bg/Landscape/01.webp',
  outroTitle: 'Until Next Time',
  outroSubtitle: '愿你带着温柔的光，继续前行。',
  outroBg: '/image/bg/Landscape/12.webp'
}
const parallaxConfig = ref(null)

const pageRef = ref(null)
const viewportsWindowRef = ref(null)

// 滚动状态：progress 为整段滚动的 0~1 进度，momentum 为最近一帧的进度增量放大后截断到 [-1,1]
const progress = ref(0)
const momentum = ref(0)
const windowInnerHeight = ref(0)   // 视口窗（.parallax-viewports-window）高度
const containerHeight = ref(0)     // 滚动容器可视高度
const viewportWidth = ref(typeof window === 'undefined' ? 1280 : window.innerWidth)
const viewportHeight = ref(typeof window === 'undefined' ? 720 : window.innerHeight)
const pixelRatio = ref(typeof window === 'undefined' ? 1 : window.devicePixelRatio || 1)

// 风景图列表（public 下 01~13），无图 story 按「(索引+1) % 13」循环回退
const LANDSCAPE_IMAGES = Array.from({ length: 13 }, (_, i) => `/image/bg/Landscape/${String(i + 1).padStart(2, '0')}.webp`)
const fallbackImage = i => LANDSCAPE_IMAGES[Math.max(i + 1, 0) % LANDSCAPE_IMAGES.length] ?? LANDSCAPE_IMAGES[0]
const normalizeAlign = a => (a === 'left' || a === 'bottom' || a === 'right' ? a : 'left')

const stories = ref([])
const viewportCount = computed(() => stories.value.length + 2)

// 低性能/小屏场景整体缩小动效幅度
const motionScale = computed(() => {
  const smallHeight = viewportHeight.value <= 900
  const highDpr = pixelRatio.value >= 1.5
  return highDpr && smallHeight
    ? 0.3
    : viewportWidth.value <= 640
      ? 0.42
      : viewportWidth.value <= 860
        ? 0.68
        : highDpr && viewportHeight.value <= 1080
          ? 0.56
          : 1
})
const motionResistance = computed(() => 0.4 + motionScale.value * 0.6)
const isMobile = computed(() => viewportWidth.value <= 640)
// 减弱动效偏好：视差位移由 JS 写入行内样式，CSS 的 prefers-reduced-motion 块管不到，需在此拦掉
const reduceMotion = ref(false)
const depthScale = computed(() => (motionScale.value < 0.4 ? 0.78 : motionScale.value < 0.7 ? 0.88 : 1))
const transitionDuration = computed(() => (isMobile.value || motionScale.value < 0.6 ? '0ms' : '180ms'))

const pageStyle = computed(() => ({
  '--parallax-container-height': `${containerHeight.value || viewportHeight.value}px`,
  '--parallax-viewport-count': `${viewportCount.value}`,
  '--parallax-scroll-progress': `${progress.value}`,
  '--parallax-scroll-momentum': `${momentum.value}`,
  '--parallax-transition-duration': transitionDuration.value
}))

const trackStyle = computed(() => ({
  transform: `translate3d(0, -${(viewportCount.value - 1) * windowInnerHeight.value * progress.value}px, 0)`
}))

// 每屏基础度量：positionDelta 为该屏相对当前焦点的偏移（单位=屏）
function metrics(i) {
  const positionDelta = i - progress.value * (viewportCount.value - 1)
  const focusProgress = Math.max(0, 1 - Math.min(Math.abs(positionDelta), 1.2) / 1.2)
  const depthProgress = Math.min(Math.abs(positionDelta), 1.6) / 1.6
  const drift = Math.max(-36, Math.min(36, positionDelta * -18 + momentum.value * 28))
  return { positionDelta, focusProgress, depthProgress, drift }
}

/** 减弱动效时的静态取值：位移/缩放全部归零，仅留一层可读的遮罩 */
const STATIC_VIEWPORT_STYLE = {
  '--parallax-bg-transform': 'none',
  '--parallax-content-transform': 'none',
  '--parallax-overlay-opacity': '0.28',
  '--parallax-glow-opacity': '0',
  '--parallax-glow-scale': '1',
  '--parallax-beam-offset': '0%',
  '--parallax-beam-opacity': '0',
  '--parallax-orbit-transform': 'none',
  '--parallax-grid-opacity': '0',
  '--parallax-content-opacity': '1',
  '--parallax-curtain-offset': '0%',
  '--parallax-curtain-opacity': '0',
  '--parallax-frame-opacity': '0',
  '--parallax-frame-shift': '0px',
  '--parallax-highlight-opacity': '0',
  '--parallax-drift': '0px'
}

function viewportStyle(i) {
  if (reduceMotion.value) return STATIC_VIEWPORT_STYLE
  const { positionDelta: t, focusProgress: l, depthProgress: o, drift } = metrics(i)
  if (isMobile.value) {
    // 移动端：只用 transform/opacity（走合成器，不触发重排/重绘），
    // 位移幅度比桌面收窄但足以形成纵深；大面积叠加层（beam/grid/curtain/frame）仍关闭。
    return {
      '--parallax-bg-transform': `translate3d(0, ${t * -9}%, 0) scale(${1.05 + l * 0.07})`,
      '--parallax-content-transform': `translate3d(0, ${t * -26}px, 0) scale(${0.965 + l * 0.035})`,
      '--parallax-overlay-opacity': `${Math.min(0.56, 0.16 + o * 0.34)}`,
      // 两个 orb 为 radial-gradient + transform/opacity，合成成本低，移动端保留（透明度降一档）
      '--parallax-glow-opacity': `${0.08 + l * 0.3}`,
      '--parallax-glow-scale': `${0.94 + l * 0.16}`,
      '--parallax-orbit-transform': `translate3d(${t * 7}%, ${t * -5}%, 0)`,
      '--parallax-beam-offset': '0%',
      '--parallax-beam-opacity': '0',
      '--parallax-grid-opacity': '0',
      '--parallax-content-opacity': `${0.5 + l * 0.5}`,
      '--parallax-curtain-offset': '0%',
      '--parallax-curtain-opacity': '0',
      '--parallax-frame-opacity': '0',
      '--parallax-frame-shift': '0px',
      '--parallax-highlight-opacity': '0',
      '--parallax-drift': '0px'
    }
  }
  const w = motionScale.value
  const f = depthScale.value
  const r = momentum.value
  return {
    '--parallax-bg-transform': `translate3d(0, ${(t * -11 + r * 3.8) * w}%, 0) scale(${1.16 + (1.22 - l * 0.08 - 1.16) * f})`,
    '--parallax-content-transform': `translate3d(0, ${(t * -34 + r * 20) * w}px, 0) scale(${0.98 + (0.92 + l * 0.08 - 0.98) * f})`,
    '--parallax-overlay-opacity': `${Math.min(0.64, 0.18 + o * 0.42)}`,
    '--parallax-glow-opacity': `${0.12 + l * 0.56}`,
    '--parallax-glow-scale': `${0.96 + (0.9 + l * 0.28 - 0.96) * f}`,
    '--parallax-beam-offset': `${(t * -18 + r * 28) * w}%`,
    '--parallax-beam-opacity': `${0.16 + l * 0.44}`,
    '--parallax-orbit-transform': `translate3d(${(t * 10 - r * 18) * w}%, ${(t * -7 + r * 16) * w}%, 0)`,
    '--parallax-grid-opacity': `${0.05 + l * 0.13}`,
    '--parallax-content-opacity': `${0.42 + l * 0.58}`,
    '--parallax-curtain-offset': `${16 + o * 34 * w}%`,
    '--parallax-curtain-opacity': `${0.12 + o * 0.5}`,
    '--parallax-frame-opacity': `${0.16 + l * 0.62}`,
    '--parallax-frame-shift': `${(t * 24 - r * 18) * w}px`,
    '--parallax-highlight-opacity': `${0.1 + l * 0.4}`,
    '--parallax-drift': `${drift}px`
  }
}

// 内容区动效强度与漂移量（移动端固定接近 1 / 不漂移）
function motionLevel(i) {
  if (reduceMotion.value) return 1
  const { focusProgress } = metrics(i)
  // 移动端区间由 0.82~1 放宽到 0.62~1：卡片升起量随之从约 9px 增到约 20px，进入焦点时纵深可感
  return isMobile.value ? 0.62 + focusProgress * 0.38 : 1 - (1 - focusProgress) * motionResistance.value
}
function driftFor(i) {
  if (reduceMotion.value) return 0
  return isMobile.value ? 0 : metrics(i).drift * motionScale.value
}

const heroStyle = computed(() => {
  const m = motionLevel(0)
  const d = driftFor(0)
  return {
    '--parallax-hero-motion-level': `${m}`,
    '--parallax-hero-drift': `${d}px`,
    '--parallax-hero-panel-shift': `${(1 - m) * 48 - d * 0.18}px`,
    '--parallax-hero-panel-scale': `${0.94 + m * 0.06}`,
    '--parallax-hero-glow-opacity': `${0.18 + m * 0.4}`,
    '--parallax-hero-copy-opacity': `${0.56 + m * 0.44}`
  }
})

function storyStyle(i) {
  const m = motionLevel(i)
  const d = driftFor(i)
  return {
    '--parallax-story-motion-level': `${m}`,
    '--parallax-story-drift': `${d}px`,
    '--parallax-story-card-shift': `${(1 - m) * 52}px`,
    '--parallax-story-card-scale': `${0.94 + m * 0.06}`,
    '--parallax-story-card-opacity': `${0.58 + m * 0.42}`,
    '--parallax-story-card-border-opacity': `${0.2 + m * 0.28}`,
    '--parallax-story-trapezoid-opacity': `${0.14 + m * 0.42}`,
    '--parallax-story-trapezoid-shift': `${d * -0.32}px`,
    '--parallax-story-line-scale': `${0.35 + m * 0.65}`
  }
}

async function loadStories() {
  try {
    const list = await api.parallaxStories() || []
    stories.value = list.map((t, i) => ({
      id: t.id,
      title: t.title || `Scene ${i + 1}`,
      description: t.description || '',
      note: t.note || '',
      align: normalizeAlign(t.align),
      imageUrl: (t.imageUrl || '').trim() || fallbackImage(i),
      sortIndex: t.sortIndex
    }))
  } catch (e) { console.warn('[视差] 加载失败:', e) }
}

// —— 滚动/尺寸测量（rAF 节流，无滚轮接管、无吸附） ——
let scroller = null
let rafId = 0
let disposed = false

function findScrollParent(el) {
  let node = el && el.parentElement
  while (node) {
    const style = getComputedStyle(node)
    if (/(auto|scroll)/.test(style.overflowY)) return node
    node = node.parentElement
  }
  return null
}

function measure() {
  const page = pageRef.value
  const win = viewportsWindowRef.value
  if (!page || !win) return
  const clientHeight = scroller instanceof HTMLElement ? scroller.clientHeight : window.innerHeight
  const rect = page.getBoundingClientRect()
  const total = Math.max(rect.height - clientHeight, 1)
  const next = Math.min(Math.max(-rect.top, 0), total) / total
  const delta = next - progress.value
  containerHeight.value = clientHeight
  windowInnerHeight.value = win.clientHeight
  viewportWidth.value = window.innerWidth
  viewportHeight.value = window.innerHeight
  pixelRatio.value = window.devicePixelRatio || 1
  momentum.value = Math.max(-1, Math.min(1, delta * 20))
  progress.value = next
}

function schedule() {
  if (rafId) window.cancelAnimationFrame(rafId)
  rafId = window.requestAnimationFrame(() => {
    measure()
    rafId = 0
  })
}

let motionQuery = null
function onMotionPrefChange(e) {
  reduceMotion.value = e.matches
}

onMounted(async () => {
  // 监听 prefers-reduced-motion：用户中途改系统设置也能即时生效
  if (typeof window.matchMedia === 'function') {
    motionQuery = window.matchMedia('(prefers-reduced-motion: reduce)')
    reduceMotion.value = motionQuery.matches
    motionQuery.addEventListener('change', onMotionPrefChange)
  }
  await loadStories()
  try {
    parallaxConfig.value = await api.siteContent('parallax-config')
  } catch (e) { console.warn('[视差] 配置加载失败:', e) }
  await nextTick()
  // 等待期间组件可能已被卸载（快速切路由）：不再挂监听，避免监听器泄漏
  if (disposed) return
  scroller = findScrollParent(pageRef.value)
  if (scroller instanceof HTMLElement) {
    scroller.addEventListener('scroll', schedule, { passive: true })
  } else {
    scroller = window
    window.addEventListener('scroll', schedule, { passive: true })
  }
  window.addEventListener('resize', schedule)
  schedule()
})

onBeforeUnmount(() => {
  disposed = true
  if (motionQuery) motionQuery.removeEventListener('change', onMotionPrefChange)
  if (rafId) window.cancelAnimationFrame(rafId)
  if (scroller instanceof HTMLElement) {
    scroller.removeEventListener('scroll', schedule)
  } else {
    window.removeEventListener('scroll', schedule)
  }
  window.removeEventListener('resize', schedule)
})
</script>
