<template>
  <div
    class="cx-input"
    :class="shellClasses"
  >
    <textarea
      v-if="type === 'textarea'"
      ref="innerRef"
      class="cx-input__inner cx-input__textarea"
      :id="id"
      :name="name"
      :value="modelValue"
      :rows="rows"
      :placeholder="placeholder"
      :disabled="disabled"
      :readonly="readonly"
      :maxlength="maxlength"
      :aria-invalid="ariaInvalid"
      :aria-describedby="ariaDescribedby"
      :aria-required="ariaRequired"
      @input="onInput"
      @change="onChange"
      @focus="emit('focus', $event)"
      @blur="emit('blur', $event)"
      @keydown="emit('keydown', $event)"
      @keyup="emit('keyup', $event)"
    ></textarea>
    <input
      v-else
      ref="innerRef"
      class="cx-input__inner"
      :id="id"
      :name="name"
      :type="type"
      :value="modelValue"
      :placeholder="placeholder"
      :disabled="disabled"
      :readonly="readonly"
      :maxlength="maxlength"
      :min="min"
      :max="max"
      :step="step"
      :list="list"
      :autocomplete="autocomplete"
      :aria-invalid="ariaInvalid"
      :aria-describedby="ariaDescribedby"
      :aria-required="ariaRequired"
      @input="onInput"
      @change="onChange"
      @focus="emit('focus', $event)"
      @blur="emit('blur', $event)"
      @keydown="emit('keydown', $event)"
      @keyup="emit('keyup', $event)"
    />
  </div>
</template>

<script setup>
/**
 * CxInput —— 全站通用输入框（前台 + 后台共用）
 *
 * 结构与 .cx-input / .cx-input__inner 类名沿用前台既有样式，
 * 前台此前手写的 <div class="cx-input"><input class="cx-input__inner">
 * 可直接替换为本组件，视觉不变。
 *
 * variant 决定皮肤，而非各端重复实现控件：
 *   default —— 前台拟物风：42px 高、内阴影、focus-within 上浮
 *   admin   —— 后台表单风：紧凑 padding、方角、--adm-focus-ring 焦点环
 * 后台表单字段密集且常处于表格/弹窗内，套用前台的上浮动效会整列抖动，
 * 所以皮肤分支保留，共用的是组件与无障碍契约。
 *
 * 数字输入统一交给 type="number"：原生步进箭头由 CSS 隐藏（系统白底控件无法换肤）。
 */
import { computed, ref } from 'vue'

const props = defineProps({
  modelValue: {
    type: [String, Number],
    default: ''
  },
  type: {
    type: String,
    default: 'text'
  },
  variant: {
    type: String,
    default: 'default',
    validator: v => ['default', 'admin'].includes(v)
  },
  rows: {
    type: [String, Number],
    default: 3
  },
  placeholder: {
    type: String,
    default: ''
  },
  disabled: Boolean,
  readonly: Boolean,
  /** 数字输入用 modifier：'number' 走 Number()，'trim' 去首尾空格 */
  modelModifier: {
    type: String,
    default: ''
  },
  maxlength: {
    type: [String, Number],
    default: undefined
  },
  min: {
    type: [String, Number],
    default: undefined
  },
  max: {
    type: [String, Number],
    default: undefined
  },
  step: {
    type: [String, Number],
    default: undefined
  },
  /** 关联 <datalist> 的 id：根元素是 div，透传属性到不了内层 input，只能走 prop */
  list: {
    type: String,
    default: undefined
  },
  autocomplete: {
    type: String,
    default: undefined
  },
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

const emit = defineEmits(['update:modelValue', 'change', 'focus', 'blur', 'keydown', 'keyup'])

const innerRef = ref(null)

const shellClasses = computed(() => ({
  'cx-input--admin': props.variant === 'admin',
  'is-textarea': props.type === 'textarea',
  'is-disabled': props.disabled
}))

/** 与 v-model.number / v-model.trim 等价：由调用端声明 modelModifier */
function normalize(raw) {
  if (props.modelModifier === 'number') {
    if (raw === '') return null
    const num = Number(raw)
    return Number.isNaN(num) ? raw : num
  }
  if (props.modelModifier === 'trim') return raw.trim()
  return raw
}

function onInput(event) {
  emit('update:modelValue', normalize(event.target.value))
}

function onChange(event) {
  emit('change', normalize(event.target.value))
}

defineExpose({
  focus: () => innerRef.value?.focus(),
  blur: () => innerRef.value?.blur(),
  get el() {
    return innerRef.value
  }
})
</script>
