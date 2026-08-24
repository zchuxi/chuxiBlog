<template>
  <section class="admin-panel">
    <!-- 顶部工具栏 -->
    <header class="admin-toolbar">
      <div class="admin-toolbar-heading">
        <h2 class="admin-toolbar-title">{{ schema.label }}</h2>
        <p class="admin-toolbar-meta">
          {{ searchQuery ? `找到 ${filteredRows.length} / ${rows.length} 条` : `共 ${rows.length} 条记录` }}
        </p>
      </div>
      <div class="admin-toolbar-actions">
        <div class="admin-list-search">
          <label class="sr-only" :for="`resource-search-${schema.key}`">搜索{{ schema.label }}</label>
          <input
            :id="`resource-search-${schema.key}`"
            v-model.trim="searchQuery"
            class="admin-input"
            type="search"
            placeholder="搜索当前列表"
          />
          <button
            v-if="searchQuery"
            type="button"
            aria-label="清空搜索"
            @click="searchQuery = ''"
          >×</button>
        </div>
        <CxButton plain :disabled="loading" @click="load">刷新</CxButton>
        <CxButton @click="openCreate">新建</CxButton>
      </div>
    </header>

    <!-- 批量操作条：选中行后浮现 -->
    <transition name="admin-fade">
      <div v-if="selected.size && !loadError" class="admin-batch-bar">
        <span class="admin-batch-count">已选 {{ selected.size }} 条</span>
        <template v-for="field in batchFields" :key="field.name">
          <AdminSelect
            v-if="field.type === 'select'"
            class="admin-batch-select"
            :model-value="''"
            :options="field.options"
            :placeholder="`批量改${shortLabel(field)}…`"
            :disabled="batching"
            @change="v => applyBatch(field, v)"
          />
          <AdminSelect
            v-else-if="field.type === 'boolean'"
            class="admin-batch-select"
            :model-value="''"
            :options="[{ label: '是', value: true }, { label: '否', value: false }]"
            :placeholder="`批量改${shortLabel(field)}…`"
            :disabled="batching"
            @change="v => applyBatch(field, v)"
          />
          <CxButton
            v-else
            plain
            :disabled="batching"
            @click="promptBatch(field)"
          >
            批量改{{ shortLabel(field) }}
          </CxButton>
        </template>
        <CxButton type="danger" :disabled="batching" @click="batchRemove">
          {{ batching ? '处理中…' : '批量删除' }}
        </CxButton>
        <button class="admin-link" :disabled="batching" @click="clearSelection">取消选择</button>
      </div>
    </transition>

    <!-- 数据表格 -->
    <div class="admin-table-card">
      <div v-if="loading" class="admin-state" aria-live="polite">加载中…</div>
      <div v-else-if="loadError" class="admin-state admin-state-error" role="alert">
        <strong>列表加载失败</strong>
        <span>{{ loadError }}</span>
        <CxButton plain @click="load">重新加载</CxButton>
      </div>
      <div v-else-if="rows.length === 0" class="admin-state">暂无数据，点击右上角「新建」添加一条吧</div>
      <div v-else-if="filteredRows.length === 0" class="admin-state">
        没有找到匹配“{{ searchQuery }}”的记录
        <button type="button" class="admin-link" @click="searchQuery = ''">清空搜索</button>
      </div>
      <div v-else class="admin-table-wrap">
        <table class="admin-table">
          <thead>
            <tr>
              <th class="admin-col-check">
                <input
                  type="checkbox"
                  class="admin-check"
                  :checked="pagedRows.length > 0 && pagedRows.every(r => selected.has(r.id))"
                  :indeterminate.prop="pagedRows.some(r => selected.has(r.id)) && !pagedRows.every(r => selected.has(r.id))"
                  @change="toggleAll"
                />
              </th>
              <th v-for="col in columns" :key="col.name" :aria-sort="ariaSort(col)">
                <span v-if="col.type === 'image'">{{ col.label }}</span>
                <button
                  v-else
                  type="button"
                  class="admin-th-sort"
                  :class="{ 'is-active': sortKey === col.name }"
                  :title="`按${col.label}排序`"
                  @click="toggleSort(col)"
                >
                  {{ col.label }}
                  <span class="admin-sort-mark" aria-hidden="true">{{
                    sortKey === col.name ? (sortDir === 'asc' ? '↑' : '↓') : '↕'
                  }}</span>
                </button>
              </th>
              <th class="admin-col-ops">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in pagedRows" :key="row.id" :class="{ 'is-checked': selected.has(row.id) }">
              <td class="admin-col-check">
                <input
                  type="checkbox"
                  class="admin-check"
                  :checked="selected.has(row.id)"
                  @change="toggleRow(row)"
                />
              </td>
              <td v-for="col in columns" :key="col.name">
                <img
                  v-if="col.type === 'image' && row[col.name]"
                  class="admin-cell-img"
                  :src="row[col.name]"
                  alt=""
                />
                <span v-else-if="col.type === 'boolean'" class="admin-badge" :class="{ off: !row[col.name] }">
                  {{ row[col.name] ? '是' : '否' }}
                </span>
                <span
                  v-else
                  class="admin-cell-text"
                  :class="{ 'is-num': col.type === 'number' || col.name === 'id' }"
                  :title="cellTitle(row[col.name])"
                >{{ cellText(row[col.name]) }}</span>
              </td>
              <td class="admin-col-ops">
                <button class="admin-link" @click="openEdit(row)">编辑</button>
                <button class="admin-link danger" @click="onRemove(row)">删除</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <!-- 分页栏：前端切页，仅多于一页时展示翻页按钮 -->
      <div v-if="!loading && !loadError && filteredRows.length > 0" class="admin-pager">
        <span class="admin-pager-info">共 {{ filteredRows.length }} 条 · 第 {{ pageNo }}/{{ totalPages }} 页</span>
        <div v-if="totalPages > 1" class="admin-pager-btns">
          <button class="admin-pager-btn" :disabled="pageNo === 1" @click="gotoPage(pageNo - 1)">上一页</button>
          <template v-for="(item, i) in pageItems" :key="i">
            <span v-if="item === '…'" class="admin-pager-ellipsis">…</span>
            <button
              v-else
              class="admin-pager-btn admin-pager-num"
              :class="{ 'is-active': item === pageNo }"
              @click="gotoPage(item)"
            >
              {{ item }}
            </button>
          </template>
          <button class="admin-pager-btn" :disabled="pageNo === totalPages" @click="gotoPage(pageNo + 1)">下一页</button>
        </div>
        <AdminSelect
          v-model="pageSize"
          class="admin-pager-size"
          :options="[
            { label: '10 条/页', value: 10 },
            { label: '20 条/页', value: 20 },
            { label: '50 条/页', value: 50 }
          ]"
        />
      </div>
    </div>

    <!-- 编辑弹窗：居中卡片 + 双列紧凑表单 -->
    <transition name="admin-fade">
      <div v-if="drawerOpen" class="admin-mask" @click.self="closeDrawer"></div>
    </transition>
    <transition name="admin-pop">
      <aside
        v-if="drawerOpen"
        class="admin-modal"
        :class="{ wide: schema.wide }"
        role="dialog"
        aria-modal="true"
        :aria-labelledby="`resource-editor-title-${schema.key}`"
      >
        <header class="admin-modal-head">
          <div class="admin-modal-title">
            <h3 :id="`resource-editor-title-${schema.key}`">
              {{ editingId == null ? '新建' : '编辑' }}{{ schema.label.replace(/管理$/, '') }}
            </h3>
            <span v-if="isDirty" class="admin-unsaved-mark">未保存</span>
          </div>
          <button
            class="admin-modal-close"
            type="button"
            aria-label="关闭编辑弹窗"
            :disabled="saving"
            @click="closeDrawer"
          >×</button>
        </header>
        <div class="admin-modal-body">
          <section v-for="group in fieldGroups" :key="group.key" class="admin-form-section">
            <header class="admin-form-section-head">
              <h4>{{ group.label }}</h4>
            </header>
            <div class="admin-form-grid">
              <FieldInput
                v-for="field in group.fields"
                :key="field.name"
                v-model="form[field.name]"
                :field="field"
                :disabled="saving || (field.name === 'id' && editingId != null)"
                :class="fieldSpanClass(field)"
              />
            </div>
          </section>
        </div>
        <footer class="admin-modal-foot">
          <p
            class="admin-save-status"
            :class="{ error: saveError }"
            :role="saveError ? 'alert' : 'status'"
            aria-live="polite"
            aria-atomic="true"
          >
            {{ saveError || (isDirty ? '有尚未保存的修改' : '当前内容已同步') }}
          </p>
          <div class="admin-modal-actions">
            <CxButton
              plain
              :disabled="saving"
              @click="closeDrawer"
            >取消</CxButton>
            <CxButton :disabled="saving || !isDirty" @click="onSave">
              {{ saving ? '保存中…' : '保存 Ctrl+S' }}
            </CxButton>
          </div>
        </footer>
      </aside>
    </transition>
  </section>
</template>

<script setup>
import { computed, inject, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { adminApi } from '../../api/admin'
import CxButton from '../../components/cx/CxButton.vue'
import FieldInput from './FieldInput.vue'
import AdminSelect from './AdminSelect.vue'
import { createFormSnapshot, filterResourceRows, groupFields, isFormDirty, sortResourceRows } from './adminUi'

// 宽字段独占整行，短字段两列并排——与番剧弹窗的紧凑排布一致
const FULL_ROW_TYPES = new Set(['textarea', 'markdown', 'image', 'audio'])
function fieldSpanClass(field) {
  return FULL_ROW_TYPES.has(field.type) || /title|name|summary|url|content/i.test(field.name)
    ? 'admin-field-full'
    : ''
}

const props = defineProps({
  schema: { type: Object, required: true }
})

const toast = inject('adminToast')
const onUnauthorized = inject('adminUnauthorized')

const rows = ref([])
const loading = ref(false)
const loadError = ref('')
const searchQuery = ref('')
const drawerOpen = ref(false)
const saving = ref(false)
const editingId = ref(null)
const form = ref({})
const initialSnapshot = ref('')
const saveError = ref('')
// 多选：选中行 id 集合；批量操作进行中标记
const selected = ref(new Set())
const batching = ref(false)
let latestRequestId = 0

const api = computed(() => adminApi[props.schema.key])
const columns = computed(() =>
  props.schema.columns.map(name => {
    const field = props.schema.fields.find(f => f.name === name)
    return field || { name, label: name === 'id' ? 'ID' : name, type: 'text' }
  })
)

// 前端分页：list 接口返回全量，这里切页展示
const pageNo = ref(1)
const pageSize = ref(10)
const filteredRows = computed(() => filterResourceRows(rows.value, columns.value, searchQuery.value))

// 点击表头排序：升 → 降 → 取消，取消后回到后端返回的原始顺序
const sortKey = ref('')
const sortDir = ref('')
const sortColumn = computed(() => columns.value.find(col => col.name === sortKey.value) || null)
const sortedRows = computed(() => sortResourceRows(filteredRows.value, sortColumn.value, sortDir.value))

function toggleSort(col) {
  // 图片列没有可比较的语义，不参与排序
  if (col.type === 'image') return
  if (sortKey.value !== col.name) {
    sortKey.value = col.name
    sortDir.value = 'asc'
  } else if (sortDir.value === 'asc') {
    sortDir.value = 'desc'
  } else {
    sortKey.value = ''
    sortDir.value = ''
  }
}

// aria-sort 只标注可排序列：图片列不可排序，报 none 会让读屏软件
// 把它当成「可排序但当前未排序」，返回 undefined 让 Vue 不渲染该属性
function ariaSort(col) {
  if (col.type === 'image') return undefined
  if (sortKey.value !== col.name) return 'none'
  return sortDir.value === 'asc' ? 'ascending' : 'descending'
}

const totalPages = computed(() => Math.max(1, Math.ceil(filteredRows.value.length / pageSize.value)))
const pagedRows = computed(() =>
  sortedRows.value.slice((pageNo.value - 1) * pageSize.value, pageNo.value * pageSize.value)
)
// 页码列表：总页数多时用省略号收敛（首页 + 当前页邻域 + 尾页）
const pageItems = computed(() => {
  const total = totalPages.value
  const cur = pageNo.value
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1)
  const items = [1]
  if (cur > 3) items.push('…')
  for (let p = Math.max(2, cur - 1); p <= Math.min(total - 1, cur + 1); p++) items.push(p)
  if (cur < total - 2) items.push('…')
  items.push(total)
  return items
})

function gotoPage(p) {
  pageNo.value = Math.min(Math.max(1, p), totalPages.value)
}

// 数据量变化（删除/改每页条数）后把当前页钉回合法范围
watch([totalPages], () => {
  if (pageNo.value > totalPages.value) pageNo.value = totalPages.value
})
watch(pageSize, () => {
  pageNo.value = 1
})
watch(searchQuery, () => {
  pageNo.value = 1
})
// 换排序后停留在第 3 页会看到不相干的记录，回到首页
watch([sortKey, sortDir], () => {
  pageNo.value = 1
})

// schema 中标记 batch: true 的字段参与批量修改（select/boolean 下拉，其余 prompt 输入）
const batchFields = computed(() => props.schema.fields.filter(f => f.batch))
const fieldGroups = computed(() => groupFields(props.schema.fields))
const isDirty = computed(() =>
  drawerOpen.value && isFormDirty(form.value, props.schema.fields, initialSnapshot.value)
)

function resetSnapshot() {
  initialSnapshot.value = createFormSnapshot(form.value, props.schema.fields)
}

// 「批量改状态」这类短标签：去掉 label 括号补充说明
function shortLabel(field) {
  return String(field.label || field.name).replace(/[（(].*$/, '')
}

// 统一错误处理：401 交给外层退出登录
function handleError(err, fallback) {
  if (err && err.unauthorized) {
    onUnauthorized && onUnauthorized()
    return
  }
  toast && toast((err && err.message) || fallback, 'error')
}

async function load() {
  const requestId = ++latestRequestId
  loading.value = true
  loadError.value = ''
  selected.value = new Set()
  try {
    const nextRows = (await api.value.list()) || []
    if (requestId !== latestRequestId) return
    rows.value = nextRows
    selected.value = new Set()
  } catch (err) {
    if (requestId !== latestRequestId) return
    selected.value = new Set()
    if (!(err && err.unauthorized)) {
      loadError.value = (err && err.message) || '加载失败，请稍后重试'
    }
    handleError(err, '加载失败')
  } finally {
    if (requestId === latestRequestId) loading.value = false
  }
}

// ---- 多选与批量操作 ----

function toggleRow(row) {
  const next = new Set(selected.value)
  if (next.has(row.id)) next.delete(row.id)
  else next.add(row.id)
  selected.value = next
}

// 表头全选作用于当前页；跨页已选的行保持选中状态
function toggleAll() {
  const next = new Set(selected.value)
  const allChecked = pagedRows.value.every(r => next.has(r.id))
  for (const r of pagedRows.value) {
    if (allChecked) next.delete(r.id)
    else next.add(r.id)
  }
  selected.value = next
}

function clearSelection() {
  selected.value = new Set()
}

// 后端 update 是整体替换，必须回传完整行数据再覆盖目标字段
async function applyBatch(field, value, e) {
  if (e && e.target) e.target.value = ''
  const targets = rows.value.filter(r => selected.value.has(r.id))
  if (!targets.length) return
  batching.value = true
  let ok = 0
  try {
    for (const row of targets) {
      await api.value.update(row.id, { ...row, [field.name]: value })
      ok += 1
    }
    toast && toast(`已将 ${ok} 条记录的${shortLabel(field)}改为「${field.type === 'boolean' ? (value ? '是' : '否') : value}」`)
    await load()
  } catch (err) {
    handleError(err, `批量修改失败（已成功 ${ok}/${targets.length} 条）`)
    await load()
  } finally {
    batching.value = false
  }
}

// number/text 类批量字段用 prompt 输入目标值
async function promptBatch(field) {
  const raw = window.prompt(`把选中的 ${selected.value.size} 条记录的「${shortLabel(field)}」改为：`)
  if (raw == null) return
  const value = field.type === 'number' ? (raw.trim() === '' ? null : Number(raw)) : raw.trim()
  if (field.type === 'number' && value != null && Number.isNaN(value)) {
    toast && toast('请输入数字', 'error')
    return
  }
  await applyBatch(field, value)
}

async function batchRemove() {
  const targets = rows.value.filter(r => selected.value.has(r.id))
  if (!targets.length) return
  if (!window.confirm(`确定删除选中的 ${targets.length} 条「${props.schema.label}」记录吗？此操作不可恢复`)) return
  batching.value = true
  let ok = 0
  try {
    for (const row of targets) {
      await api.value.remove(row.id)
      ok += 1
    }
    toast && toast(`已删除 ${ok} 条记录`)
    await load()
  } catch (err) {
    handleError(err, `批量删除失败（已成功 ${ok}/${targets.length} 条）`)
    await load()
  } finally {
    batching.value = false
  }
}

function cellText(value) {
  if (value == null || value === '') return '—'
  if (Array.isArray(value)) return value.join(', ')
  return String(value)
}

// 单元格最大 320px 且省略号截断，长内容原先无从查看。
// 只有可能被截断时才给 title，避免每个短单元格都挂无用的悬浮提示。
function cellTitle(value) {
  if (value == null || value === '') return undefined
  const text = Array.isArray(value) ? value.join(', ') : String(value)
  return text.length > 18 ? text : undefined
}

// 打开表单：编辑时把 tags 数组转成逗号字符串便于输入
function buildForm(row) {
  const model = {}
  for (const field of props.schema.fields) {
    const raw = row ? row[field.name] : undefined
    if (field.type === 'boolean') model[field.name] = raw == null ? field.default === true : !!raw
    else if (field.type === 'tags') model[field.name] = Array.isArray(raw) ? raw.join(', ') : raw || ''
    else model[field.name] = raw == null ? '' : raw
  }
  return model
}

function openCreate() {
  editingId.value = null
  form.value = buildForm(null)
  resetSnapshot()
  saveError.value = ''
  drawerOpen.value = true
}

function openEdit(row) {
  editingId.value = row.id
  form.value = buildForm(row)
  resetSnapshot()
  saveError.value = ''
  drawerOpen.value = true
}

function closeDrawer() {
  requestClose()
}

function requestClose() {
  if (!drawerOpen.value) return true
  if (saving.value) return false
  if (isDirty.value && !window.confirm('当前修改尚未保存，确定放弃并关闭吗？')) return false
  drawerOpen.value = false
  saveError.value = ''
  return true
}

defineExpose({ requestClose })

function hasOpenEditorOverlay() {
  return Boolean(document.querySelector('.adm-select-panel, .cx-date-picker__panel, .media-picker-mask, .crop-overlay'))
}

function onEditorKeydown(event) {
  if (!drawerOpen.value || saving.value) return
  const isSaveShortcut = (event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 's'
  if (hasOpenEditorOverlay() && (isSaveShortcut || event.key === 'Escape')) {
    event.preventDefault()
    event.stopPropagation()
    return
  }
  if (isSaveShortcut) {
    event.preventDefault()
    if (isDirty.value) void onSave()
    return
  }
  if (event.key === 'Escape') {
    event.preventDefault()
    closeDrawer()
  }
}

// 提交前按字段类型收敛值：tags → 数组，number → 数值，空串日期 → null
function buildPayload() {
  const payload = {}
  for (const field of props.schema.fields) {
    const raw = form.value[field.name]
    if (field.type === 'tags') {
      payload[field.name] = String(raw || '')
        .split(/[,，]/)
        .map(s => s.trim())
        .filter(Boolean)
    } else if (field.type === 'number') {
      payload[field.name] = raw === '' || raw == null ? null : Number(raw)
    } else if (field.type === 'datetime' || field.type === 'date') {
      payload[field.name] = raw === '' ? null : raw
    } else {
      payload[field.name] = raw
    }
  }
  // 兼容旧数据：存在 category/categoryId 字段时，按分类名自动维护 categoryId
  // （同名沿用已有 ID，新分类名取 max+1），无需人工填写
  if (payload.category != null && payload.category !== ''
    && props.schema.fields.some(f => f.name === 'categoryId')) {
    const match = rows.value.find(r => r.category === payload.category && r.categoryId != null)
    payload.categoryId = match
      ? Number(match.categoryId)
      : rows.value.reduce((max, r) => Math.max(max, Number(r.categoryId) || 0), 0) + 1
  }
  // 新建且未填 id 时交给服务端取 max(id)+1
  if (editingId.value == null && (payload.id == null || Number.isNaN(payload.id))) delete payload.id
  return payload
}

async function onSave() {
  if (saving.value || !isDirty.value) return
  saveError.value = ''
  const submittedSnapshot = createFormSnapshot(form.value, props.schema.fields)
  saving.value = true
  try {
    const payload = buildPayload()
    if (editingId.value == null) await api.value.create(payload)
    else await api.value.update(editingId.value, payload)
    const changedDuringSave = createFormSnapshot(form.value, props.schema.fields) !== submittedSnapshot
    initialSnapshot.value = submittedSnapshot
    toast && toast(changedDuringSave ? '已保存提交内容，保留后续修改' : '保存成功')
    if (!changedDuringSave) drawerOpen.value = false
    saveError.value = ''
    await load()
  } catch (err) {
    saveError.value = (err && err.message) || '保存失败，请检查输入后重试'
    handleError(err, '保存失败')
  } finally {
    saving.value = false
  }
}

async function onRemove(row) {
  if (!window.confirm(`确定删除这条「${props.schema.label}」记录（ID: ${row.id}）吗？`)) return
  try {
    await api.value.remove(row.id)
    toast && toast('删除成功')
    await load()
  } catch (err) {
    handleError(err, '删除失败')
  }
}

watch(
  () => props.schema.key,
  () => {
    drawerOpen.value = false
    rows.value = []
    searchQuery.value = ''
    selected.value = new Set()
    pageNo.value = 1
    // 列名在不同资源间不通用，排序状态必须跟着清空
    sortKey.value = ''
    sortDir.value = ''
    load()
  }
)

onMounted(() => {
  window.addEventListener('keydown', onEditorKeydown)
  void load()
})
onBeforeUnmount(() => window.removeEventListener('keydown', onEditorKeydown))
</script>
