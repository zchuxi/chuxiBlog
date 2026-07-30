<template>
  <header class="app-shell-top" :class="{ 'is-solid': solid }">
    <div class="shell-brand">
      <span title="返回首页" @click="router.push('/index')">{{ siteName }}</span>
      <nav ref="navRef" class="shell-nav" @mouseleave="hoverPath = ''">
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
      <div class="cx-popover-wrapper">
        <div class="cx-popover-trigger">
          <button type="button" class="shell-action-btn shell-nav-menu" @click="mobileNavOpen = !mobileNavOpen">
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
      <button type="button" class="shell-action-btn is-search" @click="$emit('open-search')">
        <SvgIcon name="common-search" class="action-icon" />
      </button>
      <button type="button" class="shell-action-btn is-theme" @click="$emit('toggle-theme')">
        <SvgIcon :name="settings.isDark ? 'common-sun' : 'common-moon'" class="action-icon" />
      </button>
      <button type="button" class="shell-action-btn is-ai" @click="$emit('toggle-ai')">
        <SvgIcon name="common-ai" class="action-icon" />
      </button>
      <button type="button" class="shell-action-btn is-music" @click="$emit('toggle-music')">
        <SvgIcon name="common-music" class="action-icon" />
      </button>
      <div class="shell-action-cat-wrap">
        <button type="button" class="shell-action-btn is-cat" @click="$emit('paw-toggle')">
          <SvgIcon name="common-cat" class="action-icon" />
        </button>
        <transition name="paw-rope">
          <div v-if="pawOpen" class="paw-rope">
            <div class="paw-rope__sway">
              <span class="paw-rope__line"></span>
              <span class="paw-rope__paw" title="返回顶部" @click="$emit('scroll-to-top')">
                <SvgIcon name="common-paw" size="22px" />
              </span>
            </div>
          </div>
        </transition>
      </div>
      <button type="button" class="shell-action-btn is-setting" @click="$emit('open-settings')">
        <SvgIcon name="common-setting" class="action-icon" />
      </button>
      <div class="cx-popover-wrapper">
        <div class="cx-popover-trigger">
          <button type="button" class="shell-action-btn is-person" @click="personMenuOpen = !personMenuOpen">
            <SvgIcon name="common-person" class="action-icon" />
          </button>
        </div>
        <transition name="cx-popover-fade">
          <div v-if="personMenuOpen" class="cx-popover login-person-popover">
            <div class="cx-popover-item" @click="openAuth">
              <span class="cx-popover-item__icon"><SvgIcon name="common-person" size="16px" /></span>
              <span class="cx-popover-item__content">登录 / 注册</span>
            </div>
            <div class="cx-popover-item" @click="goAdmin">
              <span class="cx-popover-item__icon"><SvgIcon name="common-setting" size="16px" /></span>
              <span class="cx-popover-item__content">后台管理</span>
            </div>
          </div>
        </transition>
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref, watch, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import SvgIcon from '../../components/SvgIcon.vue'
import { useSettingsStore } from '../../stores/settings'

defineProps({
  siteName: { type: String, default: '初曦的窝' },
  pawOpen: { type: Boolean, default: false },
  solid: { type: Boolean, default: false }
})

const emit = defineEmits(['open-search', 'toggle-theme', 'toggle-ai', 'toggle-music', 'open-settings', 'open-auth', 'go-admin', 'paw-toggle', 'scroll-to-top'])

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
const personMenuOpen = ref(false)

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

function openAuth() {
  personMenuOpen.value = false
  emit('open-auth')
}

function goAdmin() {
  personMenuOpen.value = false
  emit('go-admin')
}

function onResize() {
  updateIndicator()
}

onMounted(() => {
  nextTick(updateIndicator)
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
})

defineExpose({ updateIndicator })
</script>

<style scoped>
/* ========== 顶栏账号菜单 ========== */
.cx-popover.login-person-popover {
  min-width: 168px;
  margin-top: 46px;
  margin-left: -168px;
}

/* ========== 品牌区 ========== */
.shell-brand > span {
  white-space: nowrap;
  flex: none;
  cursor: pointer;
  transition: color 0.2s ease;
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
</style>
