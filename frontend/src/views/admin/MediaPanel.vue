<template>
  <section class="admin-panel">
    <!-- 顶部工具栏 -->
    <header class="admin-toolbar">
      <div>
        <h2 class="admin-toolbar-title">图片库</h2>
        <p class="media-tip">上传图片并复制链接，可在各资源的图片字段中复用</p>
      </div>
      <div class="admin-toolbar-actions">
        <button class="admin-btn admin-btn-ghost" :disabled="loading" @click="load">刷新</button>
        <button class="admin-btn" :disabled="uploading" @click="fileEl && fileEl.click()">
          {{ uploading ? '上传中…' : '上传图片' }}
        </button>
        <input ref="fileEl" type="file" accept="image/*" multiple style="display: none" @change="onUpload" />
      </div>
    </header>

    <!-- 图片网格 -->
    <div class="admin-table-card">
      <div v-if="loading" class="admin-state">加载中…</div>
      <div v-else-if="items.length === 0" class="admin-state">图库还是空的，点击右上角「上传图片」添加吧</div>
      <div v-else class="media-grid">
        <div v-for="item in items" :key="item.name" class="media-card">
          <img class="media-thumb" :src="item.url" :alt="item.name" loading="lazy" />
          <div class="media-info">
            <p class="media-name" :title="item.name">{{ item.name }}</p>
            <p class="media-meta">
              <span>{{ formatSize(item.size) }}</span>
              <span>{{ formatTime(item.lastModified) }}</span>
            </p>
          </div>
          <div class="media-ops">
            <button class="admin-link" @click="copyUrl(item)">复制链接</button>
            <button class="admin-link" @click="cropTarget = item">裁切</button>
            <button class="admin-link danger" @click="onRemove(item)">删除</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 裁切弹窗 -->
    <CropDialog
      v-if="cropTarget"
      :item="cropTarget"
      @close="cropTarget = null"
      @saved="onCropSaved"
    />
  </section>
</template>

<script setup>
import { inject, onMounted, ref } from 'vue'
import { mediaApi } from '../../api/admin'
import CropDialog from './CropDialog.vue'

const toast = inject('adminToast', () => {})
const unauthorized = inject('adminUnauthorized', () => {})

const items = ref([])
const loading = ref(false)
const uploading = ref(false)
const fileEl = ref(null)
const cropTarget = ref(null)

function handleError(err, fallback) {
  if (err && err.unauthorized) unauthorized()
  else toast((err && err.message) || fallback, 'error')
}

async function load() {
  loading.value = true
  try {
    items.value = await mediaApi.list()
  } catch (err) {
    handleError(err, '加载图库失败')
  } finally {
    loading.value = false
  }
}

async function onUpload(e) {
  const files = Array.from(e.target.files || [])
  e.target.value = ''
  if (!files.length) return
  uploading.value = true
  try {
    for (const f of files) await mediaApi.upload(f)
    toast(`已上传 ${files.length} 张图片`)
    await load()
  } catch (err) {
    handleError(err, '上传失败')
  } finally {
    uploading.value = false
  }
}

async function copyUrl(item) {
  const text = item.url
  try {
    if (navigator.clipboard && navigator.clipboard.writeText) {
      await navigator.clipboard.writeText(text)
    } else {
      // 降级方案：临时 textarea + execCommand
      const ta = document.createElement('textarea')
      ta.value = text
      ta.style.position = 'fixed'
      ta.style.opacity = '0'
      document.body.appendChild(ta)
      ta.select()
      document.execCommand('copy')
      document.body.removeChild(ta)
    }
    toast('链接已复制')
  } catch {
    toast('复制失败，请手动复制：' + text, 'error')
  }
}

async function onRemove(item) {
  if (!confirm(`确定删除「${item.name}」吗？引用它的地方会失效`)) return
  try {
    await mediaApi.remove(item.name)
    toast('已删除')
    await load()
  } catch (err) {
    handleError(err, '删除失败')
  }
}

function onCropSaved() {
  toast('裁切已保存为新图')
  load()
}

function formatSize(size) {
  if (size == null) return ''
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
  return (size / 1024 / 1024).toFixed(2) + ' MB'
}

function formatTime(ms) {
  if (!ms) return ''
  const d = new Date(ms)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

onMounted(load)
</script>
