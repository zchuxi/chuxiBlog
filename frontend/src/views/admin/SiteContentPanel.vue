<template>
  <section class="scp-panel">
    <!-- 顶部说明卡 -->
    <header class="scp-head">
      <div>
        <h2 class="scp-head-title">{{ meta.title }}</h2>
        <p class="scp-head-sub">{{ meta.desc }}</p>
      </div>
      <button class="admin-btn" type="button" :disabled="saving || loading" @click="save">
        {{ saving ? '保存中…' : '保存' }}
      </button>
    </header>

    <div v-if="loading" class="scp-card admin-state">加载中…</div>

    <!-- home-landing：首页落地页文案 -->
    <div v-else-if="contentKey === 'home-landing'" class="scp-card">
      <div class="admin-field">
        <label class="admin-field-label">主标题</label>
        <input v-model="form.title" class="admin-input" type="text" placeholder="首页大标题" />
      </div>
      <div class="admin-field">
        <label class="admin-field-label">副标题</label>
        <input v-model="form.subtitle" class="admin-input" type="text" placeholder="标题下方的一句话" />
      </div>
      <div class="admin-field">
        <label class="admin-field-label">欢迎语（每行一条，会轮流展示）</label>
        <textarea v-model="form.welcomeText" class="admin-input admin-textarea" rows="5" placeholder="第一行是一条&#10;第二行是另一条"></textarea>
      </div>
      <div class="scp-grid-2">
        <div class="admin-field">
          <label class="admin-field-label">主按钮文字</label>
          <input v-model="form.primaryBtn" class="admin-input" type="text" placeholder="如：开始探索" />
        </div>
        <div class="admin-field">
          <label class="admin-field-label">次按钮文字</label>
          <input v-model="form.secondaryBtn" class="admin-input" type="text" placeholder="如：了解更多" />
        </div>
        <div class="admin-field">
          <label class="admin-field-label">主按钮跳转目标</label>
          <AdminSelect v-model="form.primaryTarget" :options="PAGE_TARGETS" />
        </div>
        <div class="admin-field">
          <label class="admin-field-label">次按钮跳转目标</label>
          <AdminSelect v-model="form.secondaryTarget" :options="PAGE_TARGETS" />
        </div>
      </div>
    </div>

    <!-- archive-hero：归档页顶部文案 -->
    <div v-else-if="contentKey === 'archive-hero'" class="scp-card">
      <div class="admin-field">
        <label class="admin-field-label">眉标（标题上方的小字）</label>
        <input v-model="form.eyebrow" class="admin-input" type="text" placeholder="如 ARCHIVE" />
      </div>
      <div class="admin-field">
        <label class="admin-field-label">标题</label>
        <input v-model="form.title" class="admin-input" type="text" placeholder="归档页标题" />
      </div>
      <div class="admin-field">
        <label class="admin-field-label">描述</label>
        <textarea v-model="form.description" class="admin-input admin-textarea" rows="3" placeholder="归档页顶部的介绍文字"></textarea>
      </div>
      <div class="admin-field">
        <label class="admin-field-label">要点（每行一条）</label>
        <textarea v-model="form.notesText" class="admin-input admin-textarea" rows="4" placeholder="第一条要点&#10;第二条要点"></textarea>
      </div>
    </div>

    <!-- about：关于页（左编右预览） -->
    <template v-else>
      <div class="scp-card">
        <div class="admin-field">
          <label class="admin-field-label">页面标题</label>
          <input v-model="form.title" class="admin-input" type="text" placeholder="关于页标题" />
        </div>
      </div>
      <div class="scp-about-grid">
        <div class="scp-card">
          <p class="scp-col-label">正文（Markdown）</p>
          <textarea
            v-model="form.markdown"
            class="admin-input admin-textarea scp-md-input"
            placeholder="# 关于我&#10;&#10;在这里介绍你自己…"
          ></textarea>
        </div>
        <div class="scp-card scp-preview-card">
          <p class="scp-col-label">实时预览</p>
          <div class="md-editor md-editor-previewOnly" :class="{ 'md-editor-dark': settings.isDark }">
            <div class="md-editor-preview default-theme md-editor-scrn" v-html="previewHtml"></div>
          </div>
        </div>
      </div>
    </template>
  </section>
</template>

<script setup>
import { computed, inject, onMounted, ref, watch } from 'vue'
import { siteContentApi } from '../../api/admin'
import { renderMarkdown } from '../../utils/markdown'
import { useSettingsStore } from '../../stores/settings'
import AdminSelect from './AdminSelect.vue'

const props = defineProps({
  // home-landing | about | archive-hero
  contentKey: { type: String, required: true }
})

const toast = inject('adminToast', () => {})
const onUnauthorized = inject('adminUnauthorized', () => {})
const settings = useSettingsStore()

const META = {
  'home-landing': {
    title: '首页内容',
    desc: '此处内容对应前台首页落地页：大标题、副标题、轮播欢迎语与两个按钮的文字和跳转目标。'
  },
  about: {
    title: '关于页',
    desc: '此处内容对应前台「关于」页面：页面标题与 Markdown 正文。'
  },
  'archive-hero': {
    title: '归档页文案',
    desc: '此处内容对应前台归档页顶部：眉标、标题、描述与要点列表。'
  }
}

const meta = computed(() => META[props.contentKey] || { title: '站点文案', desc: '' })

// 按钮可选的跳转目标：# 开头为首页内锚点滚动，其余为站内路由
const PAGE_TARGETS = [
  { label: '首页 · 滚动到文章列表', value: '#articles' },
  { label: '首页 · 滚动到首屏轮播', value: '#hero' },
  { label: '关于页', value: '/about' },
  { label: '工具页', value: '/tool' },
  { label: '番剧记录', value: '/bangumi' },
  { label: '每日放送', value: '/calendar' },
  { label: '时间线', value: '/timeline' },
  { label: '树洞', value: '/tree-hole' },
  { label: '视差故事', value: '/parallax' },
  { label: '归档页', value: '/archive' },
  { label: '组件展示', value: '/components' }
]

const loading = ref(false)
const saving = ref(false)
const form = ref(defaultForm(props.contentKey))

// 各 key 的默认结构（表单态：数组字段用多行文本承载）
function defaultForm(key) {
  if (key === 'home-landing') {
    return { title: '', subtitle: '', welcomeText: '', primaryBtn: '', secondaryBtn: '', primaryTarget: '#articles', secondaryTarget: '/about' }
  }
  if (key === 'archive-hero') {
    return { eyebrow: '', title: '', description: '', notesText: '' }
  }
  return { title: '', markdown: '' }
}

// 多行文本 <-> 数组
function toLines(arr) {
  return Array.isArray(arr) ? arr.join('\n') : ''
}

function fromLines(text) {
  return String(text || '')
    .split('\n')
    .map(s => s.trim())
    .filter(Boolean)
}

// 后端返回可能是 SiteContent 记录（contentJson 为 JSON 字符串），也兼容直接返回对象
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
  const key = props.contentKey
  const base = defaultForm(key)
  if (!obj) {
    form.value = base
    return
  }
  if (key === 'home-landing') {
    form.value = {
      title: obj.title || '',
      subtitle: obj.subtitle || '',
      welcomeText: toLines(obj.welcome),
      primaryBtn: obj.primaryBtn || '',
      secondaryBtn: obj.secondaryBtn || '',
      primaryTarget: obj.primaryTarget || '#articles',
      secondaryTarget: obj.secondaryTarget || '/about'
    }
  } else if (key === 'archive-hero') {
    form.value = {
      eyebrow: obj.eyebrow || '',
      title: obj.title || '',
      description: obj.description || '',
      notesText: toLines(obj.notes)
    }
  } else {
    form.value = {
      title: obj.title || '',
      markdown: obj.markdown || ''
    }
  }
}

function buildContent() {
  const key = props.contentKey
  const f = form.value
  if (key === 'home-landing') {
    return {
      title: f.title || '',
      subtitle: f.subtitle || '',
      welcome: fromLines(f.welcomeText),
      primaryBtn: f.primaryBtn || '',
      secondaryBtn: f.secondaryBtn || '',
      primaryTarget: f.primaryTarget || '#articles',
      secondaryTarget: f.secondaryTarget || '/about'
    }
  }
  if (key === 'archive-hero') {
    return {
      eyebrow: f.eyebrow || '',
      title: f.title || '',
      description: f.description || '',
      notes: fromLines(f.notesText)
    }
  }
  return {
    title: f.title || '',
    markdown: f.markdown || ''
  }
}

const previewHtml = computed(() => {
  try {
    return renderMarkdown(form.value.markdown || '').html
  } catch {
    return '<p>预览渲染失败</p>'
  }
})

async function load() {
  loading.value = true
  try {
    const data = await siteContentApi.get(props.contentKey)
    fillForm(parseContent(data))
  } catch (err) {
    if (err && err.unauthorized) {
      onUnauthorized && onUnauthorized()
      return
    }
    // 无记录（code 1）视为空，使用默认结构
    fillForm(null)
  } finally {
    loading.value = false
  }
}

async function save() {
  saving.value = true
  try {
    await siteContentApi.save(props.contentKey, JSON.stringify(buildContent()))
    toast('保存成功，前台已生效')
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

watch(
  () => props.contentKey,
  () => {
    form.value = defaultForm(props.contentKey)
    load()
  }
)

onMounted(load)
</script>

<style>
/* ---------- 站点文案面板（scp-*，明暗双主题） ---------- */

.scp-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.scp-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 22px;
  background-color: #fff;
  border-radius: 18px;
  box-shadow: 0 8px 24px rgba(63, 119, 181, 0.08);
}

.scp-head-title {
  margin: 0 0 4px;
  font-size: 20px;
  color: #3f77b5;
}

.scp-head-sub {
  margin: 0;
  font-size: 14.5px;
  color: rgba(31, 49, 72, 0.55);
}

.scp-card {
  background-color: #fff;
  border-radius: 18px;
  box-shadow: 0 8px 24px rgba(63, 119, 181, 0.08);
  padding: 18px 22px;
}

.scp-grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 14px;
}

.scp-about-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.scp-col-label {
  margin: 0 0 10px;
  font-size: 13px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #8aa5c4;
}

/* 双类选择器压过 admin.css 的 .admin-textarea{min-height:72px}（同特异性按加载顺序取胜会失效） */
.admin-textarea.scp-md-input {
  min-height: 62vh;
  font-size: 14.5px;
  line-height: 1.7;
  resize: vertical;
}

.scp-preview-card {
  position: sticky;
  top: 16px;
  max-height: calc(100vh - 120px);
  overflow-y: auto;
}

@media (max-width: 960px) {
  .scp-about-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .scp-preview-card {
    position: static;
    max-height: none;
  }

  .scp-grid-2 {
    grid-template-columns: 1fr;
  }
}

/* ---------- 暗色 ---------- */

html.dark .scp-head,
html.dark .scp-card {
  background: rgba(28, 36, 52, 0.92);
  box-shadow: 0 18px 44px rgba(0, 0, 0, 0.42);
}

html.dark .scp-head-title {
  color: #e8effa;
}

html.dark .scp-head-sub {
  color: #8fa0ba;
}

/* ---------- 移动端适配（≤900px，追加）
   表单单列与 about 编辑/预览上下堆叠已由上方 960px 规则覆盖，
   此处补充头部换行与内边距收紧 ---------- */

@media (max-width: 900px) {
  .scp-head {
    flex-wrap: wrap;
  }

  .scp-head,
  .scp-card {
    padding: 14px 16px;
  }

  .admin-textarea.scp-md-input {
    min-height: 46vh;
  }
}
</style>
