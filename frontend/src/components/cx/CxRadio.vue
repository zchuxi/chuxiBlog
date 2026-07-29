<template>
  <label class="cx-radio" :class="{ 'is-checked': isChecked, 'is-disabled': isDisabled }">
    <input
      class="cx-radio__input"
      type="radio"
      :value="value"
      :name="radioName"
      :checked="isChecked"
      :disabled="isDisabled"
      @change="handleChange"
    />
    <span class="cx-radio__icon"><span class="cx-radio__inner"></span></span>
    <span class="cx-radio__label"><slot /></span>
  </label>
</template>

<script setup>
import { inject, computed } from 'vue'

const props = defineProps({
  modelValue: {
    type: [String, Number, Boolean],
    default: ''
  },
  value: {
    type: [String, Number, Boolean],
    required: true
  },
  label: {
    type: String,
    default: ''
  },
  name: {
    type: String,
    default: ''
  },
  disabled: Boolean
})

const emit = defineEmits(['update:modelValue'])

const radioGroup = inject('CxRadioGroup', null)

const isDisabled = computed(() => props.disabled || (radioGroup && radioGroup.disabled.value))
const radioName = computed(() => props.name || (radioGroup && radioGroup.name.value) || '')

const isChecked = computed(() => {
  if (radioGroup) {
    return radioGroup.modelValue.value === props.value
  }
  return props.modelValue === props.value
})

function handleChange() {
  if (!isDisabled.value) {
    if (radioGroup) {
      radioGroup.change(props.value)
    } else {
      emit('update:modelValue', props.value)
    }
  }
}
</script>
