<template>
  <header class="app-shell-top" :class="{ 'is-solid': solid }">
    <div class="shell-brand">
      <span
        title="返回首页"
        role="link"
        tabindex="0"
        aria-label="返回首页"
        @click="router.push('/index')"
        @keydown.enter.prevent="router.push('/index')"
        @keydown.space.prevent="router.push('/index')"
      >{{ siteName }}</span>
      <nav ref="navRef" class="shell-nav" aria-label="主导航" @mouseleave="hoverPath = ''">
        <span class="nav-indicator" :style="indicatorStyle"></span>
        <span class="nav-underline" :style="underlineStyle"></span>
        <RouterLink
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="nav-link"
          :class="{ 'is-active': isNavActive(item.path) }"
          @mouseenter="hoverPath = item.path"
        >
          <span class="nav-link__content">
            <SvgIcon :name="item.icon" class="nav-link__icon" />
          </span>
          <span class="nav-link__label">{{ item.label }}</span>
        </RouterLink>
      </nav>
      <div ref="navWrapRef" class="cx-popover-wrapper">
        <div class="cx-popover-trigger">
          <button type="button" class="shell-action-btn shell-nav-menu" aria-label="导航菜单" :aria-expanded="mobileNavOpen ? 'true' : 'false'" @click="mobileNavOpen = !mobileNavOpen">
            <SvgIcon name="common-menu" class="action-icon" />
          </button>
        </div>
        <transition name="cx-popover-fade">
          <div v-if="mobileNavOpen" class="cx-popover top-nav-mobile-popover">
            <div
              v-for="item in navItems"
              :key="item.path"
              class="cx-popover-item"
              @click="goNav(item.path)"
            >
              <span class="cx-popover-item__icon"><SvgIcon :name="item.icon" size="16px" /></span>
              <span class="cx-popover-item__content">{{ item.label }}</span>
            </div>
          </div>
        </transition>
      </div>
    </div>
    <div class="shell-actions">
      <button type="button" class="shell-action-btn is-search" aria-label="搜索文章" @click="$emit('open-search')">
        <SvgIcon name="common-search" class="action-icon" />
      </button>
      <button type="button" class="shell-action-btn is-theme" aria-label="切换主题" @click="$emit('toggle-theme')">
        <SvgIcon :name="settings.isDark ? 'common-sun' : 'common-moon'" class="action-icon" />
      </button>
      <button type="button" class="shell-action-btn is-music" aria-label="音乐播放器" @click="$emit('toggle-music')">
        <SvgIcon name="common-music" class="action-icon" />
      </button>
      <button
        type="button"
        class="shell-action-btn is-ai"
        :class="{ 'is-active': aiOpen }"
        :aria-pressed="aiOpen"
        :title="aiOpen ? '关闭 AI 助手' : '打开 AI 助手'"
        aria-label="打开 AI 助手"
        @click="emit('toggle-ai')"
      >
        <SvgIcon name="common-chat" class="action-icon" />
      </button>
      <div class="shell-action-cat-wrap">
        <button
          type="button"
          class="shell-action-btn is-cat"
          :class="{ 'is-active': settings.live2dEnabled, 'is-off': !settings.live2dEnabled }"
          :aria-pressed="settings.live2dEnabled"
          :title="settings.live2dEnabled ? '关闭看板娘' : '打开看板娘'"
          aria-label="切换看板娘"
          @click="$emit('toggle-live2d')"
        >
          <SvgIcon name="common-cat" class="action-icon" />
        </button>
        <transition name="paw-rope">
          <div v-show="pawProgress > 0" class="paw-rope">
            <div class="paw-rope__sway">
              <span class="paw-rope__line" :style="ropeLineStyle"></span>
              <span class="paw-rope__paw" title="返回顶部" @click="$emit('scroll-to-top')">
                <SvgIcon name="common-paw" size="22px" />
              </span>
            </div>
          </div>
        </transition>
      </div>
      <div ref="setWrapRef" class="cx-popover-wrapper">
        <div class="cx-popover-trigger">
          <button
            type="button"
            class="shell-action-btn is-setting"
            :class="{ 'is-active': settingMenuOpen }"
            :aria-pressed="settingMenuOpen"
            :title="settingMenuOpen ? '关闭菜单' : '设置 / 账号'"
            @click="settingMenuOpen = !settingMenuOpen"
          >
            <SvgIcon name="common-setting" class="action-icon" />
          </button>
        </div>
        <transition name="cx-popover-fade">
          <div v-if="settingMenuOpen" class="cx-popover login-person-popover">
            <div class="cx-popover-item" @click="openSettings">
              <span class="cx-popover-item__icon"><SvgIcon name="common-setting" size="16px" /></span>
              <span class="cx-popover-item__content">偏好设置</span>
            </div>
            <div class="cx-popover-item" @click="openAuth">
              <span class="cx-popover-item__icon"><SvgIcon name="common-person" size="16px" /></span>
              <span class="cx-popover-item__content">登录 / 注册</span>
            </div>
            <div class="cx-popover-item" @click="goAdmin">
              <span class="cx-popover-item__icon"><SvgIcon name="common-archive" size="16px" /></span>
              <span class="cx-popover-item__content">后台管理</span>
            </div>
          </div>
        </transition>
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref, watch, nextTick, onMounted, onBeforeUnmount, computed } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import SvgIcon from '../../components/SvgIcon.vue'
import { useSettingsStore } from '../../stores/settings'

const props = defineProps({
  siteName: { type: String, default: '初曦的窝' },
  pawProgress: { type: Number, default: 0 },
  solid: { type: Boolean, default: false },
  settingOpen: { type: Boolean, default: false },
  aiOpen: { type: Boolean, default: false }
})

const emit = defineEmits(['open-search', 'toggle-theme', 'toggle-ai', 'toggle-music', 'open-settings', 'close-settings', 'open-auth', 'go-admin', 'toggle-live2d', 'scroll-to-top'])
const MIN_ROPE_HEIGHT = 24
const MAX_ROPE_HEIGHT = 120

const ropeLineStyle = computed(() => ({
  height: `${MIN_ROPE_HEIGHT + (MAX_ROPE_HEIGHT - MIN_ROPE_HEIGHT) * Math.min(1, Math.max(0, props.pawProgress))}px`
}))

const settings = useSettingsStore()
const route = useRoute()
const router = useRouter()

/* ---------- 导航 ---------- */
const navItems = [
  { path: '/index', label: '首页', icon: 'common-home' },
  { path: '/tool', label: '工具', icon: 'common-tool' },
  { path: '/bangumi', label: '番剧', icon: 'common-articlePages' },
  { path: '/timeline', label: '时间线', icon: 'common-timeline' },
  { path: '/tree-hole', label: '树洞', icon: 'common-tree' },
  { path: '/parallax', label: '视差', icon: 'common-parallax' },
  { path: '/archive', label: '归档', icon: 'common-archive' },
  { path: '/about', label: '关于', icon: 'common-person' },
  { path: '/components', label: '组件', icon: 'common-component' }
]
const navRef = ref(null)
const indicatorStyle = ref({})
const underlineStyle = ref({})
const hoverPath = ref('')
const mobileNavOpen = ref(false)
const settingMenuOpen = ref(false)
const navWrapRef = ref(null)
const setWrapRef = ref(null)

function isNavActive(path) {
  if (path === '/index') return route.path === '/index' || route.path.startsWith('/article')
  return route.path === path || route.path.startsWith(`${path}/`)
}

function updateIndicator() {
  const nav = navRef.value
  if (!nav) return
  const active = nav.querySelector('.nav-link.is-active')
  if (!active) { indicatorStyle.value = { opacity: 0 }; updateUnderline(); return }
  const navRect = nav.getBoundingClientRect()
  const rect = active.getBoundingClientRect()
  const cx = rect.left - navRect.left + rect.width / 2
  const cy = rect.top - navRect.top + rect.height / 2
  indicatorStyle.value = {
    transform: `translate(${cx}px, ${cy}px) translate(-50%, -50%)`,
    width: `${rect.width}px`,
    height: `${rect.height}px`,
    opacity: 1
  }
  updateUnderline()
}

function updateUnderline() {
  const nav = navRef.value
  if (!nav) return
  const target = hoverPath.value
    ? nav.querySelector(`.nav-link[href="${hoverPath.value}"]`)
    : nav.querySelector('.nav-link.is-active')
  if (!target) { underlineStyle.value = { opacity: 0 }; return }
  const navRect = nav.getBoundingClientRect()
  const rect = target.getBoundingClientRect()
  underlineStyle.value = {
    transform: `translateX(${rect.left - navRect.left + rect.width / 2}px) translateX(-50%)`,
    width: `${Math.max(18, rect.width * 0.52)}px`,
    opacity: 1
  }
}

watch(hoverPath, () => nextTick(updateUnderline))
watch(() => route.path, () => nextTick(updateIndicator))

function goNav(path) {
  mobileNavOpen.value = false
  router.push(path)
}

function openSettings() {
  settingMenuOpen.value = false
  emit('open-settings')
}

function openAuth() {
  settingMenuOpen.value = false
  emit('open-auth')
}

function goAdmin() {
  settingMenuOpen.value = false
  emit('go-admin')
}

function onResize() {
  updateIndicator()
}

/* 点击浮层外关闭：各菜单只在「点到自己 wrapper 之外」时收起，
   因此点触发按钮本身仍由按钮的 @click 负责 toggle，不会开一下又立刻被关。
   用 pointerdown 而非 click，避免 click 在某些浏览器上被内部元素吞掉。 */
function onDocPointerDown(e) {
  const t = e.target
  if (mobileNavOpen.value && navWrapRef.value && !navWrapRef.value.contains(t)) {
    mobileNavOpen.value = false
  }
  if (settingMenuOpen.value && setWrapRef.value && !setWrapRef.value.contains(t)) {
    settingMenuOpen.value = false
  }
}

function onDocKeydown(e) {
  if (e.key !== 'Escape') return
  mobileNavOpen.value = false
  settingMenuOpen.value = false
}

// 路由切换时收起，防止跳页后菜单残留
watch(() => route.path, () => {
  mobileNavOpen.value = false
  settingMenuOpen.value = false
})

onMounted(() => {
  nextTick(updateIndicator)
  window.addEventListener('resize', onResize)
  document.addEventListener('pointerdown', onDocPointerDown)
  document.addEventListener('keydown', onDocKeydown)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  document.removeEventListener('pointerdown', onDocPointerDown)
  document.removeEventListener('keydown', onDocKeydown)
})

defineExpose({ updateIndicator })
</script>

<style scoped>
/* ========== 顶栏设置/账号菜单 ========== */
.cx-popover.login-person-popover {
  min-width: 168px;
  margin-top: 46px;
  margin-left: -168px;
}
/* 设置按钮在顶栏最右侧（猫爪之后），菜单右缘对齐按钮右缘 */
.cx-popover-wrapper:last-child .cx-popover.login-person-popover {
  right: 0;
  left: auto;
  margin-left: 0;
}

/* 移动端导航菜单：popover 为 fixed 且未设 top，会停在静态位置（与顶栏同一行），
   导致首行被右侧操作图标压住（实测重叠 122×40px）。下移到顶栏底边之下：
   静态 top 16px + 56px = 72px，顶栏底边 64px，留 8px 间隙。 */
.cx-popover.top-nav-mobile-popover {
  margin-top: 56px;
}

/* ========== 品牌区 ========== */
.shell-brand > span {
  white-space: nowrap;
  flex: none;
  cursor: pointer;
  transition: color 0.2s ease;
  outline: none;
}
.shell-brand > span:focus-visible {
  outline: 2px solid color-mix(in srgb, var(--accent-solid) 55%, transparent);
  outline-offset: 4px;
  border-radius: 4px;
}

.shell-brand > span:hover {
  color: var(--accent-text);
}

/* ========== Tab 下划线 ========== */
.shell-nav .nav-underline {
  position: absolute;
  left: 0;
  bottom: 2px;
  height: 3px;
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(109, 155, 214, 0.35), #3f77b5 45%, rgba(109, 155, 214, 0.35));
  box-shadow: 0 2px 8px rgba(63, 119, 181, 0.4);
  opacity: 0;
  pointer-events: none;
  z-index: 2;
  transition:
    transform 0.42s cubic-bezier(0.34, 1.56, 0.64, 1),
    width 0.32s cubic-bezier(0.34, 1.56, 0.64, 1),
    opacity 0.2s ease;
}

html.dark .shell-nav .nav-underline {
  background: linear-gradient(90deg, rgba(140, 190, 240, 0.3), #8cbef0 45%, rgba(140, 190, 240, 0.3));
  box-shadow: 0 2px 10px rgba(140, 190, 240, 0.35);
}

@media (max-width: 768px) {
  .shell-nav .nav-underline { display: none; }
}

/* ========== 猫图标按钮：看板娘开关状态 ========== */
.shell-action-btn.is-cat.is-active {
  color: var(--action-btn-hover-color);
  background-color: var(--action-btn-hover-bg);
}
.shell-action-btn.is-cat.is-off {
  opacity: 0.45;
}
</style>
