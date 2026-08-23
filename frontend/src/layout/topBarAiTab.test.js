import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const topBar = readFileSync(new URL('./components/TopBar.vue', import.meta.url), 'utf8')
const layout = readFileSync(new URL('./LayoutView.vue', import.meta.url), 'utf8')

test('顶部操作栏在音乐和看板娘之间提供纯图标 AI 入口', () => {
  assert.doesNotMatch(topBar, /nav-link--ai/)
  assert.match(topBar, /aria-label="打开 AI 助手"/)
  assert.match(topBar, /'is-active': aiOpen/)
  assert.match(topBar, /class="shell-action-btn is-music"[\s\S]*class="shell-action-btn is-ai"[\s\S]*class="shell-action-btn is-cat"/)
  assert.match(topBar, /class="shell-action-btn is-ai"[\s\S]*<SvgIcon name="common-chat"/)
  assert.doesNotMatch(topBar, /<span class="nav-link__label">AI 助手<\/span>/)
})

test('LayoutView 将 AI 弹窗状态传给顶部 tab', () => {
  assert.match(layout, /:ai-open="aiExpanded"/)
})

test('导航菜单不再重复显示带文字的 AI 入口', () => {
  assert.doesNotMatch(topBar, /top-nav-mobile-ai/)
})
