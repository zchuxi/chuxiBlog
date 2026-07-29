<template>
  <div class="cx-radio-group">
    <slot />
  </div>
</template>

<script setup>
import { provide, computed } from 'vue'

const props = defineProps({
  modelValue: {
    type: [String, Number, Boolean],
    default: ''
  },
  name: {
    type: String,
    default: ''
  },
  disabled: Boolean
})

const emit = defineEmits(['update:modelValue'])

provide('CxRadioGroup', {
  modelValue: computed(() => props.modelValue),
  name: computed(() => props.name),
  disabled: computed(() => props.disabled),
  change: (val) => {
    emit('update:modelValue', val)
  }
})
</script>
