# 后台管理端可读性与编辑体验升级 Implementation Plan

> **For implementer:** Use TDD throughout. Write failing test first. Watch it fail. Then implement.

**Goal:** 在不修改后端接口和数据结构的前提下，为整个后台统一模块上下文、检索、列表状态、字段说明、分组编辑、未保存保护与响应式视觉体验。

**Architecture:** 新增无框架依赖的 `adminUi.js` 纯函数层，集中处理菜单过滤、列表过滤、字段分组和表单快照；`AdminView.vue`、`ResourcePanel.vue`、`FieldInput.vue` 只负责 Vue 状态与渲染。视觉统一继续使用 `frontend/src/assets/css/admin.css` 中的 `--adm-*` 语义令牌，专用面板保留业务逻辑，仅收敛样式与反馈。

**Tech Stack:** Vue 3 Composition API、原生 CSS、Node.js `node:test`、ESLint、Vite。

---

## 执行前保护

- 当前 `frontend/src/views/admin/ResourcePanel.vue` 与 `frontend/src/views/admin/BangumiPanel.vue` 已有未提交改动。先执行 `git diff -- <file>` 保存认知；提交这些文件时必须使用 `git add -p -- <file>`，只暂存本计划产生的 hunk。
- 不修改 `frontend/src/api/admin.js`、任何后端文件、实体或 DDL。
- 每个任务都先运行指定测试观察失败，再写最小实现；不得先写实现后补测试。
- 每次提交前运行 `git diff --cached --check` 和 `git diff --cached --name-only`，确认提交边界。

### Task 1: 建立后台 UI 纯函数与单元测试基座

**Files:**
- Create: `frontend/src/views/admin/adminUi.js`
- Create: `frontend/src/views/admin/adminUi.test.js`

**Step 1: Write the failing test**

创建 `frontend/src/views/admin/adminUi.test.js`：

```js
import test from 'node:test'
import assert from 'node:assert/strict'
import {
  createFormSnapshot,
  filterMenuGroups,
  filterResourceRows,
  groupFields,
  isFormDirty
} from './adminUi.js'

test('filterMenuGroups 同时按菜单名、分组名和说明过滤，并移除空分组', () => {
  const groups = [
    { title: '内容', items: [{ key: 'articles', label: '文章管理', description: '编辑博客正文' }] },
    { title: '资源', items: [{ key: 'media', label: '图片管理', description: '上传素材' }] }
  ]
  assert.deepEqual(filterMenuGroups(groups, '博客'), [{
    title: '内容',
    items: [{ key: 'articles', label: '文章管理', description: '编辑博客正文' }]
  }])
  assert.equal(filterMenuGroups(groups, '资源')[0].items[0].key, 'media')
  assert.equal(filterMenuGroups(groups, '不存在').length, 0)
})

test('filterResourceRows 只检索展示列，忽略大小写并支持数组与布尔值', () => {
  const rows = [
    { id: 1, title: 'Hello Vue', tags: ['前端', 'Vue'], visible: true, secret: '隐藏命中' },
    { id: 2, title: 'Spring', tags: ['后端'], visible: false, secret: '' }
  ]
  const columns = [{ name: 'title' }, { name: 'tags' }, { name: 'visible', type: 'boolean' }]
  assert.deepEqual(filterResourceRows(rows, columns, 'vue').map(row => row.id), [1])
  assert.deepEqual(filterResourceRows(rows, columns, '前端').map(row => row.id), [1])
  assert.deepEqual(filterResourceRows(rows, columns, '否').map(row => row.id), [2])
  assert.equal(filterResourceRows(rows, columns, '隐藏命中').length, 0)
})

test('groupFields 按首次出现顺序分组，未配置字段进入基本信息', () => {
  const fields = [
    { name: 'title', label: '标题' },
    { name: 'cover', label: '封面', group: '内容与媒体' },
    { name: 'visible', label: '显示', group: '状态与排序' }
  ]
  assert.deepEqual(groupFields(fields).map(group => group.label), ['基本信息', '内容与媒体', '状态与排序'])
  assert.deepEqual(groupFields(fields)[0].fields.map(field => field.name), ['title'])
})

test('表单快照按字段类型规范化，等价输入不误报脏状态', () => {
  const fields = [
    { name: 'tags', type: 'tags' },
    { name: 'sortIndex', type: 'number' },
    { name: 'visible', type: 'boolean' },
    { name: 'publishedAt', type: 'datetime' }
  ]
  const initial = createFormSnapshot(
    { tags: 'Vue, 博客', sortIndex: '2', visible: 1, publishedAt: '' },
    fields
  )
  assert.equal(isFormDirty(
    { tags: 'Vue，博客', sortIndex: 2, visible: true, publishedAt: null },
    fields,
    initial
  ), false)
  assert.equal(isFormDirty(
    { tags: 'Vue, 博客, 新增', sortIndex: 2, visible: true, publishedAt: null },
    fields,
    initial
  ), true)
})
```

**Step 2: Run test — confirm it fails**

Command: `cd frontend; node --test src/views/admin/adminUi.test.js`

Expected: FAIL — `ERR_MODULE_NOT_FOUND`，因为 `adminUi.js` 尚不存在。

**Step 3: Write minimal implementation**

创建 `frontend/src/views/admin/adminUi.js`：

```js
const normalizeText = value => String(value ?? '').trim().toLocaleLowerCase('zh-CN')

export function filterMenuGroups(groups, query) {
  const keyword = normalizeText(query)
  if (!keyword) return groups
  return groups
    .map(group => {
      const groupMatched = normalizeText(group.title).includes(keyword)
      const items = groupMatched
        ? group.items
        : group.items.filter(item =>
          [item.label, item.description].some(value => normalizeText(value).includes(keyword))
        )
      return { ...group, items }
    })
    .filter(group => group.items.length > 0)
}

function searchableValue(value, column) {
  if (column.type === 'boolean') return value ? '是 true' : '否 false'
  if (Array.isArray(value)) return value.join(' ')
  return value
}

export function filterResourceRows(rows, columns, query) {
  const keyword = normalizeText(query)
  if (!keyword) return rows
  return rows.filter(row => columns.some(column =>
    normalizeText(searchableValue(row[column.name], column)).includes(keyword)
  ))
}

export function groupFields(fields) {
  const groups = new Map()
  for (const field of fields) {
    const label = field.group || '基本信息'
    if (!groups.has(label)) groups.set(label, [])
    groups.get(label).push(field)
  }
  return Array.from(groups, ([label, groupedFields]) => ({
    key: label,
    label,
    fields: groupedFields
  }))
}

function normalizeFieldValue(field, raw) {
  if (field.type === 'tags') {
    return String(raw || '')
      .split(/[,，]/)
      .map(value => value.trim())
      .filter(Boolean)
  }
  if (field.type === 'number') return raw === '' || raw == null ? null : Number(raw)
  if (field.type === 'boolean') return Boolean(raw)
  if (field.type === 'date' || field.type === 'datetime') return raw || null
  return raw ?? ''
}

export function createFormSnapshot(form, fields) {
  return JSON.stringify(Object.fromEntries(
    fields.map(field => [field.name, normalizeFieldValue(field, form[field.name])])
  ))
}

export function isFormDirty(form, fields, initialSnapshot) {
  return createFormSnapshot(form, fields) !== initialSnapshot
}
```

**Step 4: Run test — confirm it passes**

Command: `cd frontend; node --test src/views/admin/adminUi.test.js`

Expected: PASS — 4 tests。

**Step 5: Commit**

```powershell
git add -- frontend/src/views/admin/adminUi.js frontend/src/views/admin/adminUi.test.js
git diff --cached --check
git commit -m "test: 建立后台交互纯函数基座"
```

### Task 2: 统一后台设计令牌、字体、焦点与响应式基础

**Files:**
- Create: `frontend/src/views/admin/adminStructure.test.js`
- Modify: `frontend/src/assets/css/admin.css`

**Step 1: Write the failing test**

创建 `frontend/src/views/admin/adminStructure.test.js`：

```js
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const read = relative => readFile(new URL(relative, import.meta.url), 'utf8')

test('后台样式使用可读中文系统字体和统一 focus-visible 令牌', async () => {
  const css = await read('../../assets/css/admin.css')
  assert.doesNotMatch(css, /Comic Sans MS/)
  assert.match(css, /--adm-focus-ring:/)
  assert.match(css, /:focus-visible/)
  assert.match(css, /--adm-error-bg:/)
})

test('小屏编辑弹窗使用全屏安全尺寸', async () => {
  const css = await read('../../assets/css/admin.css')
  assert.match(css, /@media\s*\(max-width:\s*900px\)[\s\S]*\.admin-modal[\s\S]*inset:\s*0/)
})
```

**Step 2: Run test — confirm it fails**

Command: `cd frontend; node --test src/views/admin/adminStructure.test.js`

Expected: FAIL — 仍包含 `Comic Sans MS`，且缺少新增令牌与 900px 全屏规则。

**Step 3: Write minimal implementation**

在 `:root` 与 `html.dark` 中分别补充：

```css
--adm-error-bg: rgba(192, 86, 79, 0.1);
--adm-focus-ring: 0 0 0 3px rgba(63, 119, 181, 0.2);
--adm-sticky-bg: rgba(255, 255, 255, 0.96);
```

```css
--adm-error-bg: rgba(217, 141, 141, 0.12);
--adm-focus-ring: 0 0 0 3px rgba(141, 184, 232, 0.22);
--adm-sticky-bg: rgba(26, 34, 48, 0.96);
```

将 `.admin-root` 字体改为：

```css
font-family: Inter, "PingFang SC", "Microsoft YaHei", "Noto Sans CJK SC", system-ui, -apple-system, sans-serif;
font-size: 15px;
line-height: 1.5;
```

新增统一键盘焦点和小屏弹窗规则：

```css
.admin-root :where(button, a, input, textarea, select, [tabindex]):focus-visible {
  outline: none;
  box-shadow: var(--adm-focus-ring);
}

@media (max-width: 900px) {
  .admin-modal,
  .admin-modal.wide {
    inset: 0;
    width: 100vw;
    max-width: none;
    max-height: 100dvh;
    border-radius: 0;
  }
}
```

同时将通用正文、按钮、输入和表格字号收敛至 `14px`–`15px`，不得改动前台 CSS。

**Step 4: Run test — confirm it passes**

Command: `cd frontend; node --test src/views/admin/adminStructure.test.js`

Expected: PASS — 2 tests。

**Step 5: Commit**

```powershell
git add -- frontend/src/views/admin/adminStructure.test.js frontend/src/assets/css/admin.css
git diff --cached --check
git commit -m "style: 统一后台设计令牌与可读性基础"
```

### Task 3: 让菜单元数据驱动动态顶栏与侧栏检索

**Files:**
- Create: `frontend/src/views/admin/adminMenu.js`
- Modify: `frontend/src/views/admin/AdminView.vue`
- Modify: `frontend/src/views/admin/adminStructure.test.js`

**Step 1: Write the failing test**

向 `adminStructure.test.js` 追加：

```js
test('AdminView 使用菜单单一信息源渲染搜索和当前模块上下文', async () => {
  const source = await read('./AdminView.vue')
  assert.match(source, /import\s+\{\s*menuGroups\s*\}\s+from\s+'\.\/adminMenu'/)
  assert.match(source, /v-model\.trim="menuQuery"/)
  assert.match(source, /filteredMenuGroups/)
  assert.match(source, /currentMenu\.label/)
  assert.match(source, /currentMenu\.description/)
  assert.match(source, /没有匹配的后台模块/)
})
```

**Step 2: Run test — confirm it fails**

Command: `cd frontend; node --test src/views/admin/adminStructure.test.js`

Expected: FAIL — `AdminView.vue` 仍内嵌菜单且没有检索、动态标题与说明。

**Step 3: Write minimal implementation**

创建 `adminMenu.js`，把 `AdminView.vue` 现有 `menuGroups` 原样迁出，并为每个 item 增加 `description`。完整元数据如下（`icon` 沿用现值）：

```js
export const menuGroups = [
  { title: '', items: [
    { key: 'dashboard', label: '概览', icon: 'common-home', description: '查看内容与运营概况' },
    { key: 'articles', label: '文章管理', icon: 'common-articlePages', description: '撰写、发布和维护文章' },
    { key: 'archive-categories', label: '分类管理', icon: 'common-archive', description: '维护文章归档分类' },
    { key: 'comments', label: '评论审核', icon: 'common-chat', description: '查看和处理访客评论' },
    { key: 'timeline-events', label: '时间线', icon: 'common-timeline', description: '编辑时间线事件' },
    { key: 'timeline-carousels', label: '时间线轮播', icon: 'common-history', description: '管理时间线配图内容' },
    { key: 'barrages', label: '树洞弹幕', icon: 'common-send', description: '管理树洞公开内容' },
    { key: 'called-texts', label: '疗愈文本', icon: 'common-paw', description: '编辑疗愈文字与音频' },
    { key: 'parallax-stories', label: '视差故事', icon: 'common-parallax', description: '维护视差页面故事' }
  ] },
  { title: '首页内容', items: [
    { key: 'site-home-landing', label: '首页内容', icon: 'common-home', description: '编辑首页标题、欢迎语和按钮' },
    { key: 'scenes', label: '首屏场景', icon: 'common-component', description: '配置首页首屏场景' },
    { key: 'collapse-cards', label: '内容卡片', icon: 'common-menu', description: '维护首页折叠卡片' },
    { key: 'team-members', label: '个人介绍', icon: 'common-person', description: '编辑个人资料与头像' },
    { key: 'site-archive-hero', label: '归档页', icon: 'common-tree', description: '编辑归档页头部内容' },
    { key: 'site-about', label: '关于页', icon: 'common-cat', description: '编辑关于页介绍内容' },
    { key: 'friend-links', label: '友情链接', icon: 'common-web', description: '维护友情链接与显示顺序' }
  ] },
  { title: '资源', items: [
    { key: 'media', label: '图片管理', icon: 'common-icons', description: '上传、裁切和整理图片' },
    { key: 'background-gallery', label: '背景图库', icon: 'common-parallax', description: '管理网站背景图片' },
    { key: 'musics', label: '音乐管理', icon: 'common-music', description: '维护音乐、封面和歌词' },
    { key: 'tool-sites', label: '工具站点', icon: 'common-tool', description: '编辑工具导航站点' },
    { key: 'bangumi-records', label: '番剧管理', icon: 'common-open', description: '导入和维护追番记录' }
  ] },
  { title: '系统设置', items: [
    { key: 'site-settings', label: '站点设置', icon: 'common-setting', description: '配置站点基础与 SEO 信息' },
    { key: 'appearance-settings', label: '外观设置', icon: 'common-component', description: '配置主题和显示偏好' },
    { key: 'nav-menu', label: '导航菜单', icon: 'common-menu', description: '编辑前台导航结构' },
    { key: 'page-content', label: '页面文案', icon: 'common-articlePages', description: '集中维护页面文字' }
  ] }
]
```

在 `AdminView.vue`：

```js
import { filterMenuGroups } from './adminUi'
import { menuGroups } from './adminMenu'

const menuQuery = ref('')
const filteredMenuGroups = computed(() => filterMenuGroups(menuGroups, menuQuery.value))
const allMenuItems = menuGroups.flatMap(group => group.items)
const currentMenu = computed(() =>
  allMenuItems.find(item => item.key === currentKey.value) || allMenuItems[0]
)
```

删除原内嵌 `menuGroups`，侧栏 `v-for` 改用 `filteredMenuGroups`。品牌下方、`nav` 上方加入：

```vue
<label class="admin-nav-search">
  <span class="sr-only">搜索后台模块</span>
  <SvgIcon name="common-search" size="15px" />
  <input v-model.trim="menuQuery" type="search" placeholder="搜索后台模块" />
</label>
<p v-if="filteredMenuGroups.length === 0" class="admin-nav-empty">没有匹配的后台模块</p>
```

顶栏固定文案替换为：

```vue
<p class="admin-topbar-title">{{ currentMenu.label }}</p>
<p class="admin-topbar-sub">{{ currentMenu.description }}</p>
```

在 `admin.css` 添加 `.admin-nav-search`、`.admin-nav-empty`，使用现有令牌；输入高度 36px，整行焦点使用 `:focus-within`。

**Step 4: Run test — confirm it passes**

Command: `cd frontend; node --test src/views/admin/adminUi.test.js src/views/admin/adminStructure.test.js`

Expected: PASS — 7 tests。

**Step 5: Commit**

```powershell
git add -- frontend/src/views/admin/adminMenu.js frontend/src/views/admin/AdminView.vue frontend/src/views/admin/adminStructure.test.js frontend/src/assets/css/admin.css
git diff --cached --check
git commit -m "feat: 添加后台模块上下文与菜单检索"
```

### Task 4: 为通用资源列表增加检索、统计、错误重试与表格增强

**Files:**
- Modify: `frontend/src/views/admin/ResourcePanel.vue`
- Modify: `frontend/src/views/admin/adminStructure.test.js`
- Modify: `frontend/src/assets/css/admin.css`

**Step 1: Write the failing test**

向 `adminStructure.test.js` 追加：

```js
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
```

**Step 2: Run test — confirm it fails**

Command: `cd frontend; node --test src/views/admin/adminStructure.test.js`

Expected: FAIL — 通用列表无本地搜索、面板错误与重试状态。

**Step 3: Write minimal implementation**

在 `ResourcePanel.vue` 导入 `filterResourceRows`，新增状态：

```js
const searchQuery = ref('')
const loadError = ref('')
const filteredRows = computed(() => filterResourceRows(rows.value, columns.value, searchQuery.value))
const totalPages = computed(() => Math.max(1, Math.ceil(filteredRows.value.length / pageSize.value)))
const pagedRows = computed(() =>
  filteredRows.value.slice((pageNo.value - 1) * pageSize.value, pageNo.value * pageSize.value)
)
```

监听搜索词后回到第一页：

```js
watch(searchQuery, () => {
  pageNo.value = 1
})
```

`load()` 开始时清空错误，失败时写入错误但保留 toast：

```js
loadError.value = ''
// catch 内
loadError.value = (err && err.message) || '加载失败，请稍后重试'
handleError(err, '加载失败')
```

工具栏标题下增加统计，操作区左侧增加搜索：

```vue
<div class="admin-toolbar-heading">
  <h2 class="admin-toolbar-title">{{ schema.label }}</h2>
  <p class="admin-toolbar-meta">
    {{ searchQuery ? `找到 ${filteredRows.length} / ${rows.length} 条` : `共 ${rows.length} 条记录` }}
  </p>
</div>
<label class="admin-list-search">
  <span class="sr-only">搜索{{ schema.label }}</span>
  <input v-model.trim="searchQuery" class="admin-input" type="search" placeholder="搜索当前列表" />
  <button v-if="searchQuery" type="button" aria-label="清空搜索" @click="searchQuery = ''">×</button>
</label>
```

表格卡片状态顺序替换为：

```vue
<div v-if="loading" class="admin-state" aria-live="polite">加载中…</div>
<div v-else-if="loadError" class="admin-state admin-state-error" role="alert">
  <strong>列表加载失败</strong>
  <span>{{ loadError }}</span>
  <button type="button" class="admin-btn admin-btn-ghost" @click="load">重新加载</button>
</div>
<div v-else-if="rows.length === 0" class="admin-state">暂无数据，点击右上角「新建」添加一条吧</div>
<div v-else-if="filteredRows.length === 0" class="admin-state">
  没有找到匹配“{{ searchQuery }}”的记录
  <button type="button" class="admin-link" @click="searchQuery = ''">清空搜索</button>
</div>
```

分页统计改用 `filteredRows.length`。在 `admin.css` 完整补充 `.admin-toolbar-heading`、`.admin-toolbar-meta`、`.admin-list-search`、`.admin-state-error`，并设置：

```css
.admin-table th { position: sticky; top: 0; z-index: 2; background: var(--adm-sticky-bg); }
.admin-col-ops { position: sticky; right: 0; background: var(--adm-card); }
```

**Step 4: Run test — confirm it passes**

Command: `cd frontend; node --test src/views/admin/adminUi.test.js src/views/admin/adminStructure.test.js`

Expected: PASS。

**Step 5: Commit only new hunks**

```powershell
git add -- frontend/src/views/admin/adminStructure.test.js frontend/src/assets/css/admin.css
git add -p -- frontend/src/views/admin/ResourcePanel.vue
git diff --cached --name-only
git diff --cached --check
git commit -m "feat: 完善后台资源列表检索与状态反馈"
```

### Task 5: 统一字段说明、必填状态和分组编辑

**Files:**
- Modify: `frontend/src/views/admin/FieldInput.vue`
- Modify: `frontend/src/views/admin/ResourcePanel.vue`
- Modify: `frontend/src/views/admin/resourceSchemas.js`
- Modify: `frontend/src/views/admin/resourceSchemas.test.js`
- Modify: `frontend/src/views/admin/adminStructure.test.js`
- Modify: `frontend/src/assets/css/admin.css`

**Step 1: Write the failing tests**

向 `resourceSchemas.test.js` 追加：

```js
test('字段可选元数据 group/tip/required 类型合法', () => {
  for (const schema of resourceSchemas) {
    for (const field of schema.fields) {
      if ('group' in field) assert.ok(isNonEmptyString(field.group), `[${schema.key}.${field.name}] group 必须为非空字符串`)
      if ('tip' in field) assert.ok(isNonEmptyString(field.tip), `[${schema.key}.${field.name}] tip 必须为非空字符串`)
      if ('required' in field) assert.equal(typeof field.required, 'boolean', `[${schema.key}.${field.name}] required 必须为布尔值`)
    }
  }
})
```

向 `adminStructure.test.js` 追加：

```js
test('字段组件和通用弹窗渲染说明、必填、错误及分组', async () => {
  const field = await read('./FieldInput.vue')
  const panel = await read('./ResourcePanel.vue')
  assert.match(field, /field\.required/)
  assert.match(field, /field\.tip/)
  assert.match(field, /aria-invalid/)
  assert.match(field, /aria-describedby/)
  assert.match(panel, /fieldGroups/)
  assert.match(panel, /admin-form-section/)
})
```

**Step 2: Run tests — confirm they fail**

Command: `cd frontend; node --test src/views/admin/resourceSchemas.test.js src/views/admin/adminStructure.test.js`

Expected: FAIL — `FieldInput` 与 `ResourcePanel` 尚未渲染元数据和分组。

**Step 3: Write minimal implementation**

`FieldInput.vue` 增加 `error` prop，使用 Vue 3.5 `useId()` 生成 `inputId`、`tipId`、`errorId`；所有原生输入绑定 `:id="inputId"`、`:required="field.required"`、`:aria-invalid="!!error"`、`:aria-describedby="describedBy"`。标签替换为：

```vue
<label class="admin-field-label" :for="inputId">
  {{ field.label }}
  <span v-if="field.required" class="admin-field-required" aria-hidden="true">*</span>
</label>
```

字段尾部统一加入：

```vue
<p v-if="field.tip" :id="tipId" class="admin-field-tip">{{ field.tip }}</p>
<p v-if="error" :id="errorId" class="admin-field-error" role="alert">{{ error }}</p>
```

布尔按钮补 `:aria-pressed="!!modelValue"`、`:aria-label="field.label"`、`:disabled="disabled"`；`AdminSelect`、图片、音频输入同步传递 `disabled`。

`ResourcePanel.vue` 导入 `groupFields` 并新增：

```js
const fieldGroups = computed(() => groupFields(props.schema.fields))
```

表单网格替换为分组结构：

```vue
<section v-for="group in fieldGroups" :key="group.key" class="admin-form-section">
  <header class="admin-form-section-head">
    <h4>{{ group.label }}</h4>
  </header>
  <div class="admin-form-grid">
    <template v-for="field in group.fields" :key="field.name">
      <FieldInput
        v-if="!(field.name === 'id' && editingId == null)"
        v-model="form[field.name]"
        :field="field"
        :disabled="field.name === 'id' && editingId != null"
        :class="fieldSpanClass(field)"
      />
    </template>
  </div>
</section>
```

在 `resourceSchemas.js` 先为高字段量、实际走通用面板的 `home-carousels`、`tool-sites`、`called-texts`、`musics`、`friend-links` 字段增加以下可选分组：

- 标题、名称、分类、标签：`group: '基本信息'`
- 图片、音频、正文、描述、歌词：`group: '内容与媒体'`
- `visible`、`featured`、`sortIndex`、状态：`group: '状态与排序'`
- `id`、`createdAt`、`updatedAt`：`group: '系统信息'`

为 URL、排序、标签和只读时间字段增加简短 `tip`；对站点名、标题、URL 等业务必填项标 `required: true`。元数据不得参与 `buildPayload()` 的键集合，因此不会改变 API 载荷。

在 `admin.css` 增加 `.admin-form-section`、`.admin-form-section-head`、`.admin-field-required`、`.admin-field-error`，并保证 section 之间只用一级边界，不嵌套卡片阴影。

**Step 4: Run tests — confirm they pass**

Command: `cd frontend; node --test src/views/admin/adminUi.test.js src/views/admin/resourceSchemas.test.js src/views/admin/adminStructure.test.js`

Expected: PASS。

**Step 5: Commit only new hunks**

```powershell
git add -- frontend/src/views/admin/FieldInput.vue frontend/src/views/admin/resourceSchemas.js frontend/src/views/admin/resourceSchemas.test.js frontend/src/views/admin/adminStructure.test.js frontend/src/assets/css/admin.css
git add -p -- frontend/src/views/admin/ResourcePanel.vue
git diff --cached --check
git commit -m "feat: 统一后台字段说明与分组编辑"
```

### Task 6: 增加未保存保护、快捷保存和保存区错误反馈

**Files:**
- Modify: `frontend/src/views/admin/ResourcePanel.vue`
- Modify: `frontend/src/views/admin/adminStructure.test.js`
- Modify: `frontend/src/assets/css/admin.css`

**Step 1: Write the failing test**

向 `adminStructure.test.js` 追加：

```js
test('通用编辑弹窗保护未保存内容并支持快捷保存', async () => {
  const source = await read('./ResourcePanel.vue')
  assert.match(source, /initialSnapshot/)
  assert.match(source, /isDirty/)
  assert.match(source, /window\.confirm\('当前修改尚未保存/)
  assert.match(source, /event\.(ctrlKey|metaKey)/)
  assert.match(source, /event\.key\.toLowerCase\(\) === 's'/)
  assert.match(source, /saveError/)
  assert.match(source, /role="alert"/)
})
```

**Step 2: Run test — confirm it fails**

Command: `cd frontend; node --test src/views/admin/adminStructure.test.js`

Expected: FAIL — 当前弹窗直接关闭且只通过 toast 报告保存失败。

**Step 3: Write minimal implementation**

`ResourcePanel.vue` 导入 `createFormSnapshot`、`isFormDirty`，并把 Vue import 扩展为 `onBeforeUnmount`。新增：

```js
const initialSnapshot = ref('')
const saveError = ref('')
const isDirty = computed(() =>
  drawerOpen.value && isFormDirty(form.value, props.schema.fields, initialSnapshot.value)
)

function resetSnapshot() {
  initialSnapshot.value = createFormSnapshot(form.value, props.schema.fields)
}
```

`openCreate()`、`openEdit()` 在 `form.value` 赋值后调用 `resetSnapshot()` 并清空 `saveError`。关闭逻辑替换为：

```js
function closeDrawer() {
  if (saving.value) return
  if (isDirty.value && !window.confirm('当前修改尚未保存，确定放弃并关闭吗？')) return
  drawerOpen.value = false
  saveError.value = ''
}
```

增加快捷键：

```js
function onEditorKeydown(event) {
  if (!drawerOpen.value || saving.value) return
  if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 's') {
    event.preventDefault()
    void onSave()
  }
}

onMounted(() => {
  window.addEventListener('keydown', onEditorKeydown)
  void load()
})
onBeforeUnmount(() => window.removeEventListener('keydown', onEditorKeydown))
```

删除原 `onMounted(load)`。`onSave()` 开头清空 `saveError`；成功后 `resetSnapshot()` 再关闭；失败时：

```js
saveError.value = (err && err.message) || '保存失败，请检查输入后重试'
handleError(err, '保存失败')
```

弹窗标题加入状态，关闭按钮补齐名称：

```vue
<span v-if="isDirty" class="admin-unsaved-mark">未保存</span>
<button class="admin-modal-close" type="button" aria-label="关闭编辑弹窗" @click="closeDrawer">×</button>
```

footer 改为：

```vue
<footer class="admin-modal-foot">
  <p class="admin-save-status" :class="{ error: saveError }" aria-live="polite">
    {{ saveError || (isDirty ? '有尚未保存的修改' : '当前内容已同步') }}
  </p>
  <div class="admin-modal-actions">
    <button type="button" class="admin-btn admin-btn-ghost" :disabled="saving" @click="closeDrawer">取消</button>
    <button type="button" class="admin-btn" :disabled="saving || !isDirty" @click="onSave">
      {{ saving ? '保存中…' : '保存 Ctrl+S' }}
    </button>
  </div>
</footer>
```

在 `admin.css` 添加 `.admin-unsaved-mark`、`.admin-save-status`、`.admin-modal-actions`，并让 `.admin-modal-foot` 使用 `flex-shrink: 0; background: var(--adm-sticky-bg); border-top: 1px solid var(--adm-border-soft)`。

**Step 4: Run tests — confirm they pass**

Command: `cd frontend; node --test src/views/admin/adminUi.test.js src/views/admin/adminStructure.test.js`

Expected: PASS。

**Step 5: Commit only new hunks**

```powershell
git add -- frontend/src/views/admin/adminStructure.test.js frontend/src/assets/css/admin.css
git add -p -- frontend/src/views/admin/ResourcePanel.vue
git diff --cached --check
git commit -m "feat: 保护后台未保存编辑内容"
```

### Task 7: 收敛专用面板的亮暗主题和编辑反馈

**Files:**
- Modify: `frontend/src/views/admin/ArticlesPanel.vue`
- Modify: `frontend/src/views/admin/BangumiPanel.vue`
- Modify: `frontend/src/views/admin/SiteContentPanel.vue`
- Modify: `frontend/src/views/admin/adminStructure.test.js`
- Modify: `frontend/src/assets/css/admin.css`

**Step 1: Write the failing test**

向 `adminStructure.test.js` 追加：

```js
test('专用后台面板不再使用硬编码白色背景', async () => {
  for (const file of ['./ArticlesPanel.vue', './BangumiPanel.vue', './SiteContentPanel.vue']) {
    const source = await read(file)
    assert.doesNotMatch(source, /background(?:-color)?:\s*#fff(?:fff)?\s*;/i, `${file} 含硬编码白色背景`)
  }
})

test('文章与站点内容保存区向用户显示当前状态', async () => {
  const articles = await read('./ArticlesPanel.vue')
  const siteContent = await read('./SiteContentPanel.vue')
  assert.match(articles, /aria-live="polite"/)
  assert.match(siteContent, /aria-live="polite"/)
})
```

**Step 2: Run test — confirm it fails**

Command: `cd frontend; node --test src/views/admin/adminStructure.test.js`

Expected: FAIL — `ArticlesPanel.vue` 与 `SiteContentPanel.vue` 仍有 `#fff` 背景，保存反馈仅依赖按钮或 toast。

**Step 3: Write minimal implementation**

- 将三个专用面板 scoped style 中的硬编码白色背景分别替换为 `var(--adm-card)`、`var(--adm-card-2)` 或 `var(--adm-input-bg)`；硬编码的白色文字可保留。
- 复用 `.admin-toolbar-heading` 与 `.admin-toolbar-meta`，标题下增加一句短说明，不再额外包一层卡片。
- `ArticlesPanel.vue` 与 `SiteContentPanel.vue` 增加 `saveStatus` ref：开始保存显示“正在保存…”，成功显示“已保存”，失败显示错误文本；紧邻保存按钮渲染 `<span class="admin-inline-status" aria-live="polite">{{ saveStatus }}</span>`。
- 保存失败必须保留表单；不得修改现有 payload 组装、上传、Bangumi 同步和 API 调用。
- `BangumiPanel.vue` 只统一令牌、标题说明和表单间距，不重写导入流程。
- 在 `admin.css` 增加 `.admin-inline-status` 和 `.admin-inline-status.error`。

**Step 4: Run tests — confirm they pass**

Command: `cd frontend; node --test src/views/admin/adminStructure.test.js`

Expected: PASS。

**Step 5: Commit with dirty-file protection**

```powershell
git add -- frontend/src/views/admin/ArticlesPanel.vue frontend/src/views/admin/SiteContentPanel.vue frontend/src/views/admin/adminStructure.test.js frontend/src/assets/css/admin.css
git add -p -- frontend/src/views/admin/BangumiPanel.vue
git diff --cached --check
git commit -m "style: 统一后台专用面板编辑反馈"
```

### Task 8: 全量验证与浏览器验收

**Files:**
- Modify only if verification exposes a regression in the files above.

**Step 1: Run focused tests**

Command:

```powershell
cd frontend
node --test src/views/admin/adminUi.test.js src/views/admin/resourceSchemas.test.js src/views/admin/adminStructure.test.js
```

Expected: PASS，全部后台测试通过。

**Step 2: Run required frontend validation**

Commands:

```powershell
cd frontend
npm run lint
npm test
npm run build
```

Expected: 三条命令均 exit code 0；Vite 构建成功，无 ESLint error 或测试失败。

**Step 3: Check whitespace and unintended scope**

Commands:

```powershell
git diff --check
git status --short
```

Expected: `git diff --check` 无输出；状态中只保留用户原有改动和本计划尚未提交的文档。

**Step 4: Run browser acceptance on `/admin`**

启动现有开发服务后，在亮色、暗色和 900px 以下视口依次验证：

1. 搜索“番剧”只显示对应菜单，清空后所有分组恢复；切换模块后顶栏标题和说明同步。
2. 通用资源列表搜索会更新数量和分页；无结果、无数据、加载失败分别显示不同状态，重试可用。
3. 表头滚动时吸顶，横向滚动时操作列固定；批量栏只在有选择时出现。
4. 高字段量资源按分组显示；字段说明、必填标记和键盘焦点清晰。
5. 修改字段后点击遮罩、关闭按钮或取消均出现未保存确认；取消确认后内容仍在。
6. `Ctrl+S` 保存一次且不会重复提交；保存失败时输入保留、错误显示在保存区。
7. 小屏弹窗占满可视区域，正文可滚动且底部操作栏始终可见。
8. 文章、番剧、站点内容面板在明暗主题下没有突兀白块。

**Step 5: Commit verification-only fixes if any**

若本任务没有产生修复，不创建空提交。若有修复，只暂存对应 hunk：

```powershell
git add -p -- <修复文件>
git diff --cached --check
git commit -m "fix: 修正后台统一升级回归问题"
```

最后重新运行 `npm run lint`、`npm test`、`npm run build` 和 `git diff --check`，以最新输出作为完成证据。
