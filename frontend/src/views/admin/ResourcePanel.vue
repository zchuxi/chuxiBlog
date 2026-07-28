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

    <!-- 数据表格 -->
    <div class="admin-table-card">
      <div v-if="loading" class="admin-state">加载中…</div>
      <div v-else-if="rows.length === 0" class="admin-state">暂无数据，点击右上角「新建」添加一条吧</div>
      <div v-else class="admin-table-wrap">
        <table class="admin-table">
          <thead>
            <tr>
              <th v-for="col in columns" :key="col.name">{{ col.label }}</th>
              <th class="admin-col-ops">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.id">
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

// 宽字段独占整行，短字段两列并排——与番剧弹窗的紧凑排布一致
const FULL_ROW_TYPES = new Set(['textarea', 'markdown', 'image'])
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

const api = computed(() => adminApi[props.schema.key])
const columns = computed(() =>
  props.schema.columns.map(name => {
    const field = props.schema.fields.find(f => f.name === name)
    return field || { name, label: name === 'id' ? 'ID' : name, type: 'text' }
  })
)

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
  } catch (err) {
    handleError(err, '加载失败')
  } finally {
    loading.value = false
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
    if (field.type === 'boolean') model[field.name] = raw == null ? false : !!raw
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
    } else if (field.type === 'datetime') {
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
    load()
  }
)

onMounted(load)
</script>
