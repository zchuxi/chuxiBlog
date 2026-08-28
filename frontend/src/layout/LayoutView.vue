<template>
  <div class="app-shell">
    <!-- 背景 -->
    <BackgroundLayer ref="bgLayerRef" />

    <!-- 顶栏 -->
    <TopBar
      ref="topBarRef"
      :site-name="siteName"
      :paw-progress="pawProgress"
      :solid="topbarSolid"
      :setting-open="settingOpen"
      :ai-open="aiExpanded"
      @open-search="searchOpen = true"
      @toggle-theme="toggleTheme"
      @toggle-ai="aiExpanded = !aiExpanded"
      @toggle-music="musicBarRef?.toggleMusicBar()"
      @open-settings="settingOpen = true"
      @close-settings="settingOpen = false"
      @open-auth="openAuthDialog"
      @go-admin="goAdmin"
      @toggle-live2d="toggleLive2d"
      @scroll-to-top="scrollMainToTop"
    />

    <!-- 主体 -->
    <div class="app-shell-body">
      <div class="app-shell-body__content-col">
        <div class="app-shell-main-wrap">
          <section class="app-shell-main" tabindex="0" aria-label="页面内容">
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
      ref="live2dWidgetRef"
      class="live2d-widget"
      :class="{ 'is-hidden': !settings.live2dEnabled }"
      style="--live2d-bottom-offset: 0px; --live2d-bottom-gap: 8px;"
    >
      <div ref="live2dStageRef" class="live2d-widget__stage" :class="{ 'is-ready': live2dStageReady }">
        <canvas id="live2d-canvas" width="280" height="280" style="touch-action: none;"></canvas>
      </div>
      <!-- 加载态：模型约 15MB，首次加载有明显等待，给占位骨架避免一片空白 -->
      <div
        v-if="live2dStatus !== 'ready'"
        class="live2d-widget__placeholder"
        :class="{ 'is-error': live2dStatus === 'error' }"
        role="status"
        aria-live="polite"
      >
        <template v-if="live2dStatus === 'error'">
          <p class="live2d-widget__placeholder-text">看板娘加载失败</p>
          <button type="button" class="live2d-widget__placeholder-retry" @click="reloadLive2d">点击重试</button>
        </template>
        <template v-else>
          <span class="live2d-widget__placeholder-figure" aria-hidden="true"></span>
          <p class="live2d-widget__placeholder-text">看板娘登场中…</p>
        </template>
      </div>
      <div class="live2d-widget__actions">
        <button type="button" class="cx-button cx-button--primary is-round is-circle is-plain" aria-label="打开 AI 助手" title="打开 AI 助手" @click="aiExpanded = true">
          <span class="cx-button__content"><SvgIcon name="common-chat" size="18px" /></span>
        </button>
        <button type="button" class="cx-button cx-button--info is-round is-circle is-plain is-disabled" aria-label="更换服装（暂不可用）" title="更换服装（暂不可用）" disabled>
          <span class="cx-button__content"><SvgIcon name="common-hanger" size="18px" /></span>
        </button>
        <button type="button" class="cx-button cx-button--warning is-round is-circle is-plain" aria-label="重新加载看板娘" title="重新加载看板娘" @click="reloadLive2d">
          <span class="cx-button__content"><SvgIcon name="common-reset" size="18px" /></span>
        </button>
        <button type="button" class="cx-button cx-button--danger is-round is-circle is-plain" aria-label="关闭看板娘" title="关闭看板娘" @click="settings.update({ live2dEnabled: false })">
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
          <!-- 左侧图片封面 -->
          <aside class="login-dialog__side">
            <img
              :src="settings.selectedVerticalImage"
              class="login-dialog__side-image"
              alt=""
              decoding="async"
              draggable="false"
            />
            <button
              type="button"
              class="login-dialog__change-image"
              aria-label="更换登录侧栏图片"
              title="更换图片"
              :disabled="settings.verticalImages.length < 2"
              @click="changeAuthSideImage"
            >
              <SvgIcon name="common-exchange" size="16px" />
              <span>更换图片</span>
            </button>
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
import {
  LIVE2D_AUTO_START_DELAY_MS,
  initLive2d as initLive2dModel,
  destroyLive2d,
  handleLive2dTap,
  isLive2dReady
} from '../live2d/live2dMiku.js'
import { bindLive2dWidgetDrag } from '../live2d/live2dWidgetDrag.js'

const settings = useSettingsStore()
const router = useRouter()

const bgLayerRef = ref(null)
const topBarRef = ref(null)
const musicBarRef = ref(null)
const live2dWidgetRef = ref(null)
const live2dStageRef = ref(null)

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

function changeAuthSideImage() {
  const images = settings.verticalImages.filter(Boolean)
  if (images.length < 2) return
  const currentIndex = images.indexOf(settings.selectedVerticalImage)
  const nextImage = images[currentIndex < 0 ? 0 : (currentIndex + 1) % images.length]
  settings.update({ selectedVerticalImage: nextImage })
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
const topbarSolid = ref(false)
const PAW_SCROLL_THRESHOLD = 2400

function toggleLive2d() {
  settings.update({ live2dEnabled: !settings.live2dEnabled })
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

/* ---------- live2d 看板娘（Cubism 4 / miku；延迟加载：首屏空闲或首次交互后再初始化）---------- */
let live2dReady = false
let live2dInitTimer = null
let cleanupLive2dDrag = () => {}
const live2dStageReady = ref(false)
// 加载态三值：idle（未开始/加载中）→ ready（已就绪）| error（失败可重试）
const live2dStatus = ref('idle')

function bindLive2dDrag() {
  cleanupLive2dDrag()
  cleanupLive2dDrag = bindLive2dWidgetDrag({
    widget: live2dWidgetRef.value,
    handle: live2dStageRef.value,
    onTap: handleLive2dTap
  })
}

async function initLive2d() {
  if (live2dReady) return
  live2dReady = true
  try {
    const canvas = document.getElementById('live2d-canvas')
    if (!canvas) throw new Error('未找到 live2d 画布元素')
    await initLive2dModel(canvas)
    if (!isLive2dReady()) throw new Error('Live2D 模型未进入就绪状态')
    live2dStageReady.value = true
    live2dStatus.value = 'ready'
  } catch (e) {
    console.warn('[Live2D] 加载失败:', e)
    live2dStageReady.value = false
    live2dStatus.value = 'error'
    live2dReady = false
  }
}

function cancelLive2dInit() {
  clearTimeout(live2dInitTimer)
  live2dInitTimer = null
  window.removeEventListener('pointerdown', initLive2dOnce)
}

function scheduleLive2dInit() {
  if (!settings.live2dEnabled || live2dReady) return
  cancelLive2dInit()
  // 首屏稳定后尽快初始化；首次指针交互仍可提前触发
  live2dInitTimer = setTimeout(initLive2d, LIVE2D_AUTO_START_DELAY_MS)
  window.addEventListener('pointerdown', initLive2dOnce, { once: true })
}

function initLive2dOnce() {
  // 兜底：开关已关闭或已初始化时不再拉起脚本
  if (!settings.live2dEnabled || live2dReady) return
  initLive2d()
}

function reloadLive2d() {
  live2dStageReady.value = false
  live2dStatus.value = 'idle'
  destroyLive2d()
  live2dReady = false
  initLive2d()
}

watch(() => settings.live2dEnabled, val => { val ? scheduleLive2dInit() : cancelLive2dInit() })

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
  nextTick(bindLive2dDrag)
  window.addEventListener('keydown', onAuthKeydown)
  if (settings.live2dEnabled) scheduleLive2dInit()
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onAuthKeydown)
  cancelLive2dInit()
  cleanupLive2dDrag()
  destroyLive2d()
  if (pawScrollEl) pawScrollEl.removeEventListener('scroll', onMainScroll)
})
</script>

<style>
/* 看板娘加载占位：模型约 15MB（moc3 9MB + 贴图 6.4MB），首次加载有明显等待。
   stage 在就绪前是 opacity:0，若不给占位，用户只会看到一片空白。 */
.live2d-widget__placeholder {
  position: absolute;
  right: 0;
  bottom: var(--live2d-composer-space-desktop, 56px);
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  width: 100%;
  padding: 18px 12px;
  border: 1px dashed var(--topbar-border);
  border-radius: 20px;
  background: var(--popover-bg);
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.1);
  color: var(--text-color);
  pointer-events: none;
}

/* 失败态要能点重试，占位整体恢复可交互 */
.live2d-widget__placeholder.is-error {
  border-style: solid;
  pointer-events: auto;
}

/* 拟人形占位：圆头 + 身体，比纯色块更贴合看板娘的轮廓预期 */
.live2d-widget__placeholder-figure {
  position: relative;
  width: 46px;
  height: 58px;
  opacity: 0.55;
  animation: live2d-placeholder-pulse 1.6s ease-in-out infinite;
}

.live2d-widget__placeholder-figure::before,
.live2d-widget__placeholder-figure::after {
  content: '';
  position: absolute;
  left: 50%;
  background: currentColor;
  transform: translateX(-50%);
}

.live2d-widget__placeholder-figure::before {
  top: 0;
  width: 24px;
  height: 24px;
  border-radius: 50%;
}

.live2d-widget__placeholder-figure::after {
  top: 28px;
  width: 40px;
  height: 30px;
  border-radius: 16px 16px 12px 12px;
}

.live2d-widget__placeholder-text {
  margin: 0;
  font-size: 13px;
  line-height: 1.4;
  text-align: center;
  opacity: 0.75;
}

.live2d-widget__placeholder-retry {
  padding: 5px 14px;
  border: 1px solid var(--topbar-border);
  border-radius: 999px;
  background: transparent;
  color: inherit;
  font: inherit;
  font-size: 12.5px;
  cursor: pointer;
  transition: opacity 0.2s ease;
}

.live2d-widget__placeholder-retry:hover {
  opacity: 0.72;
}

.live2d-widget__placeholder-retry:focus-visible {
  outline: 2px solid currentColor;
  outline-offset: 2px;
}

@keyframes live2d-placeholder-pulse {
  0%, 100% { opacity: 0.32; transform: translateY(0); }
  50% { opacity: 0.62; transform: translateY(-3px); }
}

@media (prefers-reduced-motion: reduce) {
  .live2d-widget__placeholder-figure { animation: none; }
}

@media (max-width: 900px) {
  .live2d-widget__placeholder {
    bottom: var(--live2d-composer-space-mobile, 52px);
    padding: 14px 10px;
  }
  .live2d-widget__placeholder-figure { width: 38px; height: 48px; }
  .live2d-widget__placeholder-figure::before { width: 20px; height: 20px; }
  .live2d-widget__placeholder-figure::after { top: 24px; width: 34px; height: 24px; }
}

/* Pixi 会动态改写 canvas 的内联 cursor；用 !important 钉死为手型，保证悬停看板娘时光标不闪烁。
   canvas 仅负责渲染（pointer-events:none），点击/拖动/光标统一由 stage 命中面处理。 */
.live2d-widget__stage,
.live2d-widget__stage canvas {
  cursor: grab !important;
}

/* canvas 只负责渲染，stage 是唯一的 DOM 指针命中面。 */
.live2d-widget__stage canvas {
  pointer-events: none !important;
}

.live2d-widget.is-dragging .live2d-widget__stage,
.live2d-widget.is-dragging .live2d-widget__stage canvas {
  cursor: grabbing !important;
}

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

/* P2-2 滚动容器可聚焦（键盘滚动可达），聚焦时用弱化描边替代默认 outline，避免突兀 */
.app-shell .app-shell-main:focus-visible {
  outline: 2px solid color-mix(in srgb, var(--accent-solid) 55%, transparent);
  outline-offset: -2px;
  border-radius: 20px;
}
.app-shell .app-shell-main:focus {
  outline: none;
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
  border: 1px solid var(--action-btn-hover-bg);
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
  background: radial-gradient(circle at 20% 10%, var(--glow-left), transparent 55%),
    radial-gradient(circle at 85% 90%, var(--glow-right), transparent 55%),
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
  background: var(--nested-outer-card-bg);
  backdrop-filter: blur(24px) saturate(1.2);
  -webkit-backdrop-filter: blur(24px) saturate(1.2);
  border: 1px solid var(--nested-outer-card-border);
  box-shadow: var(--nested-outer-card-shadow);
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
  background: var(--accent-glow);
  color: var(--accent-strong);
  cursor: pointer;
  transition: background 0.22s ease, color 0.22s ease;
}

.login-dialog__close-icon { display: inline-flex; transition: transform 0.28s ease; }
.login-dialog__close:hover { background: color-mix(in srgb, var(--accent-glow) 180%, transparent); }
.login-dialog__close:hover .login-dialog__close-icon { transform: rotate(90deg) scale(1.08); }

.login-dialog__side {
  position: relative;
  flex-shrink: 0;
  width: 292px;
  overflow: hidden;
  background: var(--input-bg);
  box-shadow: 12px 0 32px rgba(35, 48, 78, 0.12);
}

.login-dialog__side::after {
  content: "";
  position: absolute;
  inset: 0;
  border-right: 1px solid rgba(255, 255, 255, 0.18);
  background: linear-gradient(90deg, transparent 72%, rgba(18, 28, 48, 0.12));
  pointer-events: none;
  z-index: 1;
}

.login-dialog__side-image {
  display: block;
  width: 100%;
  height: 100%;
  min-height: 500px;
  object-fit: cover;
  object-position: center;
  user-select: none;
}

.login-dialog__change-image {
  position: absolute;
  left: 50%;
  bottom: 20px;
  z-index: 2;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 44px;
  padding: 0 18px;
  border: 1px solid rgba(255, 255, 255, 0.48);
  border-radius: 999px;
  background: rgba(18, 28, 48, 0.52);
  backdrop-filter: blur(12px) saturate(1.15);
  -webkit-backdrop-filter: blur(12px) saturate(1.15);
  box-shadow: 0 10px 26px rgba(12, 20, 36, 0.24);
  color: #fff;
  font: inherit;
  font-size: 14px;
  white-space: nowrap;
  cursor: pointer;
  transform: translateX(-50%);
  transition: background-color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.login-dialog__change-image:not(:disabled):hover {
  border-color: rgba(255, 255, 255, 0.72);
  background: rgba(18, 28, 48, 0.68);
  box-shadow: 0 14px 30px rgba(12, 20, 36, 0.32);
  transform: translate(-50%, -2px);
}

.login-dialog__change-image:not(:disabled):active { transform: translate(-50%, 0) scale(0.97); }
.login-dialog__change-image:focus-visible { outline: 2px solid #fff; outline-offset: 3px; }
.login-dialog__change-image:disabled { opacity: 0.5; cursor: not-allowed; transform: translateX(-50%); }

.login-dialog__main { flex: 1; min-width: 0; display: flex; flex-direction: column; justify-content: center; padding: 40px 44px; }
.login-dialog__panel { width: 100%; max-width: 330px; margin: 0 auto; display: flex; flex-direction: column; }
.login-dialog__title { margin: 0; text-align: center; font-size: 29px; letter-spacing: 3px; color: var(--text-color); }
.login-dialog__subtitle { margin: 8px 0 20px; text-align: center; font-size: 14.5px; color: color-mix(in srgb, var(--text-color) 45%, var(--accent-solid)); }

.login-dialog__mode-switch { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; margin-bottom: 6px; }
.login-dialog__mode-btn {
  height: 34px; border-radius: 12px; border: 1.5px solid transparent;
  background: var(--accent-glow); color: color-mix(in srgb, var(--text-color) 55%, var(--accent-solid)); font: inherit; font-size: 14.5px;
  cursor: pointer; transition: background 0.22s ease, color 0.22s ease, border-color 0.22s ease, transform 0.22s ease, box-shadow 0.22s ease;
}
.login-dialog__mode-btn.is-active { background: var(--card-bg); border-color: var(--accent-border); color: var(--accent-strong); transform: translateY(-1px); box-shadow: 0 6px 14px var(--accent-glow); }
.login-dialog__mode-btn:not(.is-active):hover { background: color-mix(in srgb, var(--accent-glow) 180%, transparent); color: var(--accent-strong); }

.login-dialog__label { margin: 12px 0 6px; font-size: 14.5px; color: color-mix(in srgb, var(--text-color) 55%, var(--accent-solid)); }
.login-dialog__input { width: 100%; height: 42px; padding: 0 14px; border-radius: 14px; border: 1.5px solid var(--input-border); background: var(--input-bg); color: var(--text-color); font: inherit; font-size: 15.5px; outline: none; transition: border-color 0.24s ease, box-shadow 0.24s ease, transform 0.24s ease, background 0.24s ease; }
.login-dialog__input::placeholder { color: color-mix(in srgb, var(--text-color) 35%, var(--accent-solid)); }
.login-dialog__input:hover { border-color: var(--accent-border); }
.login-dialog__input:focus { border-color: var(--accent-solid); background: var(--card-bg); box-shadow: 0 0 0 4px var(--accent-glow), 0 6px 16px var(--accent-glow); transform: translateY(-1px); }

.login-dialog__code-row { display: flex; gap: 10px; }
.login-dialog__code-row .login-dialog__input { flex: 1; min-width: 0; }
.login-dialog__code-btn { flex-shrink: 0; padding: 0 14px; border-radius: 14px; border: 1.5px solid var(--accent-border); background: transparent; color: var(--accent-strong); font: inherit; font-size: 14.5px; cursor: pointer; transition: background 0.22s ease, transform 0.22s ease, box-shadow 0.22s ease; }
.login-dialog__code-btn:hover { background: var(--accent-glow); transform: translateY(-1px); box-shadow: 0 6px 14px var(--accent-glow); }

.login-dialog__aux { margin-top: 10px; text-align: right; }
.login-dialog__link { padding: 0; border: none; background: none; color: var(--accent-solid); font: inherit; font-size: 14.5px; cursor: pointer; transition: color 0.2s ease; }
.login-dialog__link:hover { color: var(--accent-strong); text-decoration: underline; }

.login-dialog__submit { margin-top: 16px; height: 44px; border: none; border-radius: 999px; background: linear-gradient(135deg, var(--accent-solid) 0%, color-mix(in srgb, var(--accent-solid) 60%, var(--accent-strong)) 100%); color: #fff; font: inherit; font-size: 16.5px; letter-spacing: 6px; text-indent: 6px; cursor: pointer; box-shadow: 0 12px 26px var(--accent-glow); transition: transform 0.24s ease, box-shadow 0.24s ease, filter 0.24s ease; }
.login-dialog__submit:hover { transform: translateY(-2px); box-shadow: 0 16px 32px var(--accent-glow); filter: brightness(1.04); }
.login-dialog__submit:active { transform: translateY(0) scale(0.98); box-shadow: 0 8px 18px var(--accent-glow); }

.login-dialog__tip { margin: 14px 0 0; text-align: center; font-size: 13px; color: color-mix(in srgb, var(--text-color) 45%, var(--accent-solid)); }
.login-dialog__switch-hint { margin: 8px 0 0; text-align: center; font-size: 14.5px; color: color-mix(in srgb, var(--text-color) 55%, var(--accent-solid)); }

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

/* 登录弹窗暗色模式 */
html.dark .login-dialog__mask { background: radial-gradient(circle at 20% 10%, var(--glow-left), transparent 55%), radial-gradient(circle at 85% 90%, var(--glow-right), transparent 55%), rgba(10, 14, 28, 0.55); }
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
