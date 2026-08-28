import { createRouter, createWebHistory } from 'vue-router'
import routes from './routes.js'

// 为路由定义绑定懒加载组件（Node 测试环境仅导入 routes.js，不会触发以下 .vue 解析）
const componentMap = {
  home: () => import('../views/HomeView.vue'),
  timeline: () => import('../views/TimelineView.vue'),
  treeHole: () => import('../views/TreeHoleView.vue'),
  parallax: () => import('../views/ParallaxView.vue'),
  archive: () => import('../views/ArchiveView.vue'),
  tool: () => import('../views/ToolView.vue'),
  toolDetail: () => import('../views/ToolDetailView.vue'),
  bangumi: () => import('../views/BangumiView.vue'),
  bangumiDetail: () => import('../views/BangumiDetailView.vue'),
  calendar: () => import('../views/CalendarView.vue'),
  components: () => import('../views/ComponentsShowView.vue'),
  about: () => import('../views/AboutView.vue'),
  article: () => import('../views/ArticleView.vue'),
  admin: () => import('../views/admin/AdminView.vue')
}

const routesWithComponents = routes.map(r =>
  r.name && componentMap[r.name] ? { ...r, component: componentMap[r.name] } : r
)

const router = createRouter({
  history: createWebHistory(),
  routes: routesWithComponents,
  // 页面实际滚动容器是布局里的 .app-shell-main（overflow-y:auto），而不是 window。
  // 浏览器前进/后退时优先恢复保存的滚动位置；普通跳转统一回到容器顶部。
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) return savedPosition
    const main = document.querySelector('.app-shell-main')
    if (main) {
      // 平滑回顶：直接赋值 scrollTop 会让旧页面瞬间跳到顶部，与路由
      // 淡出叠在一起观感生硬；平滑滚动让离开页随滚动一起淡出
      const reduce = typeof window.matchMedia === 'function'
        && window.matchMedia('(prefers-reduced-motion: reduce)').matches
      main.scrollTo({ top: 0, behavior: reduce ? 'auto' : 'smooth' })
    }
    return { top: 0 }
  }
})

// 动态导入（chunk 加载）失败时记录可读日志，并提示用户刷新恢复
router.onError((error, to) => {
  console.error('[Router Error]', {
    message: error?.message,
    to: to?.fullPath,
    error
  })
  const isChunkLoadError = /Failed to fetch dynamically imported module|Importing a module script failed|Loading chunk .* failed/i.test(error?.message || '')
  if (isChunkLoadError && window.confirm('页面资源加载失败，可能是站点已更新。是否刷新页面重试？')) {
    window.location.reload()
  }
})

router.afterEach((to) => {
  // 设置 title
  const title = to.meta.title ? `${to.meta.title} - 初曦の窝` : '初曦の窝'
  document.title = title

  // 设置 meta description
  let meta = document.querySelector('meta[name="description"]')
  if (!meta) {
    meta = document.createElement('meta')
    meta.name = 'description'
    document.head.appendChild(meta)
  }
  meta.content = to.meta.description || '初曦の窝 - 个人博客'

  // 设置 OG 标签
  const setOg = (property, content) => {
    let tag = document.querySelector(`meta[property="${property}"]`)
    if (!tag) {
      tag = document.createElement('meta')
      tag.setAttribute('property', property)
      document.head.appendChild(tag)
    }
    tag.content = content
  }
  setOg('og:title', title)
  setOg('og:description', to.meta.description || '初曦の窝 - 个人博客')
  setOg('og:type', 'website')
  setOg('og:url', window.location.href)
})

export default router
