<template>
  <main class="about-page">
    <div class="about-shell">
      <LxSection eyebrow="About - 关于本站">
        <section v-reveal="0" class="about-card">
          <span class="about-card__orb about-card__orb--1"></span>
          <span class="about-card__orb about-card__orb--2"></span>
          <aside class="about-profile">
            <div class="about-avatar-ring">
              <img class="about-avatar" :src="siteSettings.avatarUrl || siteSettings.logoUrl || '/favicon.png'" alt="站点头像" />
            </div>
            <h2 class="about-site-name">{{ siteSettings.siteName || '初曦的窝' }}</h2>
            <i class="about-profile-divider"></i>
            <p class="about-site-tagline">{{ siteSettings.subtitle || '收集工具、追番与灵感碎片的小小基地。' }}</p>
            <div class="about-profile-tags">
              <span>前端</span><span>AI</span><span>追番</span><span>碎碎念</span>
            </div>
          </aside>
          <div class="about-body">
            <template v-if="hasContent">
              <div class="md-editor-preview default-theme about-markdown" v-html="renderedHtml"></div>
            </template>
            <div v-else class="about-placeholder">
              <p class="about-placeholder-title">这里还没有写下自我介绍</p>
              <p class="about-placeholder-text">站长还在酝酿一段合适的开场白，先四处逛逛，稍后再来看看吧。</p>
            </div>
          </div>
        </section>
        <!-- 友情链接区块 -->
        <section v-reveal="50" class="friend-links-section">
          <div class="friend-links-header">
            <h3 class="friend-links-title">友情链接</h3>
            <p class="friend-links-desc">志同道合的小伙伴们</p>
          </div>
          <div v-if="friendLinksLoading" class="friend-links-state">加载中…</div>
          <div v-else-if="friendLinks.length === 0" class="friend-links-state friend-links-empty">
            <p>暂无友情链接，稍后再来看看吧 ✨</p>
          </div>
          <div v-else class="friend-links-grid">
            <a
              v-for="link in friendLinks"
              :key="link.id"
              class="friend-link-card"
              :href="link.siteUrl"
              target="_blank"
              rel="noopener noreferrer"
            >
              <div class="friend-link-logo">
                <img v-if="link.logoUrl" :src="link.logoUrl" :alt="link.siteName" />
                <span v-else class="friend-link-logo-fallback">{{ (link.siteName || '?').charAt(0) }}</span>
              </div>
              <div class="friend-link-info">
                <span class="friend-link-name">{{ link.siteName }}</span>
                <span v-if="link.description" class="friend-link-desc">{{ link.description }}</span>
              </div>
            </a>
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
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import LxSection from '../components/LxSection.vue'
import SvgIcon from '../components/SvgIcon.vue'
import { api } from '../api'
import { renderMarkdown } from '../utils/markdown'

const router = useRouter()

const content = ref(null)
const renderedHtml = ref('')
const hasContent = computed(() => !!(content.value && (content.value.title || content.value.markdown)))

const siteSettings = reactive({
  siteName: '初曦的窝',
  logoUrl: '/favicon.png',
  avatarUrl: '',
  subtitle: '收集工具、追番与灵感碎片的小小基地。'
})

const friendLinks = ref([])
const friendLinksLoading = ref(true)

const quickEntries = [
  { path: '/index', label: '去看文章', desc: '最新的记录都在首页', icon: 'common-archive' },
  { path: '/tool', label: '工具集合', desc: '收藏的好用站点与工具', icon: 'common-tool' },
  { path: '/bangumi', label: '追番记录', desc: '一起看看最近在追什么', icon: 'common-articlePages' }
]

onMounted(async () => {
  const [settingsData, record, list] = await Promise.all([
    api.siteContent('site-settings').catch(() => null),
    api.siteContent('about').catch(() => null),
    api.friendLinks().catch(() => [])
  ])

  // 加载站点设置
  try {
    let settingsObj = settingsData
    if (settingsData && typeof settingsData.contentJson === 'string') settingsObj = JSON.parse(settingsData.contentJson)
    if (typeof settingsObj === 'string') settingsObj = JSON.parse(settingsObj)
    if (settingsObj && typeof settingsObj === 'object') {
      siteSettings.siteName = settingsObj.siteName || '初曦的窝'
      siteSettings.logoUrl = settingsObj.logoUrl || '/favicon.png'
      siteSettings.avatarUrl = settingsObj.avatarUrl || ''
      siteSettings.subtitle = settingsObj.subtitle || '收集工具、追番与灵感碎片的小小基地。'
    }
  } catch { /* 解析失败时使用默认值 */ }

  // 加载关于页内容
  try {
    const parsed = JSON.parse((record && record.contentJson) || '')
    if (!parsed || typeof parsed !== 'object') return
    content.value = { title: parsed.title || '', markdown: parsed.markdown || '' }
    renderedHtml.value = renderMarkdown(content.value.markdown).html
  } catch { /* 无数据或解析失败时展示占位文案 */ }

  // 读取友情链接
  friendLinks.value = (list || []).filter(l => l.visible !== false)
  friendLinksLoading.value = false
})
</script>

<style>
.about-page .about-shell {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

/* ========== 主卡：外层岛屿 + 内部左右两张中层卡，对齐站内嵌套卡片语言 ========== */
.about-page .about-card {
  position: relative;
  display: grid;
  grid-template-columns: 272px minmax(0, 1fr);
  gap: 18px;
  padding: 18px;
  border-radius: 28px;
  overflow: hidden;
  /* 亮色轻透：低白色浆 + 极轻模糊，与设置弹窗同语言，背景图案可透出 */
  border: 1px solid rgba(255, 255, 255, 0.5);
  background: rgba(255, 255, 255, 0.32);
  backdrop-filter: blur(8px) saturate(1.15);
  -webkit-backdrop-filter: blur(8px) saturate(1.15);
  box-shadow: var(--nested-outer-card-shadow);
}

.about-page .about-card__orb {
  position: absolute;
  border-radius: 999px;
  pointer-events: none;
  background: radial-gradient(circle, rgba(109, 155, 214, 0.2), transparent 68%);
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
  background: radial-gradient(circle, rgba(103, 183, 207, 0.18), transparent 68%);
}

/* ----- 左侧：头像与站点信息（中层卡） ----- */
.about-page .about-profile {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  text-align: center;
  padding: 30px 22px;
  border-radius: 22px;
  /* 轻透中层卡：叠在外层上有效底约六成白，可读性与透感兼顾 */
  border: 1px solid rgba(255, 255, 255, 0.55);
  background: rgba(255, 255, 255, 0.38);
  box-shadow: 0 8px 22px rgba(88, 111, 214, 0.08);
}

.about-page .about-avatar-ring {
  width: 124px;
  height: 124px;
  padding: 5px;
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(109, 155, 214, 0.65), rgba(103, 183, 207, 0.65));
  box-shadow: 0 14px 30px rgba(63, 119, 181, 0.26);
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
  margin: 6px 0 0;
  font-size: 27px;
  letter-spacing: 2px;
  color: var(--text-color);
}

.about-page .about-profile-divider {
  display: block;
  width: 54px;
  height: 3px;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--accent-solid), transparent);
  opacity: 0.65;
}

.about-page .about-site-tagline {
  margin: 0;
  font-size: 14.5px;
  line-height: 1.8;
  color: color-mix(in srgb, var(--text-color) 62%, transparent);
}

/* 兴趣小标签：内层胶囊 */
.about-page .about-profile-tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
  margin-top: 4px;
}

.about-page .about-profile-tags span {
  padding: 4px 13px;
  border-radius: 999px;
  border: 1px solid var(--nested-inner-card-border);
  background: var(--nested-inner-card-bg);
  box-shadow: var(--nested-inner-card-shadow);
  font-size: 12.5px;
  color: var(--accent-text);
}

/* ----- 右侧：正文（中层卡） ----- */
.about-page .about-body {
  position: relative;
  z-index: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 28px 32px;
  border-radius: 22px;
  border: 1px solid rgba(255, 255, 255, 0.55);
  background: rgba(255, 255, 255, 0.38);
  box-shadow: 0 8px 22px rgba(88, 111, 214, 0.08);
}

/* 正文顶部主题色飾条，强化分区识别度 */
.about-page .about-body::before {
  content: '';
  display: block;
  width: 64px;
  height: 4px;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--accent-solid), rgba(103, 183, 207, 0.6));
  margin-bottom: 4px;
}

/* Markdown 首尾去多余留白，首标题紧贴飾条 */
.about-page .about-markdown > *:first-child {
  margin-top: 0;
}
.about-page .about-markdown > *:last-child {
  margin-bottom: 0;
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
  background: var(--nested-inner-card-bg);
}

.about-page .about-placeholder-title {
  margin: 0;
  font-size: 17.5px;
  font-weight: 600;
  color: var(--text-color);
}

.about-page .about-placeholder-text {
  margin: 0;
  font-size: 14.5px;
  color: color-mix(in srgb, var(--text-color) 58%, transparent);
}

/* ========== 底部快捷入口：与主卡同语言的独立岛屿 ========== */
.about-page .about-quick-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
  /* 与主卡拉开呼吸距，避免两层岛屿粘连 */
  margin-top: 26px;
}

.about-page .about-quick-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
  border-radius: 22px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  background: rgba(255, 255, 255, 0.34);
  backdrop-filter: blur(8px) saturate(1.15);
  -webkit-backdrop-filter: blur(8px) saturate(1.15);
  box-shadow: var(--nested-outer-card-shadow);
  color: var(--text-color);
  font: inherit;
  text-align: left;
  cursor: pointer;
  transition: transform 0.24s ease, box-shadow 0.24s ease, border-color 0.24s ease;
}

.about-page .about-quick-card:hover {
  transform: translateY(-3px);
  border-color: color-mix(in srgb, var(--accent-solid) 55%, transparent);
  box-shadow: 0 16px 34px var(--accent-glow);
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
  color: var(--accent-text);
  border: 1px solid var(--nested-inner-card-border);
  background: var(--nested-inner-card-bg);
  box-shadow: var(--nested-inner-card-shadow);
}

.about-page .about-quick-copy {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.about-page .about-quick-label {
  font-size: 16.5px;
  font-weight: 600;
}

.about-page .about-quick-desc {
  font-size: 13px;
  color: color-mix(in srgb, var(--text-color) 55%, transparent);
}

.about-page .about-quick-arrow {
  margin-left: auto;
  display: inline-flex;
  color: var(--accent-text);
  opacity: 0.55;
  transition: transform 0.24s ease, opacity 0.24s ease;
}

/* ========== 友情链接区块 ========== */
.about-page .friend-links-section {
  margin-top: 26px;
}

.about-page .friend-links-header {
  text-align: center;
  margin-bottom: 18px;
}

.about-page .friend-links-title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: var(--text-color);
  letter-spacing: 1px;
}

.about-page .friend-links-desc {
  margin: 6px 0 0;
  font-size: 14px;
  color: color-mix(in srgb, var(--text-color) 55%, transparent);
}

.about-page .friend-links-state {
  text-align: center;
  padding: 32px 20px;
  border-radius: 22px;
  border: 1.5px dashed rgba(126, 160, 198, 0.4);
  background: var(--nested-inner-card-bg);
  color: color-mix(in srgb, var(--text-color) 58%, transparent);
  font-size: 14.5px;
}

.about-page .friend-links-state p {
  margin: 0;
}

.about-page .friend-links-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.about-page .friend-link-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 18px;
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  background: rgba(255, 255, 255, 0.34);
  backdrop-filter: blur(8px) saturate(1.15);
  -webkit-backdrop-filter: blur(8px) saturate(1.15);
  box-shadow: var(--nested-outer-card-shadow);
  text-decoration: none;
  color: var(--text-color);
  transition: transform 0.24s ease, box-shadow 0.24s ease, border-color 0.24s ease;
}

.about-page .friend-link-card:hover {
  transform: translateY(-3px);
  border-color: color-mix(in srgb, var(--accent-solid) 55%, transparent);
  box-shadow: 0 16px 34px var(--accent-glow);
}

.about-page .friend-link-logo {
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  border-radius: 12px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--nested-inner-card-border);
  background: var(--nested-inner-card-bg);
  box-shadow: var(--nested-inner-card-shadow);
}

.about-page .friend-link-logo img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.about-page .friend-link-logo-fallback {
  font-size: 18px;
  font-weight: 700;
  color: var(--accent-text);
}

.about-page .friend-link-info {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.about-page .friend-link-name {
  font-size: 15.5px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.about-page .friend-link-desc {
  font-size: 12.5px;
  color: color-mix(in srgb, var(--text-color) 55%, transparent);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ========== 暗色模式：低色浆半透 + 细白描边，与设置弹窗等组件的透明语言对齐 ========== */
html.dark .about-page .about-card {
  border-color: rgba(255, 255, 255, 0.09);
  background: rgba(18, 21, 30, 0.42);
}

html.dark .about-page .about-profile,
html.dark .about-page .about-body {
  border-color: rgba(255, 255, 255, 0.09);
  background: rgba(26, 29, 37, 0.38);
  box-shadow: 0 8px 22px rgba(0, 0, 0, 0.2);
}

html.dark .about-page .about-quick-card {
  border-color: rgba(255, 255, 255, 0.09);
  background: rgba(26, 29, 37, 0.38);
}

html.dark .about-page .about-avatar {
  border-color: rgba(24, 30, 52, 0.85);
  background: rgba(24, 30, 52, 0.85);
}

html.dark .about-page .friend-link-card {
  border-color: rgba(255, 255, 255, 0.09);
  background: rgba(26, 29, 37, 0.38);
}

/* ========== 小屏适配 ========== */
@media (max-width: 860px) {
  .about-page .about-card {
    grid-template-columns: 1fr;
    gap: 14px;
    padding: 14px;
  }

  .about-page .about-profile {
    padding: 24px 18px;
  }

  .about-page .about-quick-row {
    grid-template-columns: 1fr;
  }

  .about-page .friend-links-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

/* ========== 移动端适配（≤768 / ≤480，只追加、不回归桌面） ========== */
@media (max-width: 768px) {
  .about-page .about-body {
    padding: 20px 18px;
  }

  .about-page .about-quick-row {
    gap: 14px;
  }

  .about-page .about-avatar-ring {
    width: 108px;
    height: 108px;
  }

  .about-page .about-site-name {
    font-size: 24px;
  }
}

@media (max-width: 480px) {
  .about-page .about-card {
    gap: 12px;
    padding: 12px;
  }

  .about-page .friend-links-grid {
    grid-template-columns: 1fr;
  }

  .about-page .about-avatar-ring {
    width: 96px;
    height: 96px;
  }

  .about-page .about-site-name {
    font-size: 22px;
  }

  .about-page .about-placeholder {
    padding: 24px 16px;
  }

  .about-page .about-quick-card {
    padding: 14px 16px;
  }
}
</style>
