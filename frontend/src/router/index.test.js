// 路由完整性检查：所有路由路径有效、SEO meta 完整（title + description）。
// 运行方式：在 frontend/ 目录执行 npm test（Node 内置测试器 node --test）
// 说明：直接导入 routes.js（纯数据，无 .vue 组件引用），避免 Node 环境解析 SFC。
import test from 'node:test'
import assert from 'node:assert/strict'
import routes from './routes.js'

// 需要 SEO meta 的路由：排除 redirect-only 路由和兜底路由
const seoRoutes = routes.filter(r => !r.redirect && r.name !== 'notFound')

test('所有路由路径均为非空字符串', () => {
  for (const route of routes) {
    assert.ok(typeof route.path === 'string' && route.path.length > 0, `路由 ${route.name || '(unnamed)'} 的 path 为空`)
  }
})

test('所有命名路由的 name 唯一', () => {
  const namedRoutes = routes.filter(r => r.name)
  const names = namedRoutes.map(r => r.name)
  const uniqueNames = new Set(names)
  assert.equal(names.length, uniqueNames.size, `存在重复的路由 name：${names.filter((n, i) => names.indexOf(n) !== i).join(', ')}`)
})

test('每个前台路由都有 meta.title（SEO 完整性）', () => {
  for (const route of seoRoutes) {
    assert.ok(route.meta?.title, `路由 ${route.name}（${route.path}）缺少 meta.title`)
  }
})

test('每个前台路由都有 meta.description（SEO 完整性）', () => {
  // admin 路由允许缺少 description（后台页面无需 SEO）
  const publicRoutes = seoRoutes.filter(r => r.name !== 'admin')
  for (const route of publicRoutes) {
    assert.ok(route.meta?.description, `路由 ${route.name}（${route.path}）缺少 meta.description`)
  }
})

test('路由数量合理：至少有 10 条命名路由', () => {
  const namedRoutes = routes.filter(r => r.name)
  assert.ok(namedRoutes.length >= 10, `命名路由数量不足：${namedRoutes.length}`)
})

test('redirect 路由的目标路径存在于路由列表中', () => {
  const allPaths = new Set(routes.map(r => r.path))
  const redirectRoutes = routes.filter(r => r.redirect)
  for (const route of redirectRoutes) {
    // redirect 目标可能是静态路径字符串
    if (typeof route.redirect === 'string') {
      assert.ok(allPaths.has(route.redirect), `路由 ${route.path} 的 redirect 目标 ${route.redirect} 不存在于路由列表中`)
    }
  }
})
