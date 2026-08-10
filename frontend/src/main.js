import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import reveal from './directives/reveal'
import { injectSvgSprite } from './utils/svgSprite'

// 全局样式（基础 -> 组件库 -> 布局 -> 通用）
import './assets/css/base.css'
import './assets/css/cx-button.css'
import './assets/css/cx-input.css'
import './assets/css/cx-tag.css'
import './assets/css/cx-switch.css'
import './assets/css/cx-popover.css'
import './assets/css/cx-date-picker.css'
import './assets/css/cx-section.css'
import './assets/css/layout.css'
import './assets/css/hljs-atom-one-dark.css'

injectSvgSprite()

// ── 全局错误监控 ──────────────────────────────────

// 全局未捕获异常
window.addEventListener('error', (event) => {
  console.error('[Global Error]', {
    message: event.message,
    source: event.filename,
    line: event.lineno,
    column: event.colno,
    error: event.error
  })
})

// 未处理的 Promise 拒绝
window.addEventListener('unhandledrejection', (event) => {
  console.error('[Unhandled Rejection]', {
    reason: event.reason,
    promise: event.promise
  })
})

// ── 应用启动 ──────────────────────────────────────
const app = createApp(App)
app.use(createPinia())
app.use(router)
app.directive('reveal', reveal)
app.mount('#app')
