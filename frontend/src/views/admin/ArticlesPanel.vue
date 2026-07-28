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
        <button class="admin-btn" type="button" @click="openCreate">+ 写新文章</button>
      </header>

      <div class="ap-card">
        <div v-if="loading" class="admin-state">加载中…</div>
        <div v-else-if="filteredRows.length === 0" class="admin-state">
          {{ rows.length === 0 ? '还没有文章，点击「+ 写新文章」开始创作吧' : '该状态下暂无文章' }}
        </div>
        <ul v-else class="ap-list">
          <li v-for="row in filteredRows" :key="row.id" class="ap-row">
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
      </div>
    </template>

    <!-- ================= 编辑态（全页） ================= -->
    <template v-else>
      <header class="ap-editor-top">
        <button class="admin-btn admin-btn-ghost" type="button" @click="backToList">← 返回</button>
        <h2 class="ap-editor-heading">{{ editingId == null ? '写新文章' : '编辑文章' }}</h2>
        <button class="admin-btn admin-btn-ghost" type="button" @click="showPreview = !showPreview">
          {{ showPreview ? '隐藏预览' : '显示预览' }}
        </button>
        <button class="admin-btn" type="button" :disabled="saving" @click="save">
          {{ saving ? '保存中…' : '保存' }}
        </button>
      </header>

      <div class="ap-editor-grid" :class="{ 'no-preview': !showPreview }">
        <!-- 左列：表单 -->
        <div class="ap-card ap-editor-form">
          <div class="admin-field">
            <label class="admin-field-label">文章标题</label>
            <input v-model="form.title" class="admin-input ap-title-input" type="text" placeholder="给这篇文章起个名字" />
          </div>

          <div class="admin-field">
            <label class="admin-field-label">摘要</label>
            <textarea v-model="form.summary" class="admin-input admin-textarea" rows="3" placeholder="列表页展示的一小段介绍"></textarea>
          </div>

          <div class="ap-row-2col">
            <div class="admin-field">
              <label class="admin-field-label">分类</label>
              <select v-model="form.categoryPick" class="admin-input ap-select">
                <option v-for="name in categoryOptions" :key="name" :value="name">{{ name }}</option>
                <option :value="CUSTOM_CATEGORY">自定义…</option>
              </select>
              <input
                v-if="form.categoryPick === CUSTOM_CATEGORY"
                v-model="form.customCategory"
                class="admin-input ap-custom-cat"
                type="text"
                placeholder="输入新分类名"
              />
            </div>
            <div class="admin-field">
              <label class="admin-field-label">状态</label>
              <select v-model="form.status" class="admin-input ap-select">
                <option value="已发布">已发布</option>
                <option value="草稿">草稿</option>
              </select>
              <label class="ap-pin-check">
                <input v-model="form.pinned" type="checkbox" class="ap-check" />
                <span>置顶到首页文章列表</span>
              </label>
            </div>
          </div>

          <div class="admin-field">
            <label class="admin-field-label">封面图</label>
            <img v-if="form.coverUrl" class="ap-cover-preview" :src="form.coverUrl" alt="封面预览" />
            <input v-model="form.coverUrl" class="admin-input" type="text" placeholder="封面图片 URL" />
            <div class="ap-cover-actions">
              <button class="admin-btn admin-btn-ghost" type="button" :disabled="uploading" @click="fileEl && fileEl.click()">
                {{ uploading ? '上传中…' : '上传图片' }}
              </button>
              <button class="admin-btn admin-btn-ghost" type="button" @click="pickerOpen = true">从图库选择</button>
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
            <textarea
              v-model="form.content"
              class="admin-input admin-textarea ap-content-input"
              placeholder="# 从这里开始写正文…"
            ></textarea>
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
    <input ref="fileEl" type="file" accept="image/*" style="display: none" @change="onUploadFile" />
  </section>
</template>

<script setup>
import { computed, inject, onMounted, ref } from 'vue'
import { adminApi, mediaApi } from '../../api/admin'
import { renderMarkdown } from '../../utils/markdown'
import { useSettingsStore } from '../../stores/settings'
import MediaPicker from './MediaPicker.vue'

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
  } catch (err) {
    handleError(err, '加载失败')
  } finally {
    loading.value = false
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
  font-size: 18px;
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
  font-size: 13px;
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
  font-size: 14px;
  flex: none;
}

.ap-row-title {
  margin: 0;
  font-size: 15px;
  color: #1f3148;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ap-badge {
  flex: none;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
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
  font-size: 12px;
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
  font-size: 16px;
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

.ap-title-input {
  font-size: 18px;
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
  font-size: 13px;
  cursor: pointer;
}

.ap-check {
  width: 15px;
  height: 15px;
  accent-color: #3f77b5;
  cursor: pointer;
}

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
  font-size: 12px;
}

.ap-tag button {
  border: none;
  background: none;
  padding: 0;
  color: inherit;
  font-size: 13px;
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
  font-size: 13px;
  color: inherit;
  padding: 4px 0;
}

.ap-content-input {
  min-height: 44vh;
  font-size: 13px;
  line-height: 1.7;
}

.ap-preview-card {
  position: sticky;
  top: 16px;
  max-height: calc(100vh - 120px);
  overflow-y: auto;
}

.ap-preview-label {
  margin: 4px 2px 10px;
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #8aa5c4;
}

.ap-preview-title {
  margin: 0 0 14px;
  font-size: 22px;
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

  .ap-content-input {
    min-height: 40vh;
  }

  .ap-row-2col {
    display: block;
  }
}
</style>
