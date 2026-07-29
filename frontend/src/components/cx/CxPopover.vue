<template>
  <div class="lx-popover-wrapper" ref="wrapperRef">
    <div
      class="lx-popover-trigger"
      @click="handleTriggerClick"
      @mouseenter="handleMouseEnter"
      @mouseleave="handleMouseLeave"
    >
      <slot />
    </div>
    <transition name="lx-popover-fade">
      <div
        v-if="visible"
        ref="popoverRef"
        class="lx-popover"
        :style="popoverStyle"
        @mouseenter="handleMouseEnter"
        @mouseleave="handleMouseLeave"
      >
        <div class="lx-popover__content">
          <slot name="content" :close="close" />
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'

const props = defineProps({
  trigger: {
    type: String,
    default: 'click',
    validator: v => ['click', 'hover'].includes(v)
  },
  placement: {
    type: String,
    default: 'bottom',
    validator: v => ['bottom', 'top', 'left', 'right'].includes(v)
  }
})

const wrapperRef = ref(null)
const popoverRef = ref(null)
const visible = ref(false)
const popoverStyle = ref({})

let hoverTimer = null

// Global singleton: close any other popover when opening a new one
const activeInstance = { current: null }

function open() {
  if (activeInstance.current && activeInstance.current !== close) {
    activeInstance.current()
  }
  visible.value = true
  activeInstance.current = close
  nextTick(updatePosition)
}

function close() {
  visible.value = false
  if (activeInstance.current === close) {
    activeInstance.current = null
  }
}

function toggle() {
  if (visible.value) {
    close()
  } else {
    open()
  }
}

function handleTriggerClick() {
  if (props.trigger === 'click') {
    toggle()
  }
}

function handleMouseEnter() {
  if (props.trigger === 'hover') {
    clearTimeout(hoverTimer)
    open()
  }
}

function handleMouseLeave() {
  if (props.trigger === 'hover') {
    hoverTimer = setTimeout(() => {
      close()
    }, 150)
  }
}

function updatePosition() {
  if (!wrapperRef.value || !popoverRef.value) return
  const triggerRect = wrapperRef.value.getBoundingClientRect()
  const popoverRect = popoverRef.value.getBoundingClientRect()
  const gap = 8
  let top, left

  switch (props.placement) {
    case 'top':
      top = triggerRect.top - popoverRect.height - gap
      left = triggerRect.left + (triggerRect.width - popoverRect.width) / 2
      break
    case 'left':
      top = triggerRect.top + (triggerRect.height - popoverRect.height) / 2
      left = triggerRect.left - popoverRect.width - gap
      break
    case 'right':
      top = triggerRect.top + (triggerRect.height - popoverRect.height) / 2
      left = triggerRect.right + gap
      break
    case 'bottom':
    default:
      top = triggerRect.bottom + gap
      left = triggerRect.left + (triggerRect.width - popoverRect.width) / 2
      break
  }

  popoverStyle.value = {
    position: 'fixed',
    top: `${top}px`,
    left: `${left}px`
  }
}

function handleClickOutside(e) {
  if (props.trigger === 'click' && visible.value) {
    if (wrapperRef.value && !wrapperRef.value.contains(e.target) &&
        popoverRef.value && !popoverRef.value.contains(e.target)) {
      close()
    }
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside)
  clearTimeout(hoverTimer)
  close()
})

defineExpose({ open, close, toggle })
</script>
