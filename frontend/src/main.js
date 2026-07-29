import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import reveal from './directives/reveal'
import { injectSvgSprite } from './utils/svgSprite'

// 全局样式（顺序：基础 -> 组件库 -> 布局 -> 各视图）
import './assets/css/base.css'
import './assets/css/cx-button.css'
import './assets/css/cx-input.css'
import './assets/css/cx-tag.css'
import './assets/css/cx-switch.css'
import './assets/css/cx-popover.css'
import './assets/css/cx-section.css'
import './assets/css/layout.css'
import './assets/css/home.css'
import './assets/css/article.css'
import './assets/css/timeline.css'
import './assets/css/archive.css'
import './assets/css/tree-hole.css'
import './assets/css/parallax.css'
import './assets/css/tool.css'
import './assets/css/components-show.css'
import './assets/css/preview.css'
import './assets/css/hljs-atom-one-dark.css'

injectSvgSprite()

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.directive('reveal', reveal)
app.mount('#app')
