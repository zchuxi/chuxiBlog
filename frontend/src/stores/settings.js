import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { api } from '../api/index.js'

// 内置默认图库（后台未配置「背景图库」时的兜底）
export const DEFAULT_LANDSCAPE = Array.from({ length: 13 }, (_, i) => `/image/bg/Landscape/${String(i + 1).padStart(2, '0')}.webp`)
export const DEFAULT_VERTICAL = Array.from({ length: 16 }, (_, i) => `/image/bg/Vertical/${String(i + 1).padStart(2, '0')}.webp`)

const KEY = 'chuxi-nest-settings'

function load() {
  try {
    return JSON.parse(localStorage.getItem(KEY)) || {}
  } catch {
    return {}
  }
}

export const useSettingsStore = defineStore('settings', () => {
  const saved = load()
  const theme = ref(saved.theme || 'light')
  // 背景模式：关闭后不渲染背景图，只保留纯色底
  const backgroundImageEnabled = ref(saved.backgroundImageEnabled !== false)
  const backgroundCarouselEnabled = ref(saved.backgroundCarouselEnabled !== false)
  const sakuraEnabled = ref(saved.sakuraEnabled === true)
  const live2dEnabled = ref(saved.live2dEnabled !== false)
  const selectedLandscapeImage = ref(saved.selectedLandscapeImage || DEFAULT_LANDSCAPE[0])
  const selectedVerticalImage = ref(saved.selectedVerticalImage || DEFAULT_VERTICAL[0])
  // 背景图库：可被后台「背景图库」配置（site-content: background-gallery）覆盖
  const galleryLandscape = ref(DEFAULT_LANDSCAPE.slice())
  const galleryVertical = ref(DEFAULT_VERTICAL.slice())

  const landscapeImages = computed(() => galleryLandscape.value)
  const verticalImages = computed(() => galleryVertical.value)
  const isDark = computed(() => theme.value === 'dark')

  function persist() {
    localStorage.setItem(KEY, JSON.stringify({
      theme: theme.value,
      backgroundImageEnabled: backgroundImageEnabled.value,
      backgroundCarouselEnabled: backgroundCarouselEnabled.value,
      sakuraEnabled: sakuraEnabled.value,
      live2dEnabled: live2dEnabled.value,
      selectedLandscapeImage: selectedLandscapeImage.value,
      selectedVerticalImage: selectedVerticalImage.value
    }))
  }

  function setTheme(val) {
    theme.value = val
    document.documentElement.classList.toggle('dark', val === 'dark')
    persist()
  }

  function update(patch) {
    const map = { theme, backgroundImageEnabled, backgroundCarouselEnabled, sakuraEnabled, live2dEnabled, selectedLandscapeImage, selectedVerticalImage }
    for (const [key, val] of Object.entries(patch)) {
      if (map[key]) map[key].value = val
    }
    persist()
  }

  // 拉取后台配置的背景图库；无记录/请求失败时静默保留内置默认
  async function loadRemoteGallery() {
    try {
      const data = await api.siteContent('background-gallery')
      let obj = data
      if (data && typeof data.contentJson === 'string') obj = JSON.parse(data.contentJson)
      if (typeof obj === 'string') obj = JSON.parse(obj)
      if (Array.isArray(obj?.landscape) && obj.landscape.length > 0) galleryLandscape.value = obj.landscape
      if (Array.isArray(obj?.vertical) && obj.vertical.length > 0) galleryVertical.value = obj.vertical
      // 记忆的选中图已被后台移除时，落回图库第一张
      if (!galleryLandscape.value.includes(selectedLandscapeImage.value)) {
        selectedLandscapeImage.value = galleryLandscape.value[0]
        persist()
      }
      if (!galleryVertical.value.includes(selectedVerticalImage.value)) {
        selectedVerticalImage.value = galleryVertical.value[0]
        persist()
      }
    } catch (e) {
      console.warn('[设置] 图库配置解析失败:', e)
      /* 未配置时使用内置默认图库 */
    }
  }

  return {
    theme, backgroundImageEnabled, backgroundCarouselEnabled,
    sakuraEnabled, live2dEnabled, selectedLandscapeImage, selectedVerticalImage,
    galleryLandscape, galleryVertical,
    landscapeImages, verticalImages, isDark,
    persist, setTheme, update, loadRemoteGallery
  }
})
