<template>
  <main ref="pageRef" class="home-page">
    <div class="home-dashboard">
      <!-- 首屏：HERO + 侧栏 -->
      <!-- 第一屏：落地页 -->
      <section class="home-landing">
        <div class="home-landing__copy">
          <h1 class="home-landing__title">{{ landingCopy.title }}</h1>
          <p class="home-landing__subtitle">{{ landingCopy.subtitle }}</p>
          <div class="home-landing__welcome">
            <p v-for="(w, wi) in landingCopy.welcome" :key="wi">{{ w }}</p>
          </div>
          <div class="home-landing__actions">
            <button type="button" class="home-landing__btn home-landing__btn--primary" @click="goTarget(landingCopy.primaryTarget)">
              {{ landingCopy.primaryBtn }}
              <SvgIcon name="common-arrow" size="14px" />
            </button>
            <button type="button" class="home-landing__btn" @click="goTarget(landingCopy.secondaryTarget)">
              {{ landingCopy.secondaryBtn }}
              <SvgIcon name="common-person" size="14px" />
            </button>
          </div>
        </div>
        <aside class="home-landing__aside">
          <section v-reveal="70" class="profile-card">
            <transition name="profile-spotlight" mode="out-in">
              <article :key="activeMemberIndex" class="profile-card__spotlight">
                <div class="profile-card__content">
                  <h3>{{ activeMember ? activeMember.displayName : '' }}</h3>
                  <p>{{ activeMember ? activeMember.description : '' }}</p>
                </div>
              </article>
            </transition>
            <div class="profile-card__divider"></div>
            <div class="profile-card__roster">
              <button
                v-for="(m, i) in members"
                :key="m.id"
                type="button"
                class="profile-card__member"
                :class="{ 'is-active': activeMemberIndex === i }"
                @click="activeMemberIndex = i"
              >
                <span class="profile-card__member-avatar">
                  <span class="profile-card__member-tag">{{ roleTag(m) }}</span>
                  <img class="profile-card__member-image" :src="m.avatarUrl" :alt="m.displayName" />
                </span>
              </button>
            </div>
          </section>
          <section v-reveal="120" class="signal-board-card">
            <div class="signal-board-card__header">
              <SvgIcon name="common-component" size="20px" />
              <h3>站内概览</h3>
              <span class="home-landing__views">{{ visits }} 浏览</span>
            </div>
            <div class="signal-board-card__stats">
              <article
                v-for="(s, i) in statItems"
                :key="s.label"
                class="stat-item"
                :style="{ '--stat-delay': `${i * 0.08}s` }"
              >
                <div class="stat-item__icon"><SvgIcon :name="s.icon" size="14px" /></div>
                <div class="stat-item__info">
                  <span class="stat-item__value">{{ s.display }}</span>
                  <span class="stat-item__label">{{ s.label }}</span>
                </div>
              </article>
            </div>
          </section>
        </aside>
        <button type="button" class="home-landing__scroll-hint" @click="scrollToSection('.dashboard-highlight')">↓</button>
      </section>

      <!-- 第二屏：HERO 轮播（全宽） -->
      <section class="dashboard-highlight">
        <div class="dashboard-highlight__hero">
          <div class="dashboard-section-content dashboard-highlight__hero-content">
            <LxSection eyebrow="Hero - 首屏焦点">
              <div v-reveal="0" class="hero-bento-frame">
                <div class="hero-visual">
                  <div class="hero-visual__nav">
                    <button
                      v-for="(c, i) in carousels"
                      :key="c.id"
                      type="button"
                      class="hero-nav-item"
                      :class="{ 'is-active': heroIndex === i }"
                      @mouseenter="goHero(i)"
                      @click="goHero(i)"
                    >
                      <span class="hero-nav-item__bg-number">{{ i + 1 }}</span>
                      <div class="hero-nav-item__content">
                        <span class="hero-nav-item__index">{{ c.sceneLabel || `SCENE ${String(i + 1).padStart(2, '0')}` }}</span>
                        <span class="hero-nav-item__title">{{ c.title }}</span>
                      </div>
                    </button>
                  </div>
                  <div class="hero-visual__frame">
                    <n-carousel
                      ref="heroCarousel"
                      direction="vertical"
                      :autoplay="true"
                      :interval="6000"
                      :show-dots="false"
                      :loop="true"
                      style="height: 100%"
                      @update:current-index="onHeroChange"
                    >
                      <div v-for="(c, i) in carousels" :key="c.id" class="hero-slide" :style="{ backgroundImage: HERO_GRADIENTS[i % HERO_GRADIENTS.length] }">
                        <img v-if="c.imageUrl" class="hero-slide__image" :src="c.imageUrl" :alt="c.title" />
                        <div class="hero-slide__overlay">
                          <div class="hero-slide__top">
                            <div class="hero-slide__headline"><span>{{ c.kicker || 'PERSPECTIVE' }}</span></div>
                            <strong>{{ c.title }}</strong>
                            <div class="hero-slide__text-group">
                              <p class="hero-slide__description">{{ c.description }}</p>
                              <p class="hero-slide__content">{{ c.content }}</p>
                            </div>
                          </div>
                          <div class="hero-slide__bottom">
                            <span class="hero-slide__date">{{ c.badge || mmdd(c.updatedAt) }}</span>
                          </div>
                        </div>
                      </div>
                    </n-carousel>
                  </div>
                </div>
              </div>
            </LxSection>
          </div>
        </div>
      </section>

      <!-- 折叠卡片 -->
      <div class="dashboard-featured">
        <div class="dashboard-section-content dashboard-featured__content">
          <LxSection eyebrow="Feature Cards - 折叠卡片">
            <div class="collapse-bento-section">
              <section v-reveal="120" class="collapse-main">
                <div class="fold-shell is-mobile-list">
                  <article
                    v-for="(card, i) in collapseCards"
                    :key="card.id"
                    class="fold-box"
                    :class="{ 'is-active': foldActive === i, 'is-hover': foldHover === i }"
                    @mouseenter="foldHover = i; foldActive = i"
                    @mouseleave="foldHover = -1"
                    @click="foldActive = i"
                  >
                    <div class="fold-graphic-card" :style="{ '--fold-text-color': FOLD_TEXT_COLORS[i % FOLD_TEXT_COLORS.length] }">
                      <div
                        class="fold-graphic-card__media"
                        :style="{ backgroundImage: card.imageUrl ? `var(--fold-media-overlay), url(${JSON.stringify(card.imageUrl)})` : `var(--fold-media-overlay), url(${JSON.stringify(FALLBACK_COVERS[i % FALLBACK_COVERS.length])})` }"
                      ></div>
                      <div class="fold-graphic-card__veil"></div>
                      <div class="fold-graphic-card__text-box">
                        <h3>{{ card.title }}</h3>
                        <div class="fold-graphic-card__text-group">
                          <p class="fold-graphic-card__description">{{ card.description }}</p>
                          <p class="fold-graphic-card__content">{{ card.content }}</p>
                        </div>
                      </div>
                    </div>
                  </article>
                </div>
              </section>
            </div>
          </LxSection>
        </div>
      </div>

      <!-- 文章列表 -->
      <div class="dashboard-feed-anchor">
        <div class="dashboard-section-content dashboard-feed dashboard-feed__content">
          <LxSection eyebrow="Articles - 文章列表">
            <div class="article-gallery-shell">
              <section class="article-gallery-main">
                <div class="article-gallery-rows">
                  <section
                    v-for="(row, ri) in articleRows"
                    :key="ri"
                    class="article-gallery-row"
                    :class="`article-gallery-row-count-${row.length}`"
                  >
                    <article
                      v-for="(a, ci) in row"
                      :key="a.id"
                      v-reveal="80 + ci * 70"
                      class="article-gallery-card"
                      :class="layoutClass(row.length, ci)"
                      @click="openArticle(a.id)"
                    >
                      <div class="article-gallery-card-frame">
                        <div class="article-gallery-card-media">
                          <div class="article-gallery-card-media-image-wrap">
                            <img class="article-gallery-card-media-image" :src="coverOf(a, a.__index)" :alt="a.title" loading="lazy" />
                          </div>
                          <div class="article-gallery-card-media-overlay"></div>
                          <span class="article-gallery-card-media-index">{{ String(a.__index + 1).padStart(2, '0') }}</span>
                          <span class="article-gallery-card-media-date">{{ mmdd(a.updatedAt) }}</span>
                          <span
                            class="lx-tag lx-tag--primary lx-tag--small is-round article-gallery-card-media-category"
                            :style="tagPaletteStyle(a.categoryName)"
                          >
                            <span class="lx-tag__content"><span class="lx-tag__label">分类 {{ a.categoryName }}</span></span>
                          </span>
                          <div class="article-gallery-card-media-badge-list">
                            <span
                              v-for="t in a.tags"
                              :key="t"
                              class="lx-tag lx-tag--primary lx-tag--small is-round article-gallery-card-media-badge"
                              :style="tagPaletteStyle(t)"
                            >
                              <span class="lx-tag__content"><span class="lx-tag__prefix">#</span><span class="lx-tag__label">{{ t }}</span></span>
                            </span>
                          </div>
                        </div>
                        <div class="article-gallery-card-content">
                          <div class="article-gallery-card-copy">
                            <h3 class="article-gallery-card-title">{{ a.title }}</h3>
                            <p class="article-gallery-card-summary">{{ a.summary }}</p>
                          </div>
                          <footer class="article-gallery-card-footer">
                            <div class="article-gallery-card-footer-line"></div>
                            <button type="button" class="article-gallery-card-action" @click.stop="openArticle(a.id)">打开全文</button>
                            <button type="button" class="article-gallery-card-action-icon" @click.stop="openArticle(a.id)">
                              <SvgIcon name="common-open" size="14px" />
                            </button>
                          </footer>
                        </div>
                      </div>
                    </article>
                  </section>
                </div>
                <div class="article-footer-actions">
                  <p v-if="loadingMore" class="article-loading-hint">正在加载更多…</p>
                  <p v-else-if="noMore" class="no-more-hint">已经到底啦，谢谢你看到这里。</p>
                  <button
                    v-else
                    type="button"
                    class="lx-button lx-button--primary is-round is-plain"
                    @click="loadMore"
                  >
                    <span class="lx-button__content">加载更多</span>
                  </button>
                </div>
              </section>
            </div>
          </LxSection>
        </div>
      </div>
    </div>
  </main>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { NCarousel } from 'naive-ui'
import LxSection from '../components/LxSection.vue'
import SvgIcon from '../components/SvgIcon.vue'
import { api } from '../api'
import { HERO_GRADIENTS, FOLD_TEXT_COLORS, FALLBACK_COVERS, tagPaletteStyle, coverOf, mmdd } from '../utils/display'

const router = useRouter()

/* 落地页文案：站点内容可配置，缺失时用默认文案兜底 */
const DEFAULT_LANDING = {
  title: '初曦的窝',
  subtitle: '记录一些好用的工具以及番剧内容',
  welcome: ['欢迎来到我的小站！', '希望这些分享，能给你带来一点启发与温暖。'],
  primaryBtn: '查看最新文章',
  secondaryBtn: '关于我',
  primaryTarget: '#articles',
  secondaryTarget: '/about'
}
const landingCopy = ref({ ...DEFAULT_LANDING })

function applyLandingCopy(record) {
  try {
    const parsed = JSON.parse((record && record.contentJson) || '')
    if (!parsed || typeof parsed !== 'object') return
    landingCopy.value = {
      title: parsed.title || DEFAULT_LANDING.title,
      subtitle: parsed.subtitle || DEFAULT_LANDING.subtitle,
      welcome: Array.isArray(parsed.welcome) && parsed.welcome.length ? parsed.welcome : DEFAULT_LANDING.welcome,
      primaryBtn: parsed.primaryBtn || DEFAULT_LANDING.primaryBtn,
      secondaryBtn: parsed.secondaryBtn || DEFAULT_LANDING.secondaryBtn,
      primaryTarget: parsed.primaryTarget || DEFAULT_LANDING.primaryTarget,
      secondaryTarget: parsed.secondaryTarget || DEFAULT_LANDING.secondaryTarget
    }
  } catch { /* JSON 异常时保持默认文案 */ }
}

const carousels = ref([])
const collapseCards = ref([])
const articles = ref([])
const members = ref([])
const stats = ref(null)

/* HERO */
const heroIndex = ref(0)
const heroCarousel = ref(null)

function onHeroChange(i) {
  heroIndex.value = i
}

function goHero(i) {
  heroIndex.value = i
  if (heroCarousel.value) heroCarousel.value.to(i)
}

/* 人员卡片 */
const activeMemberIndex = ref(0)
const activeMember = computed(() => members.value[activeMemberIndex.value] || null)

function roleTag(m) {
  return m.roleCode === 'SUPER_ADMIN' ? '博主' : '助手'
}

/* 信号板 count-up：文章 / 番剧 / 轮播 / 工具 */
const bangumiCount = ref(0)
const toolCount = ref(0)
const statAnimated = ref({ articleCount: 0, bangumiCount: 0, carouselCount: 0, toolCount: 0 })
const statItems = computed(() => [
  { key: 'articleCount', label: '已发布文章', icon: 'common-archive', display: statAnimated.value.articleCount },
  { key: 'bangumiCount', label: '番剧收录', icon: 'common-articlePages', display: statAnimated.value.bangumiCount },
  { key: 'carouselCount', label: '首屏轮播', icon: 'common-home', display: statAnimated.value.carouselCount },
  { key: 'toolCount', label: '工具站点', icon: 'common-tool', display: statAnimated.value.toolCount }
])

function statTargets() {
  const s = stats.value || {}
  return {
    articleCount: s.articleCount || 0,
    bangumiCount: bangumiCount.value,
    carouselCount: s.carouselCount || 0,
    toolCount: toolCount.value
  }
}

function runCountUp() {
  const target = statTargets()
  const start = performance.now()
  const durationMs = 900
  const tick = now => {
    const t = Math.min(1, (now - start) / durationMs)
    const ease = 1 - Math.pow(1 - t, 3)
    statAnimated.value = Object.fromEntries(Object.entries(target).map(([k, v]) => [k, Math.round(v * ease)]))
    if (t < 1) requestAnimationFrame(tick)
  }
  requestAnimationFrame(tick)
  // RAF 不可用/页面不可见时兜底直接落到最终值
  setTimeout(() => { statAnimated.value = statTargets() }, durationMs + 200)
}

/* 浏览计数（服务端优先，localStorage 兜底） + 平滑滚动 */
const visits = ref(0)

function bumpVisits() {
  try {
    const n = Number(localStorage.getItem('chuxi-visits') || 0) + 1
    localStorage.setItem('chuxi-visits', String(n))
    visits.value = n
  } catch { visits.value = 1 }
}

/* bump 由 LayoutView 负责，这里只读；服务端可用时不再动 localStorage */
async function loadVisits() {
  try {
    const data = await api.views()
    visits.value = Number(data && data.views) || 0
  } catch {
    bumpVisits()
  }
}

function scrollToSection(selector) {
  const el = document.querySelector(selector)
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

// 首页内锚点标识 -> 实际滚动目标
const ANCHOR_TARGETS = {
  '#articles': '.dashboard-feed-anchor',
  '#hero': '.dashboard-highlight'
}

// 按钮跳转：# 开头按锚点滚动，其余走站内路由（后台可配）
function goTarget(target) {
  const t = String(target || '')
  if (t.startsWith('#')) {
    scrollToSection(ANCHOR_TARGETS[t] || ANCHOR_TARGETS['#articles'])
    return
  }
  if (t.startsWith('/')) router.push(t)
}

/* 折叠卡片 */
const foldActive = ref(0)
const foldHover = ref(-1)

/* 文章画廊：行模式 [2,3,4,3] 循环 */
const ROW_PATTERN = [2, 3, 4, 3]
const articleRows = computed(() => {
  const rows = []
  let idx = 0
  let p = 0
  const list = articles.value.map((a, i) => ({ ...a, __index: i }))
  while (idx < list.length) {
    const size = ROW_PATTERN[p % ROW_PATTERN.length]
    rows.push(list.slice(idx, idx + size))
    idx += size
    p += 1
  }
  return rows
})

function layoutClass(rowCount, i) {
  if (rowCount === 2) return i === 0 ? 'article-gallery-card-layout-poster' : 'article-gallery-card-layout-sidebar'
  if (rowCount === 3) return i === 2 ? 'article-gallery-card-layout-poster' : 'article-gallery-card-layout-sidebar'
  if (rowCount === 4) return i === 1 ? 'article-gallery-card-layout-poster' : 'article-gallery-card-layout-sidebar'
  return 'article-gallery-card-layout-sidebar'
}

/* 加载更多 */
const pageNo = ref(1)
const pageSize = 6
const total = ref(0)
const loadingMore = ref(false)
const noMore = computed(() => total.value > 0 && articles.value.length >= total.value)

async function loadMore() {
  loadingMore.value = true
  try {
    const data = await api.homeArticles(pageNo.value + 1, pageSize)
    pageNo.value += 1
    total.value = data.total
    const known = new Set(articles.value.map(a => a.id))
    articles.value = [...articles.value, ...data.records.filter(a => !known.has(a.id))]
  } catch { /* 忽略 */ } finally {
    loadingMore.value = false
  }
}

function openArticle(id) {
  router.push(`/article/${id}`)
}

/* 首屏高度：让 HERO + 人员卡片 + 信号板 刚好占满一屏 */
const pageRef = ref(null)
let screenResizeObserver = null

function updateFirstScreen() {
  const page = pageRef.value
  if (!page) return
  const scroller = page.closest('.app-shell-main')
  if (!scroller) return
  const cs = getComputedStyle(scroller)
  const pad = parseFloat(cs.paddingTop) + parseFloat(cs.paddingBottom)
  // 窄屏（单列布局）不强制一屏
  if (window.innerWidth < 960) {
    page.style.removeProperty('--home-screen-h')
    return
  }
  const screenH = Math.max(420, scroller.clientHeight - pad)
  page.style.setProperty('--home-screen-h', `${screenH}px`)
}

onMounted(async () => {
  requestAnimationFrame(updateFirstScreen)
  setTimeout(updateFirstScreen, 400)
  window.addEventListener('resize', updateFirstScreen)
  const scroller = pageRef.value && pageRef.value.closest('.app-shell-main')
  if (scroller && typeof ResizeObserver !== 'undefined') {
    screenResizeObserver = new ResizeObserver(updateFirstScreen)
    screenResizeObserver.observe(scroller)
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateFirstScreen)
  if (screenResizeObserver) screenResizeObserver.disconnect()
})

onMounted(async () => {
  loadVisits()
  try {
    const [landing, team, bangumi, tools, landingContent] = await Promise.allSettled([
      api.homeLanding(), api.teamMembers(), api.bangumiRecords(), api.toolsLanding(), api.siteContent('home-landing')
    ])
    if (landingContent.status === 'fulfilled') applyLandingCopy(landingContent.value)
    if (landing.status === 'fulfilled') {
      carousels.value = landing.value.carousels || []
      collapseCards.value = landing.value.collapseCards || []
      articles.value = landing.value.articles || []
      stats.value = landing.value.stats || {}
      total.value = (landing.value.stats && landing.value.stats.articleCount) || 0
    }
    if (team.status === 'fulfilled') members.value = team.value || []
    if (bangumi.status === 'fulfilled') bangumiCount.value = (bangumi.value || []).length
    if (tools.status === 'fulfilled') toolCount.value = (tools.value || []).length
    runCountUp()
  } catch { /* 后端未启动时页面保持空态 */ }
})
</script>

<style>
/* ========== 第一屏：落地页 ========== */
.home-page .home-landing {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(320px, 1fr);
  gap: 32px;
  align-items: center;
  height: var(--home-screen-h, auto);
  min-height: 480px;
  padding: 0 8px;
}
.home-page .home-landing__copy {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding-left: 8px;
}
.home-page .home-landing__title {
  margin: 0;
  font-size: clamp(51px, 6.2vw, 95px);
  font-weight: 800;
  letter-spacing: 2px;
  line-height: 1.1;
  color: var(--text-color);
  text-shadow: 0 4px 24px rgba(63, 119, 181, 0.18);
}
.home-page .home-landing__subtitle {
  margin: 2px 0 0;
  font-size: 21px;
  font-weight: 600;
  color: color-mix(in srgb, var(--text-color) 82%, transparent);
}
.home-page .home-landing__welcome {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.home-page .home-landing__welcome p {
  margin: 0;
  font-size: 15.5px;
  color: color-mix(in srgb, var(--text-color) 62%, transparent);
}
.home-page .home-landing__actions {
  display: flex;
  gap: 12px;
  margin-top: 16px;
  flex-wrap: wrap;
}
.home-page .home-landing__btn {
  position: relative;
  isolation: isolate;
  overflow: hidden;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 22px;
  border-radius: 999px;
  border: 1px solid color-mix(in srgb, var(--text-color) 12%, transparent);
  background: rgba(255, 255, 255, 0.18);
  color: color-mix(in srgb, var(--text-color) 92%, transparent);
  font: inherit;
  font-size: 15.5px;
  font-weight: 600;
  letter-spacing: 0.2px;
  cursor: pointer;
  -webkit-backdrop-filter: blur(14px) saturate(1.25);
  backdrop-filter: blur(14px) saturate(1.25);
  box-shadow:
    0 4px 14px color-mix(in srgb, var(--text-color) 6%, transparent),
    inset 0 1px 0 rgba(255, 255, 255, 0.55);
  transition:
    transform 0.22s ease,
    box-shadow 0.26s ease,
    background-color 0.24s ease,
    border-color 0.24s ease,
    color 0.24s ease;
}
.home-page .home-landing__btn .svg-icon {
  opacity: 0.9;
  transition: transform 0.24s ease, opacity 0.24s ease;
}
.home-page .home-landing__btn:hover {
  transform: translateY(-1px);
  background-color: rgba(255, 255, 255, 0.32);
  border-color: color-mix(in srgb, var(--text-color) 22%, transparent);
  box-shadow:
    0 8px 22px color-mix(in srgb, var(--text-color) 10%, transparent),
    inset 0 1px 0 rgba(255, 255, 255, 0.7);
}
.home-page .home-landing__btn:hover .svg-icon {
  opacity: 1;
}
.home-page .home-landing__btn:active {
  transform: translateY(0) scale(0.98);
}
.home-page .home-landing__btn--primary {
  /* 玻璃浅底主按钮：主色用 accent 但走"边框+字+内发光"高亮，不铺实色填充 */
  color: var(--accent-strong);
  border-color: color-mix(in srgb, var(--accent-solid) 42%, transparent);
  background: color-mix(in srgb, var(--accent-solid) 14%, rgba(255, 255, 255, 0.18));
  box-shadow:
    0 6px 18px color-mix(in srgb, var(--accent-solid) 22%, transparent),
    inset 0 1px 0 rgba(255, 255, 255, 0.55),
    inset 0 -10px 18px color-mix(in srgb, var(--accent-solid) 8%, transparent);
}
.home-page .home-landing__btn--primary:hover {
  color: color-mix(in srgb, var(--accent-strong) 90%, #ffffff);
  border-color: color-mix(in srgb, var(--accent-solid) 60%, transparent);
  background-color: color-mix(in srgb, var(--accent-solid) 22%, rgba(255, 255, 255, 0.24));
  box-shadow:
    0 10px 26px color-mix(in srgb, var(--accent-solid) 32%, transparent),
    inset 0 1px 0 rgba(255, 255, 255, 0.7);
}
.home-page .home-landing__btn--primary:hover .svg-icon {
  transform: translateX(2px);
  opacity: 1;
}
html.dark .home-page .home-landing__btn {
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(255, 255, 255, 0.14);
  color: color-mix(in srgb, var(--text-color) 96%, transparent);
  box-shadow:
    0 4px 14px rgba(0, 0, 0, 0.32),
    inset 0 1px 0 rgba(255, 255, 255, 0.06);
}
html.dark .home-page .home-landing__btn:hover {
  background-color: rgba(255, 255, 255, 0.12);
  border-color: rgba(255, 255, 255, 0.24);
  box-shadow:
    0 8px 22px rgba(0, 0, 0, 0.42),
    inset 0 1px 0 rgba(255, 255, 255, 0.1);
}
html.dark .home-page .home-landing__btn--primary {
  color: color-mix(in srgb, var(--accent-solid) 88%, #ffffff);
  background: color-mix(in srgb, var(--accent-solid) 18%, rgba(255, 255, 255, 0.08));
  border-color: color-mix(in srgb, var(--accent-solid) 48%, transparent);
  box-shadow:
    0 6px 18px rgba(0, 0, 0, 0.45),
    inset 0 1px 0 rgba(255, 255, 255, 0.14),
    inset 0 -10px 18px color-mix(in srgb, var(--accent-solid) 14%, transparent);
}
html.dark .home-page .home-landing__btn--primary:hover {
  color: #ffffff;
  background-color: color-mix(in srgb, var(--accent-solid) 28%, rgba(255, 255, 255, 0.1));
  border-color: color-mix(in srgb, var(--accent-solid) 70%, transparent);
}
.home-page .home-landing__aside {
  display: flex;
  flex-direction: column;
  gap: 22px;
  min-width: 0;
}
.home-page .home-landing__views {
  margin-left: auto;
  font-size: 13px;
  color: color-mix(in srgb, var(--text-color) 55%, transparent);
}
.home-page .home-landing__scroll-hint {
  position: absolute;
  left: 50%;
  bottom: 10px;
  transform: translateX(-50%);
  width: 40px;
  height: 40px;
  border-radius: 999px;
  border: 1px solid var(--accent-border);
  background: color-mix(in srgb, var(--card-bg) 90%, transparent);
  color: var(--accent-text);
  font-size: 20px;
  cursor: pointer;
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  box-shadow: 0 8px 20px rgba(63, 119, 181, 0.14), inset 0 1px 0 rgba(255, 255, 255, 0.6);
  transition: border-color 0.24s ease, box-shadow 0.24s ease, color 0.24s ease;
  animation: home-landing-bounce 1.8s ease-in-out infinite;
}
.home-page .home-landing__scroll-hint:hover {
  border-color: color-mix(in srgb, var(--accent-solid) 55%, transparent);
  box-shadow: 0 12px 26px rgba(63, 119, 181, 0.24), inset 0 1px 0 rgba(255, 255, 255, 0.7);
}
html.dark .home-page .home-landing__scroll-hint {
  box-shadow: 0 8px 20px rgba(3, 8, 17, 0.3), inset 0 1px 0 rgba(255, 255, 255, 0.06);
}
@keyframes home-landing-bounce {
  0%, 100% { transform: translate(-50%, 0); }
  50% { transform: translate(-50%, 8px); }
}
@media (max-width: 960px) {
  .home-page .home-landing {
    grid-template-columns: 1fr;
    height: auto;
    padding-top: 24px;
  }
  .home-page .home-landing__scroll-hint { display: none; }
}

/* ========== 第二屏：HERO 轮播全宽一屏 ========== */
/* 滚动定位目标给悬浮顶栏留位 */
.home-page .dashboard-feed-anchor,
.home-page .dashboard-highlight {
  scroll-margin-top: 82px;
}
.home-page .dashboard-highlight {
  grid-template-columns: 1fr;
  align-items: stretch;
  height: var(--home-screen-h, auto);
  min-height: 0;
}
.home-page .dashboard-highlight__hero,
.home-page .dashboard-highlight__hero-content,
.home-page .dashboard-highlight__hero-content .timeline-section-container {
  display: flex;
  flex-direction: column;
  min-height: 0;
  height: 100%;
}
.home-page .dashboard-highlight__hero-content .timeline-section-container-content {
  flex: 1;
  min-height: 0;
}
.home-page .dashboard-highlight__hero .hero-bento-frame,
.home-page .dashboard-highlight__hero .hero-visual {
  height: 100%;
}
@media (max-width: 960px) {
  .home-page .dashboard-highlight { height: auto; }
  .home-page .dashboard-highlight__hero .hero-visual { height: var(--home-hero-height, 480px); }
}

/* ========== 移动端适配（≤768 / ≤480，只追加、不回归桌面） ========== */
@media (max-width: 768px) {
  .home-page .home-landing {
    padding: 16px 4px 0;
    gap: 24px;
  }
  .home-page .home-landing__copy,
  .home-page .home-landing__aside {
    min-width: 0;
    max-width: 100%;
  }
  .home-page .home-landing__copy { padding-left: 0; }
  .home-page .home-landing__title {
    font-size: clamp(37px, 9vw, 51px);
    letter-spacing: 1px;
    overflow-wrap: anywhere;
  }
  .home-page .home-landing__subtitle { font-size: 17.5px; }
  .home-page .home-landing__aside { gap: 16px; }
  .home-page .home-landing__aside .profile-card,
  .home-page .home-landing__aside .signal-board-card { padding: 18px 16px; }
  /* 第二屏：窄屏交还原站 CSS 的 aspect-ratio 高度，避免固定 480px 底部留白 */
  .home-page .dashboard-highlight__hero .hero-bento-frame,
  .home-page .dashboard-highlight__hero .hero-visual { height: auto; }
}
@media (max-width: 480px) {
  .home-page .home-landing__title { font-size: clamp(33px, 8.6vw, 42px); }
  .home-page .home-landing__actions {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
  }
  .home-page .home-landing__btn {
    width: 100%;
    justify-content: center;
    min-height: 44px;
  }
  .home-page .home-landing__aside .profile-card,
  .home-page .home-landing__aside .signal-board-card { padding: 16px 14px; }
}
</style>
