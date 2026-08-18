import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { menuGroups } from './adminMenu.js'

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

test('AdminView 使用菜单单一信息源渲染搜索和当前模块上下文', async () => {
  const source = await read('./AdminView.vue')
  assert.match(source, /import\s+\{\s*menuGroups\s*\}\s+from\s+'\.\/adminMenu'/)
  assert.match(source, /v-model\.trim="menuQuery"/)
  assert.match(source, /filteredMenuGroups/)
  assert.match(source, /currentMenu\.label/)
  assert.match(source, /currentMenu\.description/)
  assert.match(source, /没有匹配的后台模块/)
  assert.match(source, /class="admin-nav-empty"\s+role="status"\s+aria-live="polite"/)
})

test('后台菜单元数据完整且 key 唯一', () => {
  const items = menuGroups.flatMap(group => group.items)
  assert.equal(items.length, 25)
  assert.equal(new Set(items.map(item => item.key)).size, items.length)
  assert.ok(items.every(item => item.description.trim().length > 0))
})

test('侧栏搜索只由外层 focus-within 绘制焦点环', async () => {
  const css = await read('../../assets/css/admin.css')
  const generalSelector = '.admin-root :is(button, a, input, textarea, select, [tabindex]):focus-visible'
  const exemptionSelector = '.admin-root .admin-nav-search input:focus-visible'
  const exemptionIndex = css.lastIndexOf(exemptionSelector)
  assert.ok(exemptionIndex > css.lastIndexOf(generalSelector))
  assert.match(extractBlock(css, exemptionIndex), /box-shadow:\s*none/)
})

test('ResourcePanel 区分加载失败、空数据和搜索无结果', async () => {
  const source = await read('./ResourcePanel.vue')
  assert.match(source, /v-model\.trim="searchQuery"/)
  assert.match(source, /filteredRows/)
  assert.match(source, /loadError/)
  assert.match(source, /重新加载/)
  assert.match(source, /没有找到匹配/)
  assert.match(source, /aria-label="清空搜索"/)
})

test('通用表格具有吸顶表头和固定操作列', async () => {
  const css = await read('../../assets/css/admin.css')
  assert.match(css, /\.admin-table th[\s\S]*position:\s*sticky/)
  assert.match(css, /\.admin-col-ops[\s\S]*position:\s*sticky/)
})

test('ResourcePanel 仅接受最新加载请求并在失败时禁用批量操作', async () => {
  const source = await read('./ResourcePanel.vue')
  assert.match(source, /requestId/)
  assert.match(source, /latestRequest|requestId\.value\s*===\s*request/)
  assert.match(source, /selected\.value\s*=\s*new Set\(\)/)
  assert.match(source, /selected\.size\s*&&\s*!loadError|loadError[\s\S]*selected\.size/)
})

test('切换资源模块时清空搜索和选中状态', async () => {
  const source = await read('./ResourcePanel.vue')
  const schemaWatcher = source.slice(source.lastIndexOf('watch('))
  assert.match(schemaWatcher, /searchQuery\.value\s*=\s*''/)
  assert.match(schemaWatcher, /selected\.value\s*=\s*new Set\(\)/)
})

test('表格滚动容器承担纵向滚动并设置可控高度', async () => {
  const css = await read('../../assets/css/admin.css')
  const wrapIndex = css.indexOf('.admin-table-wrap')
  assert.ok(wrapIndex >= 0, '未找到表格滚动容器规则')
  const wrapBlock = extractBlock(css, wrapIndex)
  assert.match(wrapBlock, /overflow-y:\s*(auto|scroll)/)
  assert.match(wrapBlock, /(?:max-height|height):\s*[^;]+/)
  assert.match(wrapBlock, /overscroll-behavior-x:\s*contain/)
  assert.match(wrapBlock, /overscroll-behavior-y:\s*auto/)
  assert.doesNotMatch(wrapBlock, /overscroll-behavior:\s*contain/)
})
