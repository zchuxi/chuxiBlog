<template>
  <span
    class="cx-tag"
    :class="tagClasses"
    @click="handleClick"
  >
    <span class="cx-tag__content">
      <span v-if="prefix" class="cx-tag__prefix">{{ prefix }}</span>
      <span class="cx-tag__label"><slot /></span>
    </span>
    <span v-if="closable" class="cx-tag__close" @click.stop="handleClose">×</span>
  </span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  type: {
    type: String,
    default: 'primary',
    validator: v => ['primary', 'success', 'warning', 'danger', 'neutral', 'section'].includes(v)
  },
  size: {
    type: String,
    default: '',
    validator: v => !v || ['small', 'large'].includes(v)
  },
  closable: Boolean,
  plain: Boolean,
  round: Boolean,
  prefix: {
    type: String,
    default: ''
  },
  disabled: Boolean
})

const emit = defineEmits(['close', 'click'])

const tagClasses = computed(() => {
  const classes = {}
  if (props.type) classes[`cx-tag--${props.type}`] = true
  if (props.size) classes[`cx-tag--${props.size}`] = true
  if (props.plain) classes['is-plain'] = true
  if (props.round) classes['is-round'] = true
  if (props.closable) classes['is-closable'] = true
  if (props.disabled) classes['is-disabled'] = true
  return classes
})

function handleClose(e) {
  if (!props.disabled) {
    emit('close', e)
  }
}

function handleClick(e) {
  if (!props.disabled) {
    emit('click', e)
  }
}
</script>
