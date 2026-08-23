<template>
  <main class="tool-page">
    <div class="tool-page-shell">
      <!-- 工具轨道 -->
      <CxSection v-reveal="0" eyebrow="Tool Atlas - 工具轨道">
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
          </div>
        </section>
      </CxSection>

      <section v-reveal="30" class="tool-search-toolbar" aria-label="快速检索">
        <SvgIcon name="common-search" class="tool-search-icon" size="20px" />
        <input
          v-model="keyword"
          type="search"
          class="tool-search-input"
          aria-label="搜索工具网站"
          :placeholder="toolConfig?.searchPlaceholder || DEFAULT_TOOL.searchPlaceholder"
        />
        <span class="tool-search-result" aria-live="polite">{{ filtered.length }} 个结果</span>
        <button
          v-if="keyword"
          type="button"
          class="tool-search-clear"
          title="清空搜索"
          aria-label="清空搜索"
          @click="keyword = ''"
        >
          <SvgIcon name="common-close" size="16px" />
        </button>
      </section>

      <!-- 按场景筛选 -->
      <CxSection eyebrow="Atlas - 按场景筛选">
        <template #pill>按场景筛选</template>
        <section class="tool-atlas-section">
          <div class="tool-filter-panel">
            <div class="tool-filter-row">
              <button
                type="button"
                class="cx-button cx-button--primary"
                :class="{ 'is-plain': activeCategory !== '' }"
                @click="activeCategory = ''"
              >
                <span class="cx-button__content"><span class="cx-button__label">全部</span></span>
              </button>
              <button
                v-for="c in categories"
                :key="c"
                type="button"
                class="cx-button cx-button--primary"
                :class="{ 'is-plain': activeCategory !== c }"
                @click="activeCategory = c"
              >
                <span class="cx-button__content"><span class="cx-button__label">{{ c }}</span></span>
              </button>
            </div>
          </div>

          <!-- 搜索/场景筛选激活时隐藏优先浏览，让过滤结果直接顶上来 -->
          <CxSection v-if="!isFiltering" eyebrow="Spotlight - 优先浏览">
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
                        class="cx-tag cx-tag--primary cx-tag--small is-round is-plain tool-spotlight-card-tag"
                        style="--cx-tag-text: var(--archive-tag-text); --cx-tag-border: var(--archive-tag-border); --cx-tag-background: var(--archive-tag-background);"
                      >
                        <span class="cx-tag__content"><span class="cx-tag__prefix">#</span><span class="cx-tag__label">{{ tag }}</span></span>
                      </span>
                      <button type="button" class="tool-card-open-btn" title="访问站点" @click.stop.prevent="openSite(t.websiteUrl)">
                        <SvgIcon name="common-open" size="14px" />
                      </button>
                    </div>
                  </a>
                </RouterLink>
              </div>
            </section>
          </CxSection>

          <CxSection eyebrow="Directory - 工具列表">
            <template #pill>共 {{ filtered.length }} 个站点</template>
            <!-- 空状态：区分「数据没加载出来」与「筛选无结果」 -->
            <p v-if="!tools.length" class="tool-empty-state" role="status">
              站点数据还没加载出来，请稍后刷新重试。
            </p>
            <p v-else-if="!filtered.length" class="tool-empty-state" role="status">
              没有找到匹配的站点，换个关键词或场景试试。
              <button type="button" class="tool-empty-reset" @click="resetFilters">清空筛选</button>
            </p>
            <div v-else class="tool-atlas-grid">
              <!-- 点击卡片进入详情，外链按钮单独跳转 -->
              <RouterLink
                v-for="(t, i) in filtered"
                :key="t.id"
                :to="`/tool/${t.id}`"
                custom
                v-slot="{ href, navigate }"
              >
                <a
                  v-reveal="cardReveal(50 + (i % 6) * 70)"
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
                        class="cx-tag cx-tag--primary cx-tag--small is-round is-plain tool-site-card-tag"
                        style="--cx-tag-text: var(--archive-tag-text); --cx-tag-border: var(--archive-tag-border); --cx-tag-background: var(--archive-tag-background);"
                      >
                        <span class="cx-tag__content"><span class="cx-tag__prefix">#</span><span class="cx-tag__label">{{ tag }}</span></span>
                      </span>
                    </div>
                    <button type="button" class="tool-card-open-btn" title="访问站点" @click.stop.prevent="openSite(t.websiteUrl)">
                      <SvgIcon name="common-open" size="14px" />
                    </button>
                  </div>
                </a>
              </RouterLink>
            </div>
          </CxSection>
        </section>
      </CxSection>
    </div>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import CxSection from '../components/CxSection.vue'
import SvgIcon from '../components/SvgIcon.vue'
import { api } from '../api'
import '../assets/css/tool.css'

const DEFAULT_TOOL = { title: '工具地图', description: '把常用工具、网站和灵感碎片排成一张可浏览的在线地图。', searchPlaceholder: '搜索网站名、用途、域名或标签' }
const toolConfig = ref(null)

const SPOTLIGHT_VARIANTS = ['primary', 'secondary', 'tertiary', 'tertiary']

const tools = ref([])
const keyword = ref('')
const activeCategory = ref('')

const categories = computed(() => [...new Set(tools.value.map(t => t.category))])
const featuredCount = computed(() => tools.value.filter(t => t.featured).length)
const spotlight = computed(() => tools.value.filter(t => t.featured).slice(0, 4))

// 任一筛选生效即进入「结果模式」：隐藏优先浏览，只展示过滤结果
const isFiltering = computed(() => !!keyword.value.trim() || !!activeCategory.value)

const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return tools.value.filter(t => {
    if (activeCategory.value && t.category !== activeCategory.value) return false
    if (!kw) return true
    return [t.websiteName, t.websiteDescription, t.websiteUrl, t.category, ...(t.tags || [])]
      .some(v => String(v || '').toLowerCase().includes(kw))
  })
})

function resetFilters() {
  keyword.value = ''
  activeCategory.value = ''
}

// 筛选态下的列表卡片跳过入场动画：结果由用户主动触发，应立刻可见；
// 无筛选时保留原有错峰淡入
function cardReveal(delay) {
  return { delay, instant: isFiltering.value }
}

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
  } catch (e) { console.warn('[工具] 加载失败:', e) }
  try {
    toolConfig.value = await api.siteContent('tool-hero')
  } catch (e) { console.warn('[工具] 配置加载失败:', e) }
})
</script>

<style>
/* 独立搜索工具栏：容器本身即输入边界，避免重复卡片嵌套。 */
.tool-search-toolbar{display:grid;grid-template-columns:auto minmax(0,1fr) auto auto;align-items:center;gap:12px;min-height:58px;padding:10px 12px 10px 18px;border:1px solid var(--nested-outer-card-border);border-radius:20px;background:var(--nested-outer-card-bg);box-shadow:var(--nested-outer-card-shadow);transition:border-color .2s ease,box-shadow .2s ease}
.tool-search-toolbar:focus-within{border-color:color-mix(in srgb,var(--tool-page-chip-text) 42%,var(--nested-outer-card-border));box-shadow:var(--nested-outer-card-shadow),0 0 0 3px color-mix(in srgb,var(--tool-page-chip-text) 10%,transparent)}
.tool-search-icon{color:var(--tool-page-chip-text)}
.tool-search-input{min-width:0;width:100%;padding:8px 0;border:0;outline:0;background:transparent;color:var(--tool-page-text-primary);font:inherit;font-size:15.5px;line-height:1.5}
.tool-search-input::placeholder{color:var(--tool-page-text-muted);opacity:.82}
.tool-search-input::-webkit-search-cancel-button{display:none}
.tool-search-result{color:var(--tool-page-text-muted);font-size:13.5px;white-space:nowrap}
.tool-search-clear{display:inline-flex;align-items:center;justify-content:center;width:36px;height:36px;padding:0;border:0;border-radius:8px;background:transparent;color:var(--tool-page-text-muted);cursor:pointer;transition:background-color .2s ease,color .2s ease}
.tool-search-clear:hover{background:var(--nested-inner-card-bg);color:var(--tool-page-text-primary)}
.tool-search-clear:focus-visible{outline:2px solid var(--tool-page-chip-text);outline-offset:2px}
/* 筛选无结果 / 数据未加载的空状态提示 */
.tool-empty-state{display:flex;flex-wrap:wrap;align-items:center;gap:12px;margin:0;padding:26px 22px;border:1px dashed var(--nested-outer-card-border);border-radius:20px;background:var(--nested-outer-card-bg);color:var(--tool-page-text-muted);font-size:15px;line-height:1.6}
.tool-empty-reset{padding:6px 16px;border:1px solid var(--nested-inner-card-border);border-radius:999px;background:var(--nested-inner-card-bg);color:var(--tool-page-chip-text);font:inherit;font-size:14px;cursor:pointer;transition:color .2s ease,transform .2s ease}
.tool-empty-reset:hover{color:var(--tool-page-text-primary);transform:translateY(-1px)}
.tool-empty-reset:focus-visible{outline:2px solid var(--tool-page-chip-text);outline-offset:2px}
/* 卡片上的小外链按钮（不影响原布局） */
.tool-card-open-btn{position:relative;z-index:1;display:inline-flex;align-items:center;justify-content:center;flex:0 0 auto;width:32px;height:32px;padding:0;border:1px solid var(--nested-inner-card-border);border-radius:12px;background:var(--nested-inner-card-bg);box-shadow:var(--nested-inner-card-shadow);color:var(--tool-page-chip-text);cursor:pointer;transition:transform .2s ease,color .2s ease}
.tool-card-open-btn:hover{transform:translateY(-2px);color:var(--tool-page-text-primary)}
.tool-spotlight-card-tag-list .tool-card-open-btn{margin-left:auto;align-self:flex-end}
@media(max-width:860px){.tool-site-card-footer .tool-card-open-btn{align-self:flex-end}}
@media(max-width:640px){.tool-search-toolbar{grid-template-columns:auto minmax(0,1fr) auto;gap:8px;padding:10px 12px;border-radius:18px}.tool-search-result{grid-column:2;grid-row:2}.tool-search-clear{grid-column:3;grid-row:1}
/* 小屏加大触控面积（≥40px），与后台行内按钮同口径 */
.tool-search-clear{width:40px;height:40px}.tool-empty-reset{min-height:40px;padding:8px 18px}}
</style>
