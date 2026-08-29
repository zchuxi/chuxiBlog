<template>
  <div class="app-shell__background" :class="{ 'has-image': settings.backgroundImageEnabled, 'has-depth-motion': settings.backgroundImageEnabled }">
    <span
      v-for="(layer, i) in bgLayers"
      :key="i"
      class="app-shell__background-layer"
      :class="{ 'is-active': activeBgLayer === i, 'is-depth-animated': activeBgLayer === i }"
      :style="{ backgroundImage: layer && settings.backgroundImageEnabled ? `linear-gradient(rgba(255, 255, 255, 0.16), rgba(255, 255, 255, 0.3)), url(${JSON.stringify(layer)})` : 'none' }"
    ></span>
    <span class="app-shell__background-glow app-shell__background-glow--left"></span>
    <span class="app-shell__background-glow app-shell__background-glow--right"></span>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'
import { useSettingsStore } from '../../stores/settings'

const settings = useSettingsStore()

const bgLayers = ref([settings.selectedLandscapeImage, ''])
const activeBgLayer = ref(0)
let bgTimer = null

// ── P1-5 背景图预加载：切换前先 new Image() 预热，避免 30s 轮换时白屏闪烁 ──
const imageCache = new Set()
let preloadTimer = null

function preloadImage(url) {
  if (!url || imageCache.has(url)) return
  imageCache.add(url)
  const img = new Image()
  img.decoding = 'async'
  img.src = url
}

function isPortrait() {
  return window.innerHeight > window.innerWidth
}

function currentPool() {
  return isPortrait() ? settings.verticalImages : settings.landscapeImages
}

function swapBackground(img) {
  preloadImage(img)
  const next = activeBgLayer.value === 0 ? 1 : 0
  bgLayers.value[next] = img
  activeBgLayer.value = next
}

function startBgCarousel() {
  stopBgCarousel()
  if (!settings.backgroundCarouselEnabled || !settings.backgroundImageEnabled) return
  bgTimer = setInterval(() => {
    const pool = currentPool()
    const cur = bgLayers.value[activeBgLayer.value]
    let next = pool[Math.floor(Math.random() * pool.length)]
    if (next === cur) next = pool[(pool.indexOf(next) + 1) % pool.length]
    swapBackground(next)
  }, 30000)
}

function stopBgCarousel() {
  if (bgTimer) { clearInterval(bgTimer); bgTimer = null }
}

// 空闲时预加载轮换池，避免首次切图等待网络
function scheduleIdlePreload() {
  clearTimeout(preloadTimer)
  preloadTimer = setTimeout(() => {
    const pool = currentPool()
    for (const img of pool) {
      if (imageCache.size >= 4) break // 首轮只预热 4 张，控制带宽
      preloadImage(img)
    }
  }, 4000)
}

function stopIdlePreload() {
  clearTimeout(preloadTimer)
  preloadTimer = null
}

watch(() => settings.backgroundCarouselEnabled, startBgCarousel)
watch(() => settings.backgroundImageEnabled, startBgCarousel)

// 屏幕方向/尺寸变化（手机旋转、窗口缩放跨横竖屏）时切到对应方向的图池。
// resize 在旋转时也会触发，只挂这一个事件即可覆盖两种场景。
// 当前图仍属于新方向的池子时直接返回，普通窗口缩放不做任何事。
function onViewportChange() {
  const pool = currentPool()
  const cur = bgLayers.value[activeBgLayer.value]
  if (pool.includes(cur)) return
  const img = isPortrait() ? settings.selectedVerticalImage : settings.selectedLandscapeImage
  if (img && img !== cur) swapBackground(img)
  scheduleIdlePreload()
}

onMounted(() => {
  // 首张当前图直接进入缓存标记，避免与轮换池重复预加载
  if (settings.selectedLandscapeImage) imageCache.add(settings.selectedLandscapeImage)
  startBgCarousel()
  scheduleIdlePreload()
  window.addEventListener('resize', onViewportChange)
  settings.loadRemoteGallery().then(() => {
    const img = isPortrait() ? settings.selectedVerticalImage : settings.selectedLandscapeImage
    if (img && bgLayers.value[activeBgLayer.value] !== img) swapBackground(img)
    scheduleIdlePreload()
  })
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onViewportChange)
  stopBgCarousel()
  stopIdlePreload()
})

defineExpose({ swapBackground })
</script>
