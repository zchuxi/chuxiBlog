<template>
  <section class="scp-panel">
    <header class="scp-head">
      <div>
        <h2 class="scp-head-title">页面文案配置</h2>
        <p class="scp-head-sub">管理各页面顶部文案、提示语和搜索框文字。每个 Tab 对应一个页面的可配置项，保存后立即生效。</p>
      </div>
      <div class="admin-toolbar-actions">
        <CxButton plain :disabled="savingAny" @click="saveAll">
          {{ savingAny ? '保存中…' : '全部保存' }}
        </CxButton>
      </div>
    </header>

    <div v-if="loadingAny" class="scp-card admin-state">加载中…</div>

    <template v-else>
      <!-- Tab 切换 -->
      <div class="pcp-tabs">
        <button
          v-for="tab in TABS"
          :key="tab.key"
          type="button"
          class="pcp-tab"
          :class="{ active: activeTab === tab.key }"
          @click="activeTab = tab.key"
        >
          {{ tab.label }}
        </button>
      </div>

      <!-- timeline-hero -->
      <div v-show="activeTab === 'timeline-hero'" class="scp-card">
        <div class="pcp-section-head">
          <h3>时间线页顶部文案</h3>
          <CxButton :disabled="saving['timeline-hero']" @click="saveKey('timeline-hero')">
            {{ saving['timeline-hero'] ? '保存中…' : '保存' }}
          </CxButton>
        </div>
        <div class="admin-field">
          <label class="admin-field-label">眉标（标题上方小字）</label>
          <CxInput v-model="forms['timeline-hero'].eyebrow" variant="admin" />
        </div>
        <div class="admin-field">
          <label class="admin-field-label">标题</label>
          <CxInput v-model="forms['timeline-hero'].title" variant="admin" />
        </div>
        <div class="admin-field">
          <label class="admin-field-label">描述</label>
          <CxInput v-model="forms['timeline-hero'].description" type="textarea" variant="admin" :rows="3" />
        </div>
      </div>

      <!-- treehole-config -->
      <div v-show="activeTab === 'treehole-config'" class="scp-card">
        <div class="pcp-section-head">
          <h3>树洞页配置</h3>
          <CxButton :disabled="saving['treehole-config']" @click="saveKey('treehole-config')">
            {{ saving['treehole-config'] ? '保存中…' : '保存' }}
          </CxButton>
        </div>
        <div class="admin-field">
          <label class="admin-field-label">输入框占位文字</label>
          <CxInput v-model="forms['treehole-config'].placeholder" variant="admin" />
        </div>
        <div class="admin-field">
          <label class="admin-field-label">情绪选项（每行一个，对应弹幕情绪标签）</label>
          <CxInput v-model="forms['treehole-config'].moodOptionsText" type="textarea" variant="admin" :rows="4" placeholder="轻声&#10;鼓劲&#10;拥抱&#10;放空" />
        </div>
        <div class="admin-field">
          <label class="admin-field-label">底部备注（可选，显示在输入框下方）</label>
          <CxInput v-model="forms['treehole-config'].backgroundNote" variant="admin" />
        </div>
      </div>

      <!-- parallax-config -->
      <div v-show="activeTab === 'parallax-config'" class="scp-card">
        <div class="pcp-section-head">
          <h3>视差页配置</h3>
          <CxButton :disabled="saving['parallax-config']" @click="saveKey('parallax-config')">
            {{ saving['parallax-config'] ? '保存中…' : '保存' }}
          </CxButton>
        </div>
        <p class="admin-field-tip">开场屏和告别屏的文案与背景图，背景图路径从站点根目录开始（如 /image/bg/Landscape/01.webp）</p>
        <div class="pcp-group-label">开场屏</div>
        <div class="admin-field">
          <label class="admin-field-label">开场标题</label>
          <CxInput v-model="forms['parallax-config'].introTitle" variant="admin" />
        </div>
        <div class="admin-field">
          <label class="admin-field-label">开场副标题</label>
          <CxInput v-model="forms['parallax-config'].introSubtitle" type="textarea" variant="admin" :rows="2" />
        </div>
        <div class="admin-field">
          <label class="admin-field-label">开场背景图路径</label>
          <CxInput v-model="forms['parallax-config'].introBg" variant="admin" />
        </div>
        <div class="pcp-group-label">告别屏</div>
        <div class="admin-field">
          <label class="admin-field-label">告别标题</label>
          <CxInput v-model="forms['parallax-config'].outroTitle" variant="admin" />
        </div>
        <div class="admin-field">
          <label class="admin-field-label">告别副标题</label>
          <CxInput v-model="forms['parallax-config'].outroSubtitle" type="textarea" variant="admin" :rows="2" />
        </div>
        <div class="admin-field">
          <label class="admin-field-label">告别背景图路径</label>
          <CxInput v-model="forms['parallax-config'].outroBg" variant="admin" />
        </div>
      </div>

      <!-- bangumi-hero -->
      <div v-show="activeTab === 'bangumi-hero'" class="scp-card">
        <div class="pcp-section-head">
          <h3>番剧页顶部文案</h3>
          <CxButton :disabled="saving['bangumi-hero']" @click="saveKey('bangumi-hero')">
            {{ saving['bangumi-hero'] ? '保存中…' : '保存' }}
          </CxButton>
        </div>
        <div class="admin-field">
          <label class="admin-field-label">标题</label>
          <CxInput v-model="forms['bangumi-hero'].title" variant="admin" />
        </div>
        <div class="admin-field">
          <label class="admin-field-label">副标题</label>
          <CxInput v-model="forms['bangumi-hero'].subtitle" type="textarea" variant="admin" :rows="2" />
        </div>
        <div class="admin-field">
          <label class="admin-field-label">空态提示（无番剧时显示）</label>
          <CxInput v-model="forms['bangumi-hero'].emptyText" variant="admin" />
        </div>
      </div>

      <!-- calendar-hero -->
      <div v-show="activeTab === 'calendar-hero'" class="scp-card">
        <div class="pcp-section-head">
          <h3>日历页顶部文案</h3>
          <CxButton :disabled="saving['calendar-hero']" @click="saveKey('calendar-hero')">
            {{ saving['calendar-hero'] ? '保存中…' : '保存' }}
          </CxButton>
        </div>
        <div class="admin-field">
          <label class="admin-field-label">标题</label>
          <CxInput v-model="forms['calendar-hero'].title" variant="admin" />
        </div>
        <div class="admin-field">
          <label class="admin-field-label">副标题</label>
          <CxInput v-model="forms['calendar-hero'].subtitle" type="textarea" variant="admin" :rows="2" />
        </div>
      </div>

      <!-- tool-hero -->
      <div v-show="activeTab === 'tool-hero'" class="scp-card">
        <div class="pcp-section-head">
          <h3>工具页顶部文案</h3>
          <CxButton :disabled="saving['tool-hero']" @click="saveKey('tool-hero')">
            {{ saving['tool-hero'] ? '保存中…' : '保存' }}
          </CxButton>
        </div>
        <div class="admin-field">
          <label class="admin-field-label">标题</label>
          <CxInput v-model="forms['tool-hero'].title" variant="admin" />
        </div>
        <div class="admin-field">
          <label class="admin-field-label">描述</label>
          <CxInput v-model="forms['tool-hero'].description" type="textarea" variant="admin" :rows="3" />
        </div>
        <div class="admin-field">
          <label class="admin-field-label">搜索框占位文字</label>
          <CxInput v-model="forms['tool-hero'].searchPlaceholder" variant="admin" />
        </div>
      </div>
    </template>
  </section>
</template>

<script setup>
import { inject, onMounted, reactive, ref } from 'vue'
import { siteContentApi } from '../../api/admin'
import CxButton from '../../components/cx/CxButton.vue'
import CxInput from '../../components/cx/CxInput.vue'

const TABS = [
  { key: 'timeline-hero', label: '时间线' },
  { key: 'treehole-config', label: '树洞' },
  { key: 'parallax-config', label: '视差' },
  { key: 'bangumi-hero', label: '番剧' },
  { key: 'calendar-hero', label: '日历' },
  { key: 'tool-hero', label: '工具' }
]

const DEFAULTS = {
  'timeline-hero': { eyebrow: 'Timeline', title: '把时间节点排成一条可以浏览的故事轨道。', description: '每一个标记都是一段被留住的时间。' },
  'treehole-config': { placeholder: '写下一句此刻想被接住的话...', moodOptions: ['轻声', '鼓劲', '拥抱', '放空'], backgroundNote: '' },
  'parallax-config': { introTitle: 'A Quiet Opening', introSubtitle: '在光与影的缝隙间，慢慢展开一段无声的故事。', introBg: '/image/bg/Landscape/01.webp', outroTitle: 'Until Next Time', outroSubtitle: '愿你带着温柔的光，继续前行。', outroBg: '/image/bg/Landscape/12.webp' },
  'bangumi-hero': { title: '番剧记录', subtitle: '追番进度与收藏一览，记录每一段屏幕里的故事。', emptyText: '这里还空空的，快去收录第一部番剧吧。' },
  'calendar-hero': { title: '每日放送', subtitle: '查看今日播出的番剧时间表，不再错过任何一集。' },
  'tool-hero': { title: '工具地图', description: '把常用工具、网站和灵感碎片排成一张可浏览的在线地图。', searchPlaceholder: '搜索网站名、用途、域名或标签' }
}

const toast = inject('adminToast', () => {})
const onUnauthorized = inject('adminUnauthorized', () => {})

const activeTab = ref('timeline-hero')
const loadingAny = ref(false)
const saving = reactive({})
const savingAny = ref(false)

// 表单态：数组字段用多行文本承载
const forms = reactive({
  'timeline-hero': { eyebrow: '', title: '', description: '' },
  'treehole-config': { placeholder: '', moodOptionsText: '', backgroundNote: '' },
  'parallax-config': { introTitle: '', introSubtitle: '', introBg: '', outroTitle: '', outroSubtitle: '', outroBg: '' },
  'bangumi-hero': { title: '', subtitle: '', emptyText: '' },
  'calendar-hero': { title: '', subtitle: '' },
  'tool-hero': { title: '', description: '', searchPlaceholder: '' }
})

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

function toLines(arr) {
  return Array.isArray(arr) ? arr.join('\n') : ''
}
function fromLines(text) {
  return String(text || '').split('\n').map(s => s.trim()).filter(Boolean)
}

function fillForm(key, obj) {
  const def = DEFAULTS[key]
  const src = obj || def
  if (key === 'timeline-hero') {
    forms[key] = { eyebrow: src.eyebrow ?? def.eyebrow, title: src.title ?? def.title, description: src.description ?? def.description }
  } else if (key === 'treehole-config') {
    forms[key] = { placeholder: src.placeholder ?? def.placeholder, moodOptionsText: toLines(src.moodOptions || def.moodOptions), backgroundNote: src.backgroundNote ?? def.backgroundNote }
  } else if (key === 'parallax-config') {
    forms[key] = { introTitle: src.introTitle ?? def.introTitle, introSubtitle: src.introSubtitle ?? def.introSubtitle, introBg: src.introBg ?? def.introBg, outroTitle: src.outroTitle ?? def.outroTitle, outroSubtitle: src.outroSubtitle ?? def.outroSubtitle, outroBg: src.outroBg ?? def.outroBg }
  } else if (key === 'bangumi-hero') {
    forms[key] = { title: src.title ?? def.title, subtitle: src.subtitle ?? def.subtitle, emptyText: src.emptyText ?? def.emptyText }
  } else if (key === 'calendar-hero') {
    forms[key] = { title: src.title ?? def.title, subtitle: src.subtitle ?? def.subtitle }
  } else if (key === 'tool-hero') {
    forms[key] = { title: src.title ?? def.title, description: src.description ?? def.description, searchPlaceholder: src.searchPlaceholder ?? def.searchPlaceholder }
  }
}

function buildContent(key) {
  const f = forms[key]
  if (key === 'timeline-hero') return { eyebrow: f.eyebrow, title: f.title, description: f.description }
  if (key === 'treehole-config') return { placeholder: f.placeholder, moodOptions: fromLines(f.moodOptionsText), backgroundNote: f.backgroundNote }
  if (key === 'parallax-config') return { introTitle: f.introTitle, introSubtitle: f.introSubtitle, introBg: f.introBg, outroTitle: f.outroTitle, outroSubtitle: f.outroSubtitle, outroBg: f.outroBg }
  if (key === 'bangumi-hero') return { title: f.title, subtitle: f.subtitle, emptyText: f.emptyText }
  if (key === 'calendar-hero') return { title: f.title, subtitle: f.subtitle }
  if (key === 'tool-hero') return { title: f.title, description: f.description, searchPlaceholder: f.searchPlaceholder }
  return {}
}

async function loadKey(key) {
  try {
    const data = parseContent(await siteContentApi.get(key))
    fillForm(key, data)
  } catch {
    fillForm(key, null)
  }
}

async function saveKey(key) {
  saving[key] = true
  savingAny.value = true
  try {
    await siteContentApi.save(key, JSON.stringify(buildContent(key)))
    toast('保存成功，前台已生效')
  } catch (err) {
    if (err && err.unauthorized) {
      onUnauthorized && onUnauthorized()
      return
    }
    toast((err && err.message) || '保存失败', 'error')
  } finally {
    saving[key] = false
    savingAny.value = false
  }
}

async function saveAll() {
  savingAny.value = true
  let hasError = false
  for (const tab of TABS) {
    saving[tab.key] = true
    try {
      await siteContentApi.save(tab.key, JSON.stringify(buildContent(tab.key)))
    } catch (err) {
      if (err && err.unauthorized) {
        onUnauthorized && onUnauthorized()
        savingAny.value = false
        return
      }
      hasError = true
    } finally {
      saving[tab.key] = false
    }
  }
  savingAny.value = false
  if (hasError) {
    toast('部分保存失败，请检查后重试', 'error')
  } else {
    toast('全部保存成功，前台已生效')
  }
}

onMounted(async () => {
  loadingAny.value = true
  await Promise.all(TABS.map(t => loadKey(t.key)))
  loadingAny.value = false
})
</script>

<style scoped>
/* ===== 页面文案面板（pcp- 前缀） ===== */
.pcp-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 4px 0;
}
.pcp-tab {
  padding: 7px 18px;
  border: 1px solid var(--accent-border, rgba(63, 119, 181, 0.3));
  border-radius: 999px;
  background: var(--card-bg, #fff);
  color: var(--accent-text, var(--accent-strong));
  font-family: inherit;
  font-size: 14px;
  cursor: pointer;
  transition: transform 0.2s ease, background 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
}
.pcp-tab:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(63, 119, 181, 0.12);
}
.pcp-tab.active {
  background: var(--accent-solid, var(--accent-strong));
  border-color: var(--accent-solid, var(--accent-strong));
  color: #fff;
  box-shadow: 0 6px 16px rgba(63, 119, 181, 0.22);
}

.pcp-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}
.pcp-section-head h3 {
  margin: 0;
  font-size: 16px;
  color: var(--text-color, var(--text-color));
}

.pcp-group-label {
  margin: 14px 0 6px;
  font-size: 13px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: #8aa5c4;
  font-weight: 600;
}

html.dark .pcp-section-head h3 {
  color: #e8effa;
}
html.dark .pcp-group-label {
  color: #8fa0ba;
}

@media (max-width: 600px) {
  .pcp-tabs {
    gap: 6px;
  }
  .pcp-tab {
    padding: 6px 14px;
    font-size: 13px;
  }
}
</style>
