// cx-tag 柔色调色板变体的守护：
// 历史上 ArchiveView/ToolView 三处内联 style 复制同一组 --cx-tag 变量，
// 且引用的 --archive-tag-* 令牌仅在 .archive-page 作用域定义，
// ToolView 引用时静默失效（标签长期用继承色显示）。
// 抽为全局变体类 cx-tag--soft-palette 后，此文件锁定该约定。
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const read = relative => readFile(new URL(relative, import.meta.url), 'utf8')

test('cx-tag.css 提供全局 cx-tag--soft-palette 变体（亮暗双主题）', async () => {
  const css = await read('../assets/css/cx-tag.css')
  assert.match(css, /\.cx-tag--soft-palette\{[^}]*--cx-tag-text:/)
  assert.match(css, /html\.dark \.cx-tag--soft-palette\{[^}]*--cx-tag-text:/)
})

test('归档与工具页标签统一使用变体类，不再内联 --cx-tag 变量', async () => {
  for (const file of ['./ArchiveView.vue', './ToolView.vue']) {
    const source = await read(file)
    assert.doesNotMatch(source, /style="--cx-tag-text/, `${file} 不得回潮内联 --cx-tag 变量`)
    assert.match(source, /cx-tag--soft-palette/, `${file} 标签应使用 cx-tag--soft-palette 变体类`)
  }
})
