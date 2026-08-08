<template>
  <main ref="pageRef" class="home-page" data-home-scope>
    <div class="home-dashboard">
      <!-- 首屏：HERO + 侧栏 -->
      <!-- 第一屏：落地页 -->
      <section class="home-landing">
        <div class="home-landing__copy">
          <!-- 主标题=后台「主标题」字段，副标题=后台「副标题」字段：前台位置与后台标签一一对应 -->
          <h1 class="home-landing__title">{{ landingCopy.title }}</h1>
          <p class="home-landing__tagline">{{ landingCopy.subtitle }}</p>
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
        <button type="button" class="home-landing__scroll-hint" aria-label="向下滚动查看内容" @click="scrollToSection('.dashboard-highlight')">↓</button>
      </section>

      <!-- 第二屏：HERO 轮播（全宽） -->
      <section class="dashboard-highlight">
        <div class="dashboard-highlight__hero">
          <div class="dashboard-section-content dashboard-highlight__hero-content">
            <CxSection eyebrow="Hero - 首屏焦点">
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
                    <div class="hero-carousel">
                      <div
                        class="hero-carousel__track"
                        :style="{ transform: `translateY(-${heroIndex * 100}%)`, transition: 'transform 0.5s ease-in-out' }"
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
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </CxSection>
          </div>
        </div>
      </section>

      <!-- 折叠卡片 -->
      <div class="dashboard-featured">
        <div class="dashboard-section-content dashboard-featured__content">
          <CxSection eyebrow="Feature Cards - 折叠卡片">
            <div class="collapse-bento-section">
              <section v-reveal="120" class="collapse-main">
                <div class="fold-shell is-mobile-list">
                  <article
                    v-for="(card, i) in collapseCards"
                    :key="card.id"
                    :ref="el => setFoldBoxRef(el, i)"
                    :data-fold-index="i"
                    class="fold-box"
                    :class="{ 'is-active': foldActive === i, 'is-hover': foldHover === i }"
                    tabindex="0"
                    role="button"
                    :aria-expanded="foldActive === i"
                    :aria-label="card.title"
                    @mouseenter="foldHover = i; foldActive = i"
                    @mouseleave="foldHover = -1"
                    @click="foldActive = i"
                    @focus="foldActive = i"
                    @keydown.enter.prevent="foldActive = i"
                    @keydown.space.prevent="foldActive = i"
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
          </CxSection>
        </div>
      </div>

      <!-- 文章列表 -->
      <div class="dashboard-feed-anchor">
        <div class="dashboard-section-content dashboard-feed dashboard-feed__content">
          <CxSection eyebrow="Articles - 文章列表">
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
                            class="cx-tag cx-tag--primary cx-tag--small is-round article-gallery-card-media-category"
                            :style="tagPaletteStyle(a.categoryName)"
                          >
                            <span class="cx-tag__content"><span class="cx-tag__label">分类 {{ a.categoryName }}</span></span>
                          </span>
                          <div class="article-gallery-card-media-badge-list">
                            <span
                              v-for="t in a.tags"
                              :key="t"
                              class="cx-tag cx-tag--primary cx-tag--small is-round article-gallery-card-media-badge"
                              :style="tagPaletteStyle(t)"
                            >
                              <span class="cx-tag__content"><span class="cx-tag__prefix">#</span><span class="cx-tag__label">{{ t }}</span></span>
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
                    class="cx-button cx-button--primary is-round is-plain"
                    @click="loadMore"
                  >
                    <span class="cx-button__content">加载更多</span>
                  </button>
                </div>
              </section>
            </div>
          </CxSection>
        </div>
      </div>
    </div>
  </main>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import CxSection from '../components/CxSection.vue'
import SvgIcon from '../components/SvgIcon.vue'
import { api } from '../api'
import { HERO_GRADIENTS, FOLD_TEXT_COLORS, FALLBACK_COVERS, tagPaletteStyle, coverOf, mmdd } from '../utils/display'
import '../assets/css/home.css'

const router = useRouter()

/* 落地页文案：站点内容可配置，缺失时用默认文案兜底 */
/* title 渲染为 h1 主标题、subtitle 渲染为其下副标题，与后台「首页内容」字段标签保持一致 */
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
  } catch (e) { console.warn('[首页] 落地页配置解析失败:', e) }
}

const carousels = ref([])
const collapseCards = ref([])
const articles = ref([])
const members = ref([])
const stats = ref(null)

/* HERO */
const heroIndex = ref(0)
let heroAutoplayTimer = null

function goHero(i) {
  heroIndex.value = i
  resetHeroAutoplay()
}

function startHeroAutoplay() {
  stopHeroAutoplay()
  heroAutoplayTimer = setInterval(() => {
    if (carousels.value.length > 0) {
      heroIndex.value = (heroIndex.value + 1) % carousels.value.length
    }
  }, 6000)
}

function stopHeroAutoplay() {
  if (heroAutoplayTimer) {
    clearInterval(heroAutoplayTimer)
    heroAutoplayTimer = null
  }
}

function resetHeroAutoplay() {
  stopHeroAutoplay()
  startHeroAutoplay()
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
  } catch (e) { console.warn('[访问] 记录失败:', e); visits.value = 1 }
}

/* bump 由 LayoutView 负责，这里只读；服务端可用时不再动 localStorage */
async function loadVisits() {
  try {
    const data = await api.views()
    visits.value = Number(data && data.views) || 0
  } catch (e) { console.warn('[访问] 加载失败:', e); bumpVisits() }
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

/* 移动端：滚动到视口的折叠卡片自动展开（桌面 hover/click 仍生效，桌面下不 observe） */
const foldBoxes = []
let foldObserver = null
function setFoldBoxRef(el, i) {
  foldBoxes[i] = el || null
}
function teardownFoldObserver() {
  if (foldObserver) { foldObserver.disconnect(); foldObserver = null }
}
function setupFoldObserver() {
  teardownFoldObserver()
  if (typeof IntersectionObserver === 'undefined') return
  const isMobile = typeof window !== 'undefined'
    && (window.matchMedia('(hover:none)').matches || window.matchMedia('(max-width:860px)').matches)
  if (!isMobile) return
  const scroller = pageRef.value && pageRef.value.closest('.app-shell-main')
  if (!scroller) return
  // 观测区域收窄为屏幕 60% 高度处的一条水平线（比正中央略靠下）：
  // 卡片任一位置滚到该线即展开（threshold:0 表示有相交即触发）
  foldObserver = new IntersectionObserver(entries => {
    entries.forEach(e => {
      if (!e.isIntersecting) return
      const i = Number(e.target.dataset.foldIndex)
      if (Number.isInteger(i) && foldActive.value !== i) foldActive.value = i
    })
  }, { root: scroller, rootMargin: '-60% 0px -40% 0px', threshold: 0 })
  foldBoxes.forEach(el => el && foldObserver.observe(el))
}
watch(collapseCards, () => { nextTick(setupFoldObserver) }, { flush: 'post' })

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
  } catch (e) { console.warn('[文章] 加载更多失败:', e) } finally {
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
  // 高度必须是上下 padding 之和（只取 paddingTop 会少算底部，全屏时第二屏
  // HERO 被迫溢出 20px，与下方折叠卡片区域重叠，视觉上像两个组件合并）
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
  teardownFoldObserver()
  stopHeroAutoplay()
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
    startHeroAutoplay()
  } catch (e) { console.warn('[首页] 加载失败:', e) }
  nextTick(setupFoldObserver)
})
</script>

<style scoped>
/* ========== 第一屏：落地页 ========== */
[data-home-scope] .home-landing {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(320px, 1fr);
  gap: 32px;
  align-items: center;
  height: var(--home-screen-h, auto);
  min-height: 480px;
  padding: 0 8px;
}

[data-home-scope] .home-landing__copy,
[data-home-scope] .home-landing__aside {
  position: relative;
  z-index: 1;
}
[data-home-scope] .home-landing__copy {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding-left: 8px;
}
[data-home-scope] .home-landing__title {
  margin: 0;
  /* 主标题多为短品牌名，可用大字号；投影保留以压住繁忙背景 */
  font-size: clamp(48px, 6vw, 88px);
  font-weight: 800;
  letter-spacing: 2px;
  line-height: 1.1;
  color: #fff;
  text-shadow:
    0 2px 10px rgba(6, 20, 44, 0.55),
    0 4px 28px rgba(6, 20, 44, 0.32);
}
[data-home-scope] .home-landing__tagline {
  margin: 6px 0 0;
  /* 副标题承载较长定位语，20px 保证可读又不与主标题抢焦点 */
  font-size: 20px;
  font-weight: 600;
  line-height: 1.45;
  color: rgba(255, 255, 255, 0.9);
  text-shadow: 0 1px 8px rgba(6, 20, 44, 0.4);
}
[data-home-scope] .home-landing__welcome {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
[data-home-scope] .home-landing__welcome p {
  margin: 0;
  font-size: 15.5px;
  color: rgba(255, 255, 255, 0.85);
  text-shadow: 0 1px 6px rgba(6, 20, 44, 0.34);
}
[data-home-scope] .home-landing__actions {
  display: flex;
  gap: 12px;
  margin-top: 16px;
  flex-wrap: wrap;
}
[data-home-scope] .home-landing__btn {
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
[data-home-scope] .home-landing__btn .svg-icon {
  opacity: 0.9;
  transition: transform 0.24s ease, opacity 0.24s ease;
}
[data-home-scope] .home-landing__btn:hover {
  transform: translateY(-1px);
  background-color: rgba(255, 255, 255, 0.32);
  border-color: color-mix(in srgb, var(--text-color) 22%, transparent);
  box-shadow:
    0 8px 22px color-mix(in srgb, var(--text-color) 10%, transparent),
    inset 0 1px 0 rgba(255, 255, 255, 0.7);
}
[data-home-scope] .home-landing__btn:hover .svg-icon {
  opacity: 1;
}
[data-home-scope] .home-landing__btn:active {
  transform: translateY(0) scale(0.98);
}
[data-home-scope] .home-landing__btn--primary {
  /* P1-3 主 CTA 实色渐变填充，与次按钮（玻璃拟态）拉开视觉权重 */
  color: #ffffff;
  border-color: transparent;
  background: linear-gradient(135deg, var(--accent-solid) 0%, var(--accent-strong) 100%);
  box-shadow:
    0 10px 26px color-mix(in srgb, var(--accent-solid) 42%, transparent),
    0 4px 12px color-mix(in srgb, var(--accent-strong) 30%, transparent),
    inset 0 1px 0 rgba(255, 255, 255, 0.32);
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.16);
}
[data-home-scope] .home-landing__btn--primary:hover {
  color: #ffffff;
  border-color: transparent;
  background: linear-gradient(135deg, color-mix(in srgb, var(--accent-solid) 92%, #ffffff) 0%, color-mix(in srgb, var(--accent-strong) 92%, #ffffff) 100%);
  box-shadow:
    0 14px 32px color-mix(in srgb, var(--accent-solid) 52%, transparent),
    0 6px 16px color-mix(in srgb, var(--accent-strong) 38%, transparent),
    inset 0 1px 0 rgba(255, 255, 255, 0.4);
}
[data-home-scope] .home-landing__btn--primary:hover .svg-icon {
  transform: translateX(2px);
  opacity: 1;
}
html.dark [data-home-scope] .home-landing__btn {
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(255, 255, 255, 0.14);
  color: color-mix(in srgb, var(--text-color) 96%, transparent);
  box-shadow:
    0 4px 14px rgba(0, 0, 0, 0.32),
    inset 0 1px 0 rgba(255, 255, 255, 0.06);
}
html.dark [data-home-scope] .home-landing__btn:hover {
  background-color: rgba(255, 255, 255, 0.12);
  border-color: rgba(255, 255, 255, 0.24);
  box-shadow:
    0 8px 22px rgba(0, 0, 0, 0.42),
    inset 0 1px 0 rgba(255, 255, 255, 0.1);
}
html.dark [data-home-scope] .home-landing__btn--primary {
  color: #ffffff;
  border-color: transparent;
  background: linear-gradient(135deg, color-mix(in srgb, var(--accent-solid) 82%, #ffffff) 0%, color-mix(in srgb, var(--accent-strong) 82%, #ffffff) 100%);
  box-shadow:
    0 10px 26px rgba(0, 0, 0, 0.45),
    0 4px 12px rgba(0, 0, 0, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
}
html.dark [data-home-scope] .home-landing__btn--primary:hover {
  color: #ffffff;
  border-color: transparent;
  background: linear-gradient(135deg, var(--accent-solid) 0%, var(--accent-strong) 100%);
  box-shadow:
    0 14px 32px rgba(0, 0, 0, 0.5),
    0 6px 16px rgba(0, 0, 0, 0.34),
    inset 0 1px 0 rgba(255, 255, 255, 0.26);
}
[data-home-scope] .home-landing__aside {
  display: flex;
  flex-direction: column;
  gap: 22px;
  min-width: 0;
}
[data-home-scope] .home-landing__views {
  margin-left: auto;
  font-size: 13px;
  color: color-mix(in srgb, var(--text-color) 55%, transparent);
}
[data-home-scope] .home-landing__scroll-hint {
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
[data-home-scope] .home-landing__scroll-hint:hover {
  border-color: color-mix(in srgb, var(--accent-solid) 55%, transparent);
  box-shadow: 0 12px 26px rgba(63, 119, 181, 0.24), inset 0 1px 0 rgba(255, 255, 255, 0.7);
}
html.dark [data-home-scope] .home-landing__scroll-hint {
  box-shadow: 0 8px 20px rgba(3, 8, 17, 0.3), inset 0 1px 0 rgba(255, 255, 255, 0.06);
}
@keyframes home-landing-bounce {
  0%, 100% { transform: translate(-50%, 0); }
  50% { transform: translate(-50%, 8px); }
}
@media (max-width: 960px) {
  [data-home-scope] .home-landing {
    grid-template-columns: 1fr;
    height: auto;
    padding-top: 24px;
  }
  [data-home-scope] .home-landing__scroll-hint { display: none; }
}

/* ========== 自定义垂直轮播 ========== */
[data-home-scope] .hero-carousel {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
}
[data-home-scope] .hero-carousel__track {
  display: flex;
  flex-direction: column;
  height: 100%;
  will-change: transform;
}
[data-home-scope] .hero-carousel__track > .hero-slide {
  flex: 0 0 100%;
  min-height: 0;
}

/* ========== 第二屏：HERO 轮播全宽 ========== */
/* 滚动定位目标给悬浮顶栏留位 */
[data-home-scope] .dashboard-feed-anchor,
[data-home-scope] .dashboard-highlight {
  scroll-margin-top: 82px;
}
/* 模板里这一屏已没有 aside，单列铺满即可。 */
[data-home-scope] .dashboard-highlight {
  grid-template-columns: 1fr;
  align-items: start;
}
/* HERO 焦点占满整屏：把 JS 在 updateFirstScreen() 里算出的【确定像素值】
   --home-screen-h（= 可视区高度）直接落到 .hero-visual 上，减去上方 CxSection
   标签行（header 30px + gap 12px = 42px），让「标签 + HERO」正好占满一屏。

   关键：确定像素值直接赋给 .hero-visual，不经过 .dashboard-highlight → __hero →
   __hero-content 的 height:100% 百分比继承链。那条链任一环解析不出百分比时，
   .hero-visual 高度会塌成 auto，hero-slide__image 固有高度（数千 px）撑破盒子并压到
   下方折叠卡片。这里 .hero-visual 拿到确定高度后，内部 __frame{flex:1;height:100%}
   与 __nav{flex-direction:column} 会在确定父高内正确均分填充。 */
[data-home-scope] .dashboard-highlight__hero .hero-bento-frame,
[data-home-scope] .dashboard-highlight__hero .hero-visual {
  height: calc(var(--home-screen-h, 580px) - 42px);
  min-height: 0;
}
@media (max-width: 960px) {
  /* 窄屏 --home-screen-h 已被 JS 移除，交回原站 CSS 的 aspect-ratio / 固定高度 */
  [data-home-scope] .dashboard-highlight__hero .hero-bento-frame,
  [data-home-scope] .dashboard-highlight__hero .hero-visual { height: var(--home-hero-height, 480px); }
}

/* ========== 移动端适配（≤768 / ≤480，只追加、不回归桌面） ========== */
@media (max-width: 768px) {
  [data-home-scope] .home-landing {
    padding: 16px 4px 0;
    gap: 24px;
  }
  [data-home-scope] .home-landing__copy,
  [data-home-scope] .home-landing__aside {
    min-width: 0;
    max-width: 100%;
  }
  [data-home-scope] .home-landing__copy { padding-left: 0; }
  [data-home-scope] .home-landing__title {
    font-size: clamp(34px, 8vw, 46px);
    letter-spacing: 1px;
    overflow-wrap: anywhere;
  }
  [data-home-scope] .home-landing__tagline { font-size: 16.5px; }
  [data-home-scope] .home-landing__aside { gap: 16px; }
  [data-home-scope] .home-landing__aside .profile-card,
  [data-home-scope] .home-landing__aside .signal-board-card { padding: 18px 16px; }
  /* 第二屏：窄屏交还原站 CSS 的 aspect-ratio 高度，避免固定 480px 底部留白 */
  [data-home-scope] .dashboard-highlight__hero .hero-bento-frame,
  [data-home-scope] .dashboard-highlight__hero .hero-visual { height: auto; }
}
@media (max-width: 480px) {
  [data-home-scope] .home-landing__title { font-size: clamp(30px, 8.5vw, 38px); }
  [data-home-scope] .home-landing__tagline { font-size: 15.5px; }
  [data-home-scope] .home-landing__actions {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
  }
  [data-home-scope] .home-landing__btn {
    width: 100%;
    justify-content: center;
    min-height: 44px;
  }
  [data-home-scope] .home-landing__aside .profile-card,
  [data-home-scope] .home-landing__aside .signal-board-card { padding: 16px 14px; }
}
</style>
