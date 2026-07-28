<template>
  <div class="admin-field">
    <label class="admin-field-label">{{ field.label }}</label>

    <!-- 布尔开关 -->
    <button
      v-if="field.type === 'boolean'"
      type="button"
      class="admin-switch"
      :class="{ on: !!modelValue }"
      @click="emit('update:modelValue', !modelValue)"
    >
      <span class="admin-switch-dot"></span>
    </button>

    <!-- 多行文本 / Markdown -->
    <textarea
      v-else-if="field.type === 'textarea' || field.type === 'markdown'"
      class="admin-input admin-textarea"
      :class="{ 'admin-md': field.type === 'markdown' }"
      :value="modelValue"
      :rows="field.type === 'markdown' ? 20 : 3"
      @input="emit('update:modelValue', $event.target.value)"
    ></textarea>

    <!-- 数字 -->
    <input
      v-else-if="field.type === 'number'"
      class="admin-input"
      type="number"
      :value="modelValue"
      :disabled="disabled"
      @input="emit('update:modelValue', $event.target.value)"
    />

    <!-- 其余类型统一为文本输入（tags/datetime/image 带提示与预览） -->
    <template v-else>
      <!-- image 字段：URL 输入 + 图库选择 -->
      <div v-if="field.type === 'image'" class="admin-img-row">
        <input
          class="admin-input"
          type="text"
          :value="modelValue"
          :placeholder="placeholder"
          @input="emit('update:modelValue', $event.target.value)"
        />
        <button type="button" class="admin-btn admin-btn-ghost admin-img-pick" @click="pickerOpen = true">图库</button>
      </div>
      <input
        v-else
        class="admin-input"
        type="text"
        :value="modelValue"
        :placeholder="placeholder"
        @input="emit('update:modelValue', $event.target.value)"
      />
      <p v-if="field.type === 'tags'" class="admin-field-tip">多个标签用逗号分隔</p>
      <img
        v-if="field.type === 'image' && modelValue"
        class="admin-img-preview"
        :src="modelValue"
        alt="预览"
      />
      <MediaPicker
        v-if="field.type === 'image'"
        v-model="pickerOpen"
        @select="url => emit('update:modelValue', url)"
      />
    </template>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import MediaPicker from './MediaPicker.vue'

const props = defineProps({
  field: { type: Object, required: true },
  modelValue: { type: [String, Number, Boolean], default: '' },
  disabled: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue'])

// 图库选择弹窗开关（仅 image 字段用）
const pickerOpen = ref(false)

const placeholder = computed(() => {
  if (props.field.type === 'datetime') return '如 2026-03-01T12:00:00'
  if (props.field.type === 'image') return '图片 URL'
  return ''
})
</script>
