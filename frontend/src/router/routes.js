/**
 * 路由定义（纯数据，无 .vue 组件引用，可在 Node 测试环境直接导入）。
 * index.js 负责为此处的每条路由绑定懒加载组件。
 */

export default [
  { path: '/', redirect: '/index' },
  { path: '/index', name: 'home', meta: { title: '首页', description: '初曦の窝 - 个人博客首页' } },
  { path: '/timeline', name: 'timeline', meta: { title: '时间线', description: '按时间顺序浏览所有文章' } },
  { path: '/tree-hole', name: 'treeHole', meta: { title: '树洞', description: '说说你的想法，留下你的回声' } },
  { path: '/parallax', name: 'parallax', meta: { title: '视差', description: '视差滚动效果展示' } },
  { path: '/archive', name: 'archive', meta: { title: '归档', description: '文章归档，按分类和标签浏览' } },
  { path: '/tool', name: 'tool', meta: { title: '工具', description: '实用工具集合' } },
  { path: '/tool/:id', name: 'toolDetail', meta: { title: '工具详情', description: '工具详情页面' } },
  { path: '/bangumi', name: 'bangumi', meta: { title: '番剧', description: '我的番剧收藏与追番列表' } },
  { path: '/bangumi/:id', name: 'bangumiDetail', meta: { title: '番剧详情', description: '番剧详情页面' } },
  { path: '/calendar', name: 'calendar', meta: { title: '日历', description: '番剧更新日历' } },
  { path: '/components', name: 'components', meta: { title: '组件展示', description: 'UI 组件展示页面' } },
  { path: '/about', name: 'about', meta: { title: '关于', description: '关于初曦の窝和站主' } },
  { path: '/article/:id', name: 'article', meta: { title: '文章', description: '文章详情' } },
  { path: '/admin', name: 'admin', meta: { title: '管理后台' } },
  // 兜底路由：未匹配的路径统一重定向回首页
  { path: '/:pathMatch(.*)*', name: 'notFound', redirect: '/index' }
]
