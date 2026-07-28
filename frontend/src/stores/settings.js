import { defineStore } from 'pinia'
import { api } from '../api'

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

export const useSettingsStore = defineStore('settings', {
  state: () => ({
    theme: load().theme || 'light',
    // 背景模式：关闭后不渲染背景图，只保留纯色底
    backgroundImageEnabled: load().backgroundImageEnabled !== false,
    backgroundCarouselEnabled: load().backgroundCarouselEnabled !== false,
    sakuraEnabled: load().sakuraEnabled === true,
    live2dEnabled: load().live2dEnabled !== false,
    selectedLandscapeImage: load().selectedLandscapeImage || DEFAULT_LANDSCAPE[0],
    selectedVerticalImage: load().selectedVerticalImage || DEFAULT_VERTICAL[0],
    // 背景图库：可被后台「背景图库」配置（site-content: background-gallery）覆盖
    galleryLandscape: DEFAULT_LANDSCAPE.slice(),
    galleryVertical: DEFAULT_VERTICAL.slice()
  }),
  getters: {
    landscapeImages: state => state.galleryLandscape,
    verticalImages: state => state.galleryVertical,
    isDark: state => state.theme === 'dark'
  },
  actions: {
    persist() {
      localStorage.setItem(KEY, JSON.stringify({
        theme: this.theme,
        backgroundImageEnabled: this.backgroundImageEnabled,
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
    },
    // 拉取后台配置的背景图库；无记录/请求失败时静默保留内置默认
    async loadRemoteGallery() {
      try {
        const data = await api.siteContent('background-gallery')
        let obj = data
        if (data && typeof data.contentJson === 'string') obj = JSON.parse(data.contentJson)
        if (typeof obj === 'string') obj = JSON.parse(obj)
        if (Array.isArray(obj?.landscape) && obj.landscape.length > 0) this.galleryLandscape = obj.landscape
        if (Array.isArray(obj?.vertical) && obj.vertical.length > 0) this.galleryVertical = obj.vertical
        // 记忆的选中图已被后台移除时，落回图库第一张
        if (!this.galleryLandscape.includes(this.selectedLandscapeImage)) {
          this.selectedLandscapeImage = this.galleryLandscape[0]
          this.persist()
        }
        if (!this.galleryVertical.includes(this.selectedVerticalImage)) {
          this.selectedVerticalImage = this.galleryVertical[0]
          this.persist()
        }
      } catch {
        /* 未配置时使用内置默认图库 */
      }
    }
  }
})
