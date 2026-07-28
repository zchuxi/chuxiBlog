<template>
  <div class="admin-root">
    <!-- 未登录：居中登录卡片 -->
    <div v-if="!logged" class="admin-login-wrap">
      <form class="admin-login-card" @submit.prevent="onLogin">
        <div class="admin-login-brand">
          <span class="admin-login-brand-dot"></span>
          <h1 class="admin-login-title">初曦后台</h1>
        </div>
        <p class="admin-login-sub">请登录后进入工作台</p>
        <div class="admin-field">
          <label class="admin-field-label">账号</label>
          <input v-model.trim="loginForm.username" class="admin-input" type="text" autocomplete="username" />
        </div>
        <div class="admin-field">
          <label class="admin-field-label">密码</label>
          <input v-model.trim="loginForm.password" class="admin-input" type="password" autocomplete="current-password" />
        </div>
        <p v-if="loginError" class="admin-login-error">{{ loginError }}</p>
        <button class="admin-btn admin-btn-block" type="submit" :disabled="loggingIn">
          {{ loggingIn ? '登录中…' : '登录' }}
        </button>
      </form>
    </div>

    <!-- 已登录：分组侧栏 + 顶部条 + 面板区 -->
    <div v-else class="admin-shell">
      <aside class="admin-sidebar" :class="{ open: sidebarOpen }">
        <div class="admin-brand">
          <span class="admin-brand-dot"></span>
          初曦后台
        </div>
        <nav class="admin-nav">
          <div v-for="group in menuGroups" :key="group.title || 'main'" class="admin-nav-group">
            <p v-if="group.title" class="admin-nav-group-title">{{ group.title }}</p>
            <button
              v-for="item in group.items"
              :key="item.key"
              class="admin-nav-item"
              :class="{ active: item.key === currentKey }"
              @click="selectMenu(item.key)"
            >
              <SvgIcon :name="item.icon" size="17px" />
              <span>{{ item.label }}</span>
            </button>
          </div>
        </nav>
        <div class="admin-sidebar-foot">
          <button class="admin-nav-item" @click="toggleTheme">
            <SvgIcon :name="isDark ? 'common-sun' : 'common-moon'" size="17px" />
            <span>{{ isDark ? '亮色模式' : '暗色模式' }}</span>
          </button>
          <button class="admin-nav-item" @click="pwdOpen = true">
            <SvgIcon name="common-setting" size="17px" />
            <span>修改密码</span>
          </button>
          <button class="admin-nav-item" @click="openSite">
            <SvgIcon name="common-web" size="17px" />
            <span>查看站点</span>
          </button>
          <button class="admin-nav-item admin-logout" @click="logout">
            <SvgIcon name="common-exchange" size="17px" />
            <span>退出登录</span>
          </button>
        </div>
      </aside>

      <!-- 移动端抽屉遮罩：点击收起侧栏（仅 ≤900px 显示） -->
      <transition name="admin-fade">
        <div v-if="sidebarOpen" class="admin-sidebar-mask" @click="sidebarOpen = false"></div>
      </transition>

      <div class="admin-main">
        <header class="admin-topbar">
          <div class="admin-topbar-left">
            <button class="admin-menu-toggle" type="button" aria-label="打开菜单" @click="sidebarOpen = true">
              <SvgIcon name="common-menu" size="20px" />
            </button>
            <div>
              <p class="admin-topbar-title">管理后台</p>
              <p class="admin-topbar-sub">欢迎回来，站长</p>
            </div>
          </div>
          <img class="admin-avatar" src="/favicon.png" alt="站长头像" />
        </header>
        <main class="admin-content">
          <DashboardPanel v-if="currentKey === 'dashboard'" @go="handleGo" />
          <ArticlesPanel v-else-if="currentKey === 'articles'" :initial-create="articlesInitialCreate" />
          <ScenePanel v-else-if="currentKey === 'scenes'" />
          <SiteContentPanel
            v-else-if="SITE_CONTENT_KEYS[currentKey]"
            :key="currentKey"
            :content-key="SITE_CONTENT_KEYS[currentKey]"
          />
          <MediaPanel v-else-if="currentKey === 'media'" />
          <BangumiPanel v-else-if="currentKey === 'bangumi-records'" />
          <ResourcePanel v-else-if="currentSchema" :key="currentKey" :schema="currentSchema" />
        </main>
      </div>
    </div>

    <!-- 修改密码弹窗 -->
    <PasswordDialog v-if="pwdOpen" @close="pwdOpen = false" />

    <!-- 右上角简易 toast -->
    <div class="admin-toasts">
      <transition-group name="admin-toast">
        <div v-for="t in toasts" :key="t.id" class="admin-toast" :class="t.type">{{ t.text }}</div>
      </transition-group>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, provide, reactive, ref } from 'vue'
import { clearToken, getToken, login, setToken } from '../../api/admin'
import { useSettingsStore } from '../../stores/settings'
import resourceSchemas from './resourceSchemas'
import SvgIcon from '../../components/SvgIcon.vue'
import ResourcePanel from './ResourcePanel.vue'
import MediaPanel from './MediaPanel.vue'
import BangumiPanel from './BangumiPanel.vue'
import DashboardPanel from './DashboardPanel.vue'
import PasswordDialog from './PasswordDialog.vue'
import ArticlesPanel from './ArticlesPanel.vue'
import ScenePanel from './ScenePanel.vue'
import SiteContentPanel from './SiteContentPanel.vue'
import '../../assets/css/admin.css'

// 菜单 key → SiteContentPanel 的 contentKey
const SITE_CONTENT_KEYS = {
  'site-home-landing': 'home-landing',
  'site-archive-hero': 'archive-hero',
  'site-about': 'about'
}

// 分组侧栏菜单
const menuGroups = [
  {
    title: '',
    items: [
      { key: 'dashboard', label: '概览', icon: 'common-home' },
      { key: 'articles', label: '文章管理', icon: 'common-articlePages' },
      { key: 'archive-categories', label: '分类管理', icon: 'common-archive' },
      { key: 'comments', label: '评论审核', icon: 'common-chat' },
      { key: 'timeline-events', label: '时间线', icon: 'common-timeline' },
      { key: 'timeline-carousels', label: '时间线轮播', icon: 'common-history' },
      { key: 'barrages', label: '树洞弹幕', icon: 'common-send' },
      { key: 'called-texts', label: '疗愈文本', icon: 'common-paw' },
      { key: 'parallax-stories', label: '视差故事', icon: 'common-parallax' }
    ]
  },
  {
    title: '首页内容',
    items: [
      { key: 'site-home-landing', label: '首页内容', icon: 'common-home' },
      { key: 'scenes', label: '首屏场景', icon: 'common-component' },
      { key: 'collapse-cards', label: '内容卡片', icon: 'common-menu' },
      { key: 'team-members', label: '个人介绍', icon: 'common-person' },
      { key: 'site-archive-hero', label: '归档页', icon: 'common-tree' },
      { key: 'site-about', label: '关于页', icon: 'common-cat' }
    ]
  },
  {
    title: '资源',
    items: [
      { key: 'media', label: '图片管理', icon: 'common-icons' },
      { key: 'musics', label: '音乐管理', icon: 'common-music' },
      { key: 'tool-sites', label: '工具站点', icon: 'common-tool' },
      { key: 'bangumi-records', label: '番剧管理', icon: 'common-open' }
    ]
  }
]

const logged = ref(!!getToken())
const loggingIn = ref(false)
const loginError = ref('')
const loginForm = reactive({ username: '', password: '' })

const currentKey = ref('dashboard')
const currentSchema = computed(() => resourceSchemas.find(s => s.key === currentKey.value))

// 移动端（≤900px）侧栏抽屉开关：选中菜单/点遮罩后收起
const sidebarOpen = ref(false)

// 仪表盘「写新文章」→ 切到文章面板并携带新建意图（ArticlesPanel 可选 prop initialCreate）
const articlesInitialCreate = ref(false)

function selectMenu(key) {
  articlesInitialCreate.value = false
  currentKey.value = key
  sidebarOpen.value = false
}

function handleGo(key, extra) {
  articlesInitialCreate.value = !!(key === 'articles' && extra && extra.create)
  currentKey.value = key
}

// 修改密码弹窗
const pwdOpen = ref(false)

function openSite() {
  window.open('/', '_blank')
}

// 暗色模式：与前台共用 settings（html.dark + localStorage 持久化）
const settings = useSettingsStore()
const isDark = computed(() => settings.isDark)
function toggleTheme() {
  settings.setTheme(settings.isDark ? 'light' : 'dark')
}
onMounted(() => {
  document.documentElement.classList.toggle('dark', settings.isDark)
})

// 简易 toast：右上角堆叠，自动消失
const toasts = ref([])
let toastSeq = 0
function toast(text, type = 'success') {
  const id = ++toastSeq
  toasts.value.push({ id, text, type })
  setTimeout(() => {
    toasts.value = toasts.value.filter(t => t.id !== id)
  }, 2500)
}

async function onLogin() {
  if (!loginForm.username || !loginForm.password) {
    loginError.value = '请输入账号和密码'
    return
  }
  loggingIn.value = true
  loginError.value = ''
  try {
    const data = await login(loginForm.username, loginForm.password)
    setToken(data.token)
    logged.value = true
    currentKey.value = 'dashboard'
    toast(`欢迎回来，${data.displayName || '站长'}`)
  } catch (err) {
    loginError.value = (err && err.message) || '登录失败'
  } finally {
    loggingIn.value = false
  }
}

function logout() {
  clearToken()
  logged.value = false
  pwdOpen.value = false
  loginForm.password = ''
}

// 子组件用：全局提示 + 401 退回登录页
provide('adminToast', toast)
provide('adminUnauthorized', () => {
  logout()
  toast('登录已过期，请重新登录', 'error')
})
</script>
