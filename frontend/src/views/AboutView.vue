<template>
  <main class="about-page">
    <div class="about-shell">
      <LxSection eyebrow="About - 关于本站">
        <section v-reveal="0" class="about-card">
          <span class="about-card__orb about-card__orb--1"></span>
          <span class="about-card__orb about-card__orb--2"></span>
          <aside class="about-profile">
            <div class="about-avatar-ring">
              <img class="about-avatar" src="/favicon.png" alt="站点头像" />
            </div>
            <h2 class="about-site-name">初曦的窝</h2>
            <p class="about-site-tagline">收集工具、追番与灵感碎片的小小基地。</p>
          </aside>
          <div class="about-body">
            <template v-if="hasContent">
              <h3 v-if="content.title" class="about-body-title">{{ content.title }}</h3>
              <div class="md-editor-preview default-theme about-markdown" v-html="renderedHtml"></div>
            </template>
            <div v-else class="about-placeholder">
              <p class="about-placeholder-title">这里还没有写下自我介绍</p>
              <p class="about-placeholder-text">站长还在酝酿一段合适的开场白，先四处逛逛，稍后再来看看吧。</p>
            </div>
          </div>
        </section>
        <section class="about-quick-row">
          <button
            v-for="(entry, ei) in quickEntries"
            :key="entry.path"
            v-reveal="60 + ei * 70"
            type="button"
            class="about-quick-card"
            @click="router.push(entry.path)"
          >
            <span class="about-quick-icon"><SvgIcon :name="entry.icon" size="20px" /></span>
            <span class="about-quick-copy">
              <span class="about-quick-label">{{ entry.label }}</span>
              <span class="about-quick-desc">{{ entry.desc }}</span>
            </span>
            <span class="about-quick-arrow"><SvgIcon name="common-arrow" size="14px" /></span>
          </button>
        </section>
      </LxSection>
    </div>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import LxSection from '../components/LxSection.vue'
import SvgIcon from '../components/SvgIcon.vue'
import { api } from '../api'
import { renderMarkdown } from '../utils/markdown'

const router = useRouter()

const content = ref(null)
const renderedHtml = ref('')
const hasContent = computed(() => !!(content.value && (content.value.title || content.value.markdown)))

const quickEntries = [
  { path: '/index', label: '去看文章', desc: '最新的记录都在首页', icon: 'common-archive' },
  { path: '/tool', label: '工具集合', desc: '收藏的好用站点与工具', icon: 'common-tool' },
  { path: '/bangumi', label: '追番记录', desc: '一起看看最近在追什么', icon: 'common-articlePages' }
]

onMounted(async () => {
  try {
    const record = await api.siteContent('about')
    const parsed = JSON.parse((record && record.contentJson) || '')
    if (!parsed || typeof parsed !== 'object') return
    content.value = { title: parsed.title || '', markdown: parsed.markdown || '' }
    renderedHtml.value = renderMarkdown(content.value.markdown).html
  } catch { /* 无数据或解析失败时展示占位文案 */ }
})
</script>

<style>
.about-page .about-shell {
  display: flex;
  flex-direction: column;
  gap: 26px;
}

/* ========== 主卡：玻璃拟态岛屿 ========== */
.about-page .about-card {
  position: relative;
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 34px;
  padding: 38px 40px;
  border-radius: 26px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.58);
  background: rgba(255, 255, 255, 0.62);
  backdrop-filter: blur(18px) saturate(1.35);
  -webkit-backdrop-filter: blur(18px) saturate(1.35);
  box-shadow: 0 18px 44px rgba(88, 111, 214, 0.16), 0 4px 12px rgba(88, 111, 214, 0.08);
}

.about-page .about-card__orb {
  position: absolute;
  border-radius: 999px;
  pointer-events: none;
  background: radial-gradient(circle, rgba(109, 155, 214, 0.22), transparent 68%);
}

.about-page .about-card__orb--1 {
  width: 260px;
  height: 260px;
  top: -90px;
  right: -70px;
}

.about-page .about-card__orb--2 {
  width: 220px;
  height: 220px;
  bottom: -100px;
  left: 16%;
  background: radial-gradient(circle, rgba(103, 183, 207, 0.2), transparent 68%);
}

/* ----- 左侧：头像与站点信息 ----- */
.about-page .about-profile {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  text-align: center;
  padding: 12px 8px;
}

.about-page .about-avatar-ring {
  width: 132px;
  height: 132px;
  padding: 6px;
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(109, 155, 214, 0.65), rgba(103, 183, 207, 0.65));
  box-shadow: 0 14px 30px rgba(63, 119, 181, 0.28);
}

.about-page .about-avatar {
  display: block;
  width: 100%;
  height: 100%;
  border-radius: 999px;
  object-fit: cover;
  border: 3px solid rgba(255, 255, 255, 0.9);
  background: #fff;
}

.about-page .about-site-name {
  margin: 4px 0 0;
  font-size: 26px;
  letter-spacing: 2px;
  color: var(--text-color);
}

.about-page .about-site-tagline {
  margin: 0;
  font-size: 13px;
  line-height: 1.8;
  color: color-mix(in srgb, var(--text-color) 62%, transparent);
}

/* ----- 右侧：正文 ----- */
.about-page .about-body {
  position: relative;
  z-index: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
  justify-content: center;
}

.about-page .about-body-title {
  margin: 0;
  font-size: 21px;
  letter-spacing: 1px;
  color: var(--text-color);
}

.about-page .about-markdown {
  background: transparent;
}

.about-page .about-placeholder {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 34px 26px;
  border-radius: 18px;
  text-align: center;
  border: 1.5px dashed rgba(126, 160, 198, 0.45);
  background: color-mix(in srgb, rgba(255, 255, 255, 0.5) 80%, transparent);
}

.about-page .about-placeholder-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-color);
}

.about-page .about-placeholder-text {
  margin: 0;
  font-size: 13px;
  color: color-mix(in srgb, var(--text-color) 58%, transparent);
}

/* ========== 底部快捷入口 ========== */
.about-page .about-quick-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.about-page .about-quick-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.58);
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(14px) saturate(1.3);
  -webkit-backdrop-filter: blur(14px) saturate(1.3);
  box-shadow: 0 10px 26px rgba(88, 111, 214, 0.12);
  color: var(--text-color);
  font: inherit;
  text-align: left;
  cursor: pointer;
  transition: transform 0.24s ease, box-shadow 0.24s ease, border-color 0.24s ease;
}

.about-page .about-quick-card:hover {
  transform: translateY(-3px);
  border-color: rgba(109, 155, 214, 0.55);
  box-shadow: 0 16px 34px rgba(63, 119, 181, 0.22);
}

.about-page .about-quick-card:hover .about-quick-arrow {
  transform: translateX(4px);
  opacity: 1;
}

.about-page .about-quick-icon {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 14px;
  color: #3f77b5;
  background: rgba(109, 155, 214, 0.16);
  border: 1px solid rgba(109, 155, 214, 0.3);
}

.about-page .about-quick-copy {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.about-page .about-quick-label {
  font-size: 15px;
  font-weight: 600;
}

.about-page .about-quick-desc {
  font-size: 12px;
  color: color-mix(in srgb, var(--text-color) 55%, transparent);
}

.about-page .about-quick-arrow {
  margin-left: auto;
  display: inline-flex;
  color: #3f77b5;
  opacity: 0.55;
  transition: transform 0.24s ease, opacity 0.24s ease;
}

/* ========== 暗色模式 ========== */
html.dark .about-page .about-card {
  border-color: rgba(255, 255, 255, 0.1);
  background: rgba(24, 30, 52, 0.66);
  box-shadow: 0 18px 44px rgba(0, 0, 0, 0.42), 0 4px 12px rgba(0, 0, 0, 0.28);
}

html.dark .about-page .about-avatar {
  border-color: rgba(24, 30, 52, 0.85);
  background: rgba(24, 30, 52, 0.85);
}

html.dark .about-page .about-placeholder {
  border-color: rgba(127, 176, 221, 0.32);
  background: rgba(18, 22, 40, 0.4);
}

html.dark .about-page .about-quick-card {
  border-color: rgba(255, 255, 255, 0.1);
  background: rgba(24, 30, 52, 0.62);
  box-shadow: 0 10px 26px rgba(0, 0, 0, 0.36);
}

html.dark .about-page .about-quick-card:hover {
  border-color: rgba(127, 176, 221, 0.5);
  box-shadow: 0 16px 34px rgba(0, 0, 0, 0.46);
}

html.dark .about-page .about-quick-icon,
html.dark .about-page .about-quick-arrow {
  color: #a8cdf0;
}

html.dark .about-page .about-quick-icon {
  background: rgba(127, 176, 221, 0.14);
  border-color: rgba(127, 176, 221, 0.3);
}

/* ========== 小屏适配 ========== */
@media (max-width: 860px) {
  .about-page .about-card {
    grid-template-columns: 1fr;
    gap: 22px;
    padding: 28px 22px;
  }

  .about-page .about-quick-row {
    grid-template-columns: 1fr;
  }
}
</style>
