<template>
  <section class="scp-panel">
    <header class="scp-head">
      <div>
        <h2 class="scp-head-title">站点设置</h2>
        <p class="scp-head-sub">管理站名、副标题、SEO、社交链接与页脚信息。保存后前台即时生效。</p>
      </div>
      <button class="admin-btn" type="button" :disabled="saving || loading" @click="save">
        {{ saving ? '保存中…' : '保存' }}
      </button>
    </header>

    <div v-if="loading" class="scp-card admin-state">加载中…</div>

    <template v-else>
      <!-- 基本信息 -->
      <div class="scp-card">
        <p class="scp-section-label">基本信息</p>
        <div class="admin-field">
          <label class="admin-field-label">站名</label>
          <input v-model="form.siteName" class="admin-input" type="text" placeholder="如：初曦的窝" />
        </div>
        <div class="admin-field">
          <label class="admin-field-label">副标题</label>
          <input v-model="form.subtitle" class="admin-input" type="text" placeholder="站点副标题 / 标语" />
        </div>
        <div class="scp-grid-2">
          <div class="admin-field">
            <label class="admin-field-label">Logo 地址</label>
            <ImageSelect v-model="form.logoUrl" placeholder="/favicon.png" />
          </div>
          <div class="admin-field">
            <label class="admin-field-label">Favicon 地址</label>
            <ImageSelect v-model="form.faviconUrl" placeholder="/favicon.png" />
          </div>
        </div>
        <div class="admin-field">
          <label class="admin-field-label">关于页头像</label>
          <ImageSelect v-model="form.avatarUrl" placeholder="请输入或选择关于页头像图片地址" />
        </div>
      </div>

      <!-- SEO -->
      <div class="scp-card">
        <p class="scp-section-label">SEO</p>
        <div class="admin-field">
          <label class="admin-field-label">页面描述</label>
          <textarea v-model="form.seoDescription" class="admin-input admin-textarea" rows="3" placeholder="搜索引擎摘要描述"></textarea>
        </div>
        <div class="admin-field">
          <label class="admin-field-label">关键词</label>
          <input v-model="form.seoKeywords" class="admin-input" type="text" placeholder="用英文逗号分隔，如：博客,追番,工具" />
        </div>
      </div>

      <!-- 社交链接 -->
      <div class="scp-card">
        <p class="scp-section-label">社交链接</p>
        <div class="scp-grid-2">
          <div class="admin-field">
            <label class="admin-field-label">GitHub</label>
            <input v-model="form.githubUrl" class="admin-input" type="text" placeholder="https://github.com/..." />
          </div>
          <div class="admin-field">
            <label class="admin-field-label">微博</label>
            <input v-model="form.weiboUrl" class="admin-input" type="text" placeholder="https://weibo.com/..." />
          </div>
        </div>
        <div class="admin-field">
          <label class="admin-field-label">QQ</label>
          <input v-model="form.qqUrl" class="admin-input" type="text" placeholder="QQ 链接或群号" />
        </div>
      </div>

      <!-- 页脚 -->
      <div class="scp-card">
        <p class="scp-section-label">页脚</p>
        <div class="admin-field">
          <label class="admin-field-label">页脚文案</label>
          <input v-model="form.footerText" class="admin-input" type="text" placeholder="页脚版权 / 标语" />
        </div>
        <div class="admin-field">
          <label class="admin-field-label">备案号</label>
          <input v-model="form.footerIcp" class="admin-input" type="text" placeholder="如：京ICP备XXXXXXXX号" />
        </div>
      </div>
    </template>
  </section>
</template>

<script setup>
import { inject, onMounted, ref } from 'vue'
import { siteContentApi } from '../../api/admin'
import ImageSelect from './ImageSelect.vue'

const CONTENT_KEY = 'site-settings'

const toast = inject('adminToast', () => {})
const onUnauthorized = inject('adminUnauthorized', () => {})

const loading = ref(false)
const saving = ref(false)

const DEFAULT_FORM = {
  siteName: '初曦的窝',
  subtitle: '收集工具、追番与灵感碎片的小小基地。',
  logoUrl: '/favicon.png',
  faviconUrl: '/favicon.png',
  avatarUrl: '',
  seoDescription: '',
  seoKeywords: '',
  githubUrl: '',
  weiboUrl: '',
  qqUrl: '',
  footerText: '',
  footerIcp: ''
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
    siteName: obj.siteName || DEFAULT_FORM.siteName,
    subtitle: obj.subtitle || DEFAULT_FORM.subtitle,
    logoUrl: obj.logoUrl || DEFAULT_FORM.logoUrl,
    faviconUrl: obj.faviconUrl || DEFAULT_FORM.faviconUrl,
    avatarUrl: obj.avatarUrl || '',
    seoDescription: obj.seoDescription || '',
    seoKeywords: obj.seoKeywords || '',
    githubUrl: obj.githubUrl || '',
    weiboUrl: obj.weiboUrl || '',
    qqUrl: obj.qqUrl || '',
    footerText: obj.footerText || '',
    footerIcp: obj.footerIcp || ''
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
    toast('站点设置已保存，前台已生效')
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

<style>
.scp-section-label {
  margin: 0 0 14px;
  font-size: 13px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #8aa5c4;
}

html.dark .scp-section-label {
  color: #6b7fa0;
}
</style>
