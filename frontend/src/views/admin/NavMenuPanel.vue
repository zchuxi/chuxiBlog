<template>
  <section class="scp-panel">
    <header class="scp-head">
      <div>
        <h2 class="scp-head-title">导航菜单</h2>
        <p class="scp-head-sub">管理前台顶部导航栏的菜单项：显示名称、路由路径、图标和可见性。保存后前台立即生效。</p>
      </div>
      <div class="admin-toolbar-actions">
        <button class="admin-btn admin-btn-ghost" :disabled="loading" @click="load">刷新</button>
        <button class="admin-btn admin-btn-ghost" @click="resetDefaults">恢复默认</button>
        <button class="admin-btn" :disabled="saving || loading" @click="save">
          {{ saving ? '保存中…' : '保存' }}
        </button>
      </div>
    </header>

    <div v-if="loading" class="scp-card admin-state">加载中…</div>

    <div v-else class="scp-card">
      <div v-if="items.length === 0" class="admin-state">
        暂无菜单项，点击下方按钮添加。
      </div>

      <div v-for="(item, i) in items" :key="i" class="nmp-row">
        <div class="nmp-row-index">{{ i + 1 }}</div>
        <div class="nmp-row-fields">
          <div class="admin-field">
            <label class="admin-field-label">名称</label>
            <input v-model="item.label" class="admin-input" type="text" placeholder="菜单显示文字" />
          </div>
          <div class="admin-field">
            <label class="admin-field-label">路径</label>
            <input
              v-model="item.path"
              class="admin-input"
              type="text"
              placeholder="/path"
              :list="`nmp-route-list-${i}`"
            />
            <datalist :id="`nmp-route-list-${i}`">
              <option v-for="rp in routePaths" :key="rp" :value="rp">{{ rp }}</option>
            </datalist>
          </div>
          <div class="admin-field">
            <label class="admin-field-label">图标</label>
            <input v-model="item.icon" class="admin-input" type="text" placeholder="common-xxx" />
          </div>
          <div class="admin-field nmp-visible-field">
            <label class="admin-field-label">显示</label>
            <label class="nmp-switch">
              <input v-model="item.visible" type="checkbox" />
              <span class="nmp-switch-slider"></span>
            </label>
          </div>
        </div>
        <div class="nmp-row-actions">
          <button class="admin-btn admin-btn-ghost nmp-icon-btn" :disabled="i === 0" @click="moveUp(i)" title="上移">↑</button>
          <button class="admin-btn admin-btn-ghost nmp-icon-btn" :disabled="i === items.length - 1" @click="moveDown(i)" title="下移">↓</button>
          <button class="admin-btn admin-btn-ghost nmp-icon-btn nmp-remove-btn" @click="removeAt(i)" title="删除">×</button>
        </div>
      </div>

      <div class="nmp-add-row">
        <button class="admin-btn admin-btn-ghost" @click="addItem">+ 添加菜单项</button>
      </div>
    </div>
  </section>
</template>

<script setup>
import { inject, onMounted, ref } from 'vue'
import router from '../../router'
import { siteContentApi } from '../../api/admin'

const CONTENT_KEY = 'nav-menu'

const DEFAULT_NAV = [
  { path: '/index', label: '首页', icon: 'common-home', visible: true },
  { path: '/tool', label: '工具', icon: 'common-tool', visible: true },
  { path: '/bangumi', label: '番剧', icon: 'common-articlePages', visible: true },
  { path: '/timeline', label: '时间线', icon: 'common-timeline', visible: true },
  { path: '/tree-hole', label: '树洞', icon: 'common-tree', visible: true },
  { path: '/parallax', label: '视差', icon: 'common-parallax', visible: true },
  { path: '/archive', label: '归档', icon: 'common-archive', visible: true },
  { path: '/about', label: '关于', icon: 'common-person', visible: true },
  { path: '/components', label: '组件', icon: 'common-component', visible: true }
]

const toast = inject('adminToast', () => {})

/* 从路由配置中提取可选路径（排除动态参数路由和管理页） */
const routePaths = router.options.routes
  .map(r => r.path)
  .filter(p => !p.includes(':') && p !== '/admin' && p !== '/')
const onUnauthorized = inject('adminUnauthorized', () => {})

const loading = ref(false)
const saving = ref(false)
const items = ref([])

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

function normalizeItem(raw) {
  return {
    label: raw.label || '',
    path: raw.path || '',
    icon: raw.icon || '',
    visible: raw.visible !== false
  }
}

async function load() {
  loading.value = true
  try {
    const obj = parseContent(await siteContentApi.get(CONTENT_KEY))
    if (Array.isArray(obj) && obj.length > 0) {
      items.value = obj.map(normalizeItem)
    } else {
      items.value = DEFAULT_NAV.map(n => ({ ...n }))
    }
  } catch (err) {
    if (err && err.unauthorized) {
      onUnauthorized && onUnauthorized()
      return
    }
    items.value = DEFAULT_NAV.map(n => ({ ...n }))
  } finally {
    loading.value = false
  }
}

async function save() {
  if (items.value.length === 0) {
    toast('至少保留一个菜单项', 'error')
    return
  }
  saving.value = true
  try {
    const payload = items.value.map(i => ({
      label: i.label.trim(),
      path: i.path.trim(),
      icon: i.icon.trim(),
      visible: !!i.visible
    }))
    await siteContentApi.save(CONTENT_KEY, JSON.stringify(payload))
    toast('导航菜单已保存，前台刷新后生效')
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

function moveUp(i) {
  if (i <= 0) return
  const arr = items.value
  const tmp = arr[i - 1]
  arr[i - 1] = arr[i]
  arr[i] = tmp
  items.value = [...arr]
}

function moveDown(i) {
  if (i >= items.value.length - 1) return
  const arr = items.value
  const tmp = arr[i + 1]
  arr[i + 1] = arr[i]
  arr[i] = tmp
  items.value = [...arr]
}

function removeAt(i) {
  items.value.splice(i, 1)
}

function addItem() {
  items.value.push({ label: '', path: '', icon: '', visible: true })
}

function resetDefaults() {
  items.value = DEFAULT_NAV.map(n => ({ ...n }))
  toast('已回填默认菜单，记得点保存')
}

onMounted(load)
</script>

<style>
/* ===== 导航菜单面板（nmp- 前缀） ===== */
.nmp-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid rgba(63, 119, 181, 0.08);
}
.nmp-row:last-of-type {
  border-bottom: none;
}
.nmp-row-index {
  flex: 0 0 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: rgba(63, 119, 181, 0.08);
  color: #3f77b5;
  font-size: 13px;
  font-weight: 600;
  margin-top: 22px;
}
.nmp-row-fields {
  flex: 1;
  display: grid;
  grid-template-columns: 1fr 1fr 1fr auto;
  gap: 0 12px;
  align-items: end;
}
.nmp-visible-field {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
}
.nmp-row-actions {
  display: flex;
  gap: 4px;
  margin-top: 22px;
  flex-shrink: 0;
}
.nmp-icon-btn {
  width: 32px;
  height: 32px;
  padding: 0 !important;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  border-radius: 10px !important;
}
.nmp-remove-btn:hover {
  background: rgba(209, 67, 67, 0.12) !important;
  color: #d14343 !important;
}
.nmp-add-row {
  padding-top: 12px;
  border-top: 1px dashed rgba(63, 119, 181, 0.12);
  margin-top: 4px;
}

/* 开关 */
.nmp-switch {
  position: relative;
  display: inline-block;
  width: 40px;
  height: 22px;
  cursor: pointer;
}
.nmp-switch input {
  opacity: 0;
  width: 0;
  height: 0;
  position: absolute;
}
.nmp-switch-slider {
  position: absolute;
  inset: 0;
  border-radius: 999px;
  background: rgba(127, 127, 127, 0.25);
  transition: background 0.2s ease;
}
.nmp-switch-slider::before {
  content: '';
  position: absolute;
  left: 2px;
  top: 2px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #fff;
  transition: transform 0.2s ease;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.15);
}
.nmp-switch input:checked + .nmp-switch-slider {
  background: #3f77b5;
}
.nmp-switch input:checked + .nmp-switch-slider::before {
  transform: translateX(18px);
}

/* 暗色 */
html.dark .nmp-row {
  border-bottom-color: rgba(255, 255, 255, 0.06);
}
html.dark .nmp-row-index {
  background: rgba(232, 239, 250, 0.1);
  color: #e8effa;
}
html.dark .nmp-switch-slider {
  background: rgba(255, 255, 255, 0.15);
}
html.dark .nmp-switch input:checked + .nmp-switch-slider {
  background: #7fb4e8;
}

/* 移动端 */
@media (max-width: 900px) {
  .nmp-row-fields {
    grid-template-columns: 1fr 1fr;
  }
  .nmp-row {
    flex-wrap: wrap;
  }
  .nmp-row-actions {
    margin-top: 8px;
  }
}
@media (max-width: 600px) {
  .nmp-row-fields {
    grid-template-columns: 1fr;
  }
}
</style>
