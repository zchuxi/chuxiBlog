<template>
  <section class="admin-panel">
    <header class="admin-toolbar">
      <div>
        <h2 class="admin-toolbar-title">背景图库</h2>
        <p class="media-tip">
          管理前台「偏好设置」里可选的背景图。横屏用于桌面端，竖屏用于手机端；保存后立即生效。
        </p>
      </div>
      <div class="admin-toolbar-actions">
        <button class="admin-btn admin-btn-ghost" :disabled="loading" @click="load">刷新</button>
        <button class="admin-btn" :disabled="saving" @click="save">
          {{ saving ? '保存中…' : '保存' }}
        </button>
      </div>
    </header>

    <div v-if="loading" class="admin-state">加载中…</div>
    <template v-else>
      <div v-for="group in GROUPS" :key="group.key" class="admin-table-card bgl-card">
        <div class="bgl-head">
          <div>
            <h3>{{ group.label }}</h3>
            <p>{{ group.tip }}</p>
          </div>
          <span class="admin-badge">{{ form[group.key].length }} 张</span>
        </div>

        <div v-if="form[group.key].length === 0" class="admin-state">
          还没有图片，添加后前台将回退内置默认图库。
        </div>
        <div v-else class="bgl-grid" :class="{ 'is-portrait': group.key === 'vertical' }">
          <div v-for="(url, i) in form[group.key]" :key="url + i" class="bgl-item">
            <img :src="url" alt="背景" loading="lazy" />
            <span class="bgl-item-name" :title="url">{{ shortName(url) }}</span>
            <button type="button" class="bgl-item-remove" title="移除" @click="removeAt(group.key, i)">×</button>
          </div>
        </div>

        <div class="bgl-add-row">
          <input
            v-model="drafts[group.key]"
            class="admin-input"
            type="text"
            placeholder="粘贴图片 URL 后点添加，或直接上传 / 从图库选择"
            @keyup.enter="addDraft(group.key)"
          />
          <button class="admin-btn admin-btn-ghost" @click="addDraft(group.key)">添加</button>
          <button class="admin-btn admin-btn-ghost" :disabled="uploadingKey === group.key" @click="triggerUpload(group.key)">
            {{ uploadingKey === group.key ? '上传中…' : '上传图片' }}
          </button>
          <button class="admin-btn admin-btn-ghost" @click="pickerKey = group.key">从图库选择</button>
        </div>
      </div>

      <p class="admin-field-tip">
        提示：两个列表都清空并保存时，前台会使用项目内置的默认背景；「恢复默认」可一键回填内置图。
        <button class="admin-btn admin-btn-ghost bgl-reset-btn" @click="resetDefaults">恢复默认</button>
      </p>
    </template>

    <input ref="fileRef" type="file" accept="image/*" hidden @change="onUpload" />
    <MediaPicker :model-value="!!pickerKey" @update:model-value="v => !v && (pickerKey = '')" @select="onPicked" />
  </section>
</template>

<script setup>
import { inject, reactive, ref, onMounted } from 'vue'
import { siteContentApi, mediaApi } from '../../api/admin'
import { DEFAULT_LANDSCAPE, DEFAULT_VERTICAL } from '../../stores/settings'
import MediaPicker from './MediaPicker.vue'

const CONTENT_KEY = 'background-gallery'

const GROUPS = [
  { key: 'landscape', label: '横屏背景', tip: '桌面 / 横屏设备使用，建议 16:9 大图' },
  { key: 'vertical', label: '竖屏背景', tip: '手机 / 竖屏设备使用，建议 9:16 长图' }
]

const toast = inject('adminToast', () => {})

const loading = ref(false)
const saving = ref(false)
const form = reactive({ landscape: [], vertical: [] })
const drafts = reactive({ landscape: '', vertical: '' })

// 上传 / 图库选择的目标分组
const uploadingKey = ref('')
const pickerKey = ref('')
const fileRef = ref(null)
let uploadTarget = ''

function shortName(url) {
  return decodeURIComponent(String(url).split('/').pop() || url)
}

function parseContent(data) {
  try {
    if (!data) return null
    if (typeof data === 'string') return JSON.parse(data)
    if (typeof data.contentJson === 'string') return JSON.parse(data.contentJson)
    if (typeof data === 'object') return data
    return null
  } catch {
    return null
  }
}

async function load() {
  loading.value = true
  try {
    const obj = parseContent(await siteContentApi.get(CONTENT_KEY))
    form.landscape = Array.isArray(obj?.landscape) ? obj.landscape : DEFAULT_LANDSCAPE.slice()
    form.vertical = Array.isArray(obj?.vertical) ? obj.vertical : DEFAULT_VERTICAL.slice()
  } catch {
    // 尚无记录：用内置默认起步
    form.landscape = DEFAULT_LANDSCAPE.slice()
    form.vertical = DEFAULT_VERTICAL.slice()
  } finally {
    loading.value = false
  }
}

async function save() {
  saving.value = true
  try {
    await siteContentApi.save(CONTENT_KEY, JSON.stringify({ landscape: form.landscape, vertical: form.vertical }))
    toast('背景图库已保存，前台刷新后生效')
  } catch (err) {
    toast((err && err.message) || '保存失败', 'error')
  } finally {
    saving.value = false
  }
}

function addUrl(key, url) {
  const val = String(url || '').trim()
  if (!val) return
  if (form[key].includes(val)) {
    toast('这张图已在列表里', 'error')
    return
  }
  form[key].push(val)
}

function addDraft(key) {
  addUrl(key, drafts[key])
  drafts[key] = ''
}

function removeAt(key, i) {
  form[key].splice(i, 1)
}

function resetDefaults() {
  form.landscape = DEFAULT_LANDSCAPE.slice()
  form.vertical = DEFAULT_VERTICAL.slice()
  toast('已回填内置默认图，记得点保存')
}

function triggerUpload(key) {
  uploadTarget = key
  fileRef.value?.click()
}

async function onUpload(e) {
  const file = e.target.files && e.target.files[0]
  e.target.value = ''
  if (!file || !uploadTarget) return
  uploadingKey.value = uploadTarget
  try {
    const data = await mediaApi.upload(file)
    addUrl(uploadTarget, data.url)
    toast('图片已上传并加入列表')
  } catch (err) {
    toast((err && err.message) || '上传失败', 'error')
  } finally {
    uploadingKey.value = ''
  }
}

function onPicked(url) {
  if (pickerKey.value) addUrl(pickerKey.value, url)
  pickerKey.value = ''
}

onMounted(load)
</script>

<style>
/* ===== 背景图库面板（bgl- 前缀） ===== */
.bgl-card {
  margin-bottom: 16px;
  padding: 18px 20px;
}
.bgl-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}
.bgl-head h3 {
  margin: 0 0 4px;
  font-size: 17px;
}
.bgl-head p {
  margin: 0;
  font-size: 13px;
  opacity: 0.6;
}
.bgl-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}
.bgl-grid.is-portrait {
  grid-template-columns: repeat(auto-fill, minmax(96px, 1fr));
}
.bgl-item {
  position: relative;
  border-radius: 12px;
  overflow: hidden;
  aspect-ratio: 16 / 9;
  background: rgba(127, 127, 127, 0.12);
}
.bgl-grid.is-portrait .bgl-item {
  aspect-ratio: 9 / 16;
}
.bgl-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.bgl-item-name {
  position: absolute;
  left: 6px;
  bottom: 6px;
  max-width: calc(100% - 12px);
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  font-size: 11px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.bgl-item-remove {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 22px;
  height: 22px;
  border: none;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  font-size: 14px;
  line-height: 1;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.18s ease, background 0.18s ease;
}
.bgl-item:hover .bgl-item-remove {
  opacity: 1;
}
.bgl-item-remove:hover {
  background: rgba(209, 67, 67, 0.9);
}
.bgl-add-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.bgl-add-row .admin-input {
  flex: 1;
  min-width: 220px;
}
.bgl-reset-btn {
  margin-left: 8px;
  padding: 2px 10px;
}
</style>
