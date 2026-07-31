<template>
  <main class="timeline-page">
    <div class="timeline-shell">
      <!-- 页面概览 -->
      <CxSection eyebrow="Summary - 页面概览">
        <template #pill>快速了解当前页面的结构信息与内容分布。</template>
        <div v-reveal="40" class="timeline-summary-panel">
          <div class="timeline-summary-panel__head">
            <p class="timeline-summary-panel__eyebrow">{{ heroConfig?.eyebrow || DEFAULT_HERO.eyebrow }}</p>
            <h1 class="timeline-summary-panel__title">{{ heroConfig?.title || DEFAULT_HERO.title }}</h1>
            <p class="timeline-summary-panel__description">{{ heroConfig?.description || DEFAULT_HERO.description }}</p>
          </div>
          <div class="timeline-summary-panel__stats">
            <div class="timeline-summary-panel__stat-item">
              <span class="timeline-summary-panel__stat-label">轮播幕数</span>
              <strong class="timeline-summary-panel__stat-value">{{ carousels.length }}</strong>
            </div>
            <div class="timeline-summary-panel__stat-item">
              <span class="timeline-summary-panel__stat-label">时间跨度</span>
              <strong class="timeline-summary-panel__stat-value">{{ yearSpan }}</strong>
            </div>
            <div class="timeline-summary-panel__stat-item">
              <span class="timeline-summary-panel__stat-label">章节节点</span>
              <strong class="timeline-summary-panel__stat-value">{{ timelines.length }}</strong>
            </div>
          </div>
          <div class="timeline-summary-panel__tags">
            <span v-for="t in timelines.slice(0, 4)" :key="t.id" class="timeline-summary-panel__tag-item">{{ t.title }}</span>
          </div>
        </div>
      </CxSection>

      <!-- 顶部轮播 -->
      <CxSection eyebrow="Timeline - 顶部轮播" class="timeline-carousel-deck">
        <template #pill>点击两侧卡片后，可以切换查看当前时间节点的重点内容。</template>
        <div v-reveal="100" class="timeline-carousel-stage">
          <button
            v-if="carousels.length > 1"
            type="button"
            class="timeline-carousel-card timeline-carousel-card--prev"
            :style="{ backgroundImage: `url(${JSON.stringify(bgOf(prevIndex))})` }"
            @click="go(prevIndex)"
          ></button>
          <transition name="card-slide" mode="out-in">
            <div
              :key="carouselIndex"
              class="timeline-carousel-card timeline-carousel-card--main"
              :style="{ backgroundImage: `url(${JSON.stringify(bgOf(carouselIndex))})` }"
            >
              <div class="timeline-carousel-card-panel">
                <span class="timeline-carousel-card-panel__eyebrow">Scene {{ String(carouselIndex + 1).padStart(2, '0') }}</span>
                <strong class="timeline-carousel-card-panel__title">{{ current?.title }}</strong>
                <p class="timeline-carousel-card-panel__content">{{ current?.content }}</p>
                <span class="timeline-carousel-card-panel__date">{{ dateOf(current) }}</span>
              </div>
            </div>
          </transition>
          <button
            v-if="carousels.length > 1"
            type="button"
            class="timeline-carousel-card timeline-carousel-card--next"
            :style="{ backgroundImage: `url(${JSON.stringify(bgOf(nextIndex))})` }"
            @click="go(nextIndex)"
          ></button>
        </div>
      </CxSection>

      <!-- 内容时间线 -->
      <CxSection eyebrow="Story Rail - 内容时间线">
        <template #pill>沿着时间顺序继续向下阅读，查看每个阶段的完整叙事。</template>
        <section class="timeline-story-shell">
          <div class="timeline-story-content">
            <div ref="storyListRef" class="timeline-story-list">
              <article
                v-for="(t, i) in timelines"
                :key="t.id"
                v-reveal="120 + Math.min(i, 5) * 70"
                class="timeline-story-item"
                :class="{ 'is-active': activeStory === i }"
                :data-story-index="i"
              >
                <div class="timeline-story-item__dot"></div>
                <div class="timeline-story-card">
                  <div class="timeline-story-card__glow"></div>
                  <div class="timeline-story-card__meta">
                    <div class="timeline-story-card__meta-group">
                      <span class="timeline-story-card__index">{{ String(i + 1).padStart(2, '0') }}</span>
                      <span class="timeline-story-card__year">{{ (t.timelineDate || '').slice(0, 4) }}年</span>
                    </div>
                    <span class="timeline-story-card__day">{{ (t.timelineDate || '').slice(5).replace('-', '/') }}</span>
                  </div>
                  <div class="timeline-story-card__body has-visual">
                    <div class="timeline-story-card__copy">
                      <span class="timeline-story-card__badge">时间节点</span>
                      <h3 class="timeline-story-card__title">{{ t.title }}</h3>
                      <p class="timeline-story-card__text">{{ t.content }}</p>
                    </div>
                    <div class="timeline-story-card__visual">
                      <div class="timeline-story-card__image-mask"></div>
                      <img class="timeline-story-card__image" :src="t.imageUrl || landscape(i)" :alt="t.title" loading="lazy" />
                    </div>
                  </div>
                </div>
              </article>
            </div>
          </div>
        </section>
      </CxSection>
    </div>
  </main>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import CxSection from '../components/CxSection.vue'
import { api } from '../api'
import '../assets/css/timeline.css'

const DEFAULT_HERO = { eyebrow: 'Timeline', title: '把时间节点排成一条可以浏览的故事轨道。', description: '每一个标记都是一段被留住的时间。' }
const heroConfig = ref(null)

const carousels = ref([])
const timelines = ref([])
const carouselIndex = ref(0)
const activeStory = ref(0)
const storyListRef = ref(null)

const current = computed(() => carousels.value[carouselIndex.value] || null)
const prevIndex = computed(() => (carouselIndex.value - 1 + carousels.value.length) % Math.max(carousels.value.length, 1))
const nextIndex = computed(() => (carouselIndex.value + 1) % Math.max(carousels.value.length, 1))

const yearSpan = computed(() => {
  const years = timelines.value.map(t => Number((t.timelineDate || '').slice(0, 4))).filter(Boolean)
  if (!years.length) return '—'
  const min = Math.min(...years), max = Math.max(...years)
  return min === max ? String(min) : `${min} - ${max}`
})

function landscape(i) {
  return `/image/bg/Landscape/${String((i % 13) + 1).padStart(2, '0')}.webp`
}

function bgOf(i) {
  const c = carousels.value[i]
  return c && c.imageUrl ? c.imageUrl : landscape(i)
}

function dateOf(c) {
  if (!c || !c.createdAt) return ''
  return String(c.createdAt).slice(0, 10).replaceAll('-', '/')
}

function go(i) {
  carouselIndex.value = i
}

let storyObserver = null

onMounted(async () => {
  try {
    const data = await api.timelineLanding()
    carousels.value = data.carousels || []
    timelines.value = data.timelines || []
  } catch (e) { console.warn('[时间线] 加载失败:', e) }
  try {
    heroConfig.value = await api.siteContent('timeline-hero')
  } catch (e) { console.warn('[时间线] 配置加载失败:', e) }
  requestAnimationFrame(() => {
    if (!storyListRef.value) return
    storyObserver = new IntersectionObserver(entries => {
      for (const e of entries) {
        if (e.isIntersecting) activeStory.value = Number(e.target.dataset.storyIndex)
      }
    }, { rootMargin: '-40% 0px -50% 0px' })
    storyListRef.value.querySelectorAll('.timeline-story-item').forEach(el => storyObserver.observe(el))
  })
})

onBeforeUnmount(() => {
  if (storyObserver) storyObserver.disconnect()
})
</script>
