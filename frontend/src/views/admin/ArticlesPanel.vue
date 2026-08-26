<template>
  <section class="ap-panel">
    <!-- ================= 列表态 ================= -->
    <template v-if="mode === 'list'">
      <header class="ap-toolbar">
        <h2 class="ap-toolbar-title">文章管理</h2>
        <div class="ap-filter">
          <button
            v-for="opt in FILTERS"
            :key="opt"
            type="button"
            class="ap-filter-btn"
            :class="{ active: filter === opt }"
            @click="filter = opt"
          >
            {{ opt }}
          </button>
        </div>
        <CxButton @click="openCreate">+ 写新文章</CxButton>
      </header>

      <!-- 批量操作条：选中后浮现（复用通用 admin-batch-bar 样式） -->
      <div v-if="selected.size" class="admin-batch-bar">
        <span class="admin-batch-count">已选 {{ selected.size }} 篇</span>
        <CxButton plain :disabled="batching" @click="applyBatchStatus('已发布')">设为已发布</CxButton>
        <CxButton plain :disabled="batching" @click="applyBatchStatus('草稿')">设为草稿</CxButton>
        <CxButton type="danger" :disabled="batching" @click="batchRemove">
          {{ batching ? '处理中…' : '批量删除' }}
        </CxButton>
        <button class="admin-link" type="button" :disabled="batching" @click="selected = new Set()">取消选择</button>
      </div>

      <div class="ap-card">
        <div v-if="loading" class="admin-state">加载中…</div>
        <div v-else-if="filteredRows.length === 0" class="admin-state">
          {{ rows.length === 0 ? '还没有文章，点击「+ 写新文章」开始创作吧' : '该状态下暂无文章' }}
        </div>
        <template v-else>
          <div class="ap-list-head">
            <label class="ap-select-all">
              <CxCheckbox
                size="small"
                :model-value="filteredRows.length > 0 && selected.size === filteredRows.length"
                :indeterminate="selected.size > 0 && selected.size < filteredRows.length"
                @change="toggleAll"
              />
              <span>全选</span>
            </label>
            <span class="ap-list-total">共 {{ filteredRows.length }} 篇</span>
          </div>
          <ul class="ap-list">
            <li v-for="row in filteredRows" :key="row.id" class="ap-row" :class="{ 'is-checked': selected.has(row.id) }">
              <CxCheckbox
                size="small"
                class="ap-row-check"
                :model-value="selected.has(row.id)"
                :aria-label="`选中《${row.title}》`"
                @change="toggleRow(row)"
              />
              <div class="ap-row-main">
              <div class="ap-row-title-line">
                <span v-if="row.pinned" class="ap-pin" title="置顶文章">★</span>
                <h3 class="ap-row-title">{{ row.title || '无标题' }}</h3>
                <span v-if="row.categoryName" class="ap-badge ap-badge-cat">{{ row.categoryName }}</span>
                <span class="ap-badge" :class="statusOf(row) === '草稿' ? 'ap-badge-draft' : 'ap-badge-pub'">
                  {{ statusOf(row) }}
                </span>
              </div>
              <p class="ap-row-meta">更新于 {{ fmtTime(row.updatedAt) }}</p>
            </div>
              <div class="ap-row-ops">
                <button class="admin-link" type="button" @click="openEdit(row)">✎ 编辑</button>
                <button class="admin-link danger" type="button" @click="removeOne(row)">删除</button>
              </div>
            </li>
          </ul>
        </template>
      </div>
    </template>

    <!-- ================= 编辑态（全页） ================= -->
    <template v-else>
      <header class="ap-editor-top">
        <CxButton plain @click="backToList">← 返回</CxButton>
        <h2 class="ap-editor-heading">{{ editingId == null ? '写新文章' : '编辑文章' }}</h2>
        <CxButton plain @click="showPreview = !showPreview">
          {{ showPreview ? '隐藏预览' : '显示预览' }}
        </CxButton>
        <CxButton :disabled="saving" @click="save">
          {{ saving ? '保存中…' : '保存' }}
        </CxButton>
      </header>

      <div class="ap-editor-grid" :class="{ 'no-preview': !showPreview }">
        <!-- 左列：表单 -->
        <div class="ap-card ap-editor-form">
          <div class="admin-field">
            <label class="admin-field-label">文章标题</label>
            <CxInput v-model="form.title" class="ap-title-input" variant="admin" placeholder="给这篇文章起个名字" />
          </div>

          <div class="admin-field">
            <label class="admin-field-label">摘要</label>
            <CxInput v-model="form.summary" type="textarea" variant="admin" :rows="3" placeholder="列表页展示的一小段介绍" />
          </div>

          <div class="ap-row-2col">
            <div class="admin-field">
              <label class="admin-field-label">分类</label>
              <AdminSelect
                v-model="form.categoryPick"
                class="ap-select"
                :options="[...categoryOptions, { label: '自定义…', value: CUSTOM_CATEGORY }]"
              />
              <CxInput
                v-if="form.categoryPick === CUSTOM_CATEGORY"
                v-model="form.customCategory"
                class="ap-custom-cat"
                variant="admin"
                placeholder="输入新分类名"
              />
            </div>
            <div class="admin-field">
              <label class="admin-field-label">状态</label>
              <AdminSelect v-model="form.status" class="ap-select" :options="['已发布', '草稿']" />
              <label class="ap-pin-check">
                <CxCheckbox v-model="form.pinned" size="small" />
                <span>置顶到首页文章列表</span>
              </label>
            </div>
          </div>

          <div class="admin-field">
            <label class="admin-field-label">封面图</label>
            <img v-if="form.coverUrl" class="ap-cover-preview" :src="form.coverUrl" alt="封面预览" />
            <CxInput v-model="form.coverUrl" variant="admin" placeholder="封面图片 URL" />
            <div class="ap-cover-actions">
              <CxButton plain :disabled="uploading" @click="fileEl && fileEl.click()">
                {{ uploading ? '上传中…' : '上传图片' }}
              </CxButton>
              <CxButton plain @click="pickerOpen = true">从图库选择</CxButton>
              <CxButton v-if="canCrop" plain :disabled="fetching" @click="openCrop">
                {{ fetching ? '取回中…' : '裁切' }}
              </CxButton>
            </div>
          </div>

          <div class="admin-field">
            <label class="admin-field-label">标签（输入后按回车添加）</label>
            <div class="ap-tags">
              <span v-for="(tag, i) in form.tags" :key="tag + i" class="ap-tag">
                {{ tag }}
                <button type="button" title="移除标签" @click="removeTag(i)">×</button>
              </span>
              <input
                v-model="tagInput"
                type="text"
                :placeholder="form.tags.length ? '' : '如 随笔、教程'"
                @keydown.enter.prevent="onTagEnter"
              />
            </div>
          </div>

          <div class="admin-field">
            <label class="admin-field-label">正文（Markdown）</label>
            <CxInput
              v-model="form.content"
              class="ap-content-input"
              type="textarea"
              variant="admin"
              placeholder="# 从这里开始写正文…"
            />
          </div>
        </div>

        <!-- 右列：实时预览 -->
        <div v-if="showPreview" class="ap-card ap-preview-card">
          <p class="ap-preview-label">实时预览</p>
          <h1 v-if="form.title" class="ap-preview-title">{{ form.title }}</h1>
          <div class="md-editor md-editor-previewOnly" :class="{ 'md-editor-dark': settings.isDark }">
            <div class="md-editor-preview default-theme md-editor-scrn" v-html="previewHtml"></div>
          </div>
        </div>
      </div>
    </template>

    <MediaPicker v-model="pickerOpen" @select="url => (form.coverUrl = url)" />
    <!-- 封面裁切：前台封面以 16:10 横版展示（首页文章卡 / 文章页头图），预设该比例 -->
    <CropDialog
      v-if="cropTarget"
      :item="cropTarget"
      :ratio="16 / 10"
      @close="cropTarget = null"
      @saved="onCropped"
    />
    <input ref="fileEl" type="file" accept="image/*" style="display: none" @change="onUploadFile" />
  </section>
</template>

<script setup>
import { computed, inject, onMounted, ref, watch } from 'vue'
import { adminApi, mediaApi } from '../../api/admin'
import { renderMarkdown } from '../../utils/markdown'
import { useSettingsStore } from '../../stores/settings'
import CxButton from '../../components/cx/CxButton.vue'
import CxCheckbox from '../../components/cx/CxCheckbox.vue'
import CxInput from '../../components/cx/CxInput.vue'
import MediaPicker from './MediaPicker.vue'
import CropDialog from './CropDialog.vue'
import AdminSelect from './AdminSelect.vue'

const props = defineProps({
  // true 时挂载后直接进入「写新文章」编辑态（供概览页快捷操作使用）
  initialCreate: { type: Boolean, default: false }
})

const api = adminApi.articles
const toast = inject('adminToast', () => {})
const onUnauthorized = inject('adminUnauthorized', () => {})
const settings = useSettingsStore()

const FILTERS = ['全部', '已发布', '草稿']
const CUSTOM_CATEGORY = '__custom__'

const mode = ref('list') // list | edit
const rows = ref([])
const loading = ref(false)
const filter = ref('全部')
// 多选：选中文章 id 集合；批量操作进行中标记
const selected = ref(new Set())
const batching = ref(false)

const editingId = ref(null)
const saving = ref(false)
const showPreview = ref(true)
const form = ref(emptyForm())
const tagInput = ref('')
// 编辑时透传但不在表单中的字段（完整回填，避免整体覆盖丢数据）
let carry = {}

const pickerOpen = ref(false)
const uploading = ref(false)
const fileEl = ref(null)
const cropTarget = ref(null)
// 取回外链中的 loading：拉远端图转副本期间防止重复点「裁切」
const fetching = ref(false)

// 站内图（/api/uploads/）与外链 http(s) 图均可裁：站内直接打开，外链先让后端取回转本地副本再裁
const canCrop = computed(
  () => typeof form.value.coverUrl === 'string'
    && (form.value.coverUrl.startsWith('/api/uploads/') || /^https?:\/\//.test(form.value.coverUrl))
)

async function openCrop() {
  const raw = form.value.coverUrl
  const name = decodeURIComponent((raw.split('?')[0].split('/').pop() || ''))
  if (!name) return
  if (raw.startsWith('/api/uploads/')) {
    cropTarget.value = { name, url: raw }
    return
  }
  // 外链：canvas 跨域会被污染无法直接裁，先让后端下载到站内并回填
  fetching.value = true
  try {
    const data = await mediaApi.fetch(raw)
    if (data && data.url) {
      form.value.coverUrl = data.url
      cropTarget.value = { name: data.name || name, url: data.url }
    }
  } catch (err) {
    toast((err && err.message) || '取回失败，请稍后重试', 'error')
  } finally {
    fetching.value = false
  }
}

// 裁切保存为新图后直接回填封面字段
function onCropped(data) {
  if (data && data.url) {
    form.value.coverUrl = data.url
    toast('裁切完成，已回填新图')
  }
}

// ---- 工具 ----

// 旧数据 status 可能为空，一律按已发布处理
function statusOf(row) {
  return row.status || '已发布'
}

function fmtTime(v) {
  if (!v) return '—'
  return String(v).replace('T', ' ').slice(0, 16)
}

function nowStr() {
  const d = new Date()
  const p = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

function emptyForm() {
  return {
    title: '',
    summary: '',
    content: '',
    coverUrl: '',
    categoryPick: '',
    customCategory: '',
    status: '已发布',
    pinned: false,
    tags: []
  }
}

function handleError(err, fallback) {
  if (err && err.unauthorized) {
    onUnauthorized && onUnauthorized()
    return
  }
  toast((err && err.message) || fallback, 'error')
}

// ---- 列表 ----

const filteredRows = computed(() => {
  const list = filter.value === '全部' ? rows.value : rows.value.filter(r => statusOf(r) === filter.value)
  // 置顶优先，再按更新时间倒序（与前台首页一致）
  return [...list].sort((a, b) => {
    const pa = a.pinned ? 1 : 0
    const pb = b.pinned ? 1 : 0
    if (pa !== pb) return pb - pa
    return String(b.updatedAt || '').localeCompare(String(a.updatedAt || ''))
  })
})

async function load() {
  loading.value = true
  try {
    rows.value = (await api.list()) || []
    selected.value = new Set()
  } catch (err) {
    handleError(err, '加载失败')
  } finally {
    loading.value = false
  }
}

// ---- 多选与批量操作 ----

// 切换筛选时清空选择，避免跨状态误操作不可见行
watch(filter, () => {
  selected.value = new Set()
})

function toggleRow(row) {
  const next = new Set(selected.value)
  if (next.has(row.id)) next.delete(row.id)
  else next.add(row.id)
  selected.value = next
}

function toggleAll() {
  selected.value =
    selected.value.size === filteredRows.value.length ? new Set() : new Set(filteredRows.value.map(r => r.id))
}

// 后端 update 是整体替换，回传完整行再覆盖 status
async function applyBatchStatus(status) {
  const targets = rows.value.filter(r => selected.value.has(r.id))
  if (!targets.length) return
  batching.value = true
  let ok = 0
  try {
    for (const row of targets) {
      await api.update(row.id, { ...row, status })
      ok += 1
    }
    toast(`已将 ${ok} 篇文章设为「${status}」`)
    await load()
  } catch (err) {
    handleError(err, `批量修改失败（已成功 ${ok}/${targets.length} 篇）`)
    await load()
  } finally {
    batching.value = false
  }
}

async function batchRemove() {
  const targets = rows.value.filter(r => selected.value.has(r.id))
  if (!targets.length) return
  if (!window.confirm(`确定删除选中的 ${targets.length} 篇文章吗？其评论会一并删除`)) return
  batching.value = true
  let ok = 0
  try {
    for (const row of targets) {
      await api.remove(row.id)
      ok += 1
    }
    toast(`已删除 ${ok} 篇文章`)
    await load()
  } catch (err) {
    handleError(err, `批量删除失败（已成功 ${ok}/${targets.length} 篇）`)
    await load()
  } finally {
    batching.value = false
  }
}

// ---- 分类：现有文章 categoryName 去重 + 自定义 ----

const categoryOptions = computed(() => {
  const names = []
  for (const r of rows.value) {
    if (r.categoryName && !names.includes(r.categoryName)) names.push(r.categoryName)
  }
  // 编辑旧文章时其分类可能已不在现有列表（被筛选前）中，兜底加入
  if (form.value.categoryPick && form.value.categoryPick !== CUSTOM_CATEGORY && !names.includes(form.value.categoryPick)) {
    names.push(form.value.categoryPick)
  }
  return names
})

// 分类名 -> categoryId：同名沿用；新分类取 max+1
function resolveCategory() {
  const name =
    form.value.categoryPick === CUSTOM_CATEGORY
      ? String(form.value.customCategory || '').trim()
      : String(form.value.categoryPick || '').trim()
  if (!name) return { categoryId: null, categoryName: '' }
  const hit = rows.value.find(r => r.categoryName === name && r.categoryId != null)
  if (hit) return { categoryId: hit.categoryId, categoryName: name }
  const maxId = rows.value.reduce((max, r) => Math.max(max, r.categoryId == null ? 0 : Number(r.categoryId)), 0)
  return { categoryId: maxId + 1, categoryName: name }
}

// ---- 编辑态 ----

function openCreate() {
  editingId.value = null
  carry = {}
  form.value = emptyForm()
  form.value.categoryPick = categoryOptions.value[0] || CUSTOM_CATEGORY
  tagInput.value = ''
  mode.value = 'edit'
}

function openEdit(row) {
  editingId.value = row.id
  carry = {
    mood: row.mood == null ? null : row.mood,
    publishedAt: row.publishedAt || null,
    createdAt: row.createdAt || null
  }
  form.value = {
    title: row.title || '',
    summary: row.summary || '',
    content: row.content || '',
    coverUrl: row.coverUrl || '',
    categoryPick: row.categoryName || CUSTOM_CATEGORY,
    customCategory: '',
    status: statusOf(row),
    pinned: !!row.pinned,
    tags: Array.isArray(row.tags) ? [...row.tags] : []
  }
  tagInput.value = ''
  mode.value = 'edit'
}

function backToList() {
  mode.value = 'list'
  load()
}

// ---- 标签 chips ----

function onTagEnter(e) {
  if (e.isComposing) return
  const t = tagInput.value.trim()
  if (!t) return
  if (!form.value.tags.includes(t)) form.value.tags.push(t)
  tagInput.value = ''
}

function removeTag(i) {
  form.value.tags.splice(i, 1)
}

// ---- 预览 ----

const previewHtml = computed(() => {
  try {
    return renderMarkdown(form.value.content || '').html
  } catch {
    return '<p>预览渲染失败</p>'
  }
})

// ---- 保存 ----

async function save() {
  const f = form.value
  if (!String(f.title || '').trim()) {
    toast('请先填写文章标题', 'error')
    return
  }
  const { categoryId, categoryName } = resolveCategory()
  if (!categoryName) {
    toast('请选择或输入一个分类', 'error')
    return
  }
  // 阅读时长：按正文字数粗算，每 400 字约 1 分钟
  const words = String(f.content || '').length
  const readingTime = `${Math.max(1, Math.ceil(words / 400))} 分钟`

  const payload = {
    title: f.title.trim(),
    summary: f.summary || '',
    content: f.content || '',
    coverUrl: f.coverUrl || '',
    categoryId,
    categoryName,
    archiveCategory: categoryName,
    tags: [...f.tags],
    status: f.status || '已发布',
    pinned: !!f.pinned,
    readingTime,
    mood: carry.mood == null ? null : carry.mood,
    publishedAt: editingId.value == null ? nowStr() : carry.publishedAt || nowStr(),
    createdAt: carry.createdAt || null
  }

  saving.value = true
  try {
    if (editingId.value == null) await api.create(payload)
    else await api.update(editingId.value, { ...payload, id: editingId.value })
    toast('保存成功')
    mode.value = 'list'
    await load()
  } catch (err) {
    handleError(err, '保存失败')
  } finally {
    saving.value = false
  }
}

// ---- 删除 ----

async function removeOne(row) {
  if (!window.confirm(`确定删除文章「${row.title || row.id}」吗？其评论会一并删除`)) return
  try {
    await api.remove(row.id)
    toast('删除成功')
    await load()
  } catch (err) {
    handleError(err, '删除失败')
  }
}

// ---- 封面上传 ----

async function onUploadFile(e) {
  const file = e.target.files && e.target.files[0]
  e.target.value = ''
  if (!file) return
  uploading.value = true
  try {
    const data = await mediaApi.upload(file)
    if (data && data.url) form.value.coverUrl = data.url
    toast('上传成功')
  } catch (err) {
    handleError(err, '上传失败')
  } finally {
    uploading.value = false
  }
}

onMounted(async () => {
  await load()
  if (props.initialCreate) openCreate()
})
</script>

<style>
/* ---------- 文章管理面板（ap-*，明暗双主题） ---------- */

.ap-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.ap-toolbar {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 20px;
  background-color: #fff;
  border-radius: 18px;
  box-shadow: 0 8px 24px rgba(63, 119, 181, 0.08);
}

.ap-toolbar-title {
  flex: 1;
  margin: 0;
  font-size: 20px;
  color: #3f77b5;
}

.ap-filter {
  display: flex;
  gap: 6px;
}

.ap-filter-btn {
  padding: 5px 14px;
  border-radius: 999px;
  border: 1px solid rgba(63, 119, 181, 0.25);
  background-color: transparent;
  color: rgba(31, 49, 72, 0.6);
  font-family: inherit;
  font-size: 14.5px;
  cursor: pointer;
  transition: background-color 0.2s ease, color 0.2s ease;
}

.ap-filter-btn.active {
  background-color: #3f77b5;
  border-color: #3f77b5;
  color: #fff;
}

.ap-card {
  background-color: #fff;
  border-radius: 18px;
  box-shadow: 0 8px 24px rgba(63, 119, 181, 0.08);
  padding: 12px 18px;
}

.ap-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

/* 列表头：全选 + 总数 */
.ap-list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 6px;
  border-bottom: 1px dashed rgba(63, 119, 181, 0.15);
}

.ap-select-all {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: rgba(31, 49, 72, 0.6);
  cursor: pointer;
}

.ap-list-total {
  font-size: 13px;
  color: rgba(31, 49, 72, 0.45);
}

.ap-row-check {
  flex: none;
}

.ap-row.is-checked {
  background-color: rgba(63, 119, 181, 0.07);
  border-radius: 12px;
}

.ap-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 6px;
  border-bottom: 1px dashed rgba(63, 119, 181, 0.15);
}

.ap-row:last-child {
  border-bottom: none;
}

.ap-row-main {
  flex: 1;
  min-width: 0;
}

.ap-row-title-line {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.ap-pin {
  color: #e5a33c;
  font-size: 15.5px;
  flex: none;
}

.ap-row-title {
  margin: 0;
  font-size: 16.5px;
  color: #1f3148;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ap-badge {
  flex: none;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 13px;
}

.ap-badge-cat {
  background-color: rgba(63, 119, 181, 0.12);
  color: #3f77b5;
}

.ap-badge-pub {
  background-color: rgba(76, 154, 98, 0.14);
  color: #4c9a62;
}

.ap-badge-draft {
  background-color: rgba(229, 163, 60, 0.16);
  color: #c98a2e;
}

.ap-row-meta {
  margin: 4px 0 0;
  font-size: 13px;
  color: rgba(31, 49, 72, 0.5);
}

.ap-row-ops {
  flex: none;
  display: flex;
  gap: 2px;
}

/* ---------- 编辑态 ---------- */

.ap-editor-top {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 18px;
  background-color: #fff;
  border-radius: 16px;
  box-shadow: 0 8px 24px rgba(63, 119, 181, 0.08);
}

.ap-editor-heading {
  flex: 1;
  margin: 0;
  font-size: 17.5px;
  color: #3f77b5;
}

.ap-editor-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.ap-editor-grid.no-preview {
  grid-template-columns: minmax(0, 1fr);
}

/* 类名落在 CxInput 根 div 上，字号与内边距要写到内层 input 才生效 */
.ap-title-input .cx-input__inner {
  font-size: 20px;
  padding: 12px 14px;
}

.ap-select {
  appearance: auto;
  cursor: pointer;
}

.ap-custom-cat {
  margin-top: 8px;
}

.ap-pin-check {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
  font-size: 14.5px;
  cursor: pointer;
}

/* ap-check 已统一使用 CxCheckbox（size="small"） */

.ap-cover-preview {
  display: block;
  width: 100%;
  max-height: 160px;
  object-fit: cover;
  border-radius: 12px;
  border: 1px solid rgba(63, 119, 181, 0.2);
  margin-bottom: 8px;
}

.ap-cover-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.ap-tags {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border: 1px solid rgba(63, 119, 181, 0.3);
  border-radius: 12px;
  background-color: #fff;
}

.ap-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border-radius: 999px;
  background-color: rgba(63, 119, 181, 0.12);
  color: #3f77b5;
  font-size: 13px;
}

.ap-tag button {
  border: none;
  background: none;
  padding: 0;
  color: inherit;
  font-size: 14.5px;
  line-height: 1;
  cursor: pointer;
}

.ap-tags input {
  flex: 1;
  min-width: 110px;
  border: none;
  outline: none;
  background: transparent;
  font-family: inherit;
  font-size: 14.5px;
  color: inherit;
  padding: 4px 0;
}

/* 同理：定高作用在内层 textarea 上。
   选择器必须带 .admin-root 前缀提特异性：打包后 admin.css 排在组件样式之后，
   同优先级的 .cx-input--admin .cx-input__textarea（min-height:72px）会把本规则盖掉，
   正文框只剩三行高。 */
.admin-root .ap-content-input .cx-input__textarea {
  min-height: 62vh;
  font-size: 15px;
  line-height: 1.85;
  resize: vertical;
}

.ap-preview-card {
  position: sticky;
  top: 16px;
  max-height: calc(100vh - 120px);
  overflow-y: auto;
}

.ap-preview-label {
  margin: 4px 2px 10px;
  font-size: 13px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #8aa5c4;
}

.ap-preview-title {
  margin: 0 0 14px;
  font-size: 24px;
  color: #1f3148;
}

@media (max-width: 960px) {
  .ap-editor-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .ap-preview-card {
    position: static;
    max-height: none;
  }
}

@media (max-width: 720px) {
  .ap-toolbar {
    flex-wrap: wrap;
  }

  .ap-toolbar-title {
    flex: 1 1 100%;
  }
}

/* ---------- 暗色 ---------- */

html.dark .ap-toolbar,
html.dark .ap-card,
html.dark .ap-editor-top {
  background: rgba(28, 36, 52, 0.92);
  box-shadow: 0 18px 44px rgba(0, 0, 0, 0.42);
}

html.dark .ap-toolbar-title,
html.dark .ap-editor-heading {
  color: #e8effa;
}

html.dark .ap-row {
  border-color: rgba(255, 255, 255, 0.06);
}

html.dark .ap-list-head {
  border-color: rgba(255, 255, 255, 0.06);
}

html.dark .ap-select-all {
  color: #b9c6da;
}

html.dark .ap-list-total {
  color: #8fa0ba;
}

html.dark .ap-row.is-checked {
  background-color: rgba(95, 149, 207, 0.12);
}

html.dark .ap-row-title,
html.dark .ap-preview-title {
  color: #dbe4f0;
}

html.dark .ap-row-meta {
  color: #8fa0ba;
}

html.dark .ap-filter-btn {
  border-color: rgba(255, 255, 255, 0.14);
  color: #b9c6da;
}

html.dark .ap-filter-btn.active {
  background: #3f77b5;
  border-color: #3f77b5;
  color: #f2f7fd;
}

html.dark .ap-badge-cat {
  background: rgba(95, 149, 207, 0.18);
  color: #a8cdf0;
}

html.dark .ap-badge-pub {
  background: rgba(76, 154, 98, 0.2);
  color: #8fd0a3;
}

html.dark .ap-badge-draft {
  background: rgba(229, 163, 60, 0.2);
  color: #e8bd77;
}

html.dark .ap-tags {
  background: rgba(16, 22, 34, 0.85);
  border-color: rgba(255, 255, 255, 0.12);
}

html.dark .ap-tag {
  background: rgba(95, 149, 207, 0.18);
  color: #a8cdf0;
}

html.dark .ap-tags input {
  color: #e2eaf5;
}

html.dark .ap-pin-check {
  color: #c3d0e2;
}

html.dark .ap-cover-preview {
  border-color: rgba(255, 255, 255, 0.1);
}

/* ---------- 移动端适配（≤900px，追加） ---------- */

@media (max-width: 900px) {
  /* 列表工具栏：标题独占一行，筛选与新建换行排布 */
  .ap-toolbar {
    flex-wrap: wrap;
    gap: 10px;
    padding: 12px 16px;
  }

  .ap-toolbar-title {
    flex: 1 1 100%;
  }

  .ap-filter {
    flex-wrap: wrap;
  }

  .ap-filter-btn {
    padding: 8px 15px;
  }

  /* 列表行允许换行，操作按钮不挤压标题 */
  .ap-row {
    flex-wrap: wrap;
    gap: 6px 12px;
  }

  .ap-row-main {
    flex: 1 1 220px;
  }

  .ap-row-ops {
    margin-left: auto;
  }

  /* 编辑态：顶部条换行（保留「隐藏预览」开关），左右两列已由 960px 规则改上下堆叠，预览在下 */
  .ap-editor-top {
    flex-wrap: wrap;
    gap: 8px;
    padding: 10px 14px;
  }

  .ap-editor-heading {
    flex: 1 1 auto;
  }

  .admin-root .ap-content-input .cx-input__textarea {
    min-height: 46vh;
  }

  .ap-row-2col {
    display: block;
  }
}
</style>
