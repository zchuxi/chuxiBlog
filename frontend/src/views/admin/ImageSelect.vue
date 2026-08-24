<template>
  <div class="adm-img-select" :class="{ 'is-open': open }">
    <div class="adm-img-select-row">
      <input
        ref="inputRef"
        class="admin-input adm-img-select-input"
        type="text"
        :value="modelValue"
        :placeholder="placeholder"
        @input="onInput"
      />
      <CxButton
        ref="triggerRef"
        plain
        class="adm-img-select-btn"
        :disabled="loading"
        :title="loading ? '加载中…' : '从图库选择'"
        @click="toggle"
      >
        <svg v-if="loading" class="adm-img-select-spin" viewBox="0 0 16 16" width="16" height="16">
          <circle cx="8" cy="8" r="6" fill="none" stroke="currentColor" stroke-width="1.8" stroke-dasharray="28" stroke-linecap="round" />
        </svg>
        <svg v-else viewBox="0 0 16 16" width="16" height="16" aria-hidden="true">
          <path d="M2 4.5h12M2 8h12M2 11.5h12" fill="none" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" />
        </svg>
      </CxButton>
      <CxButton
        v-if="canCrop"
        plain
        class="adm-img-select-btn"
        :disabled="fetching"
        :title="fetching ? '取回中…' : (ratio ? '按展示比例裁切' : '裁切')"
        @click="openCrop"
      >
        <svg viewBox="0 0 16 16" width="16" height="16" aria-hidden="true">
          <path d="M5 1.5v8.5a1 1 0 0 0 1 1H15M1.5 5H10a1 1 0 0 1 1 1v8.5" fill="none" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
      </CxButton>
    </div>

    <CropDialog
      v-if="cropTarget"
      :item="cropTarget"
      :ratio="ratio"
      @close="cropTarget = null"
      @saved="onCropped"
    />

    <teleport to="body">
      <transition name="adm-select-fade">
        <div v-if="open" ref="panelRef" class="adm-select-panel adm-img-select-panel" :style="panelStyle">
          <div v-if="loading" class="adm-img-select-loading">加载中…</div>
          <div v-else-if="images.length === 0" class="adm-img-select-loading">图库为空，请先上传图片</div>
          <div v-else class="adm-img-select-grid">
            <button
              v-for="img in images"
              :key="img.name"
              type="button"
              class="adm-img-select-item"
              :class="{ 'is-active': img.url === modelValue }"
              :title="img.name"
              @click="pick(img)"
            >
              <img :src="img.url" :alt="img.name" loading="lazy" />
              <span class="adm-img-select-name">{{ img.name }}</span>
            </button>
          </div>
        </div>
      </transition>
    </teleport>
  </div>
</template>

<script setup>
import { computed, inject, onBeforeUnmount, ref, watch } from 'vue'
import { mediaApi } from '../../api/admin'
import CropDialog from './CropDialog.vue'
import CxButton from '../../components/cx/CxButton.vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: '/favicon.png' },
  // 预设裁切比例（宽/高），如头像 1；0 表示自由裁切
  ratio: { type: Number, default: 0 }
})
const emit = defineEmits(['update:modelValue'])

const toast = inject('adminToast', () => {})
const cropTarget = ref(null)
// 取回外链中的 loading：拉远端图转副本期间防止重复点「裁切」
const fetching = ref(false)

// 站内图（/api/uploads/）与外链 http(s) 图均可裁：站内直接打开，外链先让后端取回转本地副本再裁
const canCrop = computed(
  () => typeof props.modelValue === 'string'
    && (props.modelValue.startsWith('/api/uploads/') || /^https?:\/\//.test(props.modelValue))
)

async function openCrop() {
  const raw = props.modelValue
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
      emit('update:modelValue', data.url)
      cropTarget.value = { name: data.name || name, url: data.url }
    }
  } catch (err) {
    toast((err && err.message) || '取回失败，请稍后重试', 'error')
  } finally {
    fetching.value = false
  }
}

// 裁切保存为新图后直接回填字段
function onCropped(data) {
  if (data && data.url) {
    emit('update:modelValue', data.url)
    toast('裁切完成，已回填新图')
  }
}

const inputRef = ref(null)
const triggerRef = ref(null)
const panelRef = ref(null)
const open = ref(false)
const loading = ref(false)
const images = ref([])
const panelStyle = ref({})

function onInput(e) {
  emit('update:modelValue', e.target.value)
}

async function loadImages() {
  loading.value = true
  try {
    images.value = await mediaApi.list()
  } catch {
    images.value = []
  } finally {
    loading.value = false
  }
}

function place() {
  // ref 挂在 CxButton 组件上，取 $el 才是根 button 元素
  const el = triggerRef.value?.$el || triggerRef.value
  if (!el) return
  const rect = el.getBoundingClientRect()
  const panelMax = 240
  const openUp = window.innerHeight - rect.bottom < panelMax + 12 && rect.top > panelMax + 12
  panelStyle.value = {
    position: 'fixed',
    left: `${rect.left}px`,
    minWidth: `${Math.max(rect.width, 280)}px`,
    ...(openUp
      ? { bottom: `${window.innerHeight - rect.top + 6}px` }
      : { top: `${rect.bottom + 6}px` })
  }
}

function toggle() {
  if (open.value) {
    close()
  } else {
    place()
    open.value = true
    if (images.value.length === 0) loadImages()
  }
}

function close() {
  open.value = false
}

function pick(img) {
  emit('update:modelValue', img.url)
  close()
}

function onPointerDown(e) {
  if (!open.value) return
  const root = (triggerRef.value?.$el || triggerRef.value)?.closest('.adm-img-select')
  if (root?.contains(e.target) || panelRef.value?.contains(e.target)) return
  close()
}

function onScrollOrResize(e) {
  if (!open.value) return
  if (panelRef.value && e.target instanceof Node && panelRef.value.contains(e.target)) return
  close()
}

watch(open, val => {
  if (val) {
    document.addEventListener('pointerdown', onPointerDown, true)
    window.addEventListener('scroll', onScrollOrResize, true)
    window.addEventListener('resize', onScrollOrResize)
  } else {
    document.removeEventListener('pointerdown', onPointerDown, true)
    window.removeEventListener('scroll', onScrollOrResize, true)
    window.removeEventListener('resize', onScrollOrResize)
  }
})

onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', onPointerDown, true)
  window.removeEventListener('scroll', onScrollOrResize, true)
  window.removeEventListener('resize', onScrollOrResize)
})
</script>
