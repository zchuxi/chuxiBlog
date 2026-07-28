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
                <img class="parallax-viewport-image" src="/image/bg/Landscape/01.webp" alt="" />
              </div>
              <div v-if="!isMobile" class="parallax-viewport-curtain">
                <span class="parallax-viewport-curtain-top"></span>
                <span class="parallax-viewport-curtain-bottom"></span>
              </div>
              <div v-if="!isMobile" class="parallax-viewport-atmosphere">
                <span class="parallax-viewport-beam"></span>
                <span class="parallax-viewport-orb parallax-viewport-orb-primary"></span>
                <span class="parallax-viewport-orb parallax-viewport-orb-secondary"></span>
                <span class="parallax-viewport-grid"></span>
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
                    <p class="parallax-hero-section-intro">A Quiet Opening</p>
                    <div class="parallax-hero-section-main">
                      <span class="parallax-hero-section-badge">Parallax Story</span>
                      <h1 class="parallax-hero-section-title">给认真生活的你，一段缓慢展开的风景。</h1>
                      <p class="parallax-hero-section-welcome">欢迎来到这组视差页面。每次滚动都会翻过一层山色，也把一句轻一点的话留在你身边。</p>
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
              <div v-if="!isMobile" class="parallax-viewport-atmosphere">
                <span class="parallax-viewport-beam"></span>
                <span class="parallax-viewport-orb parallax-viewport-orb-primary"></span>
                <span class="parallax-viewport-orb parallax-viewport-orb-secondary"></span>
                <span class="parallax-viewport-grid"></span>
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
                <img class="parallax-viewport-image" src="/image/bg/Landscape/12.webp" alt="" />
              </div>
              <div v-if="!isMobile" class="parallax-viewport-curtain">
                <span class="parallax-viewport-curtain-top"></span>
                <span class="parallax-viewport-curtain-bottom"></span>
              </div>
              <div v-if="!isMobile" class="parallax-viewport-atmosphere">
                <span class="parallax-viewport-beam"></span>
                <span class="parallax-viewport-orb parallax-viewport-orb-primary"></span>
                <span class="parallax-viewport-orb parallax-viewport-orb-secondary"></span>
                <span class="parallax-viewport-grid"></span>
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
                    <span class="parallax-farewell-section-tag">Until Next Time</span>
                    <h2 class="parallax-farewell-section-title">愿你接下来的路，仍有风景，也总有人回应。</h2>
                    <p class="parallax-farewell-section-blessing">谢谢你把这一程看完。愿接下来的日常里，既有被理解的瞬间，也有自己照亮自己的勇气。</p>
                    <p class="parallax-farewell-section-contact">如果还想继续聊聊、继续联系、继续分享新的故事，我们就在下一次相遇里见。</p>
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

function viewportStyle(i) {
  const { positionDelta: t, focusProgress: l, depthProgress: o, drift } = metrics(i)
  if (isMobile.value) {
    // 移动端只保留轻量位移，装饰层全部关闭
    return {
      '--parallax-bg-transform': `translate3d(0, ${t * -4}%, 0) scale(${1.04 + l * 0.04})`,
      '--parallax-content-transform': `translate3d(0, ${t * -16}px, 0) scale(${0.98 + l * 0.02})`,
      '--parallax-overlay-opacity': `${Math.min(0.52, 0.2 + o * 0.24)}`,
      '--parallax-glow-opacity': '0',
      '--parallax-glow-scale': '1',
      '--parallax-beam-offset': '0%',
      '--parallax-beam-opacity': '0',
      '--parallax-orbit-transform': 'translate3d(0, 0, 0)',
      '--parallax-grid-opacity': '0',
      '--parallax-content-opacity': `${0.72 + l * 0.28}`,
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
  const { focusProgress } = metrics(i)
  return isMobile.value ? 0.82 + focusProgress * 0.18 : 1 - (1 - focusProgress) * motionResistance.value
}
function driftFor(i) {
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
  } catch { /* 后端未启动时保持空列表 */ }
}

// —— 滚动/尺寸测量（rAF 节流，无滚轮接管、无吸附） ——
let scroller = null
let rafId = 0

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

onMounted(async () => {
  await loadStories()
  await nextTick()
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
  if (rafId) window.cancelAnimationFrame(rafId)
  if (scroller instanceof HTMLElement) {
    scroller.removeEventListener('scroll', schedule)
  } else {
    window.removeEventListener('scroll', schedule)
  }
  window.removeEventListener('resize', schedule)
})
</script>
