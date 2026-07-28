<template>
  <section class="admin-panel">
    <!-- 顶部工具栏 -->
    <header class="admin-toolbar">
      <h2 class="admin-toolbar-title">{{ schema.label }}</h2>
      <div class="admin-toolbar-actions">
        <button class="admin-btn admin-btn-ghost" :disabled="loading" @click="load">刷新</button>
        <button class="admin-btn" @click="openCreate">新建</button>
      </div>
    </header>

    <!-- 批量操作条：选中行后浮现 -->
    <transition name="admin-fade">
      <div v-if="selected.size" class="admin-batch-bar">
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
          <button
            v-else
            class="admin-btn admin-btn-ghost"
            :disabled="batching"
            @click="promptBatch(field)"
          >
            批量改{{ shortLabel(field) }}
          </button>
        </template>
        <button class="admin-btn admin-btn-danger" :disabled="batching" @click="batchRemove">
          {{ batching ? '处理中…' : '批量删除' }}
        </button>
        <button class="admin-link" :disabled="batching" @click="clearSelection">取消选择</button>
      </div>
    </transition>

    <!-- 数据表格 -->
    <div class="admin-table-card">
      <div v-if="loading" class="admin-state">加载中…</div>
      <div v-else-if="rows.length === 0" class="admin-state">暂无数据，点击右上角「新建」添加一条吧</div>
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
              <th v-for="col in columns" :key="col.name">{{ col.label }}</th>
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
                <span v-else class="admin-cell-text">{{ cellText(row[col.name]) }}</span>
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
      <div v-if="!loading && rows.length > 0" class="admin-pager">
        <span class="admin-pager-info">共 {{ rows.length }} 条 · 第 {{ pageNo }}/{{ totalPages }} 页</span>
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
      <aside v-if="drawerOpen" class="admin-modal" :class="{ wide: schema.wide }">
        <header class="admin-modal-head">
          <h3>{{ editingId == null ? '新建' : '编辑' }}{{ schema.label.replace(/管理$/, '') }}</h3>
          <button class="admin-modal-close" @click="closeDrawer">×</button>
        </header>
        <div class="admin-modal-body">
          <div class="admin-form-grid">
            <FieldInput
              v-for="field in schema.fields"
              :key="field.name"
              v-model="form[field.name]"
              :field="field"
              :disabled="field.name === 'id' && editingId != null"
              :class="fieldSpanClass(field)"
            />
          </div>
        </div>
        <footer class="admin-modal-foot">
          <button class="admin-btn admin-btn-ghost" @click="closeDrawer">取消</button>
          <button class="admin-btn" :disabled="saving" @click="onSave">{{ saving ? '保存中…' : '保存' }}</button>
        </footer>
      </aside>
    </transition>
  </section>
</template>

<script setup>
import { computed, inject, onMounted, ref, watch } from 'vue'
import { adminApi } from '../../api/admin'
import FieldInput from './FieldInput.vue'
import AdminSelect from './AdminSelect.vue'

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
const drawerOpen = ref(false)
const saving = ref(false)
const editingId = ref(null)
const form = ref({})
// 多选：选中行 id 集合；批量操作进行中标记
const selected = ref(new Set())
const batching = ref(false)

// 前端分页：list 接口返回全量，这里切页展示
const pageNo = ref(1)
const pageSize = ref(10)
const totalPages = computed(() => Math.max(1, Math.ceil(rows.value.length / pageSize.value)))
const pagedRows = computed(() =>
  rows.value.slice((pageNo.value - 1) * pageSize.value, pageNo.value * pageSize.value)
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

const api = computed(() => adminApi[props.schema.key])
const columns = computed(() =>
  props.schema.columns.map(name => {
    const field = props.schema.fields.find(f => f.name === name)
    return field || { name, label: name === 'id' ? 'ID' : name, type: 'text' }
  })
)
// schema 中标记 batch: true 的字段参与批量修改（select/boolean 下拉，其余 prompt 输入）
const batchFields = computed(() => props.schema.fields.filter(f => f.batch))

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
  loading.value = true
  try {
    rows.value = (await api.value.list()) || []
    selected.value = new Set()
  } catch (err) {
    handleError(err, '加载失败')
  } finally {
    loading.value = false
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
  drawerOpen.value = true
}

function openEdit(row) {
  editingId.value = row.id
  form.value = buildForm(row)
  drawerOpen.value = true
}

function closeDrawer() {
  drawerOpen.value = false
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
  // 新建且未填 id 时交给服务端取 max(id)+1
  if (editingId.value == null && (payload.id == null || Number.isNaN(payload.id))) delete payload.id
  return payload
}

async function onSave() {
  saving.value = true
  try {
    const payload = buildPayload()
    if (editingId.value == null) await api.value.create(payload)
    else await api.value.update(editingId.value, payload)
    toast && toast('保存成功')
    drawerOpen.value = false
    await load()
  } catch (err) {
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
    pageNo.value = 1
    load()
  }
)

onMounted(load)
</script>
