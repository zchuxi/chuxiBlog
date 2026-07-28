import { defineStore } from 'pinia'

const LANDSCAPE = Array.from({ length: 13 }, (_, i) => `/image/bg/Landscape/${String(i + 1).padStart(2, '0')}.webp`)
const VERTICAL = Array.from({ length: 16 }, (_, i) => `/image/bg/Vertical/${String(i + 1).padStart(2, '0')}.webp`)

const KEY = 'chuxi-nest-settings'

function load() {
  try {
    return JSON.parse(localStorage.getItem(KEY)) || {}
  } catch {
    return {}
  }
}

export const useSettingsStore = defineStore('settings', {
  state: () => ({
    theme: load().theme || 'light',
    backgroundCarouselEnabled: load().backgroundCarouselEnabled !== false,
    sakuraEnabled: load().sakuraEnabled === true,
    live2dEnabled: load().live2dEnabled !== false,
    selectedLandscapeImage: load().selectedLandscapeImage || LANDSCAPE[0],
    selectedVerticalImage: load().selectedVerticalImage || VERTICAL[0]
  }),
  getters: {
    landscapeImages: () => LANDSCAPE,
    verticalImages: () => VERTICAL,
    isDark: state => state.theme === 'dark'
  },
  actions: {
    persist() {
      localStorage.setItem(KEY, JSON.stringify({
        theme: this.theme,
        backgroundCarouselEnabled: this.backgroundCarouselEnabled,
        sakuraEnabled: this.sakuraEnabled,
        live2dEnabled: this.live2dEnabled,
        selectedLandscapeImage: this.selectedLandscapeImage,
        selectedVerticalImage: this.selectedVerticalImage
      }))
    },
    setTheme(theme) {
      this.theme = theme
      document.documentElement.classList.toggle('dark', theme === 'dark')
      this.persist()
    },
    update(patch) {
      Object.assign(this, patch)
      this.persist()
    }
  }
})
