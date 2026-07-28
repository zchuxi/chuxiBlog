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
      <button
        ref="triggerRef"
        type="button"
        class="admin-btn admin-btn-ghost adm-img-select-btn"
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
      </button>
    </div>

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
import { onBeforeUnmount, ref, watch } from 'vue'
import { mediaApi } from '../../api/admin'

defineProps({
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: '/favicon.png' }
})
const emit = defineEmits(['update:modelValue'])

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
  const el = triggerRef.value
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
  const root = triggerRef.value?.closest('.adm-img-select')
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
