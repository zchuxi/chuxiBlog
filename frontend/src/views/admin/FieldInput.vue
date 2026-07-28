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

    <!-- 下拉选择 -->
    <select
      v-else-if="field.type === 'select'"
      class="admin-input admin-select"
      :value="modelValue"
      @change="emit('update:modelValue', $event.target.value)"
    >
      <option v-for="opt in field.options || []" :key="opt" :value="opt">{{ opt }}</option>
    </select>

    <!-- 图片：缩略图 + URL 输入 + 上传/图库 -->
    <div v-else-if="field.type === 'image'" class="admin-img-field">
      <div class="admin-img-thumb" :class="{ 'is-empty': !modelValue }">
        <img v-if="modelValue" :src="modelValue" alt="预览" referrerpolicy="no-referrer" @error="thumbBroken = true" />
        <span v-else>暂无图片</span>
      </div>
      <div class="admin-img-side">
        <input
          class="admin-input"
          type="text"
          :value="modelValue"
          :placeholder="placeholder"
          @input="emit('update:modelValue', $event.target.value)"
        />
        <div class="admin-img-actions">
          <button type="button" class="admin-btn admin-btn-ghost" :disabled="uploading" @click="fileRef?.click()">
            {{ uploading ? '上传中…' : '上传图片' }}
          </button>
          <button type="button" class="admin-btn admin-btn-ghost" @click="pickerOpen = true">从图库选择</button>
        </div>
        <input ref="fileRef" type="file" accept="image/*" hidden @change="onUpload" />
      </div>
      <MediaPicker v-model="pickerOpen" @select="url => emit('update:modelValue', url)" />
    </div>

    <!-- 其余类型统一为文本输入（tags/datetime 带提示） -->
    <template v-else>
      <input
        class="admin-input"
        type="text"
        :value="modelValue"
        :placeholder="placeholder"
        @input="emit('update:modelValue', $event.target.value)"
      />
      <p v-if="field.type === 'tags'" class="admin-field-tip">多个标签用逗号分隔</p>
    </template>
  </div>
</template>

<script setup>
import { computed, inject, ref } from 'vue'
import MediaPicker from './MediaPicker.vue'
import { mediaApi } from '../../api/admin'

const props = defineProps({
  field: { type: Object, required: true },
  modelValue: { type: [String, Number, Boolean], default: '' },
  disabled: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue'])

// 图库选择弹窗开关（仅 image 字段用）
const pickerOpen = ref(false)
const fileRef = ref(null)
const uploading = ref(false)
const thumbBroken = ref(false)
const toast = inject('adminToast', () => {})

// 直接上传并回填 URL，省去先去图片库再回来的往返
async function onUpload(e) {
  const file = e.target.files && e.target.files[0]
  e.target.value = ''
  if (!file) return
  uploading.value = true
  try {
    const data = await mediaApi.upload(file)
    emit('update:modelValue', data.url)
    thumbBroken.value = false
    toast('图片已上传')
  } catch (err) {
    toast((err && err.message) || '上传失败', 'error')
  } finally {
    uploading.value = false
  }
}

const placeholder = computed(() => {
  if (props.field.type === 'datetime') return '如 2026-03-01T12:00:00'
  if (props.field.type === 'image') return '图片 URL'
  return ''
})
</script>
