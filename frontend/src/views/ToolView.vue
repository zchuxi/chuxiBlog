<template>
  <main class="tool-page">
    <div class="tool-page-shell">
      <!-- 工具轨道 -->
      <LxSection v-reveal="0" eyebrow="Tool Atlas - 工具轨道">
        <template #pill>搜索、摘要与跳转入口合并成一张浏览地图</template>
        <section class="tool-hero-section">
          <div class="tool-hero-copy-panel">
            <h1 class="tool-hero-title">{{ toolConfig?.title || DEFAULT_TOOL.title }}</h1>
            <p class="tool-hero-description">{{ toolConfig?.description || DEFAULT_TOOL.description }}</p>
            <div class="tool-hero-stat-grid">
              <article class="tool-hero-stat-card"><span>收录站点</span><strong>{{ tools.length }}</strong></article>
              <article class="tool-hero-stat-card"><span>分类数量</span><strong>{{ categories.length }}</strong></article>
              <article class="tool-hero-stat-card"><span>精选站点</span><strong>{{ featuredCount }}</strong></article>
              <article class="tool-hero-stat-card"><span>当前结果</span><strong>{{ filtered.length }}</strong></article>
            </div>
            <div class="tool-hero-search-panel">
              <p class="tool-hero-panel-title">快速检索</p>
              <div class="lx-input">
                <input v-model="keyword" class="lx-input__inner" type="text" :placeholder="toolConfig?.searchPlaceholder || DEFAULT_TOOL.searchPlaceholder" />
              </div>
            </div>
          </div>
        </section>
      </LxSection>

      <!-- 按场景筛选 -->
      <LxSection eyebrow="Atlas - 按场景筛选">
        <template #pill>按场景筛选</template>
        <section class="tool-atlas-section">
          <div class="tool-filter-panel">
            <div class="tool-filter-row">
              <button
                type="button"
                class="lx-button lx-button--primary"
                :class="{ 'is-plain': activeCategory !== '' }"
                @click="activeCategory = ''"
              >
                <span class="lx-button__content"><span class="lx-button__label">全部</span></span>
              </button>
              <button
                v-for="c in categories"
                :key="c"
                type="button"
                class="lx-button lx-button--primary"
                :class="{ 'is-plain': activeCategory !== c }"
                @click="activeCategory = c"
              >
                <span class="lx-button__content"><span class="lx-button__label">{{ c }}</span></span>
              </button>
            </div>
          </div>

          <LxSection eyebrow="Spotlight - 优先浏览">
            <template #pill>优先浏览的站点</template>
            <section class="tool-spotlight-section">
              <div class="tool-spotlight-grid">
                <!-- 点击卡片进入详情，外链按钮单独跳转 -->
                <RouterLink
                  v-for="(t, i) in spotlight"
                  :key="t.id"
                  :to="`/tool/${t.id}`"
                  custom
                  v-slot="{ href, navigate }"
                >
                  <a
                    v-reveal="40 + i * 80"
                    class="tool-spotlight-card"
                    :class="`tool-spotlight-card-${SPOTLIGHT_VARIANTS[Math.min(i, SPOTLIGHT_VARIANTS.length - 1)]}`"
                    :href="href"
                    @click="navigate"
                  >
                    <div class="tool-spotlight-card-brand">
                      <div class="tool-spotlight-card-icon"><SvgIcon name="common-web" size="28px" /></div>
                      <div>
                        <h3 class="tool-spotlight-card-title">{{ t.websiteName }}</h3>
                        <p class="tool-spotlight-card-address">{{ t.websiteUrl }}</p>
                      </div>
                    </div>
                    <p class="tool-spotlight-card-description">{{ t.websiteDescription }}</p>
                    <div class="tool-spotlight-card-tag-list">
                      <span
                        v-for="tag in t.tags"
                        :key="tag"
                        class="lx-tag lx-tag--primary lx-tag--small is-round is-plain tool-spotlight-card-tag"
                        style="--lx-tag-text: var(--archive-tag-text); --lx-tag-border: var(--archive-tag-border); --lx-tag-background: var(--archive-tag-background);"
                      >
                        <span class="lx-tag__content"><span class="lx-tag__prefix">#</span><span class="lx-tag__label">{{ tag }}</span></span>
                      </span>
                      <button type="button" class="tool-card-open-btn" title="访问站点" @click.stop.prevent="openSite(t.websiteUrl)">
                        <SvgIcon name="common-open" size="14px" />
                      </button>
                    </div>
                  </a>
                </RouterLink>
              </div>
            </section>
          </LxSection>

          <LxSection eyebrow="Directory - 工具列表">
            <template #pill>共 {{ filtered.length }} 个站点</template>
            <div class="tool-atlas-grid">
              <!-- 点击卡片进入详情，外链按钮单独跳转 -->
              <RouterLink
                v-for="(t, i) in filtered"
                :key="t.id"
                :to="`/tool/${t.id}`"
                custom
                v-slot="{ href, navigate }"
              >
                <a
                  v-reveal="50 + (i % 6) * 70"
                  class="tool-site-card"
                  :class="siteCardVariant(i)"
                  :href="href"
                  @click="navigate"
                >
                  <div class="tool-site-card-content">
                    <div class="tool-site-card-header">
                      <div class="tool-site-card-brand">
                        <div class="tool-site-card-icon"><SvgIcon name="common-web" size="28px" /></div>
                        <div class="tool-site-card-brand-copy">
                          <h3 class="tool-site-card-title">{{ t.websiteName }}</h3>
                          <p class="tool-site-card-address">{{ t.websiteUrl }}</p>
                        </div>
                      </div>
                    </div>
                    <p class="tool-site-card-description">{{ t.websiteDescription }}</p>
                  </div>
                  <div class="tool-site-card-footer">
                    <div class="tool-site-card-tag-list">
                      <span
                        v-for="tag in t.tags"
                        :key="tag"
                        class="lx-tag lx-tag--primary lx-tag--small is-round is-plain tool-site-card-tag"
                        style="--lx-tag-text: var(--archive-tag-text); --lx-tag-border: var(--archive-tag-border); --lx-tag-background: var(--archive-tag-background);"
                      >
                        <span class="lx-tag__content"><span class="lx-tag__prefix">#</span><span class="lx-tag__label">{{ tag }}</span></span>
                      </span>
                    </div>
                    <button type="button" class="tool-card-open-btn" title="访问站点" @click.stop.prevent="openSite(t.websiteUrl)">
                      <SvgIcon name="common-open" size="14px" />
                    </button>
                  </div>
                </a>
              </RouterLink>
            </div>
          </LxSection>
        </section>
      </LxSection>
    </div>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import LxSection from '../components/LxSection.vue'
import SvgIcon from '../components/SvgIcon.vue'
import { api } from '../api'

const DEFAULT_TOOL = { title: '工具地图', description: '把常用工具、网站和灵感碎片排成一张可浏览的在线地图。', searchPlaceholder: '搜索网站名、用途、域名或标签' }
const toolConfig = ref(null)

const SPOTLIGHT_VARIANTS = ['primary', 'secondary', 'tertiary', 'tertiary']

const tools = ref([])
const keyword = ref('')
const activeCategory = ref('')

const categories = computed(() => [...new Set(tools.value.map(t => t.category))])
const featuredCount = computed(() => tools.value.filter(t => t.featured).length)
const spotlight = computed(() => tools.value.filter(t => t.featured).slice(0, 4))

const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return tools.value.filter(t => {
    if (activeCategory.value && t.category !== activeCategory.value) return false
    if (!kw) return true
    return [t.websiteName, t.websiteDescription, t.websiteUrl, ...(t.tags || [])]
      .some(v => String(v || '').toLowerCase().includes(kw))
  })
})

// 外链按钮：新窗口打开原站点
function openSite(url) {
  window.open(url, '_blank', 'noopener,noreferrer')
}

function siteCardVariant(i) {
  if (i % 8 === 0) return 'tool-site-card-wide'
  if (i % 8 === 3) return 'tool-site-card-tall'
  return ''
}

onMounted(async () => {
  try {
    tools.value = await api.toolsLanding() || []
  } catch { /* 后端未启动 */ }
  try {
    toolConfig.value = await api.siteContent('tool-hero')
  } catch { /* 使用默认值 */ }
})
</script>

<style>
/* 卡片上的小外链按钮（不影响原布局） */
.tool-card-open-btn{position:relative;z-index:1;display:inline-flex;align-items:center;justify-content:center;flex:0 0 auto;width:32px;height:32px;padding:0;border:1px solid var(--nested-inner-card-border);border-radius:12px;background:var(--nested-inner-card-bg);box-shadow:var(--nested-inner-card-shadow);color:var(--tool-page-chip-text);cursor:pointer;transition:transform .2s ease,color .2s ease}
.tool-card-open-btn:hover{transform:translateY(-2px);color:var(--tool-page-text-primary)}
.tool-spotlight-card-tag-list .tool-card-open-btn{margin-left:auto;align-self:flex-end}
@media(max-width:860px){.tool-site-card-footer .tool-card-open-btn{align-self:flex-end}}
</style>
