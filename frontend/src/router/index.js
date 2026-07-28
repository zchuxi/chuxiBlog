import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/index' },
    { path: '/index', name: 'home', component: () => import('../views/HomeView.vue') },
    { path: '/timeline', name: 'timeline', component: () => import('../views/TimelineView.vue') },
    { path: '/tree-hole', name: 'treeHole', component: () => import('../views/TreeHoleView.vue') },
    { path: '/parallax', name: 'parallax', component: () => import('../views/ParallaxView.vue') },
    { path: '/archive', name: 'archive', component: () => import('../views/ArchiveView.vue') },
    { path: '/tool', name: 'tool', component: () => import('../views/ToolView.vue') },
    { path: '/tool/:id', name: 'toolDetail', component: () => import('../views/ToolDetailView.vue') },
    { path: '/bangumi', name: 'bangumi', component: () => import('../views/BangumiView.vue') },
    { path: '/bangumi/:id', name: 'bangumiDetail', component: () => import('../views/BangumiDetailView.vue') },
    { path: '/components', name: 'components', component: () => import('../views/ComponentsShowView.vue') },
    { path: '/about', name: 'about', component: () => import('../views/AboutView.vue') },
    { path: '/article/:id', name: 'article', component: () => import('../views/ArticleView.vue') },
    { path: '/admin', name: 'admin', component: () => import('../views/admin/AdminView.vue') }
  ],
  scrollBehavior() {
    return { top: 0 }
  }
})

export default router
