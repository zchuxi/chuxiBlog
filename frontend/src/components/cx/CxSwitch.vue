<template>
  <button
    class="cx-switch"
    :class="{ 'is-checked': modelValue, 'is-disabled': disabled }"
    type="button"
    role="switch"
    :aria-checked="modelValue ? 'true' : 'false'"
    :disabled="disabled"
    @click="handleClick"
  >
    <span class="cx-switch__core"><span class="cx-switch__action"></span></span>
  </button>
</template>

<script setup>
const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  disabled: Boolean
})

const emit = defineEmits(['update:modelValue', 'change'])

function handleClick() {
  if (!props.disabled) {
    const newVal = !props.modelValue
    emit('update:modelValue', newVal)
    emit('change', newVal)
  }
}
</script>
