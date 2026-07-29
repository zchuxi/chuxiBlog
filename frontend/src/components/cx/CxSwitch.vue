<template>
  <button
    class="lx-switch"
    :class="{ 'is-checked': modelValue, 'is-disabled': disabled }"
    type="button"
    :disabled="disabled"
    @click="handleClick"
  >
    <span class="lx-switch__core"><span class="lx-switch__action"></span></span>
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
