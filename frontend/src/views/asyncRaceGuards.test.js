import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

// 这批断言锁的是「异步回来先验代次」这个写法本身。
// 触发过的现象：快速连点上一篇/下一篇，慢的旧响应后到，把新文章的正文和评论覆盖成上一篇的。
// 直接跑组件需要 DOM + 路由 + axios 三套桩，本仓库既有测试统一用源码断言，这里沿用。

const articleView = new URL('./ArticleView.vue', import.meta.url)
const bangumiDetailView = new URL('./BangumiDetailView.vue', import.meta.url)
const searchOverlay = new URL('../layout/components/SearchOverlay.vue', import.meta.url)
const calendarView = new URL('./CalendarView.vue', import.meta.url)
const bangumiPanel = new URL('./admin/BangumiPanel.vue', import.meta.url)

test('文章页每次加载领取代次，正文与评论都按代次丢弃过期响应', async () => {
  const source = await readFile(articleView, 'utf8')

  assert.match(source, /let loadGeneration = 0/)
  assert.match(source, /async function load\(\)\s*\{\s*\n\s*const generation = \+\+loadGeneration/)

  // 评论加载要接住调用方的代次和文章 id，不能在 await 之后回头读 articleId.value
  assert.match(
    source,
    /async function loadComments\(page = 1, generation = loadGeneration, targetId = articleId\.value\)/
  )
  assert.match(source, /const data = await api\.articleComments\(targetId, page, 20\)/)

  // 成功与失败两条路径都要校验，否则失败分支仍会把新文章清空
  const guards = source.match(/if \(generation !== loadGeneration\) return/g) || []
  assert.ok(guards.length >= 5, `代次校验点过少：${guards.length}`)
  assert.match(source, /catch \(e\) \{\s*\n\s*if \(generation !== loadGeneration\) return/)

  // load() 把自己的代次交给评论，两者必须同代次
  assert.match(source, /await loadComments\(1, generation, targetId\)/)
})

test('番剧详情页切换条目时丢弃旧响应，且用局部 detail 而非已被清空的 record', async () => {
  const source = await readFile(bangumiDetailView, 'utf8')

  assert.match(source, /let loadGeneration = 0/)
  assert.match(source, /async function load\(id\)\s*\{\s*\n\s*const generation = \+\+loadGeneration/)

  // record.value 在函数开头被清空，subjectId 必须从本次请求的局部结果里取
  assert.match(source, /let detail = null/)
  assert.match(source, /detail = await api\.bangumiDetail\(id\)/)
  assert.match(source, /const sid = detail\?\.subjectId/)

  // loading 与 record 的写入都要过代次，否则旧请求收尾会把新请求的骨架屏关掉
  assert.match(source, /if \(generation === loadGeneration\) \{\s*\n\s*record\.value = detail\s*\n\s*loading\.value = false/)

  // 三个 bgm 区块合并回填前再校验一次
  assert.match(source, /\]\)\s*\n\s*if \(generation !== loadGeneration\) return/)
})

test('搜索浮层连续换关键词时只认最后一次结果', async () => {
  const source = await readFile(searchOverlay, 'utf8')

  assert.match(source, /let searchGeneration = 0/)
  assert.match(source, /const generation = \+\+searchGeneration/)
  assert.match(source, /const data = await api\.searchArticles\(kw, 1, 12\)/)
  assert.match(source, /if \(generation !== searchGeneration\) return\s*\n\s*searchResult\.value = data/)

  // loading 收尾也要过代次，否则旧请求结束会提前撤掉新请求的 loading
  assert.match(source, /if \(generation === searchGeneration\) searchLoading\.value = false/)
})

test('直连 api.bgm.tv 的 fetch 全部带超时，避免被墙时挂死', async () => {
  // api.bgm.tv 在国内会被墙，裸 fetch 没有默认超时，浏览器会一直挂着，
  // 表现为「同步中…」永久转圈或详情页区块永不降级。
  for (const url of [calendarView, bangumiDetailView, bangumiPanel]) {
    const source = await readFile(url, 'utf8')
    const file = url.href.split('/').pop()

    assert.match(source, /const BGM_TIMEOUT_MS = \d+/, `${file} 缺少超时常量`)

    const directCalls = source.match(/fetch\([^)]*api\.bgm\.tv[\s\S]*?\n/g) || []
    const bgmApiCalls = source.match(/fetch\(`\$\{BGM_API\}[\s\S]*?\n/g) || []
    const total = directCalls.length + bgmApiCalls.length
    assert.ok(total > 0, `${file} 未匹配到直连 fetch，断言已失效`)

    const signals = source.match(/AbortSignal\.timeout\(BGM_TIMEOUT_MS\)/g) || []
    assert.ok(
      signals.length >= total,
      `${file} 有 ${total} 处直连 fetch，但只有 ${signals.length} 处带 AbortSignal.timeout`
    )
  }
})
