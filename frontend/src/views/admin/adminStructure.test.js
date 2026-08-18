import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const read = relative => readFile(new URL(relative, import.meta.url), 'utf8')

function extractBlock(source, startIndex) {
  const openIndex = source.indexOf('{', startIndex)
  assert.notEqual(openIndex, -1, '未找到声明块起始花括号')
  let depth = 0
  for (let index = openIndex; index < source.length; index += 1) {
    if (source[index] === '{') depth += 1
    if (source[index] === '}') depth -= 1
    if (depth === 0) return source.slice(startIndex, index + 1)
  }
  assert.fail('声明块花括号未闭合')
}

test('后台样式使用可读中文系统字体和统一 focus-visible 令牌', async () => {
  const css = await read('../../assets/css/admin.css')
  assert.doesNotMatch(css, /Comic Sans MS/)
  assert.match(css, /--adm-focus-ring:/)
  assert.match(css, /:focus-visible/)
  assert.match(css, /--adm-error-bg:/)
})

test('后台具体控件不会覆盖统一键盘焦点，并收敛通用辅助文字字号', async () => {
  const css = await read('../../assets/css/admin.css')
  assert.match(css, /\.admin-input:focus\s*\{[^}]*box-shadow:\s*var\(--adm-focus-ring\)/)
  assert.match(css, /\.admin-input\[type='datetime-local'\]:focus\s*\{[^}]*box-shadow:\s*var\(--adm-focus-ring\)/)
  assert.match(css, /\.admin-check:focus-visible\s*\{[^}]*box-shadow:\s*var\(--adm-focus-ring\)/)
  assert.match(css, /\.admin-input\[type='datetime-local'\]::-webkit-datetime-edit\s*\{[^}]*font-size:\s*15px/)
  assert.match(css, /\.admin-login-sub\s*\{[^}]*font-size:\s*15px/)
})

test('统一键盘焦点规则以足够优先级覆盖按钮 hover 阴影', async () => {
  const css = await read('../../assets/css/admin.css')
  const selector = '.admin-root :is(button, a, input, textarea, select, [tabindex]):focus-visible'
  const focusIndex = css.lastIndexOf(selector)
  assert.ok(focusIndex > css.lastIndexOf('.admin-btn:hover:not(:disabled)'))
  assert.ok(focusIndex > css.lastIndexOf('.admin-btn-ghost:hover:not(:disabled)'))
  assert.match(extractBlock(css, focusIndex), /box-shadow:\s*var\(--adm-focus-ring\)/)
})

test('小屏编辑弹窗使用全屏安全尺寸', async () => {
  const css = await read('../../assets/css/admin.css')
  const mediaBlocks = Array.from(css.matchAll(/@media\s*\(max-width:\s*900px\)/g), match =>
    extractBlock(css, match.index)
  )
  const modalMedia = mediaBlocks.find(block => block.includes('.admin-modal'))
  assert.ok(modalMedia, '未找到 900px 下的编辑弹窗规则')
  const modalIndex = modalMedia.indexOf('.admin-modal')
  assert.match(extractBlock(modalMedia, modalIndex), /inset:\s*0/)
})
