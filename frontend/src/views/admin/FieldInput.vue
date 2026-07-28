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

    <!-- 下拉选择（自绘面板，主题一致） -->
    <AdminSelect
      v-else-if="field.type === 'select'"
      :model-value="modelValue"
      :options="field.options || []"
      @update:model-value="v => emit('update:modelValue', v)"
    />

    <!-- 音频：试听 + URL 输入 + 导入音频文件 -->
    <div v-else-if="field.type === 'audio'" class="admin-audio-field">
      <input
        class="admin-input"
        type="text"
        :value="modelValue"
        placeholder="音频 URL，可直接粘贴或点击下方导入"
        @input="emit('update:modelValue', $event.target.value)"
      />
      <div class="admin-img-actions">
        <button type="button" class="admin-btn admin-btn-ghost" :disabled="uploading" @click="fileRef?.click()">
          {{ uploading ? `上传中…${uploadPercent}%` : '导入音频文件' }}
        </button>
        <audio v-if="modelValue" class="admin-audio-preview" :src="modelValue" controls preload="none"></audio>
      </div>
      <input ref="fileRef" type="file" accept="audio/*" hidden @change="onUploadAudio" />
    </div>

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
          <button v-if="canCrop" type="button" class="admin-btn admin-btn-ghost" :disabled="fetching" @click="openCrop">
            {{ fetching ? '取回中…' : '裁切' }}
          </button>
        </div>
        <input ref="fileRef" type="file" accept="image/*" hidden @change="onUpload" />
      </div>
      <MediaPicker v-model="pickerOpen" @select="url => emit('update:modelValue', url)" />
      <CropDialog
        v-if="cropTarget"
        :item="cropTarget"
        :ratio="field.ratio || 0"
        @close="cropTarget = null"
        @saved="onCropped"
      />
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
import CropDialog from './CropDialog.vue'
import AdminSelect from './AdminSelect.vue'
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
const uploadPercent = ref(0)
const thumbBroken = ref(false)
const cropTarget = ref(null)
// 取回外链中的 loading：拉远端图转副本期间防止重复点「裁切」
const fetching = ref(false)
const toast = inject('adminToast', () => {})

// 任何 HTTP 图都可裁：站内 /api/uploads/ 直接打开；外链（如 OSS）先让后端取回转本地副本再裁
const canCrop = computed(
  () => typeof props.modelValue === 'string' && /^https?:\/\//.test(props.modelValue)
)

async function openCrop() {
  const raw = props.modelValue
  const name = decodeURIComponent((raw.split('?')[0].split('/').pop() || ''))
  if (!name) return
  if (raw.startsWith('/api/uploads/')) {
    cropTarget.value = { name, url: raw }
    return
  }
  // 外链：先让后端下载到站内，再打开裁切（canvas 跨域会被污染，无法直接 toBlob）
  fetching.value = true
  try {
    const data = await mediaApi.fetch(raw)
    if (data && data.url) {
      emit('update:modelValue', data.url)
      thumbBroken.value = false
      cropTarget.value = { name: data.name || name, url: data.url }
    }
  } catch (err) {
    toast((err && err.message) || '取回失败，请稍后重试', 'error')
  } finally {
    fetching.value = false
  }
}

// 裁切保存为新图后直接回填字段
function onCropped(data) {
  if (data && data.url) {
    emit('update:modelValue', data.url)
    thumbBroken.value = false
    toast('裁切完成，已回填新图')
  }
}

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

// 音频导入：文件大，带上传进度提示
async function onUploadAudio(e) {
  const file = e.target.files && e.target.files[0]
  e.target.value = ''
  if (!file) return
  uploading.value = true
  uploadPercent.value = 0
  try {
    const data = await mediaApi.upload(file, file.name, p => (uploadPercent.value = p))
    emit('update:modelValue', data.url)
    toast('音频已导入')
  } catch (err) {
    toast((err && err.message) || '导入失败', 'error')
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
