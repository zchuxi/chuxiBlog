<template>
  <main class="archive-page">
    <div class="archive-shell">
      <!-- 归档开场 -->
      <section v-reveal="0" class="timeline-section-container">
        <div class="timeline-section-container-header">
          <div class="timeline-section-container-header-main">
            <div class="timeline-section-container-header-title-row">
              <p class="timeline-section-container-header-eyebrow">Hero - 归档开场</p>
            </div>
          </div>
        </div>
        <div class="timeline-section-container-content">
          <section class="archive-hero-section">
            <div class="archive-hero-copy">
              <span class="archive-hero-eyebrow">{{ heroCopy.eyebrow }}</span>
              <h1 class="archive-hero-title">{{ heroCopy.title }}</h1>
              <p class="archive-hero-description">{{ heroCopy.description }}</p>
              <div class="archive-hero-note-list">
                <p v-for="(note, ni) in heroCopy.notes" :key="ni" class="archive-hero-note">{{ note }}</p>
              </div>
            </div>
            <article v-if="featured" class="archive-hero-feature-card" @click="openArticle(featured.id)">
              <div class="archive-hero-feature-head">
                <span class="archive-hero-feature-badge">本季聚焦</span>
                <span class="archive-hero-feature-date">{{ mmdd(featured.publishedAt) }} · {{ featured.readingTime }}</span>
              </div>
              <h2 class="archive-hero-feature-title">{{ featured.title }}</h2>
              <p class="archive-hero-feature-summary">{{ featured.summary }}</p>
              <div class="archive-hero-feature-meta">
                <span class="archive-hero-feature-category">{{ featured.category }}</span>
                <span v-for="t in featured.tags" :key="t" class="archive-hero-feature-tag"># {{ t }}</span>
              </div>
              <p class="archive-hero-feature-mood">{{ featured.mood }}</p>
            </article>
          </section>
        </div>
      </section>

      <!-- 归档概览 -->
      <CxSection eyebrow="Highlights - 归档概览">
        <section class="archive-highlight-section">
          <article
            v-for="(h, i) in highlights"
            :key="h.label"
            v-reveal="i * 70"
            class="archive-highlight-card"
          >
            <div class="archive-highlight-icon"><SvgIcon :name="h.icon" size="18px" /></div>
            <p class="archive-highlight-label">{{ h.label }}</p>
            <p class="archive-highlight-value">{{ h.value }}</p>
            <p class="archive-highlight-detail">{{ h.detail }}</p>
          </article>
        </section>
      </CxSection>

      <!-- 归档总览 -->
      <CxSection eyebrow="Archive - 归档总览">
        <section class="archive-directory-section">
          <div class="archive-main-column">
            <section
              v-for="year in years"
              :id="`year-${year.year}`"
              :key="year.year"
              class="timeline-section-container archive-year-container"
            >
              <div class="timeline-section-container-header">
                <div class="timeline-section-container-header-main">
                  <div class="timeline-section-container-header-title-row">
                    <p class="timeline-section-container-header-eyebrow">Year {{ year.year }} - {{ year.year }} 年归档</p>
                    <div class="timeline-section-container-header-tag-list"></div>
                  </div>
                  <div class="timeline-section-container-header-description">
                    <div class="timeline-section-container-header-slot-pill">
                      这一年共收录 {{ year.total }} 篇文章，内容主要分布在 {{ year.categories.join('、') }} 这些方向。
                    </div>
                  </div>
                </div>
              </div>
              <div class="timeline-section-container-content">
                <section class="archive-year-section">
                  <header class="archive-year-header">
                    <div class="archive-year-header-main">
                      <p class="archive-year-header-eyebrow">Year {{ year.year }}</p>
                      <h2 class="archive-year-header-title">{{ year.year }} 年内容归档</h2>
                    </div>
                  </header>
                  <div class="archive-month-list">
                    <article
                      v-for="(month, mi) in year.months"
                      :id="`year-${year.year}-month-${month.month}`"
                      :key="month.month"
                      v-reveal="40 + mi * 70"
                      class="archive-month-card"
                    >
                      <div class="archive-month-card-head">
                        <div>
                          <p class="archive-month-card-title">{{ month.month }} 月</p>
                          <p class="archive-month-card-caption">本月共 {{ month.entries.length }} 篇，适合顺着时间看这一阶段的内容变化。</p>
                        </div>
                        <span class="archive-month-card-total">{{ month.entries.length }} 篇</span>
                      </div>
                      <div class="archive-entry-grid">
                        <article
                          v-for="(e, ei) in month.entries"
                          :key="e.id"
                          v-reveal="70 + ei * 60"
                          class="archive-entry-card"
                        >
                          <div class="archive-entry-card-meta">
                            <span class="archive-entry-card-category">{{ e.category }}</span>
                            <span class="archive-entry-card-date">{{ mmdd(e.publishedAt) }}</span>
                            <span class="archive-entry-card-date">{{ e.readingTime }}</span>
                          </div>
                          <h3 class="archive-entry-card-title">{{ e.title }}</h3>
                          <p class="archive-entry-card-summary">{{ e.summary }}</p>
                          <p class="archive-entry-card-mood">{{ e.mood }}</p>
                          <div class="archive-entry-card-footer">
                            <div class="archive-entry-card-tags">
                              <span
                                v-for="t in e.tags"
                                :key="t"
                                class="cx-tag cx-tag--primary cx-tag--small is-round is-plain archive-entry-card-tag"
                                style="--cx-tag-text: var(--archive-tag-text); --cx-tag-border: var(--archive-tag-border); --cx-tag-background: var(--archive-tag-background);"
                              >
                                <span class="cx-tag__content"><span class="cx-tag__prefix">#</span><span class="cx-tag__label">{{ t }}</span></span>
                              </span>
                            </div>
                            <RouterLink class="archive-entry-card-link" :to="`/article/${e.id}`">
                              回看文章
                              <SvgIcon name="common-arrow" size="12px" />
                            </RouterLink>
                          </div>
                        </article>
                      </div>
                    </article>
                  </div>
                </section>
              </div>
            </section>
          </div>
          <aside class="archive-directory-section-sidebar">
            <section class="timeline-section-container archive-directory-card">
              <div class="timeline-section-container-header">
                <div class="timeline-section-container-header-main">
                  <div class="timeline-section-container-header-title-row">
                    <p class="timeline-section-container-header-eyebrow">Catalog - 目录导航</p>
                  </div>
                </div>
              </div>
              <div class="timeline-section-container-content">
                <div class="archive-directory-card-panel">
                  <div class="archive-directory-card-scroll">
                    <nav class="archive-directory-card-nav">
                      <template v-for="year in years" :key="year.year">
                        <button
                          type="button"
                          class="archive-directory-card-link"
                          :class="{ 'is-active': activeAnchor === `year-${year.year}` }"
                          style="--archive-directory-card-level: 0;"
                          @click="scrollToAnchor(`year-${year.year}`)"
                        >
                          <span class="archive-directory-card-link-text">{{ year.year }} 年</span>
                          <span class="archive-directory-card-link-meta">{{ year.total }} 篇</span>
                        </button>
                        <button
                          v-for="month in year.months"
                          :key="`${year.year}-${month.month}`"
                          type="button"
                          class="archive-directory-card-link"
                          :class="{ 'is-active': activeAnchor === `year-${year.year}-month-${month.month}` }"
                          style="--archive-directory-card-level: 1;"
                          @click="scrollToAnchor(`year-${year.year}-month-${month.month}`)"
                        >
                          <span class="archive-directory-card-link-text">{{ month.month }} 月</span>
                          <span class="archive-directory-card-link-meta">{{ month.entries.length }} 篇</span>
                        </button>
                      </template>
                      <div v-if="!years.length" class="archive-directory-card-empty">暂无归档内容</div>
                    </nav>
                  </div>
                </div>
              </div>
            </section>
          </aside>
        </section>
      </CxSection>
    </div>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import CxSection from '../components/CxSection.vue'
import SvgIcon from '../components/SvgIcon.vue'
import { api } from '../api'
import { mmdd } from '../utils/display'
import '../assets/css/archive.css'
import '../assets/css/article.css'

const router = useRouter()
const entries = ref([])
const categories = ref([])
const activeAnchor = ref('')

/* 归档开场文案：站点内容可配置，缺失时用默认文案兜底 */
const DEFAULT_ARCHIVE_HERO = {
  eyebrow: 'Archive Atlas',
  title: '把散落在时间里的文章，整理成一页适合慢慢翻看的站点目录。',
  description: '这里收纳了不同年份留下来的文章、手记与专题记录。你可以按年份回看站点的变化，也可以直接从分类和关键词进入感兴趣的内容。',
  notes: [
    '从季节性的专题到日常维护手记，所有内容都按年份归拢，方便整段回看。',
    '每个月份保留了当时最有代表性的文章摘要，让归档页不只是标题清单。',
    '如果你想更快找到感兴趣的内容，可以直接从分类、年份索引和关键词开始。'
  ]
}
const heroCopy = ref({ ...DEFAULT_ARCHIVE_HERO })

function applyHeroCopy(record) {
  try {
    const parsed = JSON.parse((record && record.contentJson) || '')
    if (!parsed || typeof parsed !== 'object') return
    heroCopy.value = {
      eyebrow: parsed.eyebrow || DEFAULT_ARCHIVE_HERO.eyebrow,
      title: parsed.title || DEFAULT_ARCHIVE_HERO.title,
      description: parsed.description || DEFAULT_ARCHIVE_HERO.description,
      notes: Array.isArray(parsed.notes) && parsed.notes.length ? parsed.notes : DEFAULT_ARCHIVE_HERO.notes
    }
  } catch { /* JSON 异常时保持默认文案 */ }
}

const featured = computed(() => entries.value[0] || null)

const years = computed(() => {
  const byYear = new Map()
  for (const e of entries.value) {
    const y = String(e.publishedAt || '').slice(0, 4)
    const m = String(e.publishedAt || '').slice(5, 7)
    if (!byYear.has(y)) byYear.set(y, new Map())
    const months = byYear.get(y)
    if (!months.has(m)) months.set(m, [])
    months.get(m).push(e)
  }
  return [...byYear.entries()]
    .sort((a, b) => b[0].localeCompare(a[0]))
    .map(([year, months]) => {
      const monthList = [...months.entries()]
        .sort((a, b) => b[0].localeCompare(a[0]))
        .map(([month, list]) => ({ month, entries: list }))
      const cats = [...new Set([...months.values()].flat().map(e => e.category))]
      return { year, months: monthList, total: monthList.reduce((s, m) => s + m.entries.length, 0), categories: cats }
    })
})

const highlights = computed(() => {
  const total = entries.value.length
  const yearCount = years.value.length
  const catCount = new Set(entries.value.map(e => e.category)).size
  const tagCount = new Set(entries.value.flatMap(e => e.tags || [])).size
  const latest = featured.value
  return [
    { icon: 'common-archive', label: '归档文章', value: String(total), detail: '以年份和月份重组浏览顺序' },
    { icon: 'common-timeline', label: '年份跨度', value: String(yearCount), detail: '从站点早期记录延续到当前更新' },
    { icon: 'common-tree', label: '主题分类', value: String(catCount), detail: `${tagCount} 个高频标签参与交叉索引` },
    { icon: 'common-component', label: '最近更新', value: latest ? mmdd(latest.publishedAt) : '—', detail: latest ? latest.category : '' }
  ]
})

function scrollToAnchor(id) {
  activeAnchor.value = id
  const el = document.getElementById(id)
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function openArticle(id) {
  router.push(`/article/${id}`)
}

onMounted(async () => {
  const [landing, heroContent] = await Promise.allSettled([
    api.archiveLanding(), api.siteContent('archive-hero')
  ])
  if (heroContent.status === 'fulfilled') applyHeroCopy(heroContent.value)
  if (landing.status === 'fulfilled') {
    const data = landing.value
    entries.value = data.entries || []
    categories.value = data.categories || []
    if (years.value.length) activeAnchor.value = `year-${years.value[0].year}`
  }
})
</script>
