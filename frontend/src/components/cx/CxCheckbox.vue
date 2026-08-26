<template>
  <input
    ref="innerRef"
    class="cx-checkbox"
    :class="checkboxClasses"
    type="checkbox"
    :id="id"
    :name="name"
    :checked="isChecked"
    :disabled="disabled"
    :aria-invalid="ariaInvalid"
    :aria-describedby="ariaDescribedby"
    :aria-required="ariaRequired"
    @change="onChange"
  />
</template>

<script setup>
/**
 * CxCheckbox —— 全站通用复选框（前台 + 后台共用）
 *
 * 原生 checkbox 只能靠 accent-color 换色，在暗色下是突兀的白方块，
 * 所以自绘：appearance:none + ::after 画勾/半选横线，跟随主题令牌。
 *
 * indeterminate 是 DOM 属性、无对应 HTML attribute，
 * 必须命令式写入（列表表头「跨页部分选中」依赖它）。
 *
 * size='small' 供表单内联使用（16px），默认 18px 供列表多选。
 */
import { computed, onMounted, ref, watch } from 'vue'

const props = defineProps({
  modelValue: {
    type: [Boolean, Array],
    default: false
  },
  /** modelValue 为数组时，选中即把该值收进数组 */
  value: {
    type: [String, Number, Boolean],
    default: undefined
  },
  size: {
    type: String,
    default: '',
    validator: v => !v || ['small'].includes(v)
  },
  indeterminate: Boolean,
  disabled: Boolean,
  id: {
    type: String,
    default: undefined
  },
  name: {
    type: String,
    default: undefined
  },
  ariaInvalid: {
    type: [Boolean, String],
    default: undefined
  },
  ariaDescribedby: {
    type: String,
    default: undefined
  },
  ariaRequired: {
    type: [Boolean, String],
    default: undefined
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

const innerRef = ref(null)

const checkboxClasses = computed(() => ({
  'cx-checkbox--small': props.size === 'small'
}))

const isChecked = computed(() => {
  if (Array.isArray(props.modelValue)) return props.modelValue.includes(props.value)
  return Boolean(props.modelValue)
})

// indeterminate 只能通过 DOM 属性设置，attribute 无效
function syncIndeterminate() {
  if (innerRef.value) innerRef.value.indeterminate = props.indeterminate
}
onMounted(syncIndeterminate)
watch(() => props.indeterminate, syncIndeterminate)

function onChange(event) {
  const checked = event.target.checked
  if (Array.isArray(props.modelValue)) {
    const next = checked
      ? [...props.modelValue, props.value]
      : props.modelValue.filter(item => item !== props.value)
    emit('update:modelValue', next)
    emit('change', next)
    return
  }
  emit('update:modelValue', checked)
  emit('change', checked)
}
</script>
