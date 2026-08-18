<template>
  <div class="adm-select" :class="{ 'is-open': open }">
    <button
      ref="triggerRef"
      type="button"
      class="admin-input adm-select-trigger"
      :id="id"
      :name="name"
      :aria-invalid="ariaInvalid"
      :aria-describedby="ariaDescribedby"
      :aria-required="ariaRequired"
      :disabled="disabled"
      @click="toggle"
      @keydown="onKeydown"
    >
      <span class="adm-select-label" :class="{ 'is-placeholder': !currentOption }">
        {{ currentOption ? currentOption.label : placeholder }}
      </span>
      <svg class="adm-select-arrow" viewBox="0 0 12 12" width="12" height="12" aria-hidden="true">
        <path d="M2.5 4.5 L6 8 L9.5 4.5" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" />
      </svg>
    </button>

    <teleport to="body">
      <transition name="adm-select-fade">
        <div v-if="open" ref="panelRef" class="adm-select-panel" :style="panelStyle" role="listbox">
          <button
            v-for="(opt, i) in normalized"
            :key="i"
            type="button"
            class="adm-select-option"
            :class="{ 'is-active': isSelected(opt), 'is-hover': i === hoverIndex }"
            role="option"
            :aria-selected="isSelected(opt)"
            @mouseenter="hoverIndex = i"
            @click="pick(opt)"
          >
            <span class="adm-select-option-label">{{ opt.label }}</span>
            <svg v-if="isSelected(opt)" class="adm-select-check" viewBox="0 0 12 12" width="12" height="12" aria-hidden="true">
              <path d="M2 6.2 L4.8 9 L10 3.4" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
          </button>
        </div>
      </transition>
    </teleport>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'

// 自绘下拉：原生 <select> 的弹出菜单无法用 CSS 定制（系统白底），
// 这里用按钮 + teleport 固定定位面板实现主题一致的下拉选项
const props = defineProps({
  modelValue: { type: [String, Number, Boolean], default: '' },
  // 选项：字符串数组，或 { label, value } 对象数组
  options: { type: Array, default: () => [] },
  placeholder: { type: String, default: '请选择…' },
  disabled: { type: Boolean, default: false },
  id: { type: String, default: undefined },
  name: { type: String, default: undefined },
  ariaInvalid: { type: [Boolean, String], default: undefined },
  ariaDescribedby: { type: String, default: undefined },
  ariaRequired: { type: [Boolean, String], default: undefined }
})
const emit = defineEmits(['update:modelValue', 'change'])

const open = ref(false)
const triggerRef = ref(null)
const panelRef = ref(null)
const hoverIndex = ref(-1)
const panelStyle = ref({})

const normalized = computed(() =>
  props.options.map(opt =>
    opt != null && typeof opt === 'object' ? opt : { label: String(opt), value: opt }
  )
)
const currentOption = computed(() => normalized.value.find(o => o.value === props.modelValue) || null)

function isSelected(opt) {
  return opt.value === props.modelValue
}

// 打开时按触发器矩形固定定位；下方空间不足时向上翻
function place() {
  const el = triggerRef.value
  if (!el) return
  const rect = el.getBoundingClientRect()
  const panelMax = 264
  const openUp = window.innerHeight - rect.bottom < panelMax + 12 && rect.top > panelMax + 12
  panelStyle.value = {
    position: 'fixed',
    left: `${rect.left}px`,
    minWidth: `${rect.width}px`,
    ...(openUp
      ? { bottom: `${window.innerHeight - rect.top + 6}px` }
      : { top: `${rect.bottom + 6}px` })
  }
}

function toggle() {
  if (props.disabled) return
  open.value ? close() : openPanel()
}

function openPanel() {
  place()
  hoverIndex.value = normalized.value.findIndex(o => isSelected(o))
  open.value = true
}

function close() {
  open.value = false
}

function pick(opt) {
  emit('update:modelValue', opt.value)
  emit('change', opt.value)
  close()
}

function onKeydown(e) {
  if (e.key === 'Escape') { close(); return }
  if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
    e.preventDefault()
    if (!open.value) { openPanel(); return }
    const total = normalized.value.length
    if (!total) return
    const step = e.key === 'ArrowDown' ? 1 : -1
    hoverIndex.value = (hoverIndex.value + step + total) % total
    return
  }
  if ((e.key === 'Enter' || e.key === ' ') && open.value) {
    e.preventDefault()
    const opt = normalized.value[hoverIndex.value]
    if (opt) pick(opt)
  }
}

// 点面板/触发器以外区域关闭；滚动或缩放时收起避免面板悬空错位
function onPointerDown(e) {
  if (!open.value) return
  if (triggerRef.value?.contains(e.target) || panelRef.value?.contains(e.target)) return
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
