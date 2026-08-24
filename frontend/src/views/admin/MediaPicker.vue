<template>
  <teleport to="body">
    <div v-if="modelValue" class="media-picker-mask" @click.self="close">
      <div class="media-picker">
        <header class="media-picker-head">
          <h3>从图库选择</h3>
          <div class="media-picker-actions">
            <CxButton plain :disabled="uploading" @click="fileEl && fileEl.click()">
              {{ uploading ? '上传中…' : '上传' }}
            </CxButton>
            <input ref="fileEl" type="file" accept="image/*" style="display: none" @change="onUpload" />
            <button class="admin-drawer-close" type="button" @click="close">×</button>
          </div>
        </header>
        <div class="media-picker-body">
          <div v-if="loading" class="admin-state">加载中…</div>
          <div v-else-if="items.length === 0" class="admin-state">图库还是空的，先上传一张吧</div>
          <div v-else class="media-pick-grid">
            <button
              v-for="item in items"
              :key="item.name"
              type="button"
              class="media-pick-item"
              :title="item.name"
              @click="pick(item)"
            >
              <img :src="item.url" :alt="item.name" loading="lazy" />
              <span class="media-pick-name">{{ item.name }}</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  </teleport>
</template>

<script setup>
import { inject, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { mediaApi } from '../../api/admin'
import CxButton from '../../components/cx/CxButton.vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue', 'select'])

const toast = inject('adminToast', () => {})

const items = ref([])
const loading = ref(false)
const uploading = ref(false)
const fileEl = ref(null)

// 每次打开时刷新列表
watch(
  () => props.modelValue,
  open => {
    if (open) load()
  }
)

async function load() {
  loading.value = true
  try {
    items.value = await mediaApi.list()
  } catch (err) {
    toast((err && err.message) || '加载图库失败', 'error')
  } finally {
    loading.value = false
  }
}

async function onUpload(e) {
  const file = e.target.files && e.target.files[0]
  e.target.value = ''
  if (!file) return
  uploading.value = true
  try {
    await mediaApi.upload(file)
    toast('上传成功，点选即可使用')
    await load()
  } catch (err) {
    toast((err && err.message) || '上传失败', 'error')
  } finally {
    uploading.value = false
  }
}

function pick(item) {
  emit('select', item.url)
  close()
}

function close() {
  emit('update:modelValue', false)
}

function onDocumentKeydown(event) {
  if (!props.modelValue) return
  const isSaveShortcut = (event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 's'
  if (event.key !== 'Escape' && !isSaveShortcut) return
  event.preventDefault()
  event.stopPropagation()
  if (event.key === 'Escape') close()
}

onMounted(() => document.addEventListener('keydown', onDocumentKeydown))
onBeforeUnmount(() => document.removeEventListener('keydown', onDocumentKeydown))
</script>
