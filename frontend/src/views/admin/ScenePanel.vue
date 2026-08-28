<template>
  <section class="scene-panel">
    <!-- 顶部卡：标题 + 说明 + 新建 -->
    <header class="scene-head">
      <div class="scene-head-text">
        <h2 class="scene-head-title">首屏场景轮播</h2>
        <p class="scene-head-sub">管理首页第一屏的场景卡片：文案、配图、排序与显示开关，前台只展示「显示中」的场景。</p>
      </div>
      <CxButton @click="openCreate">+ 新建场景</CxButton>
    </header>

    <!-- 批量删除条：选中若干后出现 -->
    <transition name="admin-fade">
      <div v-if="selectedIds.length" class="scene-batch">
        <span class="scene-batch-text">已选中 {{ selectedIds.length }} 个场景</span>
        <CxButton type="danger" :disabled="batchBusy" @click="removeSelected">
          {{ batchBusy ? '删除中…' : '批量删除' }}
        </CxButton>
        <CxButton plain @click="selectedIds = []">取消选择</CxButton>
      </div>
    </transition>

    <!-- 场景列表卡 -->
    <div class="scene-card">
      <div v-if="loading" class="admin-state">加载中…</div>
      <div v-else-if="rows.length === 0" class="admin-state">还没有场景，点击右上角「+ 新建场景」添加第一屏吧</div>
      <template v-else>
        <div class="scene-list-head">
          <label class="scene-check-label">
            <CxCheckbox size="small" :model-value="allSelected" @change="toggleAll" />
            <span>全选</span>
          </label>
          <span class="scene-count">共 {{ rows.length }} 个场景</span>
        </div>
        <ul class="scene-list">
          <li v-for="(row, idx) in rows" :key="row.id" class="scene-row" :class="{ off: !isVisible(row) }">
            <CxCheckbox
              size="small"
              :model-value="selectedIds.includes(row.id)"
              :aria-label="`选中「${row.title || '未命名场景'}」`"
              @change="toggleSelect(row.id)"
            />
            <div class="scene-thumb">
              <img v-if="row.imageUrl" :src="row.imageUrl" alt="" loading="lazy" />
              <span v-else class="scene-thumb-empty">无图</span>
            </div>
            <div class="scene-info">
              <p class="scene-eyebrow">{{ eyebrowOf(row, idx) }}</p>
              <h3 class="scene-title">{{ row.title || '未命名场景' }}</h3>
              <p class="scene-desc">{{ row.description || '—' }}</p>
            </div>
            <span class="scene-badge">{{ badgeOf(row) }}</span>
            <button
              type="button"
              class="scene-visible"
              :class="{ off: !isVisible(row) }"
              :disabled="togglingId === row.id"
              :title="isVisible(row) ? '点击隐藏该场景' : '点击在首页显示该场景'"
              @click="toggleVisible(row)"
            >
              {{ togglingId === row.id ? '切换中…' : isVisible(row) ? '显示中' : '已隐藏' }}
            </button>
            <div class="scene-ops">
              <button class="admin-link" type="button" title="编辑" @click="openEdit(row)">✎ 编辑</button>
              <button class="admin-link danger" type="button" @click="removeOne(row)">删除</button>
            </div>
          </li>
        </ul>
      </template>
    </div>

    <!-- 编辑 / 新建弹窗（居中 modal） -->
    <teleport to="body">
      <transition name="admin-fade">
        <div v-if="modalOpen" class="scene-modal-mask" @click.self="closeModal">
          <div class="scene-modal">
            <header class="scene-modal-head">
              <h3>{{ editingId == null ? '新建场景' : '编辑场景' }}</h3>
              <button class="admin-drawer-close" type="button" @click="closeModal">×</button>
            </header>
            <div class="scene-modal-body">
              <div class="scene-form-grid">
                <div class="admin-field">
                  <label class="admin-field-label">编号标签（留空自动按序号，如 SCENE 01）</label>
                  <CxInput v-model="form.sceneLabel" variant="admin" placeholder="SCENE 01" />
                </div>
                <div class="admin-field">
                  <label class="admin-field-label">眉标 Kicker（留空默认 PERSPECTIVE）</label>
                  <CxInput v-model="form.kicker" variant="admin" placeholder="PERSPECTIVE" />
                </div>
              </div>
              <div class="admin-field">
                <label class="admin-field-label">标题</label>
                <CxInput v-model="form.title" variant="admin" placeholder="场景大标题" />
              </div>
              <div class="admin-field">
                <label class="admin-field-label">副标题</label>
                <CxInput v-model="form.description" type="textarea" variant="admin" :rows="2" placeholder="标题下方的一句话" />
              </div>
              <div class="admin-field">
                <label class="admin-field-label">描述</label>
                <CxInput v-model="form.content" type="textarea" variant="admin" :rows="3" placeholder="更长的场景介绍文字" />
              </div>
              <div class="admin-field">
                <label class="admin-field-label">背景图片</label>
                <img v-if="form.imageUrl" class="scene-img-preview" :src="form.imageUrl" alt="背景预览" />
                <CxInput v-model="form.imageUrl" variant="admin" placeholder="图片 URL" />
                <div class="scene-img-actions">
                  <CxButton plain :disabled="uploading" @click="fileEl && fileEl.click()">
                    {{ uploading ? '上传中…' : '上传图片' }}
                  </CxButton>
                  <CxButton plain @click="pickerOpen = true">从图库选择</CxButton>
                  <CxButton v-if="canCrop" plain :disabled="fetching" @click="openCrop">
                    {{ fetching ? '取回中…' : '裁切' }}
                  </CxButton>
                </div>
              </div>
              <div class="scene-form-grid">
                <div class="admin-field">
                  <label class="admin-field-label">角标 Badge（留空自动推导，如 04/19）</label>
                  <CxInput v-model="form.badge" variant="admin" placeholder="04/19" />
                </div>
                <div class="admin-field">
                  <label class="admin-field-label">排序（数字越小越靠前）</label>
                  <CxInput v-model="form.sortIndex" type="number" variant="admin" />
                </div>
              </div>
              <label class="scene-visible-check">
                <CxCheckbox v-model="form.visible" size="small" />
                <span>在首页显示该场景</span>
              </label>
            </div>
            <footer class="scene-modal-foot">
              <CxButton plain @click="closeModal">取消</CxButton>
              <CxButton :disabled="saving" @click="save">
                {{ saving ? '保存中…' : '保存' }}
              </CxButton>
            </footer>
          </div>
        </div>
      </transition>
    </teleport>

    <MediaPicker v-model="pickerOpen" @select="onPickImage" />
    <CropDialog
      v-if="cropOpen && cropItem"
      :item="cropItem"
      :ratio="16 / 10"
      @close="closeCrop"
      @saved="onCropSaved"
    />
    <input ref="fileEl" type="file" accept="image/*" style="display: none" @change="onUploadFile" />
  </section>
</template>

<script setup>
import { computed, inject, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { adminApi, mediaApi } from '../../api/admin'
import CxButton from '../../components/cx/CxButton.vue'
import CxCheckbox from '../../components/cx/CxCheckbox.vue'
import CxInput from '../../components/cx/CxInput.vue'
import MediaPicker from './MediaPicker.vue'
import CropDialog from './CropDialog.vue'

const api = adminApi['home-carousels']
const toast = inject('adminToast', () => {})
const onUnauthorized = inject('adminUnauthorized', () => {})

const rows = ref([])
const loading = ref(false)
const selectedIds = ref([])
const batchBusy = ref(false)
const togglingId = ref(null)

const modalOpen = ref(false)
const editingId = ref(null)
const saving = ref(false)
const form = ref(emptyForm())
// 打开弹窗时的初始快照：点遮罩/取消前据此判断是否有未保存修改
const initialSnapshot = ref('')

function snapshotOf(model) {
  return JSON.stringify(model)
}
const isDirty = computed(() => modalOpen.value && snapshotOf(form.value) !== initialSnapshot.value)

const pickerOpen = ref(false)
const cropOpen = ref(false)
const uploading = ref(false)
const fileEl = ref(null)

const allSelected = computed(() => rows.value.length > 0 && selectedIds.value.length === rows.value.length)

// 站内图（/api/uploads/）与外链 http(s) 图均可裁：站内直接打开，外链先让后端取回转本地副本再裁
const fetching = ref(false)
const cropItem = ref(null)
const canCrop = computed(
  () => typeof form.value.imageUrl === 'string'
    && (form.value.imageUrl.startsWith('/api/uploads/') || /^https?:\/\//.test(form.value.imageUrl))
)

async function openCrop() {
  const raw = form.value.imageUrl
  const name = decodeURIComponent((raw.split('?')[0].split('/').pop() || ''))
  if (!name) return
  if (raw.startsWith('/api/uploads/')) {
    cropItem.value = { name, url: raw }
    cropOpen.value = true
    return
  }
  // 外链：canvas 跨域会被污染无法直接裁，先让后端下载到站内并回填
  fetching.value = true
  try {
    const data = await mediaApi.fetch(raw)
    if (data && data.url) {
      form.value.imageUrl = data.url
      cropItem.value = { name: data.name || name, url: data.url }
      cropOpen.value = true
    }
  } catch (err) {
    handleError(err, '取回失败，请稍后重试')
  } finally {
    fetching.value = false
  }
}

function closeCrop() {
  cropOpen.value = false
  cropItem.value = null
}

function emptyForm() {
  return {
    sceneLabel: '',
    kicker: '',
    title: '',
    description: '',
    content: '',
    imageUrl: '',
    badge: '',
    sortIndex: '',
    visible: true
  }
}

function pad2(n) {
  return String(n).padStart(2, '0')
}

// 旧数据 visible 可能为 null，按显示处理
function isVisible(row) {
  return row.visible !== false
}

function eyebrowOf(row, idx) {
  const label = row.sceneLabel || `SCENE ${pad2(idx + 1)}`
  const kicker = row.kicker || 'PERSPECTIVE'
  return `${label} · ${kicker}`
}

// badge 为空时按前台回退逻辑展示 updatedAt 的 MM/DD
function badgeOf(row) {
  if (row.badge) return row.badge
  if (row.updatedAt) {
    const d = new Date(row.updatedAt)
    if (!Number.isNaN(d.getTime())) return `${pad2(d.getMonth() + 1)}/${pad2(d.getDate())}`
  }
  return '—'
}

function handleError(err, fallback) {
  if (err && err.unauthorized) {
    onUnauthorized && onUnauthorized()
    return
  }
  toast((err && err.message) || fallback, 'error')
}

async function load() {
  loading.value = true
  try {
    const list = (await api.list()) || []
    list.sort((a, b) => {
      const sa = a.sortIndex == null ? Number.MAX_SAFE_INTEGER : a.sortIndex
      const sb = b.sortIndex == null ? Number.MAX_SAFE_INTEGER : b.sortIndex
      return sa - sb || a.id - b.id
    })
    rows.value = list
    selectedIds.value = selectedIds.value.filter(id => list.some(r => r.id === id))
  } catch (err) {
    handleError(err, '加载失败')
  } finally {
    loading.value = false
  }
}

// ---- 选择 ----

function toggleSelect(id) {
  const i = selectedIds.value.indexOf(id)
  if (i >= 0) selectedIds.value.splice(i, 1)
  else selectedIds.value.push(id)
}

function toggleAll() {
  selectedIds.value = allSelected.value ? [] : rows.value.map(r => r.id)
}

// ---- 显隐开关：后端 PUT 为整体覆盖，需带全字段 ----

function fullPayload(row) {
  return {
    id: row.id,
    title: row.title,
    description: row.description,
    content: row.content,
    imageUrl: row.imageUrl,
    sortIndex: row.sortIndex,
    sceneLabel: row.sceneLabel || null,
    kicker: row.kicker || null,
    badge: row.badge || null,
    visible: isVisible(row),
    createdAt: row.createdAt || null,
    updatedAt: row.updatedAt || null // 保留原时间，避免仅切显隐导致前台角标日期变化
  }
}

async function toggleVisible(row) {
  togglingId.value = row.id
  try {
    const payload = fullPayload(row)
    payload.visible = !isVisible(row)
    await api.update(row.id, payload)
    row.visible = payload.visible
    toast(payload.visible ? '该场景已在首页显示' : '该场景已隐藏')
  } catch (err) {
    handleError(err, '切换失败')
  } finally {
    togglingId.value = null
  }
}

// ---- 编辑 / 新建 ----

function openCreate() {
  editingId.value = null
  const next = rows.value.reduce((max, r) => Math.max(max, r.sortIndex == null ? 0 : r.sortIndex), 0) + 1
  form.value = { ...emptyForm(), sortIndex: next }
  initialSnapshot.value = snapshotOf(form.value)
  modalOpen.value = true
}

function openEdit(row) {
  editingId.value = row.id
  form.value = {
    sceneLabel: row.sceneLabel || '',
    kicker: row.kicker || '',
    title: row.title || '',
    description: row.description || '',
    content: row.content || '',
    imageUrl: row.imageUrl || '',
    badge: row.badge || '',
    sortIndex: row.sortIndex == null ? '' : row.sortIndex,
    visible: isVisible(row)
  }
  initialSnapshot.value = snapshotOf(form.value)
  modalOpen.value = true
}

function closeModal() {
  requestClose()
}

// 遮罩点击/×/取消共用同一守卫：有未保存修改先确认，保存中直接拒绝
function requestClose() {
  if (!modalOpen.value) return true
  if (saving.value) return false
  if (isDirty.value && !window.confirm('当前修改尚未保存，确定放弃并关闭吗？')) return false
  modalOpen.value = false
  cropOpen.value = false
  return true
}

async function save() {
  saving.value = true
  try {
    const f = form.value
    const payload = {
      sceneLabel: String(f.sceneLabel || '').trim() || null,
      kicker: String(f.kicker || '').trim() || null,
      title: String(f.title || '').trim(),
      description: f.description || '',
      content: f.content || '',
      imageUrl: f.imageUrl || '',
      badge: String(f.badge || '').trim() || null,
      sortIndex: f.sortIndex === '' || f.sortIndex == null ? null : Number(f.sortIndex),
      visible: !!f.visible
    }
    if (editingId.value == null) await api.create(payload)
    else await api.update(editingId.value, { ...payload, id: editingId.value })
    toast('保存成功')
    modalOpen.value = false
    await load()
  } catch (err) {
    handleError(err, '保存失败')
  } finally {
    saving.value = false
  }
}

// ---- 删除 ----

async function removeOne(row) {
  if (!window.confirm(`确定删除场景「${row.title || row.id}」吗？`)) return
  try {
    await api.remove(row.id)
    toast('删除成功')
    await load()
  } catch (err) {
    handleError(err, '删除失败')
  }
}

async function removeSelected() {
  if (!window.confirm(`确定删除选中的 ${selectedIds.value.length} 个场景吗？`)) return
  batchBusy.value = true
  try {
    for (const id of selectedIds.value) await api.remove(id)
    toast('批量删除成功')
    selectedIds.value = []
    await load()
  } catch (err) {
    handleError(err, '批量删除失败')
    await load()
  } finally {
    batchBusy.value = false
  }
}

// ---- 图片：上传 / 图库 / 裁切 ----

async function onUploadFile(e) {
  const file = e.target.files && e.target.files[0]
  e.target.value = ''
  if (!file) return
  uploading.value = true
  try {
    const data = await mediaApi.upload(file)
    if (data && data.url) form.value.imageUrl = data.url
    toast('上传成功')
  } catch (err) {
    handleError(err, '上传失败')
  } finally {
    uploading.value = false
  }
}

function onPickImage(url) {
  form.value.imageUrl = url
}

// CropDialog 的 saved 事件带回新图 { name, url }，直接回填表单
function onCropSaved(data) {
  closeCrop()
  if (data && data.url) {
    form.value.imageUrl = data.url
    toast('裁切完成，已替换为新图')
  } else {
    toast('裁切结果已存入图库，请从图库选择')
  }
}

// 弹层（图库/裁切）打开时把 Escape 让给子弹层，不触发主弹窗守卫
function hasOpenSubOverlay() {
  return pickerOpen.value || cropOpen.value
}

function onModalKeydown(event) {
  if (!modalOpen.value || event.key !== 'Escape') return
  if (hasOpenSubOverlay()) return
  event.preventDefault()
  requestClose()
}

watch(modalOpen, open => {
  if (open) window.addEventListener('keydown', onModalKeydown)
  else window.removeEventListener('keydown', onModalKeydown)
})

onMounted(load)
onBeforeUnmount(() => window.removeEventListener('keydown', onModalKeydown))
</script>

<style scoped>
/* ---------- 首屏场景面板（scene-*，明暗双主题） ---------- */

.scene-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.scene-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 22px;
  background-color: #fff;
  border-radius: 18px;
  box-shadow: 0 8px 24px rgba(63, 119, 181, 0.08);
}

.scene-head-title {
  margin: 0 0 4px;
  font-size: 20px;
  color: var(--accent-strong);
}

.scene-head-sub {
  margin: 0;
  font-size: 14.5px;
  color: rgba(31, 49, 72, 0.55);
}

.scene-batch {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 18px;
  background-color: rgba(192, 86, 79, 0.08);
  border: 1px dashed rgba(192, 86, 79, 0.4);
  border-radius: 14px;
  font-size: 14.5px;
}

.scene-batch-text {
  flex: 1;
  color: #c0564f;
}

.scene-card {
  background-color: #fff;
  border-radius: 18px;
  box-shadow: 0 8px 24px rgba(63, 119, 181, 0.08);
  padding: 12px 18px;
}

.scene-list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 6px 12px;
  border-bottom: 1px dashed rgba(63, 119, 181, 0.2);
  font-size: 14.5px;
  color: rgba(31, 49, 72, 0.6);
}

.scene-check-label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

/* scene-check 已统一使用 CxCheckbox（size="small"） */

.scene-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.scene-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 6px;
  border-bottom: 1px dashed rgba(63, 119, 181, 0.15);
}

.scene-row:last-child {
  border-bottom: none;
}

.scene-row.off .scene-thumb,
.scene-row.off .scene-info,
.scene-row.off .scene-badge {
  opacity: 0.5;
}

.scene-thumb {
  width: 96px;
  height: 60px;
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  overflow: hidden;
  background-color: rgba(63, 119, 181, 0.08);
  border: 1px solid rgba(63, 119, 181, 0.15);
}

.scene-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.scene-thumb-empty {
  font-size: 13px;
  color: rgba(31, 49, 72, 0.4);
}

.scene-info {
  flex: 1;
  min-width: 0;
}

.scene-eyebrow {
  margin: 0 0 2px;
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #8aa5c4;
}

.scene-title {
  margin: 0 0 2px;
  font-size: 16.5px;
  color: var(--text-color);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.scene-desc {
  margin: 0;
  font-size: 13px;
  color: rgba(31, 49, 72, 0.55);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.scene-badge {
  flex: none;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 13px;
  background-color: rgba(63, 119, 181, 0.12);
  color: var(--accent-strong);
}

.scene-visible {
  flex: none;
  padding: 4px 12px;
  border-radius: 999px;
  border: 1px solid rgba(63, 119, 181, 0.35);
  background-color: rgba(63, 119, 181, 0.08);
  color: var(--accent-strong);
  font-family: inherit;
  font-size: 13px;
  cursor: pointer;
  transition: background-color 0.2s ease, color 0.2s ease;
}

.scene-visible.off {
  border-color: rgba(31, 49, 72, 0.2);
  background-color: rgba(31, 49, 72, 0.06);
  color: rgba(31, 49, 72, 0.5);
}

.scene-visible:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.scene-ops {
  flex: none;
  display: flex;
  gap: 2px;
}

/* ---------- 编辑弹窗 ---------- */

.scene-modal-mask {
  position: fixed;
  inset: 0;
  z-index: 60;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background-color: rgba(31, 49, 72, 0.45);
}

.scene-modal {
  width: min(640px, 96vw);
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  background-color: #fff;
  border-radius: 20px;
  box-shadow: 0 24px 64px rgba(31, 49, 72, 0.25);
  font-family: var(--font-family-cute, "Comic Sans MS", cursive, sans-serif);
}

.scene-modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 22px;
  border-bottom: 1px dashed rgba(63, 119, 181, 0.25);
}

.scene-modal-head h3 {
  margin: 0;
  font-size: 17.5px;
  color: var(--accent-strong);
}

.scene-modal-body {
  flex: 1;
  overflow-y: auto;
  padding: 18px 22px;
}

.scene-modal-foot {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 22px;
  border-top: 1px dashed rgba(63, 119, 181, 0.25);
}

.scene-form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 14px;
}

.scene-img-preview {
  display: block;
  width: 100%;
  max-height: 180px;
  object-fit: cover;
  border-radius: 12px;
  border: 1px solid rgba(63, 119, 181, 0.2);
  margin-bottom: 8px;
}

.scene-img-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.scene-visible-check {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 4px 0 6px;
  font-size: 15.5px;
  cursor: pointer;
}

@media (max-width: 720px) {
  .scene-badge {
    display: none;
  }

  .scene-thumb {
    width: 68px;
    height: 46px;
  }

  .scene-form-grid {
    grid-template-columns: 1fr;
  }
}

/* ---------- 暗色 ---------- */

html.dark .scene-head,
html.dark .scene-card,
html.dark .scene-modal {
  background: rgba(28, 36, 52, 0.92);
  box-shadow: 0 18px 44px rgba(0, 0, 0, 0.42);
}

html.dark .scene-head-title,
html.dark .scene-modal-head h3 {
  color: #e8effa;
}

html.dark .scene-head-sub,
html.dark .scene-desc,
html.dark .scene-thumb-empty {
  color: #8fa0ba;
}

html.dark .scene-list-head {
  color: #8fa0ba;
  border-color: rgba(255, 255, 255, 0.1);
}

html.dark .scene-row {
  border-color: rgba(255, 255, 255, 0.06);
}

html.dark .scene-title {
  color: #dbe4f0;
}

html.dark .scene-badge {
  background: rgba(95, 149, 207, 0.18);
  color: #a8cdf0;
}

html.dark .scene-visible {
  border-color: rgba(95, 149, 207, 0.45);
  background: rgba(95, 149, 207, 0.14);
  color: #a8cdf0;
}

html.dark .scene-visible.off {
  border-color: rgba(255, 255, 255, 0.14);
  background: rgba(255, 255, 255, 0.05);
  color: #8fa0ba;
}

html.dark .scene-thumb {
  background: rgba(16, 22, 34, 0.7);
  border-color: rgba(255, 255, 255, 0.08);
}

html.dark .scene-modal-mask {
  background: rgba(5, 9, 16, 0.65);
}

html.dark .scene-modal-head,
html.dark .scene-modal-foot {
  border-color: rgba(255, 255, 255, 0.1);
}

html.dark .scene-batch {
  background: rgba(192, 86, 79, 0.12);
  border-color: rgba(217, 141, 141, 0.4);
}

html.dark .scene-batch-text {
  color: #d98d8d;
}

html.dark .scene-visible-check {
  color: #c3d0e2;
}

html.dark .scene-img-preview {
  border-color: rgba(255, 255, 255, 0.1);
}

/* ---------- 移动端适配（≤900px，追加） ---------- */

@media (max-width: 900px) {
  .scene-head {
    flex-wrap: wrap;
  }

  /* 行卡两行布局：第一行 勾选+缩略图+文案；第二行 badge/显隐/操作 */
  .scene-row {
    display: grid;
    grid-template-columns: auto 68px minmax(0, 1fr) auto;
    grid-template-areas:
      "check thumb info info"
      "badge badge vis  ops";
    gap: 8px 10px;
    align-items: center;
  }

  .scene-row > .cx-checkbox {
    grid-area: check;
  }

  .scene-thumb {
    grid-area: thumb;
    width: 68px;
    height: 46px;
  }

  .scene-info {
    grid-area: info;
  }

  .scene-badge {
    grid-area: badge;
    justify-self: start;
  }

  .scene-visible {
    grid-area: vis;
    justify-self: start;
    padding: 8px 14px;
  }

  .scene-ops {
    grid-area: ops;
    justify-self: end;
  }

  /* 编辑弹窗：占满可视宽度、可滚动 */
  .scene-modal-mask {
    padding: 12px;
  }

  .scene-modal {
    width: calc(100vw - 24px);
    max-height: calc(100vh - 24px);
  }

  .scene-modal-body {
    padding: 14px 16px;
  }

  .scene-modal-head,
  .scene-modal-foot {
    padding-left: 16px;
    padding-right: 16px;
  }
}
</style>
