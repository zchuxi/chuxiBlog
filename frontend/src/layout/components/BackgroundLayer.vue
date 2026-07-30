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

function isPortrait() {
  return window.innerHeight > window.innerWidth
}

function currentPool() {
  return isPortrait() ? settings.verticalImages : settings.landscapeImages
}

function swapBackground(img) {
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

watch(() => settings.backgroundCarouselEnabled, startBgCarousel)
watch(() => settings.backgroundImageEnabled, startBgCarousel)

onMounted(() => {
  startBgCarousel()
  settings.loadRemoteGallery().then(() => {
    const img = isPortrait() ? settings.selectedVerticalImage : settings.selectedLandscapeImage
    if (img && bgLayers.value[activeBgLayer.value] !== img) swapBackground(img)
  })
})

onBeforeUnmount(() => {
  stopBgCarousel()
})

defineExpose({ swapBackground })
</script>
