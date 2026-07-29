<template>
  <button
    class="cx-button"
    :class="buttonClasses"
    :type="nativeType"
    :disabled="isDisabled"
    :style="customStyle"
    @click="handleClick"
  >
    <span class="cx-button__content">
      <slot />
    </span>
  </button>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  type: {
    type: String,
    default: 'primary',
    validator: v => ['primary', 'success', 'warning', 'danger', 'info', 'section'].includes(v)
  },
  round: Boolean,
  circle: Boolean,
  plain: Boolean,
  loading: Boolean,
  disabled: Boolean,
  nativeType: {
    type: String,
    default: 'button'
  },
  color: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['click'])

const isDisabled = computed(() => props.disabled || props.loading)

const buttonClasses = computed(() => {
  const classes = {}
  if (props.type && props.type !== 'primary') {
    classes[`cx-button--${props.type}`] = true
  }
  if (props.round) classes['is-round'] = true
  if (props.circle) classes['is-circle'] = true
  if (props.plain) classes['is-plain'] = true
  if (props.loading) classes['is-loading'] = true
  if (props.disabled) classes['is-disabled'] = true
  return classes
})

const customStyle = computed(() => {
  if (!props.color) return undefined
  return props.color
})

function handleClick(e) {
  if (!isDisabled.value) {
    emit('click', e)
  }
}
</script>
