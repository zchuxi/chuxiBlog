<template>
  <span ref="trackRef" class="scroll-grow-track" :style="{ height: `${height}px` }">
    <svg
      class="scroll-grow-line"
      :viewBox="`0 0 ${VIEW_W} ${VIEW_H}`"
      preserveAspectRatio="none"
      role="presentation"
      aria-hidden="true"
    >
      <rect
        class="scroll-grow-line__bar"
        :x="rectX"
        :y="0"
        :width="rectWidth"
        :height="VIEW_H"
        :rx="VIEW_H / 2"
        :ry="VIEW_H / 2"
      />
    </svg>
  </span>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

/**
 * 随页面下滑从短变长 → 触底后固定最大长度的进度条。
 * 使用 SVG <rect> 渲染，靠 window scrollY / 最大可滚动距离 驱动，
 * 进度到 1 后立刻稳定在最大宽度（不会继续增长也不会回退）。
 *
 * 父级可放任意文字/图标标签；本组件只占用进度条自身的水平空间。
 */

const props = defineProps({
  // 最小（未滚动）宽度（px）
  minWidth: { type: Number, default: 60 },
  // 最大（触底）宽度（px）
  maxWidth: { type: Number, default: 320 },
  // 条形高度（px）
  height: { type: Number, default: 6 }
})

const VIEW_W = 1000
const VIEW_H = 6
const trackRef = ref(null)
const progress = ref(0) // 0..1
let scroller = null // 实际滚动容器：布局里的 .app-shell-main（本站点 window 不滚动）

function findScroller() {
  return document.querySelector('.app-shell-main') || document.scrollingElement || document.documentElement
}

function updateProgress() {
  const el = scroller || findScroller()
  const max = Math.max(1, el.scrollHeight - el.clientHeight)
  progress.value = Math.min(1, Math.max(0, el.scrollTop / max))
}

let rafId = 0
function onScroll() {
  if (rafId) return
  rafId = requestAnimationFrame(() => { rafId = 0; updateProgress() })
}

function onResize() {
  scroller = findScroller()
  onScroll()
}

onMounted(() => {
  scroller = findScroller()
  updateProgress()
  scroller.addEventListener('scroll', onScroll, { passive: true })
  window.addEventListener('resize', onResize, { passive: true })
})

onBeforeUnmount(() => {
  if (scroller) scroller.removeEventListener('scroll', onScroll)
  window.removeEventListener('resize', onResize)
  if (rafId) cancelAnimationFrame(rafId)
})

// 用 viewBox 0..VIEW_W 渲染，等比缩放到 [minWidth, maxWidth]
const rectWidth = computed(() => {
  const w = props.minWidth + (props.maxWidth - props.minWidth) * progress.value
  // 映射到 viewBox 坐标系（VIEW_W px → 实际 w px 渲染宽度，等比）
  return (w / props.maxWidth) * VIEW_W
})
// 居中：用 x 让 rect 在 [0..VIEW_W] 居中（条形随宽度从中心向两侧展开）
const rectX = computed(() => (VIEW_W - rectWidth.value) / 2)
</script>

<style scoped>
.scroll-grow-track {
  display: inline-block;
  position: relative;
  width: 100%;
  vertical-align: middle;
}
.scroll-grow-line {
  display: block;
  width: 100%;
  height: 100%;
  overflow: visible;
}
.scroll-grow-line__bar {
  fill: currentColor;
  color: var(--accent-solid, #7cd6c0);
  transition: fill 0.3s ease;
}
</style>