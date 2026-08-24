<template>
  <div v-if="!field.hidden" class="admin-field">
    <label class="admin-field-label" :for="inputId">
      {{ field.label }}
      <span v-if="field.required" class="admin-field-required" aria-hidden="true">*</span>
    </label>

    <!-- 布尔开关 -->
    <CxSwitch
      v-if="field.type === 'boolean'"
      :id="inputId"
      :name="field.name"
      :model-value="!!modelValue"
      :aria-label="field.label"
      :aria-invalid="error ? 'true' : undefined"
      :aria-describedby="describedBy"
      :aria-required="field.required ? 'true' : undefined"
      :disabled="disabled"
      @update:model-value="v => emit('update:modelValue', v)"
    />

    <!-- 多行文本 / Markdown -->
    <textarea
      v-else-if="field.type === 'textarea' || field.type === 'markdown'"
      :id="inputId"
      :name="field.name"
      class="admin-input admin-textarea"
      :class="{ 'admin-md': field.type === 'markdown' }"
      :value="modelValue"
      :rows="field.type === 'markdown' ? 20 : 3"
      :required="field.required"
      :disabled="disabled"
      :aria-invalid="error ? 'true' : undefined"
      :aria-describedby="describedBy"
      @input="emit('update:modelValue', $event.target.value)"
    ></textarea>

    <!-- 数字 -->
    <input
      v-else-if="field.type === 'number'"
      :id="inputId"
      :name="field.name"
      class="admin-input"
      type="number"
      :value="modelValue"
      :required="field.required"
      :disabled="disabled"
      :aria-invalid="error ? 'true' : undefined"
      :aria-describedby="describedBy"
      @input="emit('update:modelValue', $event.target.value)"
    />

    <!-- 日期时间 / 日期：自绘日历（原生弹层不受站点主题控制，且在管理端弹窗内会溢出） -->
    <CxDatePicker
      v-else-if="field.type === 'datetime'"
      :id="inputId"
      :name="field.name"
      type="datetime"
      :model-value="dateValue"
      :disabled="disabled"
      :aria-invalid="error ? 'true' : undefined"
      :aria-describedby="describedBy"
      :aria-required="field.required ? 'true' : undefined"
      @update:model-value="emit('update:modelValue', $event)"
    />
    <CxDatePicker
      v-else-if="field.type === 'date'"
      :id="inputId"
      :name="field.name"
      type="date"
      :model-value="dateValue"
      :disabled="disabled"
      :aria-invalid="error ? 'true' : undefined"
      :aria-describedby="describedBy"
      :aria-required="field.required ? 'true' : undefined"
      @update:model-value="emit('update:modelValue', $event)"
    />

    <!-- 下拉选择（自绘面板，主题一致）；allowCustom 时末尾追加「自定义…」，可手动输入新类型 -->
    <template v-else-if="field.type === 'select'">
      <AdminSelect
        :id="inputId"
        :name="field.name"
        :model-value="selectModelValue"
        :options="selectOptions"
        :disabled="disabled"
        :aria-invalid="error ? 'true' : undefined"
        :aria-describedby="describedBy"
        :aria-required="field.required ? 'true' : undefined"
        @update:model-value="v => emit('update:modelValue', v)"
      />
      <input
        v-if="field.allowCustom && isCustomCategory"
        class="admin-input cx-field-custom-input"
        type="text"
        :value="customCategoryText"
        :placeholder="`输入新的${field.label || '选项'}`"
        :disabled="disabled"
        @input="e => emit('update:modelValue', e.target.value)"
      />
    </template>

    <!-- 音频：试听 + URL 输入 + 导入音频文件 -->
    <div v-else-if="field.type === 'audio'" class="admin-audio-field">
      <input
        :id="inputId"
        :name="field.name"
        class="admin-input"
        type="text"
        :value="modelValue"
        :required="field.required"
        :disabled="disabled"
        :aria-invalid="error ? 'true' : undefined"
        :aria-describedby="describedBy"
        placeholder="音频 URL，可直接粘贴或点击下方导入"
        @input="emit('update:modelValue', $event.target.value)"
      />
      <div class="admin-img-actions">
        <CxButton plain :disabled="disabled || uploading" @click="fileRef?.click()">
          {{ uploading ? `上传中…${uploadPercent}%` : '导入音频文件' }}
        </CxButton>
        <audio v-if="modelValue" class="admin-audio-preview" :src="modelValue" controls preload="none"></audio>
      </div>
      <input ref="fileRef" type="file" accept="audio/*" :disabled="disabled" hidden @change="onUploadAudio" />
    </div>

    <!-- 图片：缩略图 + URL 输入 + 上传/图库 -->
    <div v-else-if="field.type === 'image'" class="admin-img-field">
      <div class="admin-img-thumb" :class="{ 'is-empty': !modelValue }">
        <img v-if="modelValue" :src="modelValue" alt="预览" referrerpolicy="no-referrer" @error="thumbBroken = true" />
        <span v-else>暂无图片</span>
      </div>
      <div class="admin-img-side">
        <input
          :id="inputId"
          :name="field.name"
          class="admin-input"
          type="text"
          :value="modelValue"
          :placeholder="placeholder"
          :required="field.required"
          :disabled="disabled"
          :aria-invalid="error ? 'true' : undefined"
          :aria-describedby="describedBy"
          @input="emit('update:modelValue', $event.target.value)"
        />
        <div class="admin-img-actions">
          <CxButton plain :disabled="disabled || uploading" @click="fileRef?.click()">
            {{ uploading ? '上传中…' : '上传图片' }}
          </CxButton>
          <CxButton plain :disabled="disabled" @click="pickerOpen = true">从图库选择</CxButton>
          <CxButton v-if="canCrop" plain :disabled="disabled || fetching" @click="openCrop">
            {{ fetching ? '取回中…' : '裁切' }}
          </CxButton>
        </div>
        <input ref="fileRef" type="file" accept="image/*" :disabled="disabled" hidden @change="onUpload" />
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

    <!-- 其余类型统一为文本输入（tags 带提示） -->
    <template v-else>
      <input
        :id="inputId"
        :name="field.name"
        class="admin-input"
        type="text"
        :value="modelValue"
        :placeholder="placeholder"
        :required="field.required"
        :disabled="disabled"
        :aria-invalid="error ? 'true' : undefined"
        :aria-describedby="describedBy"
        @input="emit('update:modelValue', $event.target.value)"
      />
    </template>

    <p v-if="displayTip" :id="tipId" class="admin-field-tip">{{ displayTip }}</p>
    <p v-if="error" :id="errorId" class="admin-field-error" role="alert">{{ error }}</p>
  </div>
</template>

<script setup>
import { computed, inject, ref, useId } from 'vue'
import MediaPicker from './MediaPicker.vue'
import CropDialog from './CropDialog.vue'
import AdminSelect from './AdminSelect.vue'
import CxDatePicker from '../../components/cx/CxDatePicker.vue'
import CxSwitch from '../../components/cx/CxSwitch.vue'
import CxButton from '../../components/cx/CxButton.vue'
import { mediaApi } from '../../api/admin'

const props = defineProps({
  field: { type: Object, required: true },
  modelValue: { type: [String, Number, Boolean], default: '' },
  disabled: { type: Boolean, default: false },
  error: { type: String, default: '' }
})
const emit = defineEmits(['update:modelValue'])

// 自定义下拉标记：allowCustom 的 select 在固定选项后追加「自定义…」，选中后展示文本输入框
const CUSTOM_MARKER = '__cx_custom__'

const selectOptions = computed(() => {
  const base = Array.isArray(props.field.options) ? [...props.field.options] : []
  if (props.field.allowCustom) base.push({ label: '自定义…', value: CUSTOM_MARKER })
  return base
})

// 当前值不在固定选项内（且非空）视为自定义值
const isCustomCategory = computed(() => {
  if (!props.field.allowCustom) return false
  if (props.modelValue === '' || props.modelValue == null) return false
  const known = Array.isArray(props.field.options)
    && props.field.options.some(o => (o && typeof o === 'object' ? o.value : o) === props.modelValue)
  return !known
})

const selectModelValue = computed(() => (isCustomCategory.value ? CUSTOM_MARKER : props.modelValue))
const customCategoryText = computed(() => (props.modelValue === CUSTOM_MARKER ? '' : props.modelValue))

const uid = useId()
const inputId = `admin-field-${uid}`
const tipId = `${inputId}-tip`
const errorId = `${inputId}-error`
const displayTip = computed(() => props.field.tip || (props.field.type === 'tags' ? '多个标签用逗号分隔' : ''))
const describedBy = computed(() => [
  displayTip.value ? tipId : '',
  props.error ? errorId : ''
].filter(Boolean).join(' ') || undefined)

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

// 站内图（/api/uploads/）与外链 http(s) 图均可裁：站内直接打开，外链先让后端取回转本地副本再裁
const canCrop = computed(
  () => typeof props.modelValue === 'string'
    && (props.modelValue.startsWith('/api/uploads/') || /^https?:\/\//.test(props.modelValue))
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

// 日历控件只认 "YYYY-MM-DD[THH:mm:ss]"，裁掉后端 LocalDateTime 可能带的纳秒尾巴
const dateValue = computed(() => {
  const v = typeof props.modelValue === 'string' ? props.modelValue : ''
  return props.field.type === 'date' ? v.slice(0, 10) : v.slice(0, 19)
})

const placeholder = computed(() => {
  if (props.field.type === 'image') return '图片 URL'
  return ''
})
</script>

<style scoped>
.cx-field-custom-input {
  margin-top: 6px;
}
</style>
