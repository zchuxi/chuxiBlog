<template>
  <div class="app-shell">
    <!-- 背景 -->
    <BackgroundLayer ref="bgLayerRef" />

    <!-- 顶栏 -->
    <TopBar
      ref="topBarRef"
      :site-name="siteName"
      :paw-progress="pawEnabled ? pawProgress : 0"
      :solid="topbarSolid"
      :setting-open="settingOpen"
      @open-search="searchOpen = true"
      @toggle-theme="toggleTheme"
      @toggle-ai="aiExpanded = !aiExpanded"
      @toggle-music="musicBarRef?.toggleMusicBar()"
      @open-settings="settingOpen = true"
      @close-settings="settingOpen = false"
      @open-auth="openAuthDialog"
      @go-admin="goAdmin"
      @paw-toggle="togglePaw"
      @scroll-to-top="scrollMainToTop"
    />

    <!-- 主体 -->
    <div class="app-shell-body">
      <div class="app-shell-body__content-col">
        <div class="app-shell-main-wrap">
          <section class="app-shell-main">
            <RouterView v-slot="{ Component }">
              <transition name="content-route" mode="out-in">
                <component :is="Component" />
              </transition>
            </RouterView>
          </section>
        </div>

        <!-- 底部音乐条 -->
        <MusicBar ref="musicBarRef" v-model:music-bar-open="musicBarOpen" />
      </div>

      <!-- AI 助手弹窗 -->
      <AiChatPanel v-model="aiExpanded" />
    </div>

    <!-- live2d 看板娘 -->
    <div
      class="live2d-widget"
      :class="{ 'is-hidden': !settings.live2dEnabled }"
      style="--live2d-bottom-offset: 0px; --live2d-bottom-gap: 8px;"
    >
      <div class="live2d-widget__stage">
        <canvas id="live2d-canvas" width="280" height="280" style="touch-action: none;"></canvas>
      </div>
      <div class="live2d-widget__actions">
        <button type="button" class="cx-button cx-button--primary is-round is-circle is-plain" @click="aiExpanded = true">
          <span class="cx-button__content"><SvgIcon name="common-chat" size="18px" /></span>
        </button>
        <button type="button" class="cx-button cx-button--info is-round is-circle is-plain is-disabled" disabled>
          <span class="cx-button__content"><SvgIcon name="common-hanger" size="18px" /></span>
        </button>
        <button type="button" class="cx-button cx-button--warning is-round is-circle is-plain" @click="reloadLive2d">
          <span class="cx-button__content"><SvgIcon name="common-reset" size="18px" /></span>
        </button>
        <button type="button" class="cx-button cx-button--danger is-round is-circle is-plain" @click="settings.update({ live2dEnabled: false })">
          <span class="cx-button__content"><SvgIcon name="common-big-close" size="18px" /></span>
        </button>
      </div>
    </div>

    <!-- 樱花画布 -->
    <SakuraCanvas />

    <!-- 文章搜索浮层 -->
    <SearchOverlay v-model="searchOpen" />

    <!-- 设置弹窗 -->
    <SettingsDialog v-model="settingOpen" @choose-background="chooseBackground" />

    <!-- 登录/注册弹窗 -->
    <transition name="login-dialog-fade">
      <div v-if="authOpen" class="login-dialog" role="dialog" aria-modal="true">
        <div class="login-dialog__mask" @click="authOpen = false"></div>
        <div class="login-dialog__card">
          <button type="button" class="login-dialog__close" @click="authOpen = false">
            <span class="login-dialog__close-icon"><SvgIcon name="common-big-close" size="14px" /></span>
          </button>
          <!-- 左侧渐变装饰侧板 -->
          <aside class="login-dialog__side">
            <span class="login-dialog__orb login-dialog__orb--1"></span>
            <span class="login-dialog__orb login-dialog__orb--2"></span>
            <span class="login-dialog__orb login-dialog__orb--3"></span>
            <span class="login-dialog__star login-dialog__star--1">✦</span>
            <span class="login-dialog__star login-dialog__star--2">✧</span>
            <span class="login-dialog__star login-dialog__star--3">✦</span>
            <div class="login-dialog__side-body">
              <span class="login-dialog__paw"><SvgIcon name="common-paw" size="28px" /></span>
              <h3 class="login-dialog__side-title">{{ authPanel === 'login' ? 'Hi，朋友！' : '欢迎加入！' }}</h3>
              <p class="login-dialog__side-desc">{{ authPanel === 'login' ? '登录后可以点赞、评论，和这个小站有更多互动。' : '注册一个账号，把喜欢的内容都收藏起来。' }}</p>
              <button
                type="button"
                class="login-dialog__side-btn"
                @click="authPanel = authPanel === 'login' ? 'register' : 'login'"
              >{{ authPanel === 'login' ? '去注册' : '去登录' }}</button>
            </div>
          </aside>
          <!-- 右侧表单区 -->
          <div class="login-dialog__main">
            <transition name="login-dialog-panel" mode="out-in">
              <!-- 登录面板 -->
              <form v-if="authPanel === 'login'" key="login" class="login-dialog__panel" @submit.prevent>
                <h3 class="login-dialog__title">欢迎回来</h3>
                <p class="login-dialog__subtitle">和这个小站继续昨天的故事吧</p>
                <div class="login-dialog__mode-switch">
                  <button
                    type="button"
                    class="login-dialog__mode-btn"
                    :class="{ 'is-active': authMode === 'password' }"
                    @click="authMode = 'password'"
                  >密码登录</button>
                  <button
                    type="button"
                    class="login-dialog__mode-btn"
                    :class="{ 'is-active': authMode === 'code' }"
                    @click="authMode = 'code'"
                  >验证码登录</button>
                </div>
                <label class="login-dialog__label">邮箱</label>
                <input class="login-dialog__input" type="text" placeholder="请输入邮箱" />
                <template v-if="authMode === 'password'">
                  <label class="login-dialog__label">密码</label>
                  <input class="login-dialog__input" type="password" placeholder="请输入密码" />
                </template>
                <template v-else>
                  <label class="login-dialog__label">验证码</label>
                  <div class="login-dialog__code-row">
                    <input class="login-dialog__input" type="text" placeholder="6 位验证码" />
                    <button type="button" class="login-dialog__code-btn">获取验证码</button>
                  </div>
                </template>
                <div class="login-dialog__aux">
                  <button type="button" class="login-dialog__link">忘记密码？</button>
                </div>
                <button type="button" class="login-dialog__submit" @click="authOpen = false">登 录</button>
                <p class="login-dialog__tip">演示站点：登录能力未开放，仅展示界面。</p>
                <p class="login-dialog__switch-hint">
                  还没有账号？<button type="button" class="login-dialog__link" @click="authPanel = 'register'">去注册</button>
                </p>
              </form>
              <!-- 注册面板 -->
              <form v-else key="register" class="login-dialog__panel" @submit.prevent>
                <h3 class="login-dialog__title">创建账号</h3>
                <p class="login-dialog__subtitle">只差一步，就能把喜欢都收藏起来</p>
                <label class="login-dialog__label">邮箱</label>
                <input class="login-dialog__input" type="text" placeholder="请输入邮箱" />
                <label class="login-dialog__label">验证码</label>
                <div class="login-dialog__code-row">
                  <input class="login-dialog__input" type="text" placeholder="6 位验证码" />
                  <button type="button" class="login-dialog__code-btn">获取验证码</button>
                </div>
                <label class="login-dialog__label">密码</label>
                <input class="login-dialog__input" type="password" placeholder="设置密码（至少 8 位）" />
                <button type="button" class="login-dialog__submit" @click="authOpen = false">注 册</button>
                <p class="login-dialog__tip">演示站点：注册能力未开放，仅展示界面。</p>
                <p class="login-dialog__switch-hint">
                  已有账号？<button type="button" class="login-dialog__link" @click="authPanel = 'login'">去登录</button>
                </p>
              </form>
            </transition>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { RouterView, useRouter } from 'vue-router'
import SvgIcon from '../components/SvgIcon.vue'
import { useSettingsStore } from '../stores/settings'
import { api } from '../api'
import BackgroundLayer from './components/BackgroundLayer.vue'
import TopBar from './components/TopBar.vue'
import MusicBar from './components/MusicBar.vue'
import AiChatPanel from './components/AiChatPanel.vue'
import SearchOverlay from './components/SearchOverlay.vue'
import SettingsDialog from './components/SettingsDialog.vue'
import SakuraCanvas from './components/SakuraCanvas.vue'

const settings = useSettingsStore()
const router = useRouter()

const bgLayerRef = ref(null)
const topBarRef = ref(null)
const musicBarRef = ref(null)

/* ---------- 站点全局设置 ---------- */
const siteSettings = reactive({
  siteName: '初曦的窝',
  subtitle: '',
  logoUrl: '/favicon.png',
  faviconUrl: '/favicon.png',
  seoDescription: '',
  seoKeywords: '',
  githubUrl: '',
  weiboUrl: '',
  qqUrl: '',
  footerText: '',
  footerIcp: ''
})

const siteName = computed(() => siteSettings.siteName || '初曦的窝')

async function loadSiteSettings() {
  try {
    const data = await api.siteContent('site-settings')
    let obj = data
    if (data && typeof data.contentJson === 'string') obj = JSON.parse(data.contentJson)
    if (typeof obj === 'string') obj = JSON.parse(obj)
    if (obj && typeof obj === 'object') {
      Object.assign(siteSettings, {
        siteName: obj.siteName || '初曦的窝',
        subtitle: obj.subtitle || '',
        logoUrl: obj.logoUrl || '/favicon.png',
        faviconUrl: obj.faviconUrl || '/favicon.png',
        seoDescription: obj.seoDescription || '',
        seoKeywords: obj.seoKeywords || '',
        githubUrl: obj.githubUrl || '',
        weiboUrl: obj.weiboUrl || '',
        qqUrl: obj.qqUrl || '',
        footerText: obj.footerText || '',
        footerIcp: obj.footerIcp || ''
      })
      if (siteSettings.siteName) {
        document.title = siteSettings.siteName
      }
    }
  } catch (e) { console.warn('[设置] 站点配置加载失败:', e) }
}

/* ---------- 外观设置 ---------- */
async function loadAppearanceSettings() {
  try {
    const data = await api.siteContent('appearance-settings')
    let obj = data
    if (data && typeof data.contentJson === 'string') obj = JSON.parse(data.contentJson)
    if (typeof obj === 'string') obj = JSON.parse(obj)
    if (!obj || typeof obj !== 'object') return

    if (obj.primaryColor) {
      document.documentElement.style.setProperty('--cx-primary', obj.primaryColor)
    }

    const saved = JSON.parse(localStorage.getItem('chuxi-nest-settings') || '{}')
    const patch = {}
    if (obj.defaultTheme && !saved.theme) {
      if (obj.defaultTheme === 'dark') {
        patch.theme = 'dark'
      } else if (obj.defaultTheme === 'light') {
        patch.theme = 'light'
      } else if (obj.defaultTheme === 'system') {
        patch.theme = window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
      }
    }
    if (obj.sakuraEnabled !== undefined && saved.sakuraEnabled === undefined) {
      patch.sakuraEnabled = !!obj.sakuraEnabled
    }
    if (obj.live2dEnabled !== undefined && saved.live2dEnabled === undefined) {
      patch.live2dEnabled = !!obj.live2dEnabled
    }
    if (Object.keys(patch).length > 0) {
      settings.update(patch)
    }
  } catch (e) { console.warn('[设置] 外观配置加载失败:', e) }
}

/* ---------- 主题 ---------- */
function toggleTheme() {
  settings.setTheme(settings.isDark ? 'light' : 'dark')
}

/* ---------- 背景（设置弹窗选择） ---------- */
function isPortrait() {
  return window.innerHeight > window.innerWidth
}

function chooseBackground(img) {
  settings.update(isPortrait() ? { selectedVerticalImage: img } : { selectedLandscapeImage: img })
  bgLayerRef.value?.swapBackground(img)
}

/* ---------- 共享弹窗状态 ---------- */
const musicBarOpen = ref(false)
const aiExpanded = ref(false)
const searchOpen = ref(false)
const settingOpen = ref(false)
const authOpen = ref(false)
const authMode = ref('password')
const authPanel = ref('login')
const pawProgress = ref(0)
const pawEnabled = ref(true)
const topbarSolid = ref(false)
const PAW_SCROLL_THRESHOLD = 1600

function togglePaw() {
  pawEnabled.value = !pawEnabled.value
}

// 路由切换时关闭所有浮层，避免 keep-alive / 滚动导致的状态残留
const route = useRoute()
watch(() => route.path, () => {
  settingOpen.value = false
  aiExpanded.value = false
  searchOpen.value = false
  musicBarOpen.value = false
  authOpen.value = false
})

function openAuthDialog() {
  authPanel.value = 'login'
  authOpen.value = true
}

function goAdmin() {
  router.push('/admin')
}

// ESC 关闭登录弹窗 / 设置弹窗
function onAuthKeydown(e) {
  if (e.key !== 'Escape') return
  if (settingOpen.value) { settingOpen.value = false; return }
  if (authOpen.value) authOpen.value = false
}

/* ---------- live2d ---------- */
function loadLive2dScript() {
  return new Promise((resolve, reject) => {
    if (window.loadlive2d) { resolve(); return }
    const s = document.createElement('script')
    s.src = '/live2d/live2d.min.js'
    s.onload = resolve
    s.onerror = reject
    document.body.appendChild(s)
  })
}

async function initLive2d() {
  try {
    await loadLive2dScript()
    if (window.loadlive2d) {
      window.loadlive2d('live2d-canvas', '/live2d/model/mashiro/shifuku.model.json')
    }
  } catch (e) { console.warn('[Live2D] 加载失败:', e) }
}

function reloadLive2d() {
  if (window.loadlive2d) {
    window.loadlive2d('live2d-canvas', '/live2d/model/mashiro/shifuku.model.json')
  }
}

watch(() => settings.live2dEnabled, val => { if (val) nextTick(initLive2d) })

/* ---------- 猫爪回顶：滚动监听 ---------- */
function scrollMainToTop() {
  const main = document.querySelector('.app-shell-main')
  if (main) main.scrollTo({ top: 0, behavior: 'smooth' })
}

let pawScrollEl = null
function onMainScroll() {
  const st = pawScrollEl ? pawScrollEl.scrollTop : 0
  pawProgress.value = Math.min(1, Math.max(0, st / PAW_SCROLL_THRESHOLD))
  topbarSolid.value = st > 24
}

function bindPawScroll() {
  pawScrollEl = document.querySelector('.app-shell-main')
  if (pawScrollEl) {
    pawScrollEl.addEventListener('scroll', onMainScroll, { passive: true })
    onMainScroll()
  }
}

/* ---------- 生命周期 ---------- */
onMounted(() => {
  document.documentElement.classList.toggle('dark', settings.isDark)
  api.bumpViews().catch(() => {})
  loadSiteSettings()
  loadAppearanceSettings()
  nextTick(bindPawScroll)
  window.addEventListener('keydown', onAuthKeydown)
  if (settings.live2dEnabled) initLive2d()
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onAuthKeydown)
  if (pawScrollEl) pawScrollEl.removeEventListener('scroll', onMainScroll)
})
</script>

<style>
/* ========== 顶栏悬浮岛定位 ========== */
.app-shell > header.app-shell-top {
  position: absolute;
  top: 10px;
  left: 14px;
  right: 14px;
  margin: 0;
  z-index: 30;
  border-radius: 20px;
  border: 1px solid transparent;
  background: transparent;
  backdrop-filter: none;
  -webkit-backdrop-filter: none;
  box-shadow: none;
  transition: background-color 0.36s ease, border-color 0.36s ease, box-shadow 0.36s ease,
    backdrop-filter 0.36s ease;
}

.app-shell > .app-shell-top.is-solid {
  border-color: rgba(255, 255, 255, 0.32);
  background: rgba(255, 255, 255, 0.24);
  backdrop-filter: blur(5px) saturate(1.1);
  -webkit-backdrop-filter: blur(5px) saturate(1.1);
  box-shadow: 0 6px 18px rgba(88, 111, 214, 0.08);
}

.app-shell .app-shell-main {
  padding-top: 82px;
}

.layout-right-sidebar .layout-right-sidebar__inner {
  padding-top: 62px;
}

.app-shell > .app-shell-top::before {
  border-radius: inherit;
}

html.dark .app-shell > .app-shell-top {
  border-color: transparent;
  background: transparent;
  box-shadow: none;
}

html.dark .app-shell > .app-shell-top.is-solid {
  border-color: rgba(255, 255, 255, 0.08);
  background: rgba(24, 24, 24, 0.34);
  backdrop-filter: none;
  -webkit-backdrop-filter: none;
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.22);
}

/* ========== 底部音乐条悬浮岛 ========== */
.app-shell-body__content-col > .music-bottom-bar-shell {
  width: min(860px, calc(100% - 32px));
  margin: 0 auto 14px;
  border-radius: 22px;
  border: 1px solid rgba(255, 255, 255, 0.58);
  background: rgba(255, 255, 255, 0.66);
  backdrop-filter: blur(16px) saturate(1.4);
  -webkit-backdrop-filter: blur(16px) saturate(1.4);
  box-shadow: 0 12px 32px rgba(88, 111, 214, 0.16), 0 3px 10px rgba(88, 111, 214, 0.1);
  transition: max-height 0.28s ease, opacity 0.22s ease, transform 0.22s ease,
    margin-bottom 0.28s ease;
}

.app-shell-body__content-col > .music-bottom-bar-shell.is-hidden {
  margin-bottom: 0;
}

.app-shell-body__content-col > .music-bottom-bar-shell .music-bottom-bar {
  border: none;
  background: transparent;
}

html.dark .app-shell-body__content-col > .music-bottom-bar-shell {
  border-color: rgba(255, 255, 255, 0.12);
  background: rgba(24, 30, 52, 0.72);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.45), 0 3px 10px rgba(0, 0, 0, 0.3);
}

/* ========== 登录/注册弹窗 ========== */
.login-dialog {
  position: fixed;
  inset: 0;
  z-index: 260;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.login-dialog__mask {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 20% 10%, rgba(157, 180, 255, 0.28), transparent 55%),
    radial-gradient(circle at 85% 90%, rgba(217, 161, 239, 0.24), transparent 55%),
    rgba(78, 96, 148, 0.32);
  backdrop-filter: blur(10px) saturate(1.15);
  -webkit-backdrop-filter: blur(10px) saturate(1.15);
}

.login-dialog__card {
  position: relative;
  display: flex;
  width: min(780px, 100%);
  min-height: 500px;
  border-radius: 28px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.86);
  backdrop-filter: blur(24px) saturate(1.2);
  -webkit-backdrop-filter: blur(24px) saturate(1.2);
  border: 1px solid rgba(255, 255, 255, 0.7);
  box-shadow: 0 28px 68px rgba(88, 111, 214, 0.28), 0 6px 18px rgba(88, 111, 214, 0.14),
    inset 0 1px 0 rgba(255, 255, 255, 0.85);
}

.login-dialog__close {
  position: absolute;
  top: 16px;
  right: 16px;
  z-index: 5;
  width: 32px;
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 999px;
  background: rgba(95, 149, 207, 0.12);
  color: #3f77b5;
  cursor: pointer;
  transition: background 0.22s ease, color 0.22s ease;
}

.login-dialog__close-icon { display: inline-flex; transition: transform 0.28s ease; }
.login-dialog__close:hover { background: rgba(95, 149, 207, 0.22); }
.login-dialog__close:hover .login-dialog__close-icon { transform: rotate(90deg) scale(1.08); }

.login-dialog__side {
  position: relative;
  flex-shrink: 0;
  width: 292px;
  padding: 40px 32px;
  display: flex;
  align-items: center;
  overflow: hidden;
  color: #fff;
  background: linear-gradient(165deg, #9ec6ea 0%, #86b3e0 52%, #8fd4dd 100%);
}

.login-dialog__side-body {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 14px;
}

.login-dialog__paw {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.22);
  border: 1px solid rgba(255, 255, 255, 0.45);
  box-shadow: 0 8px 20px rgba(58, 100, 150, 0.25);
  animation: login-dialog-bob 3.2s ease-in-out infinite;
}

.login-dialog__side-title { margin: 0; font-size: 29px; letter-spacing: 2px; text-shadow: 0 2px 8px rgba(58, 100, 150, 0.3); }
.login-dialog__side-desc { margin: 0; font-size: 15.5px; line-height: 1.8; opacity: 0.92; }

.login-dialog__side-btn {
  margin-top: 6px;
  padding: 9px 30px;
  border-radius: 999px;
  border: 1.5px solid rgba(255, 255, 255, 0.75);
  background: rgba(255, 255, 255, 0.14);
  color: #fff;
  font: inherit;
  font-size: 15.5px;
  letter-spacing: 3px;
  cursor: pointer;
  transition: background 0.24s ease, color 0.24s ease, transform 0.24s ease, box-shadow 0.24s ease;
}

.login-dialog__side-btn:hover {
  background: rgba(255, 255, 255, 0.92);
  color: #4a7cb8;
  transform: translateY(-2px);
  box-shadow: 0 10px 22px rgba(58, 100, 150, 0.32);
}
.login-dialog__side-btn:active { transform: translateY(0) scale(0.97); }

.login-dialog__orb { position: absolute; border-radius: 999px; background: rgba(255, 255, 255, 0.16); border: 1px solid rgba(255, 255, 255, 0.22); pointer-events: none; }
.login-dialog__orb--1 { width: 140px; height: 140px; top: -46px; left: -40px; animation: login-dialog-float 7s ease-in-out infinite alternate; }
.login-dialog__orb--2 { width: 90px; height: 90px; right: -28px; top: 38%; animation: login-dialog-float 8.5s ease-in-out infinite alternate-reverse; }
.login-dialog__orb--3 { width: 120px; height: 120px; bottom: -42px; left: 24%; animation: login-dialog-float 6s ease-in-out infinite alternate; }

.login-dialog__star { position: absolute; color: rgba(255, 255, 255, 0.85); pointer-events: none; animation: login-dialog-twinkle 2.8s ease-in-out infinite; }
.login-dialog__star--1 { top: 14%; right: 22%; font-size: 16.5px; }
.login-dialog__star--2 { top: 42%; left: 12%; font-size: 13px; animation-delay: 0.9s; }
.login-dialog__star--3 { bottom: 16%; right: 16%; font-size: 14.5px; animation-delay: 1.7s; }

.login-dialog__main { flex: 1; min-width: 0; display: flex; flex-direction: column; justify-content: center; padding: 40px 44px; }
.login-dialog__panel { width: 100%; max-width: 330px; margin: 0 auto; display: flex; flex-direction: column; }
.login-dialog__title { margin: 0; text-align: center; font-size: 29px; letter-spacing: 3px; color: #3d4668; }
.login-dialog__subtitle { margin: 8px 0 20px; text-align: center; font-size: 14.5px; color: #93a0c4; }

.login-dialog__mode-switch { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; margin-bottom: 6px; }
.login-dialog__mode-btn {
  height: 34px; border-radius: 12px; border: 1.5px solid transparent;
  background: rgba(95, 149, 207, 0.1); color: #6b7aa8; font: inherit; font-size: 14.5px;
  cursor: pointer; transition: background 0.22s ease, color 0.22s ease, border-color 0.22s ease, transform 0.22s ease, box-shadow 0.22s ease;
}
.login-dialog__mode-btn.is-active { background: rgba(255, 255, 255, 0.95); border-color: rgba(95, 149, 207, 0.55); color: #3f77b5; transform: translateY(-1px); box-shadow: 0 6px 14px rgba(95, 149, 207, 0.22); }
.login-dialog__mode-btn:not(.is-active):hover { background: rgba(95, 149, 207, 0.18); color: #3f77b5; }

.login-dialog__label { margin: 12px 0 6px; font-size: 14.5px; color: #6b7aa8; }
.login-dialog__input { width: 100%; height: 42px; padding: 0 14px; border-radius: 14px; border: 1.5px solid rgba(130, 150, 220, 0.35); background: rgba(255, 255, 255, 0.75); color: #3d4668; font: inherit; font-size: 15.5px; outline: none; transition: border-color 0.24s ease, box-shadow 0.24s ease, transform 0.24s ease, background 0.24s ease; }
.login-dialog__input::placeholder { color: #a9b4d4; }
.login-dialog__input:hover { border-color: rgba(95, 149, 207, 0.55); }
.login-dialog__input:focus { border-color: #6d9bd6; background: #fff; box-shadow: 0 0 0 4px rgba(95, 149, 207, 0.18), 0 6px 16px rgba(95, 149, 207, 0.16); transform: translateY(-1px); }

.login-dialog__code-row { display: flex; gap: 10px; }
.login-dialog__code-row .login-dialog__input { flex: 1; min-width: 0; }
.login-dialog__code-btn { flex-shrink: 0; padding: 0 14px; border-radius: 14px; border: 1.5px solid rgba(95, 149, 207, 0.5); background: transparent; color: #3f77b5; font: inherit; font-size: 14.5px; cursor: pointer; transition: background 0.22s ease, transform 0.22s ease, box-shadow 0.22s ease; }
.login-dialog__code-btn:hover { background: rgba(95, 149, 207, 0.14); transform: translateY(-1px); box-shadow: 0 6px 14px rgba(95, 149, 207, 0.18); }

.login-dialog__aux { margin-top: 10px; text-align: right; }
.login-dialog__link { padding: 0; border: none; background: none; color: #6d9bd6; font: inherit; font-size: 14.5px; cursor: pointer; transition: color 0.2s ease; }
.login-dialog__link:hover { color: #3f77b5; text-decoration: underline; }

.login-dialog__submit { margin-top: 16px; height: 44px; border: none; border-radius: 999px; background: linear-gradient(135deg, #6d9bd6 0%, #67b7cf 100%); color: #fff; font: inherit; font-size: 16.5px; letter-spacing: 6px; text-indent: 6px; cursor: pointer; box-shadow: 0 12px 26px rgba(95, 149, 207, 0.38); transition: transform 0.24s ease, box-shadow 0.24s ease, filter 0.24s ease; }
.login-dialog__submit:hover { transform: translateY(-2px); box-shadow: 0 16px 32px rgba(95, 149, 207, 0.46); filter: brightness(1.04); }
.login-dialog__submit:active { transform: translateY(0) scale(0.98); box-shadow: 0 8px 18px rgba(95, 149, 207, 0.32); }

.login-dialog__tip { margin: 14px 0 0; text-align: center; font-size: 13px; color: #93a0c4; }
.login-dialog__switch-hint { margin: 8px 0 0; text-align: center; font-size: 14.5px; color: #6b7aa8; }

.login-dialog-fade-enter-active { transition: opacity 0.3s ease; }
.login-dialog-fade-enter-active .login-dialog__card { transition: transform 0.42s cubic-bezier(0.34, 1.56, 0.64, 1), opacity 0.3s ease; }
.login-dialog-fade-leave-active { transition: opacity 0.22s ease; }
.login-dialog-fade-leave-active .login-dialog__card { transition: transform 0.22s ease, opacity 0.22s ease; }
.login-dialog-fade-enter-from, .login-dialog-fade-leave-to { opacity: 0; }
.login-dialog-fade-enter-from .login-dialog__card { transform: translateY(26px) scale(0.94); opacity: 0; }
.login-dialog-fade-leave-to .login-dialog__card { transform: translateY(12px) scale(0.97); opacity: 0; }

.login-dialog-panel-enter-active, .login-dialog-panel-leave-active { transition: opacity 0.22s ease, transform 0.22s ease; }
.login-dialog-panel-enter-from { opacity: 0; transform: translateX(16px); }
.login-dialog-panel-leave-to { opacity: 0; transform: translateX(-16px); }

@keyframes login-dialog-float { from { transform: translateY(-8px); } to { transform: translateY(10px); } }
@keyframes login-dialog-bob { 0%, 100% { transform: translateY(0) rotate(-4deg); } 50% { transform: translateY(-6px) rotate(6deg); } }
@keyframes login-dialog-twinkle { 0%, 100% { opacity: 0.35; transform: scale(0.85); } 50% { opacity: 1; transform: scale(1.12); } }

/* 登录弹窗暗色模式 */
html.dark .login-dialog__mask { background: radial-gradient(circle at 20% 10%, rgba(70, 120, 180, 0.2), transparent 55%), radial-gradient(circle at 85% 90%, rgba(80, 150, 180, 0.16), transparent 55%), rgba(10, 14, 28, 0.55); }
html.dark .login-dialog__card { background: rgba(32, 38, 62, 0.9); border-color: rgba(255, 255, 255, 0.08); box-shadow: 0 28px 68px rgba(0, 0, 0, 0.5), 0 6px 18px rgba(0, 0, 0, 0.35), inset 0 1px 0 rgba(255, 255, 255, 0.06); }
html.dark .login-dialog__close { background: rgba(95, 149, 207, 0.16); color: #a8cdf0; }
html.dark .login-dialog__close:hover { background: rgba(95, 149, 207, 0.3); }
html.dark .login-dialog__side { background: linear-gradient(165deg, #274a75 0%, #2f5d88 55%, #2f7a8a 100%); }
html.dark .login-dialog__title { color: #e8ecff; }
html.dark .login-dialog__subtitle, html.dark .login-dialog__tip { color: #8d97bd; }
html.dark .login-dialog__label, html.dark .login-dialog__switch-hint { color: #aab4d8; }
html.dark .login-dialog__mode-btn { background: rgba(255, 255, 255, 0.06); color: #aab4d8; }
html.dark .login-dialog__mode-btn.is-active { background: rgba(95, 149, 207, 0.22); border-color: rgba(127, 176, 221, 0.6); color: #bcd9f2; box-shadow: 0 6px 14px rgba(0, 0, 0, 0.3); }
html.dark .login-dialog__mode-btn:not(.is-active):hover { background: rgba(255, 255, 255, 0.1); color: #bcd9f2; }
html.dark .login-dialog__input { border-color: rgba(127, 176, 221, 0.28); background: rgba(18, 22, 40, 0.65); color: #e8ecff; }
html.dark .login-dialog__input::placeholder { color: #6b7599; }
html.dark .login-dialog__input:hover { border-color: rgba(127, 176, 221, 0.5); }
html.dark .login-dialog__input:focus { border-color: #7fb0dd; background: rgba(18, 22, 40, 0.9); box-shadow: 0 0 0 4px rgba(127, 176, 221, 0.16), 0 6px 16px rgba(0, 0, 0, 0.3); }
html.dark .login-dialog__code-btn { border-color: rgba(127, 176, 221, 0.45); color: #a8cdf0; }
html.dark .login-dialog__code-btn:hover { background: rgba(127, 176, 221, 0.14); box-shadow: 0 6px 14px rgba(0, 0, 0, 0.3); }
html.dark .login-dialog__link { color: #a8cdf0; }
html.dark .login-dialog__link:hover { color: #bcd9f2; }
html.dark .login-dialog__submit { background: linear-gradient(135deg, #4f86c6 0%, #67b7cf 100%); box-shadow: 0 12px 26px rgba(0, 0, 0, 0.42); }
html.dark .login-dialog__submit:hover { box-shadow: 0 16px 32px rgba(0, 0, 0, 0.5); }

/* ========== 移动端适配 ========== */
@media (max-width: 768px) {
  .app-shell > header.app-shell-top { top: 8px; left: 10px; right: 10px; padding: 8px 12px; gap: 10px; }
  .app-shell-top .shell-brand { flex: 1 1 auto; min-width: 0; gap: 8px; }
  .app-shell-top .shell-brand > span { font-size: 16.5px; }
  .app-shell-top .shell-brand > .cx-popover-wrapper { margin-left: auto; flex: none; }
  .app-shell-top .shell-action-btn { width: 40px; height: 40px; }
  .app-shell-top .shell-actions { gap: 5px; }
  .shell-actions .shell-action-btn.is-ai { display: inline-flex; }
  .layout-right-sidebar.is-expanded { width: 100%; flex-basis: 100%; max-width: 100%; }
  .live2d-widget { display: none !important; }
  .app-shell-body__content-col > .music-bottom-bar-shell { width: calc(100% - 20px); }
  .music-bottom-bar .track-left .meta-wrap { display: none; }
  .app-shell .layout-article-search-overlay { padding: 8vh 12px 20px; }
  .app-shell .layout-article-search-panel { width: calc(100vw - 24px); padding: 12px; gap: 12px; }
  .app-shell .setting-dialog { padding: 12px; }
  .app-shell .setting-dialog__card { width: calc(100vw - 24px); padding: 20px 14px 16px; }
  .setting-dialog .gallery-strip { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10px; overflow-x: visible; padding-bottom: 4px; }
  .setting-dialog .gallery-item, .setting-dialog .gallery-item.is-portrait { width: 100%; height: 64px; }
  .setting-dialog .gallery-item img { width: 100%; height: 100%; object-fit: cover; display: block; }
  .setting-dialog .gallery-item__label { left: 8px; bottom: 8px; padding: 3px 8px; font-size: 11px; }
  .app-shell .login-dialog { padding: 12px; }
  .cx-popover.login-person-popover, .cx-popover.top-nav-mobile-popover { max-width: calc(100vw - 24px); }
}

@media (max-width: 480px) {
  .app-shell > .app-shell-top { padding: 8px 10px; }
  .app-shell-top .shell-brand > span { font-size: 15.5px; }
  .app-shell-top .shell-actions { gap: 3px; }
  .app-shell-top .shell-action-cat-wrap, .app-shell-top .shell-action-btn.is-setting { display: none; }
  .music-bottom-bar .control-btn.is-seek-back, .music-bottom-bar .control-btn.is-seek-forward, .music-bottom-bar .control-btn.is-repeat-dup { display: none; }
  .login-dialog__main { padding: 28px 18px 24px; }
  .login-dialog__title { font-size: 24px; }
}

@media (max-width: 720px) {
  .login-dialog__card { min-height: 0; }
  .login-dialog__side { display: none; }
  .login-dialog__main { padding: 36px 26px 30px; }
}
</style>
