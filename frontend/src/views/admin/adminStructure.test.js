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
  // .admin-input 仅剩 AdminSelect 的下拉触发按钮在用
  assert.match(css, /\.admin-input:focus\s*\{[^}]*box-shadow:\s*var\(--adm-focus-ring\)/)
  // 输入框已换 CxInput：焦点环画在外壳上（内层再画一次会成双环）
  assert.match(css, /\.cx-input--admin:focus-within\s*\{[^}]*box-shadow:\s*var\(--adm-focus-ring\)/)
  // 复选框已换 CxCheckbox，焦点环经 --cx-check-ring 间接层改指后台令牌
  assert.match(css, /\.admin-root \.cx-checkbox\s*\{[^}]*--cx-check-ring:\s*var\(--adm-focus-ring\)/)
  assert.match(css, /\.cx-input--admin \.cx-input__inner\s*\{[^}]*font-size:\s*15px/)
  assert.match(css, /\.admin-login-sub\s*\{[^}]*font-size:\s*15px/)
})

test('统一键盘焦点规则存在，且自建按钮样式已迁移到 cx-button', async () => {
  const css = await read('../../assets/css/admin.css')
  const selector = '.admin-root :is(button, a, input, textarea, select, [tabindex]):focus-visible'
  const focusIndex = css.lastIndexOf(selector)
  assert.ok(focusIndex > 0)
  // 自建控件样式已由 cx 组件取代，不允许回潮
  assert.ok(!css.includes('.admin-btn'))
  assert.ok(!css.includes('.admin-switch'))
  assert.ok(!css.includes('.admin-check'), '复选框样式已迁到 cx-checkbox.css')
  assert.ok(!css.includes('.admin-textarea'), '多行输入样式已迁到 .cx-input--admin .cx-input__textarea')
  assert.ok(!css.includes(".admin-input[type='date']"), '日期输入已换 CxDatePicker，原生日期皮肤不再需要')
  assert.match(extractBlock(css, focusIndex), /box-shadow:\s*var\(--adm-focus-ring\)/)
})

test('斑马纹作用在 td 上，且 hover/选中高亮写在其后以胜出层叠', async () => {
  const css = await read('../../assets/css/admin.css')
  // 斑马纹染的是 td 背景，高亮若写在 tr 上会被 td 背景盖掉，等于悬停无反馈
  assert.match(css, /\.admin-table tbody tr:nth-child\(even\) td\s*\{[^}]*background-color/)
  const zebraIndex = css.lastIndexOf('.admin-table tbody tr:nth-child(even) td {')
  const highlightIndex = css.lastIndexOf('.admin-table tbody tr.is-checked td {')
  assert.ok(highlightIndex > zebraIndex, 'hover/选中高亮必须写在斑马纹之后')

  // sticky 操作列的两条规则特异性相同且都带 !important，只靠源码顺序决胜
  const zebraOpsIndex = css.lastIndexOf('.admin-table tbody tr:nth-child(even) td.admin-col-ops {')
  const hoverOpsIndex = css.lastIndexOf('.admin-table tbody tr:hover td.admin-col-ops {')
  const checkedOpsIndex = css.lastIndexOf('.admin-table tbody tr.is-checked td.admin-col-ops {')
  assert.ok(zebraOpsIndex > 0, '偶数行操作列需要不透明背景，否则横向滚动时正文穿透')
  assert.ok(hoverOpsIndex > zebraOpsIndex, '操作列 hover 背景必须写在斑马纹之后')
  assert.ok(checkedOpsIndex > zebraOpsIndex, '操作列选中背景必须写在斑马纹之后')
})

test('需要阅读的辅助文字不使用低对比度的 text-faint', async () => {
  const css = await read('../../assets/css/admin.css')
  // text-faint 在白底上约 2.6:1，低于 WCAG AA 的 4.5:1，只允许留给装饰性字符
  for (const selector of ['.admin-toolbar-meta', '.admin-state', '.admin-field-tip']) {
    const block = extractBlock(css, css.lastIndexOf(`${selector} {`))
    assert.doesNotMatch(block, /--adm-text-faint/, `${selector} 是要读的文字，应改用 --adm-text-dim`)
  }
  // 也不能改用 opacity 绕过：同样会压低实际对比度
  assert.doesNotMatch(extractBlock(css, css.lastIndexOf('.admin-pager-info {')), /opacity/)
})

test('引用的 --adm-* 令牌都有定义，不留悬空引用', async () => {
  const css = await read('../../assets/css/admin.css')
  // var() 引用未定义令牌且无 fallback 时，整条声明在计算值阶段失效、静默退回初始值。
  // 删令牌时漏改引用就会这样：批次三删 --adm-input-bg 后 CxInput 后台皮肤丢了底色。
  const defined = new Set(Array.from(css.matchAll(/(--adm-[a-z0-9-]+)\s*:/g), m => m[1]))
  const dangling = new Set()
  for (const [, token, next] of css.matchAll(/var\(\s*(--adm-[a-z0-9-]+)\s*([,)])/g)) {
    if (next === ')' && !defined.has(token)) dangling.add(token)
  }
  assert.deepEqual([...dangling], [], '这些令牌被引用但没有定义，声明会静默失效')
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
  assert.equal(items.length, 26)
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
  // 搜索框已换 CxInput：v-model.trim 的等价写法是 model-modifier="trim"
  // （修饰符只作用于原生元素，组件上要由 props 声明），去空格行为必须保留
  assert.match(source, /v-model="searchQuery"/)
  assert.match(source, /model-modifier="trim"/)
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

test('固定操作列层级低于吸顶表头，滚动时按钮不盖住标题栏', async () => {
  // 曾出现 .admin-col-ops 整体 z-index:3 高于表头的 2，纵向滚动时
  // 行内「编辑/删除」单元格绘制在吸顶表头之上
  const css = await read('../../assets/css/admin.css')
  const zIndexOf = selector => {
    const index = css.indexOf(selector)
    assert.ok(index >= 0, `未找到 ${selector} 规则`)
    const match = extractBlock(css, index).match(/z-index:\s*(\d+)/)
    assert.ok(match, `${selector} 必须显式声明 z-index`)
    return Number(match[1])
  }
  const headerZ = zIndexOf('.admin-table th')
  const opsHeaderZ = zIndexOf('th.admin-col-ops')
  const opsCellZ = zIndexOf('td.admin-col-ops')
  assert.ok(opsCellZ < headerZ, '操作列 td 必须低于吸顶表头 th')
  assert.ok(opsHeaderZ > headerZ, '表头右上交叉格必须高于普通表头')
  // 基础 .admin-col-ops 同时命中 th 与 td，不得在这里整体抬高 z-index
  const baseIndex = css.indexOf('\n.admin-col-ops {')
  assert.ok(baseIndex >= 0, '未找到 .admin-col-ops 基础规则')
  assert.doesNotMatch(extractBlock(css, baseIndex), /z-index/)
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

test('字段组件和通用弹窗渲染说明、必填、错误及分组', async () => {
  const field = await read('./FieldInput.vue')
  const panel = await read('./ResourcePanel.vue')
  assert.match(field, /field\.required/)
  assert.match(field, /field\.tip/)
  assert.match(field, /aria-invalid/)
  assert.match(field, /aria-describedby/)
  assert.match(field, /useId\(\)/)
  assert.match(field, /<CxSwitch[\s\S]*?:model-value="!!modelValue"/)
  assert.match(field, /<CxSwitch[\s\S]*?@update:model-value="v => emit\('update:modelValue', v\)"/)
  assert.match(panel, /groupFields/)
  assert.match(panel, /fieldGroups/)
  assert.match(panel, /admin-form-section/)
})

test('只读字段渲染为展示块且不参与输入，样式与可编辑输入框区分', async () => {
  const field = await read('./FieldInput.vue')
  // readonly 分支必须排在所有输入控件之前（v-if 抢在 CxSwitch 的 v-else-if 前面）
  const readonlyIndex = field.indexOf('v-if="field.readonly"')
  const switchIndex = field.indexOf('v-else-if="field.type === \'boolean\'"')
  assert.ok(readonlyIndex > -1, '缺少 field.readonly 只读分支')
  assert.ok(switchIndex > readonlyIndex, 'readonly 分支必须位于布尔开关之前')
  assert.match(field, /admin-field-readonly/)
  assert.match(field, /readonlyDisplay/)
  // 只读块必须是 div 而非 input：任何 input 都不应绑定 readonly 展示值
  assert.doesNotMatch(field, /<input[^>]*readonlyDisplay/)
  // 样式虚线框 + 次级底色，与可编辑输入框区分
  const css = await read('../../assets/css/admin.css')
  const roIndex = css.indexOf('.admin-field-readonly')
  assert.ok(roIndex >= 0, '缺少 .admin-field-readonly 样式')
  const block = extractBlock(css, roIndex)
  assert.match(block, /border:\s*1px dashed/)
  assert.match(block, /var\(--adm-card-2\)/)
})

test('字段分组使用一级边界且适配小屏，不创建嵌套卡片阴影', async () => {
  const css = await read('../../assets/css/admin.css')
  const sectionIndex = css.indexOf('.admin-form-section')
  assert.ok(sectionIndex >= 0, '缺少字段分组样式')
  assert.doesNotMatch(extractBlock(css, sectionIndex), /box-shadow/)
  assert.match(css, /@media\s*\(max-width:\s*900px\)[\s\S]*\.admin-form-section/)
  assert.match(css, /\.admin-field-error/)
  assert.match(css, /\.admin-field-required/)
})

test('自绘选择与日期控件把无障碍属性绑定到真实触发按钮', async () => {
  const sources = [
    await read('./AdminSelect.vue'),
    await read('../../components/cx/CxDatePicker.vue')
  ]
  for (const source of sources) {
    assert.match(source, /ariaInvalid/)
    assert.match(source, /ariaDescribedby/)
    assert.match(source, /ariaRequired/)
    const trigger = source.match(/<button[\s\S]*?>/)?.[0] || ''
    assert.match(trigger, /:id="id"/)
    assert.match(trigger, /:name="name"/)
    assert.match(trigger, /:aria-invalid="ariaInvalid"/)
    assert.match(trigger, /:aria-describedby="ariaDescribedby"/)
    assert.match(trigger, /:aria-required="ariaRequired"/)
  }
})

test('后台自绘字段触发按钮呈现错误边框并保留焦点环', async () => {
  const css = await read('../../assets/css/admin.css')
  assert.match(css, /\.adm-select-trigger[\s\S]*\.cx-date-picker__field[\s\S]*\[aria-invalid='true'\][\s\S]*border-color:\s*var\(--adm-danger\)/)
  assert.match(css, /\[aria-invalid='true'\]:focus-visible[\s\S]*box-shadow:\s*var\(--adm-focus-ring\)/)
})

test('字段调用端向自绘控件与布尔开关传递名称和必填语义', async () => {
  const field = await read('./FieldInput.vue')
  const datePickers = Array.from(field.matchAll(/<CxDatePicker[\s\S]*?\/>/g), match => match[0])
  assert.equal(datePickers.length, 2)
  for (const picker of datePickers) {
    assert.match(picker, /:name="field\.name"/)
    assert.match(picker, /:aria-required="field\.required \? 'true' : undefined"/)
  }

  const select = field.match(/<AdminSelect[\s\S]*?\/>/)?.[0] || ''
  assert.match(select, /:name="field\.name"/)
  assert.match(select, /:aria-required="field\.required \? 'true' : undefined"/)

  const booleanSwitch = field.match(/<!-- 布尔开关 -->[\s\S]*?<CxSwitch[\s\S]*?\/>/)?.[0] || ''
  assert.match(booleanSwitch, /:aria-required="field\.required \? 'true' : undefined"/)
})

test('通用编辑弹窗保护未保存内容并支持快捷保存', async () => {
  const source = await read('./ResourcePanel.vue')
  assert.match(source, /initialSnapshot/)
  assert.match(source, /isDirty/)
  assert.match(source, /window\.confirm\('当前修改尚未保存/)
  assert.match(source, /event\.(ctrlKey|metaKey)/)
  assert.match(source, /event\.key\.toLowerCase\(\) === 's'/)
  assert.match(source, /saveError/)
  assert.match(source, /:role="saveError \? 'alert' : 'status'"/)
  assert.match(source, /:disabled="saving \|\| \(field\.name === 'id' && editingId != null\)"/)
})

test('点击弹窗外的遮罩总先确认再关闭，未修改时也提示', async () => {
  const source = await read('./ResourcePanel.vue')
  // 遮罩点击单独走 onMaskClick，不能复用 requestClose（仅脏才确认）
  const maskOpenMatch = source.match(/<div v-if="drawerOpen" class="admin-mask"[^>]*?@click\.self="onMaskClick"[^>]*?><\/div>/)
  assert.ok(maskOpenMatch, '弹窗遮罩点击应绑定 onMaskClick')
  assert.doesNotMatch(source, /admin-mask"\s+@click\.self="closeDrawer"/)
  assert.match(source, /function onMaskClick\(\)/)
  // 文案区分两种状态：脏时提示放弃修改，未脏时仍提示一次避免误关
  const onMaskBlock = extractBlock(source, source.lastIndexOf('function onMaskClick()'))
  assert.match(onMaskBlock, /当前修改尚未保存，确定放弃并关闭吗/)
  assert.match(onMaskBlock, /确定关闭当前弹窗吗/)
  // 提示必须先于关闭：onMaskClick 只亮出站内确认层，不许自己把弹窗关掉，
  // 也不许用 window.confirm（原生对话框弹出时弹窗已从画面上消失）
  assert.doesNotMatch(onMaskBlock, /window\.confirm/)
  assert.doesNotMatch(onMaskBlock, /drawerOpen\.value = false/)
  assert.match(source, /v-if="drawerOpen && closeConfirmTip"/)
  const confirmBlock = extractBlock(source, source.lastIndexOf('function confirmMaskClose()'))
  assert.match(confirmBlock, /drawerOpen\.value = false/)
  // 确认层要盖在编辑弹窗（z-index 120）之上，否则又变成「弹窗先看不见才提示」
  const css = await read('../../assets/css/admin.css')
  const confirmCss = css.slice(css.indexOf('.admin-confirm-mask'))
  const zIndex = Number((confirmCss.match(/z-index:\s*(\d+)/) || [])[1])
  assert.ok(zIndex > 120, `确认层 z-index 应高于编辑弹窗，实际 ${zIndex}`)
  // 关闭按钮、取消按钮、Esc 仍走 requestClose（脏才确认），与 mask 路径区分
  const closeDrawerBlock = extractBlock(source, source.lastIndexOf('function closeDrawer()'))
  assert.match(closeDrawerBlock, /requestClose\(\)/)
})

test('编辑器让已打开的子弹层独占 Escape 和 Ctrl+S', async () => {
  const panel = await read('./ResourcePanel.vue')
  const select = await read('./AdminSelect.vue')
  const datePicker = await read('../../components/cx/CxDatePicker.vue')
  const mediaPicker = await read('./MediaPicker.vue')
  const cropDialog = await read('./CropDialog.vue')
  assert.match(panel, /hasOpenEditorOverlay\(\)/)
  assert.match(panel, /if \(hasOpenEditorOverlay\(\) &&/)
  assert.match(select, /stopPropagation\(\)/)
  assert.match(datePicker, /stopPropagation\(\)/)
  assert.match(mediaPicker, /addEventListener\('keydown'/)
  assert.match(cropDialog, /addEventListener\('keydown'/)
})

test('保存完成后保留保存期间产生的新修改', async () => {
  const source = await read('./ResourcePanel.vue')
  assert.match(source, /submittedSnapshot/)
  assert.match(source, /createFormSnapshot\(form\.value, props\.schema\.fields\)/)
  assert.match(source, /submittedSnapshot[\s\S]*drawerOpen\.value = false/)
  assert.match(source, /initialSnapshot\.value = submittedSnapshot/)
})

test('后台菜单切换经过资源编辑器关闭守卫', async () => {
  const panel = await read('./ResourcePanel.vue')
  const view = await read('./AdminView.vue')
  assert.match(panel, /defineExpose\(\{\s*requestClose\s*\}\)/)
  assert.match(view, /ref\(null\)/)
  assert.match(view, /requestClose/)
  assert.match(view, /if \(!canLeaveCurrentPanel\(\)\) return/)
})

test('ResourcePanel 的 columns 声明在依赖它的 computed 之前', async () => {
  // 曾出现 columns 声明晚于 filteredRows，触发 TDZ「Cannot access 'columns' before initialization」，
  // 导致所有通用资源面板渲染时抛错、内容区整块空白
  const source = await read('./ResourcePanel.vue')
  const columnsIndex = source.indexOf('const columns = computed(')
  const apiIndex = source.indexOf('const api = computed(')
  const filteredRowsIndex = source.indexOf('const filteredRows = computed(')
  assert.ok(columnsIndex > -1 && apiIndex > -1 && filteredRowsIndex > -1)
  assert.ok(columnsIndex < filteredRowsIndex, 'columns 必须声明在 filteredRows 之前')
  assert.ok(apiIndex < source.indexOf('async function load('), 'api 必须声明在 load 之前')
})

test('后台入口不残留调试代码，默认停在概览面板', async () => {
  const view = await read('./AdminView.vue')
  assert.match(view, /const currentKey = ref\('dashboard'\)/)
  assert.doesNotMatch(view, /data-test-debug/)
  assert.doesNotMatch(view, /onErrorCaptured/)
})

test('日期弹层定位 top/bottom 互斥，上翻时清除样式表兑底的 top', async () => {
  const source = await read('../../components/cx/CxDatePicker.vue')
  // 样式表给未定位态兑底 top: calc(100% + 8px)。上翻时只设 bottom 不清 top，
  // 两条同时生效会把面板压扁在视口底部（曾渲染成 30px 细条，看似下方被截断）。
  assert.match(source, /openUp[\s\S]*?top:\s*'auto'[\s\S]*?bottom:/)
  assert.match(source, /\{\s*top:\s*`\$\{rootRect\.bottom \+ gap\}px`,\s*bottom:\s*'auto'\s*\}/)
})

test('长文编辑面板的 textarea 定高用 :deep() 穿透，不被基础规则覆盖', async () => {
  // scoped 化后用 :deep() 穿透 CxInput 子组件：组件样式经 PostCSS 重排后
  // 排在 admin.css 的 .cx-input--admin .cx-input__textarea（min-height:72px）之后，
  // 同特异性下以后者（组件内 :deep 规则）生效，无需再借 .admin-root 全局前缀提权。
  // 这里守护两点：① 必须是 :deep() 穿透 ② 不许退回 .admin-root 全局写法（scoped 下会失效）。
  const articles = await read('./ArticlesPanel.vue')
  const siteContent = await read('./SiteContentPanel.vue')
  assert.match(articles, /\.ap-content-input :deep\(\.cx-input__textarea\)\s*\{[^}]*min-height:\s*62vh/)
  assert.match(articles, /\.ap-content-input :deep\(\.cx-input__textarea\)\s*\{[^}]*min-height:\s*46vh/)
  assert.doesNotMatch(articles, /\.admin-root \.ap-content-input/)
  assert.match(siteContent, /\.scp-md-input :deep\(\.cx-input__textarea\)\s*\{[^}]*min-height:\s*62vh/)
  assert.match(siteContent, /\.scp-md-input :deep\(\.cx-input__textarea\)\s*\{[^}]*min-height:\s*46vh/)
  assert.doesNotMatch(siteContent, /\.admin-root \.scp-md-input/)
})

test('全局层级遵守样式规约，图片预览不覆盖全局反馈', async () => {
  const sources = await Promise.all([
    read('../../assets/css/admin.css'),
    read('../../assets/css/article.css'),
    read('../../assets/css/cx-date-picker.css'),
    read('../../assets/css/cx-popover.css'),
    read('../../assets/css/layout/music.css'),
    read('../../assets/css/layout/shell.css'),
    read('../../assets/css/layout/ai-chat.css'),
    read('../../assets/css/layout/sidebar.css'),
    read('../../assets/css/layout/topbar.css'),
    read('../../assets/css/layout/search.css'),
    read('../../assets/css/layout/auth.css'),
    read('../../assets/css/layout/dialogs.css'),
    read('../../assets/css/layout/live2d.css'),
    read('../../assets/css/preview.css'),
    read('../../components/cx/CxMessage.vue'),
    read('../../layout/LayoutView.vue')
  ])
  for (const source of sources) {
    assert.doesNotMatch(source, /z-index\s*:\s*\d{6,}/, 'z-index 不得使用六位及以上数值')
  }
  assert.match(sources[13], /\.medium-zoom-overlay,\.medium-zoom-image--opened\{z-index:10000\}/)
  assert.match(sources[14], /z-index:\s*99999/)
})
