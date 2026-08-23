import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const viewUrl = new URL('./ToolView.vue', import.meta.url)

test('工具页搜索栏独立放置在 Hero 与场景筛选之间', async () => {
  const source = await readFile(viewUrl, 'utf8')
  const heroEnd = source.indexOf('</CxSection>')
  const toolbar = source.indexOf('class="tool-search-toolbar"')
  const filterSection = source.indexOf('eyebrow="Atlas - 按场景筛选"')

  assert.equal(source.includes('tool-hero-search-panel'), false)
  assert.ok(heroEnd >= 0 && toolbar > heroEnd && toolbar < filterSection)
  assert.match(source, /<input[\s\S]*?type="search"[\s\S]*?class="tool-search-input"[\s\S]*?aria-label="搜索工具网站"/)
  assert.match(source, /class="tool-search-result"[\s\S]*?\{\{ filtered\.length \}\} 个结果/)
  assert.match(source, /v-if="keyword"[\s\S]*?class="tool-search-clear"[\s\S]*?aria-label="清空搜索"[\s\S]*?@click="keyword = ''"/)
  assert.match(source, /<SvgIcon name="common-search"/)
  assert.match(source, /<SvgIcon name="common-close"/)
})

test('筛选激活时隐藏优先浏览，让过滤结果直接可见', async () => {
  // 曾出现输入关键词后视野里只有不响应搜索的 Spotlight 区块，
  // 真正被过滤的工具列表在折叠区外，看起来像「搜索没实现」
  const source = await readFile(viewUrl, 'utf8')
  assert.match(source, /<CxSection v-if="!isFiltering" eyebrow="Spotlight - 优先浏览">/)
  assert.match(source, /const isFiltering = computed\(\(\) => !!keyword\.value\.trim\(\) \|\| !!activeCategory\.value\)/)
})

test('工具列表提供未加载与无结果两种空状态', async () => {
  const source = await readFile(viewUrl, 'utf8')
  assert.match(source, /v-if="!tools\.length"[\s\S]*?class="tool-empty-state"/)
  assert.match(source, /v-else-if="!filtered\.length"[\s\S]*?class="tool-empty-state"/)
  assert.match(source, /class="tool-empty-reset"\s+@click="resetFilters"/)
  assert.match(source, /function resetFilters\(\)/)
  assert.match(source, /\.tool-empty-state\{/)
})

test('关键词匹配覆盖名称、简介、域名、分类与标签', async () => {
  const source = await readFile(viewUrl, 'utf8')
  assert.match(source, /\[t\.websiteName, t\.websiteDescription, t\.websiteUrl, t\.category, \.\.\.\(t\.tags \|\| \[\]\)\]/)
})

test('小屏下清空按钮与重置按钮保持 40px 触控面积', async () => {
  // 桌面端 36px 在移动端低于 40px 触控标准，需在 640px 断点补足
  const source = await readFile(viewUrl, 'utf8')
  const mobileBlock = source.slice(source.indexOf('@media(max-width:640px)'))
  assert.match(mobileBlock, /\.tool-search-clear\{width:40px;height:40px\}/)
  assert.match(mobileBlock, /\.tool-empty-reset\{min-height:40px/)
})
