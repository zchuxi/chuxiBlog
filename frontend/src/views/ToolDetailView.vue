<template>
  <main class="tool-detail-page">
    <div class="tool-detail-shell">
      <!-- 面包屑返回 -->
      <div class="tool-detail-breadcrumb" v-reveal="0">
        <button type="button" class="tool-detail-back-btn" @click="goBack">
          <SvgIcon name="common-expand-left" size="16px" />
          <span>返回工具列表</span>
        </button>
      </div>

      <!-- 找不到站点的空态 -->
      <section v-if="loaded && !site" class="tool-detail-empty" v-reveal="40">
        <div class="tool-detail-empty-icon"><SvgIcon name="common-web" size="40px" /></div>
        <h2>没有找到这个站点</h2>
        <p>它可能已被移除，或者链接有误。回到工具列表继续浏览吧。</p>
        <button type="button" class="cx-button cx-button--primary" @click="goBack">
          <span class="cx-button__content"><span class="cx-button__label">返回工具列表</span></span>
        </button>
      </section>

      <template v-else-if="site">
        <!-- 顶部 hero 岛 -->
        <CxSection v-reveal="0" eyebrow="Tool Atlas - 站点档案">
          <template #pill>{{ site.websiteName }} 的收录详情</template>
          <section class="tool-detail-hero">
            <div class="tool-detail-hero-main">
              <div class="tool-detail-hero-icon"><SvgIcon name="common-web" size="44px" /></div>
              <div class="tool-detail-hero-copy">
                <div class="tool-detail-hero-title-row">
                  <h1 class="tool-detail-hero-title">{{ site.websiteName }}</h1>
                  <span class="cx-tag cx-tag--primary cx-tag--small is-round is-plain">
                    <span class="cx-tag__content"><span class="cx-tag__label">{{ site.category }}</span></span>
                  </span>
                  <span v-if="site.featured" class="tool-detail-featured-badge">精选</span>
                </div>
                <p class="tool-detail-hero-domain">{{ domain }}</p>
                <p class="tool-detail-hero-meta">收录于 {{ formatDate(site.createdAt) }} · 更新于 {{ formatDate(site.updatedAt) }}</p>
              </div>
            </div>
            <div class="tool-detail-hero-actions">
              <a class="cx-button cx-button--primary tool-detail-visit-btn" :href="site.websiteUrl" target="_blank" rel="noopener">
                <span class="cx-button__content">
                  <SvgIcon name="common-open" size="15px" />
                  <span class="cx-button__label">访问站点</span>
                </span>
              </a>
              <button type="button" class="cx-button cx-button--primary is-plain" @click="copyLink">
                <span class="cx-button__content"><span class="cx-button__label">{{ copied ? '已复制' : '复制链接' }}</span></span>
              </button>
            </div>
          </section>
        </CxSection>

        <!-- 简介 + 速览两栏 -->
        <CxSection eyebrow="Overview - 站点资料">
          <template #pill>简介与速览信息</template>
          <div class="tool-detail-columns">
            <article class="tool-detail-card" v-reveal="40">
              <h2 class="tool-detail-card-title">站点简介</h2>
              <figure v-if="site.imageUrl && !shotFailed" class="tool-detail-shot">
                <img
                  class="tool-detail-shot-image"
                  :src="site.imageUrl"
                  :alt="`${site.websiteName} 展示图`"
                  loading="lazy"
                  referrerpolicy="no-referrer"
                  @error="shotFailed = true"
                  @click="shotZoom = true"
                />
                <figcaption class="tool-detail-shot-caption">点击查看大图</figcaption>
              </figure>
              <p class="tool-detail-intro-text">{{ site.websiteDescription }}</p>
              <blockquote v-if="site.highlight" class="tool-detail-highlight">{{ site.highlight }}</blockquote>
            </article>
            <aside class="tool-detail-card" v-reveal="100">
              <h2 class="tool-detail-card-title">速览</h2>
              <dl class="tool-detail-glance-list">
                <div class="tool-detail-glance-item">
                  <dt>分类</dt>
                  <dd>
                    <span class="cx-tag cx-tag--primary cx-tag--small is-round is-plain">
                      <span class="cx-tag__content"><span class="cx-tag__label">{{ site.category }}</span></span>
                    </span>
                  </dd>
                </div>
                <div class="tool-detail-glance-item">
                  <dt>标签</dt>
                  <dd>
                    <div class="tool-detail-glance-tags">
                      <span v-for="tag in site.tags" :key="tag" class="cx-tag cx-tag--primary cx-tag--small is-round is-plain">
                        <span class="cx-tag__content"><span class="cx-tag__prefix">#</span><span class="cx-tag__label">{{ tag }}</span></span>
                      </span>
                      <span v-if="!site.tags || !site.tags.length" class="tool-detail-glance-plain">暂无标签</span>
                    </div>
                  </dd>
                </div>
                <div class="tool-detail-glance-item">
                  <dt>收录时间</dt>
                  <dd><span class="tool-detail-glance-plain">{{ formatDate(site.createdAt) }}</span></dd>
                </div>
                <div class="tool-detail-glance-item">
                  <dt>链接</dt>
                  <dd><a class="tool-detail-glance-link" :href="site.websiteUrl" target="_blank" rel="noopener">{{ site.websiteUrl }}</a></dd>
                </div>
              </dl>
            </aside>
          </div>
        </CxSection>

        <!-- 同类站点 -->
        <CxSection v-if="related.length" eyebrow="Related - 同类站点">
          <template #pill>「{{ site.category }}」分类下的其他站点</template>
          <div class="tool-detail-related-row">
            <button
              v-for="(r, i) in related"
              :key="r.id"
              v-reveal="40 + i * 60"
              type="button"
              class="tool-detail-related-card"
              @click="goDetail(r.id)"
            >
              <div class="tool-detail-related-brand">
                <span class="tool-detail-related-icon"><SvgIcon name="common-web" size="20px" /></span>
                <h3 class="tool-detail-related-title">{{ r.websiteName }}</h3>
              </div>
              <p class="tool-detail-related-description">{{ r.websiteDescription }}</p>
              <span class="tool-detail-related-category">{{ r.category }}</span>
            </button>
          </div>
        </CxSection>
      </template>
    </div>

    <!-- 展示图大图查看 -->
    <transition name="tool-detail-zoom">
      <div v-if="shotZoom" class="tool-detail-zoom-mask" @click="shotZoom = false">
        <img class="tool-detail-zoom-image" :src="site && site.imageUrl" :alt="site && site.websiteName" referrerpolicy="no-referrer" />
      </div>
    </transition>
  </main>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import CxSection from '../components/CxSection.vue'
import SvgIcon from '../components/SvgIcon.vue'
import { api } from '../api'

const route = useRoute()
const router = useRouter()

const tools = ref([])
const loaded = ref(false)
const copied = ref(false)
const shotFailed = ref(false)
const shotZoom = ref(false)
let copiedTimer = null

// 按路由参数匹配当前站点（Number 化比较）
const site = computed(() => {
  const id = Number(route.params.id)
  return tools.value.find(t => Number(t.id) === id) || null
})

// 同分类的其他站点，最多 6 个
const related = computed(() => {
  if (!site.value) return []
  return tools.value
    .filter(t => t.category === site.value.category && Number(t.id) !== Number(site.value.id))
    .slice(0, 6)
})

const domain = computed(() => {
  if (!site.value) return ''
  try { return new URL(site.value.websiteUrl).hostname } catch { return site.value.websiteUrl }
})

function formatDate(v) {
  if (!v) return '—'
  const s = String(v)
  return s.length >= 10 ? s.slice(0, 10) : s
}

function goBack() { router.push('/tool') }
function goDetail(id) { router.push(`/tool/${id}`) }

async function copyLink() {
  if (!site.value) return
  const url = site.value.websiteUrl
  try {
    await navigator.clipboard.writeText(url)
  } catch {
    // 剪贴板 API 不可用时的兜底
    const ta = document.createElement('textarea')
    ta.value = url
    document.body.appendChild(ta)
    ta.select()
    document.execCommand('copy')
    document.body.removeChild(ta)
  }
  copied.value = true
  clearTimeout(copiedTimer)
  copiedTimer = setTimeout(() => { copied.value = false }, 1600)
}

onMounted(async () => {
  try {
    tools.value = await api.toolsLanding() || []
  } catch { /* 后端未启动 */ }
  loaded.value = true
})

onBeforeUnmount(() => clearTimeout(copiedTimer))
</script>

<style>
.tool-detail-page{--tool-detail-text-primary: var(--text-color);--tool-detail-text-secondary: color-mix(in srgb, var(--text-color) 68%, transparent);--tool-detail-text-muted: #6f8093;width:min(1180px,100%);margin:0 auto;padding:12px 0 40px;scrollbar-gutter:stable}
html.dark .tool-detail-page{--tool-detail-text-muted: rgba(226, 232, 240, .72)}
.tool-detail-shell{display:flex;flex-direction:column;gap:28px}

/* 面包屑返回 */
.tool-detail-breadcrumb{display:flex}
.tool-detail-back-btn{display:inline-flex;align-items:center;gap:8px;padding:10px 18px;border:1px solid var(--nested-outer-card-border);border-radius:999px;background:var(--nested-outer-card-bg);box-shadow:var(--nested-outer-card-shadow);color:var(--tool-detail-text-secondary);font-size:15.5px;font-weight:600;cursor:pointer;transition:transform .2s ease,color .2s ease}
.tool-detail-back-btn:hover{transform:translateX(-3px);color:var(--tool-detail-text-primary)}

/* hero 岛 */
.tool-detail-hero{position:relative;overflow:hidden;display:flex;flex-wrap:wrap;align-items:center;justify-content:space-between;gap:22px;padding:30px;border:1px solid var(--nested-outer-card-border);border-radius:30px;background:var(--nested-outer-card-bg);box-shadow:var(--nested-outer-card-shadow)}
.tool-detail-hero:before{content:"";position:absolute;top:-42%;right:-6%;width:250px;height:250px;border-radius:50%;background:radial-gradient(circle,rgba(130,98,85,.14),transparent 70%),radial-gradient(circle at 30% 30%,rgba(111,128,147,.12),transparent 66%);pointer-events:none}
.tool-detail-hero-main{position:relative;display:flex;align-items:center;gap:18px;min-width:0}
.tool-detail-hero-icon{display:inline-flex;align-items:center;justify-content:center;flex:0 0 auto;width:86px;height:86px;border:1px solid var(--nested-inner-card-border);border-radius:26px;background:var(--nested-inner-card-bg);box-shadow:var(--nested-inner-card-shadow);color:#536c88}
html.dark .tool-detail-hero-icon{color:#9db4cd}
.tool-detail-hero-copy{min-width:0}
.tool-detail-hero-title-row{display:flex;flex-wrap:wrap;align-items:center;gap:10px}
.tool-detail-hero-title{margin:0;color:var(--tool-detail-text-primary);font-size:clamp(29px,3vw,40px);line-height:1.15}
.tool-detail-featured-badge{display:inline-flex;align-items:center;padding:4px 12px;border:1px solid rgba(234,209,170,.7);border-radius:999px;background:linear-gradient(135deg,#fff7ee,#f8ead2);color:#8b6122;font-size:13px;font-weight:700;letter-spacing:.04em}
html.dark .tool-detail-featured-badge{border-color:rgba(234,209,170,.24);background:rgba(139,97,34,.2);color:#e8c98d}
.tool-detail-hero-domain{margin:8px 0 0;color:var(--tool-detail-text-muted);font-size:15.5px;line-height:1.5;word-break:break-all}
.tool-detail-hero-meta{margin:6px 0 0;color:var(--tool-detail-text-muted);font-size:14.5px;line-height:1.6}
.tool-detail-hero-actions{position:relative;display:flex;flex-wrap:wrap;align-items:center;gap:10px}
.tool-detail-visit-btn{min-width:128px;height:42px;text-decoration:none}

/* 两栏卡片 */
.tool-detail-columns{display:grid;grid-template-columns:minmax(0,1.6fr) minmax(0,1fr);gap:14px}
.tool-detail-card{position:relative;overflow:hidden;display:flex;flex-direction:column;gap:14px;padding:24px;border:1px solid var(--nested-outer-card-border);border-radius:26px;background:var(--nested-outer-card-bg);box-shadow:var(--nested-outer-card-shadow)}
.tool-detail-card-title{margin:0;color:var(--tool-detail-text-primary);font-size:22px;line-height:1.3}
.tool-detail-intro-text{margin:0;color:var(--tool-detail-text-secondary);font-size:16.5px;line-height:1.9}
.tool-detail-highlight{margin:0;padding:14px 18px;border-left:3px solid var(--accent-solid,#7ea8d2);border-radius:14px;background:var(--nested-inner-card-bg);box-shadow:var(--nested-inner-card-shadow);color:var(--tool-detail-text-primary);font-size:15.5px;line-height:1.8}
.tool-detail-glance-list{display:flex;flex-direction:column;gap:12px;margin:0}
.tool-detail-glance-item{display:flex;flex-direction:column;gap:8px;padding:12px 14px;border:1px solid var(--nested-inner-card-border);border-radius:16px;background:var(--nested-inner-card-bg);box-shadow:var(--nested-inner-card-shadow)}
.tool-detail-glance-item dt{color:var(--tool-detail-text-muted);font-size:13px;font-weight:700;letter-spacing:.08em;text-transform:uppercase}
.tool-detail-glance-item dd{margin:0;min-width:0}
.tool-detail-glance-tags{display:flex;flex-wrap:wrap;gap:8px}
.tool-detail-glance-plain{color:var(--tool-detail-text-secondary);font-size:15.5px}
.tool-detail-glance-link{color:var(--accent-strong,#587699);font-size:15.5px;line-height:1.6;word-break:break-all;text-decoration:none}
.tool-detail-glance-link:hover{text-decoration:underline}
html.dark .tool-detail-glance-link{color:#9db4cd}

/* 同类站点横向列表 */
.tool-detail-related-row{display:flex;gap:14px;overflow-x:auto;padding:2px 2px 10px;scrollbar-width:thin}
.tool-detail-related-card{display:flex;flex:0 0 250px;flex-direction:column;gap:10px;padding:18px;border:1px solid var(--nested-outer-card-border);border-radius:22px;background:var(--nested-outer-card-bg);box-shadow:var(--nested-outer-card-shadow);text-align:left;font:inherit;cursor:pointer;transition:transform .24s ease}
.tool-detail-related-card:hover{transform:translateY(-4px)}
.tool-detail-related-brand{display:flex;align-items:center;gap:10px;min-width:0}
.tool-detail-related-icon{display:inline-flex;align-items:center;justify-content:center;flex:0 0 auto;width:38px;height:38px;border:1px solid var(--nested-inner-card-border);border-radius:13px;background:var(--nested-inner-card-bg);box-shadow:var(--nested-inner-card-shadow);color:#536c88}
html.dark .tool-detail-related-icon{color:#9db4cd}
.tool-detail-related-title{margin:0;overflow:hidden;color:var(--tool-detail-text-primary);font-size:18.5px;line-height:1.3;text-overflow:ellipsis;white-space:nowrap}
.tool-detail-related-description{display:-webkit-box;-webkit-box-orient:vertical;-webkit-line-clamp:2;overflow:hidden;margin:0;color:var(--tool-detail-text-secondary);font-size:14.5px;line-height:1.7}
.tool-detail-related-category{margin-top:auto;color:var(--tool-detail-text-muted);font-size:13px;letter-spacing:.06em}

/* 空态 */
.tool-detail-empty{display:flex;flex-direction:column;align-items:center;gap:14px;padding:48px 24px;border:1px solid var(--nested-outer-card-border);border-radius:30px;background:var(--nested-outer-card-bg);box-shadow:var(--nested-outer-card-shadow);text-align:center}
.tool-detail-empty-icon{display:inline-flex;align-items:center;justify-content:center;width:76px;height:76px;border:1px solid var(--nested-inner-card-border);border-radius:24px;background:var(--nested-inner-card-bg);box-shadow:var(--nested-inner-card-shadow);color:#536c88}
html.dark .tool-detail-empty-icon{color:#9db4cd}
.tool-detail-empty h2{margin:0;color:var(--tool-detail-text-primary);font-size:26px}
.tool-detail-empty p{margin:0;color:var(--tool-detail-text-secondary);font-size:15.5px;line-height:1.7}

@media(max-width:1120px){.tool-detail-columns{grid-template-columns:1fr}}
@media(max-width:860px){
  .tool-detail-page{padding:0 0 24px}
  .tool-detail-shell{gap:20px}
  .tool-detail-hero{border-radius:24px;padding:22px}
  .tool-detail-hero-icon{width:66px;height:66px;border-radius:20px}
  .tool-detail-hero-actions{width:100%}
  .tool-detail-card{border-radius:22px;padding:18px}
  .tool-detail-related-card{flex-basis:220px}
}
@media(max-width:640px){
  .tool-detail-hero-title{font-size:26px}
  .tool-detail-hero-main{align-items:flex-start}
  .tool-detail-visit-btn{flex:1}
}

/* ===== 移动端适配（≤768 / ≤480，只追加、不回归桌面） ===== */
@media(max-width:768px){
  .tool-detail-hero{flex-direction:column;align-items:stretch;gap:18px;padding:20px}
  .tool-detail-hero-main{gap:14px}
  .tool-detail-hero-actions{width:100%}
  .tool-detail-visit-btn{flex:1 1 auto}
  .tool-detail-columns{grid-template-columns:minmax(0,1fr)}
  .tool-detail-related-row{gap:12px;-webkit-overflow-scrolling:touch}
  .tool-detail-related-card{flex-basis:210px}
}
@media(max-width:480px){
  .tool-detail-hero{padding:18px 16px}
  .tool-detail-hero-icon{width:56px;height:56px;border-radius:16px}
  .tool-detail-hero-title{font-size:24px}
  .tool-detail-hero-actions{flex-direction:column;align-items:stretch}
  .tool-detail-hero-actions .cx-button{width:100%}
  .tool-detail-visit-btn{width:100%;min-width:0}
  .tool-detail-card{padding:16px 14px}
  .tool-detail-related-card{flex-basis:180px}
}

/* 展示图 */
.tool-detail-shot {
  margin: 0 0 16px;
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid rgba(126, 160, 198, 0.3);
  background: rgba(255, 255, 255, 0.24);
  position: relative;
}
.tool-detail-shot-image {
  display: block;
  width: 100%;
  max-height: 340px;
  object-fit: cover;
  cursor: zoom-in;
  transition: transform 0.5s cubic-bezier(0.2, 0.7, 0.3, 1);
}
.tool-detail-shot:hover .tool-detail-shot-image { transform: scale(1.03); }
.tool-detail-shot-caption {
  position: absolute;
  right: 10px;
  bottom: 10px;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 13px;
  color: #f3f8ff;
  background: rgba(20, 36, 56, 0.55);
  backdrop-filter: blur(6px);
  pointer-events: none;
}
html.dark .tool-detail-shot {
  border-color: rgba(140, 190, 240, 0.22);
  background: rgba(18, 26, 40, 0.4);
}
.tool-detail-zoom-mask {
  position: fixed;
  inset: 0;
  z-index: 300;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(8, 14, 24, 0.78);
  backdrop-filter: blur(8px);
  cursor: zoom-out;
}
.tool-detail-zoom-image {
  max-width: min(1100px, 94vw);
  max-height: 90vh;
  border-radius: 16px;
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.5);
}
.tool-detail-zoom-enter-active,
.tool-detail-zoom-leave-active { transition: opacity 0.24s ease; }
.tool-detail-zoom-enter-from,
.tool-detail-zoom-leave-to { opacity: 0; }
@media (max-width: 768px) {
  .tool-detail-shot-image { max-height: 220px; }
  .tool-detail-zoom-mask { padding: 12px; }
}
</style>
