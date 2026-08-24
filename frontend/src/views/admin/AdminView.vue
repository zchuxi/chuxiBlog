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

        <CxButton class="admin-block" native-type="submit" :disabled="loggingIn">
          {{ loggingIn ? '登录中…' : '登录' }}
        </CxButton>
      </form>
    </div>

    <!-- 已登录：分组侧栏 + 顶部条 + 面板区 -->
    <div v-else class="admin-shell">
      <aside class="admin-sidebar" :class="{ open: sidebarOpen }">
        <RouterLink class="admin-brand" to="/index" title="返回网站首页">
          <span class="admin-brand-dot"></span>
          初曦后台
        </RouterLink>
        <label class="admin-nav-search">
          <span class="sr-only">搜索后台模块</span>
          <SvgIcon name="common-search" size="15px" />
          <input v-model.trim="menuQuery" type="search" placeholder="搜索后台模块" />
        </label>
        <p
          v-if="filteredMenuGroups.length === 0"
          class="admin-nav-empty"
          role="status"
          aria-live="polite"
        >
          没有匹配的后台模块
        </p>
        <nav class="admin-nav">
          <div v-for="group in filteredMenuGroups" :key="group.title || 'main'" class="admin-nav-group">
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
              <p class="admin-topbar-title">{{ currentMenu.label }}</p>
              <p class="admin-topbar-sub">{{ currentMenu.description }}</p>
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
          <BackgroundPanel v-else-if="currentKey === 'background-gallery'" />
          <SiteSettingsPanel v-else-if="currentKey === 'site-settings'" />
          <AppearancePanel v-else-if="currentKey === 'appearance-settings'" />
          <AiConfigPanel v-else-if="currentKey === 'ai-config'" />
          <NavMenuPanel v-else-if="currentKey === 'nav-menu'" />
          <PageContentPanel v-else-if="currentKey === 'page-content'" />
          <BangumiPanel v-else-if="currentKey === 'bangumi-records'" />
          <ResourcePanel
            v-else-if="currentSchema"
            :key="currentKey"
            ref="resourcePanelRef"
            :schema="currentSchema"
          />
        </main>
      </div>
    </div>

    <!-- 修改密码弹窗 -->
    <PasswordDialog v-if="pwdOpen" @close="pwdOpen = false" />
  </div>
</template>

<script setup>
import { computed, onMounted, provide, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { login, logout as logoutApi, me } from '../../api/admin'
import { useSettingsStore } from '../../stores/settings'
import { toastSuccess, toastError } from '../../utils/toast'
import resourceSchemas from './resourceSchemas'
import { filterMenuGroups } from './adminUi'
import { menuGroups } from './adminMenu'
import SvgIcon from '../../components/SvgIcon.vue'
import CxButton from '../../components/cx/CxButton.vue'
import ResourcePanel from './ResourcePanel.vue'
import MediaPanel from './MediaPanel.vue'
import BackgroundPanel from './BackgroundPanel.vue'
import BangumiPanel from './BangumiPanel.vue'
import DashboardPanel from './DashboardPanel.vue'
import PasswordDialog from './PasswordDialog.vue'
import ArticlesPanel from './ArticlesPanel.vue'
import ScenePanel from './ScenePanel.vue'
import SiteContentPanel from './SiteContentPanel.vue'
import SiteSettingsPanel from './SiteSettingsPanel.vue'
import AppearancePanel from './AppearancePanel.vue'
import AiConfigPanel from './AiConfigPanel.vue'
import NavMenuPanel from './NavMenuPanel.vue'
import PageContentPanel from './PageContentPanel.vue'
import '../../assets/css/admin.css'
import '../../assets/css/preview.css'

// 菜单 key → SiteContentPanel 的 contentKey
const SITE_CONTENT_KEYS = {
  'site-home-landing': 'home-landing',
  'site-archive-hero': 'archive-hero',
  'site-about': 'about'
}

const logged = ref(false)
const loggingIn = ref(false)
const loginError = ref('')
const loginForm = reactive({ username: '', password: '' })

const currentKey = ref('dashboard')
const currentSchema = computed(() => resourceSchemas.find(s => s.key === currentKey.value))
const menuQuery = ref('')
const filteredMenuGroups = computed(() => filterMenuGroups(menuGroups, menuQuery.value))
const allMenuItems = menuGroups.flatMap(group => group.items)
const currentMenu = computed(() =>
  allMenuItems.find(item => item.key === currentKey.value) || allMenuItems[0]
)

// 移动端（≤900px）侧栏抽屉开关：选中菜单/点遮罩后收起
const sidebarOpen = ref(false)

// 仪表盘「写新文章」→ 切到文章面板并携带新建意图（ArticlesPanel 可选 prop initialCreate）
const articlesInitialCreate = ref(false)
const resourcePanelRef = ref(null)

function canLeaveCurrentPanel() {
  return resourcePanelRef.value?.requestClose?.() !== false
}

function selectMenu(key) {
  if (key === currentKey.value) {
    sidebarOpen.value = false
    return
  }
  if (!canLeaveCurrentPanel()) return
  articlesInitialCreate.value = false
  currentKey.value = key
  sidebarOpen.value = false
}

function handleGo(key, extra) {
  if (key !== currentKey.value && !canLeaveCurrentPanel()) return
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
onMounted(async () => {
  document.documentElement.classList.toggle('dark', settings.isDark)
  try {
    await me()
    logged.value = true
  } catch {
    logged.value = false
  }
})

// toast 复用 App.vue 挂载的全局 CxMessage 单例（与前台同款提示）
function toast(text, type = 'success') {
  (type === 'error' ? toastError : toastSuccess)(text)
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
    logged.value = true
    currentKey.value = 'dashboard'
    toast(`欢迎回来，${data.displayName || '站长'}`)
  } catch (err) {
    loginError.value = (err && err.message) || '登录失败'
  } finally {
    loggingIn.value = false
  }
}

async function logout() {
  try {
    await logoutApi()
  } catch {
    // 即使网络异常也立即清理当前页面的登录态。
  }
  logged.value = false
  pwdOpen.value = false
  loginForm.password = ''
}

// 子组件用：全局提示 + 401 退回登录页
provide('adminToast', toast)
provide('adminUnauthorized', () => {
  void logout()
  toast('登录已过期，请重新登录', 'error')
})
</script>
