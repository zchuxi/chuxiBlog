import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/index' },
    { path: '/index', name: 'home', component: () => import('../views/HomeView.vue'), meta: { title: '首页', description: '初曦の巢 - 个人博客首页' } },
    { path: '/timeline', name: 'timeline', component: () => import('../views/TimelineView.vue'), meta: { title: '时间线', description: '按时间顺序浏览所有文章' } },
    { path: '/tree-hole', name: 'treeHole', component: () => import('../views/TreeHoleView.vue'), meta: { title: '树洞', description: '说说你的想法，留下你的回声' } },
    { path: '/parallax', name: 'parallax', component: () => import('../views/ParallaxView.vue'), meta: { title: '视差', description: '视差滚动效果展示' } },
    { path: '/archive', name: 'archive', component: () => import('../views/ArchiveView.vue'), meta: { title: '归档', description: '文章归档，按分类和标签浏览' } },
    { path: '/tool', name: 'tool', component: () => import('../views/ToolView.vue'), meta: { title: '工具', description: '实用工具集合' } },
    { path: '/tool/:id', name: 'toolDetail', component: () => import('../views/ToolDetailView.vue'), meta: { title: '工具详情', description: '工具详情页面' } },
    { path: '/bangumi', name: 'bangumi', component: () => import('../views/BangumiView.vue'), meta: { title: '番剧', description: '我的番剧收藏与追番列表' } },
    { path: '/bangumi/:id', name: 'bangumiDetail', component: () => import('../views/BangumiDetailView.vue'), meta: { title: '番剧详情', description: '番剧详情页面' } },
    { path: '/calendar', name: 'calendar', component: () => import('../views/CalendarView.vue'), meta: { title: '日历', description: '番剧更新日历' } },
    { path: '/components', name: 'components', component: () => import('../views/ComponentsShowView.vue'), meta: { title: '组件展示', description: 'UI 组件展示页面' } },
    { path: '/about', name: 'about', component: () => import('../views/AboutView.vue'), meta: { title: '关于', description: '关于初曦の巢和站主' } },
    { path: '/article/:id', name: 'article', component: () => import('../views/ArticleView.vue'), meta: { title: '文章', description: '文章详情' } },
    { path: '/admin', name: 'admin', component: () => import('../views/admin/AdminView.vue'), meta: { title: '管理后台' } }
  ],
  scrollBehavior() {
    return { top: 0 }
  }
})

router.afterEach((to) => {
  // 设置 title
  const title = to.meta.title ? `${to.meta.title} - 初曦の巢` : '初曦の巢'
  document.title = title

  // 设置 meta description
  let meta = document.querySelector('meta[name="description"]')
  if (!meta) {
    meta = document.createElement('meta')
    meta.name = 'description'
    document.head.appendChild(meta)
  }
  meta.content = to.meta.description || '初曦の巢 - 个人博客'

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
  setOg('og:description', to.meta.description || '初曦の巢 - 个人博客')
  setOg('og:type', 'website')
  setOg('og:url', window.location.href)
})

export default router
