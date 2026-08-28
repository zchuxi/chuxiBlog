<template>
  <section class="scp-panel">
    <header class="scp-head">
      <div>
        <h2 class="scp-head-title">外观设置</h2>
        <p class="scp-head-sub">管理主题色、默认主题与页面特效开关。保存后前台即时生效。</p>
      </div>
      <CxButton :disabled="saving || loading" @click="save">
        {{ saving ? '保存中…' : '保存' }}
      </CxButton>
    </header>

    <div v-if="loading" class="scp-card admin-state">加载中…</div>

    <template v-else>
      <div class="scp-card">
        <p class="scp-section-label">主题与颜色</p>
        <div class="scp-grid-2">
          <div class="admin-field">
            <label class="admin-field-label">主题色</label>
            <div class="appearance-color-row">
              <input v-model="form.primaryColor" type="color" class="appearance-color-input" />
              <CxInput v-model="form.primaryColor" variant="admin" placeholder="#ff6b81" />
            </div>
          </div>
          <div class="admin-field">
            <label class="admin-field-label">默认主题</label>
            <AdminSelect v-model="form.defaultTheme" :options="THEME_OPTIONS" />
          </div>
        </div>
      </div>

      <div class="scp-card">
        <p class="scp-section-label">页面特效</p>
        <div class="appearance-switch-list">
          <label class="appearance-switch-item">
            <span>
              <strong>樱花飘落</strong>
              <p>粉色花瓣飘落动效</p>
            </span>
            <CxCheckbox v-model="form.sakuraEnabled" />
          </label>
          <label class="appearance-switch-item">
            <span>
              <strong>看板娘</strong>
              <p>页面右下角 Live2D 角色</p>
            </span>
            <CxCheckbox v-model="form.live2dEnabled" />
          </label>
          <label class="appearance-switch-item">
            <span>
              <strong>音乐播放器</strong>
              <p>底部音乐播放条</p>
            </span>
            <CxCheckbox v-model="form.musicEnabled" />
          </label>
        </div>
      </div>
    </template>
  </section>
</template>

<script setup>
import { inject, onMounted, ref } from 'vue'
import { siteContentApi } from '../../api/admin'
import CxButton from '../../components/cx/CxButton.vue'
import CxInput from '../../components/cx/CxInput.vue'
import CxCheckbox from '../../components/cx/CxCheckbox.vue'
import AdminSelect from './AdminSelect.vue'
import { THEME_OPTIONS } from './constants'

const CONTENT_KEY = 'appearance-settings'

const toast = inject('adminToast', () => {})
const onUnauthorized = inject('adminUnauthorized', () => {})

const loading = ref(false)
const saving = ref(false)

const DEFAULT_FORM = {
  primaryColor: '#ff6b81',
  defaultTheme: 'system',
  sakuraEnabled: true,
  live2dEnabled: true,
  musicEnabled: true
}

const form = ref({ ...DEFAULT_FORM })

function parseContent(data) {
  try {
    if (!data) return null
    if (typeof data === 'string') return JSON.parse(data)
    if (typeof data.contentJson === 'string') return JSON.parse(data.contentJson)
    if (typeof data === 'object') return data
    return null
  } catch {
    return null
  }
}

function fillForm(obj) {
  if (!obj) {
    form.value = { ...DEFAULT_FORM }
    return
  }
  form.value = {
    primaryColor: obj.primaryColor || DEFAULT_FORM.primaryColor,
    defaultTheme: obj.defaultTheme || DEFAULT_FORM.defaultTheme,
    sakuraEnabled: obj.sakuraEnabled !== undefined ? !!obj.sakuraEnabled : DEFAULT_FORM.sakuraEnabled,
    live2dEnabled: obj.live2dEnabled !== undefined ? !!obj.live2dEnabled : DEFAULT_FORM.live2dEnabled,
    musicEnabled: obj.musicEnabled !== undefined ? !!obj.musicEnabled : DEFAULT_FORM.musicEnabled
  }
}

async function load() {
  loading.value = true
  try {
    const data = await siteContentApi.get(CONTENT_KEY)
    fillForm(parseContent(data))
  } catch (err) {
    if (err && err.unauthorized) {
      onUnauthorized && onUnauthorized()
      return
    }
    fillForm(null)
  } finally {
    loading.value = false
  }
}

async function save() {
  saving.value = true
  try {
    await siteContentApi.save(CONTENT_KEY, JSON.stringify(form.value))
    toast('外观设置已保存，前台已生效')
  } catch (err) {
    if (err && err.unauthorized) {
      onUnauthorized && onUnauthorized()
      return
    }
    toast((err && err.message) || '保存失败', 'error')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.appearance-color-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.appearance-color-input {
  flex: none;
  width: 42px;
  height: 38px;
  padding: 2px;
  border: 1px solid var(--adm-border, rgba(63, 119, 181, 0.18));
  border-radius: 10px;
  cursor: pointer;
  background: transparent;
}

.appearance-color-input::-webkit-color-swatch-wrapper {
  padding: 2px;
}

.appearance-color-input::-webkit-color-swatch {
  border: none;
  border-radius: 6px;
}

.appearance-switch-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.appearance-switch-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border-radius: 14px;
  border: 1px solid var(--adm-border, rgba(63, 119, 181, 0.12));
  background: var(--adm-card-bg, rgba(63, 119, 181, 0.04));
  cursor: pointer;
}

.appearance-switch-item strong {
  display: block;
  font-size: 15px;
  margin-bottom: 2px;
}

.appearance-switch-item p {
  margin: 0;
  font-size: 13px;
  opacity: 0.6;
}

html.dark .appearance-switch-item {
  border-color: rgba(255, 255, 255, 0.08);
  background: rgba(28, 36, 52, 0.6);
}
</style>
