<template>
  <main class="bangumi-page">
    <div class="bangumi-shell">
      <!-- 顶部 hero 岛 -->
      <section v-reveal="0" class="bangumi-hero">
        <p class="bangumi-hero-eyebrow">Bangumi Tracker</p>
        <h1 class="bangumi-hero-title">{{ bangumiConfig?.title || DEFAULT_BANGUMI.title }}</h1>
        <p class="bangumi-hero-sub">{{ bangumiConfig?.subtitle || DEFAULT_BANGUMI.subtitle }}</p>
        <div class="bangumi-hero-stats">
          <article class="bangumi-stat-card"><span>在看</span><strong>{{ countBy('在看') }}</strong></article>
          <article class="bangumi-stat-card"><span>看过</span><strong>{{ countBy('看过') }}</strong></article>
          <article class="bangumi-stat-card"><span>想看</span><strong>{{ countBy('想看') }}</strong></article>
          <article class="bangumi-stat-card bangumi-stat-wide">
            <span>总集进度 · {{ watchedTotal }} / {{ epsTotal }} 话</span>
            <div class="bangumi-bar"><i :style="{ width: totalPercent + '%' }"></i></div>
          </article>
        </div>
      </section>

      <!-- 状态筛选 chips + 每日放送入口 -->
      <div class="bangumi-filter">
        <button
          v-for="s in FILTERS"
          :key="s"
          type="button"
          class="bangumi-chip"
          :class="{ active: activeFilter === s }"
          @click="activeFilter = s"
        >
          {{ s }}
        </button>
        <RouterLink class="bangumi-chip bangumi-calendar-link" to="/calendar">每日放送 →</RouterLink>
      </div>

      <!-- 去重提示：与参考站一致，同一 bgm 条目只展示一条 -->
      <p v-if="hiddenCount > 0" class="bangumi-dedup-note">
        已隐藏 {{ hiddenCount }} 条按 Bangumi subject 重复的番剧，仅在管理后台可见。
      </p>

      <!-- 列表 -->
      <div v-if="loading" class="bangumi-state">追番小本本翻页中…</div>
      <div v-else-if="filtered.length === 0" class="bangumi-state">
        {{ bangumiConfig?.emptyText || DEFAULT_BANGUMI.emptyText }}
      </div>
      <div v-else class="bangumi-grid">
        <article
          v-for="(r, i) in pagedList"
          :key="r.id"
          v-reveal="(i % 6) * 60"
          class="bangumi-card"
          @click="openDetail(r)"
        >
          <div class="bangumi-cover">
            <img
              v-if="hasCover(r)"
              :src="r.coverUrl"
              :alt="r.nameCn || r.name"
              referrerpolicy="no-referrer"
              loading="lazy"
              @error="markBroken(r)"
            />
            <div v-else class="bangumi-cover-fallback" :class="gradClass(r)">
              <span>{{ initialOf(r) }}</span>
            </div>
            <span class="bangumi-status-badge" :class="statusClass(r.status)">{{ normStatus(r.status) }}</span>
          </div>
          <div class="bangumi-card-body">
            <h3 class="bangumi-card-title">{{ r.nameCn || r.name }}</h3>
            <p class="bangumi-card-origin">{{ r.name }}</p>
            <div class="bangumi-progress-row">
              <div class="bangumi-bar"><i :style="{ width: percentOf(r) + '%' }"></i></div>
              <span class="bangumi-progress-text">{{ r.watchedEps || 0 }}/{{ r.totalEps || '?' }}</span>
            </div>
            <div class="bangumi-meta-row">
              <span v-if="r.rating != null" class="bangumi-stars" :title="`个人评分 ${r.rating}/10`">
                {{ starsOf(r.rating) }} <b>{{ r.rating }}</b>
              </span>
              <span v-else class="bangumi-stars is-empty">未评分</span>
              <span v-if="r.score != null" class="bangumi-score-badge">bgm {{ fmtScore(r.score) }}</span>
            </div>
            <p v-if="r.summary" class="bangumi-card-summary">{{ r.summary }}</p>
          </div>
        </article>
      </div>

      <!-- 分页：岛屿胶囊风，与筛选条同语言；仅多于一页时展示 -->
      <nav v-if="totalPages > 1" class="bangumi-pager">
        <button type="button" class="bangumi-chip" :disabled="pageNo === 1" @click="gotoPage(pageNo - 1)">
          ← 上一页
        </button>
        <template v-for="(item, i) in pageItems" :key="i">
          <span v-if="item === '…'" class="bangumi-pager-ellipsis">…</span>
          <button
            v-else
            type="button"
            class="bangumi-chip bangumi-pager-num"
            :class="{ active: item === pageNo }"
            @click="gotoPage(item)"
          >
            {{ item }}
          </button>
        </template>
        <button type="button" class="bangumi-chip" :disabled="pageNo === totalPages" @click="gotoPage(pageNo + 1)">
          下一页 →
        </button>
        <span class="bangumi-pager-info">共 {{ filtered.length }} 部 · {{ pageNo }}/{{ totalPages }} 页</span>
      </nav>
    </div>
  </main>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { api } from '../api'

const DEFAULT_BANGUMI = { title: '番剧记录', subtitle: '追番进度与收藏一览，记录每一段屏幕里的故事。', emptyText: '这里还空空的，快去收录第一部番剧吧。' }
const bangumiConfig = ref(null)

const FILTERS = ['全部', '在看', '想看', '看过', '搁置', '弃番']
const GRAD_COUNT = 5

const router = useRouter()
const records = ref([])
const loading = ref(true)
const activeFilter = ref('全部')
// 加载失败的封面 id 集合，回退到渐变占位
const broken = ref(new Set())

// 旧数据的“看完”归一到“看过”
const normStatus = s => (s === '看完' ? '看过' : s || '想看')

// 同一 bgm 条目只展示最早收录的一条（无 subjectId 的不参与去重），与参考站行为对齐
const deduped = computed(() => {
  const seen = new Set()
  return records.value.filter(r => {
    const sid = Number(r.subjectId)
    if (!sid) return true
    if (seen.has(sid)) return false
    seen.add(sid)
    return true
  })
})
const hiddenCount = computed(() => records.value.length - deduped.value.length)

const filtered = computed(() =>
  activeFilter.value === '全部' ? deduped.value : deduped.value.filter(r => normStatus(r.status) === activeFilter.value)
)

// 前端分页：每页 10 部（两排）；切筛选回第一页，数据变少时钉回合法页
const PAGE_SIZE = 10
const pageNo = ref(1)
const totalPages = computed(() => Math.max(1, Math.ceil(filtered.value.length / PAGE_SIZE)))
const pagedList = computed(() => filtered.value.slice((pageNo.value - 1) * PAGE_SIZE, pageNo.value * PAGE_SIZE))
const pageItems = computed(() => {
  const total = totalPages.value
  const cur = pageNo.value
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1)
  const items = [1]
  if (cur > 3) items.push('…')
  for (let p = Math.max(2, cur - 1); p <= Math.min(total - 1, cur + 1); p++) items.push(p)
  if (cur < total - 2) items.push('…')
  items.push(total)
  return items
})

function gotoPage(p) {
  const next = Math.min(Math.max(1, p), totalPages.value)
  if (next === pageNo.value) return
  pageNo.value = next
  // 翻页后滚回筛选条（scroll-margin 已给悬浮顶栏留位）
  document.querySelector('.bangumi-filter')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

watch(activeFilter, () => {
  pageNo.value = 1
})
watch(totalPages, () => {
  if (pageNo.value > totalPages.value) pageNo.value = totalPages.value
})

const watchedTotal = computed(() => deduped.value.reduce((s, r) => s + (r.watchedEps || 0), 0))
const epsTotal = computed(() => deduped.value.reduce((s, r) => s + (r.totalEps || 0), 0))
const totalPercent = computed(() => (epsTotal.value > 0 ? Math.min(100, Math.round((watchedTotal.value / epsTotal.value) * 100)) : 0))

const countBy = s => deduped.value.filter(r => normStatus(r.status) === s).length
const hasCover = r => !!r.coverUrl && !broken.value.has(r.id)
const initialOf = r => (r.nameCn || r.name || '?').trim().charAt(0)
const gradClass = r => `bangumi-grad-${Number(r.id || 0) % GRAD_COUNT}`
const percentOf = r => (r.totalEps > 0 ? Math.min(100, Math.round(((r.watchedEps || 0) / r.totalEps) * 100)) : 0)
const fmtScore = v => Number(v).toFixed(1)

// 10 分制 → 5 星展示（实心+空心），与参考站星级样式对齐
const starsOf = rating => {
  const full = Math.max(0, Math.min(5, Math.round(Number(rating) / 2)))
  return '★'.repeat(full) + '☆'.repeat(5 - full)
}

function statusClass(status) {
  const s = normStatus(status)
  if (s === '在看') return 'is-watching'
  if (s === '看过') return 'is-done'
  if (s === '搁置') return 'is-hold'
  if (s === '弃番') return 'is-drop'
  return 'is-wish'
}

function markBroken(r) {
  broken.value = new Set(broken.value).add(r.id)
}

function openDetail(r) {
  router.push('/bangumi/' + r.id)
}

onMounted(async () => {
  try {
    records.value = (await api.bangumiRecords()) || []
  } catch {
    /* 后端未启动时保持空态 */
  } finally {
    loading.value = false
  }
  try {
    bangumiConfig.value = await api.siteContent('bangumi-hero')
  } catch { /* 使用默认值 */ }
})
</script>

<style>
/* ===== 番剧记录页（bangumi- 前缀，非 scoped） ===== */
.bangumi-page {
  min-height: 100%;
  /* 顶栏已改悬浮覆盖（.app-shell-main 全局 padding-top 82px 让位），
     页面自身不再叠加大留白 */
  padding: 10px 20px 72px;
}
.bangumi-shell {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 26px;
}

/* hero 岛 */
.bangumi-hero {
  padding: 30px 32px 26px;
  border: 1px solid var(--card-border);
  border-radius: 28px;
  background: var(--nested-outer-card-bg);
  box-shadow: var(--nested-outer-card-shadow);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
}
.bangumi-hero-eyebrow {
  font-size: 13px;
  letter-spacing: 0.26em;
  text-transform: uppercase;
  color: var(--accent-text);
  opacity: 0.75;
}
.bangumi-hero-title {
  margin-top: 6px;
  font-size: clamp(29px, 4.6vw, 37px);
  font-weight: 700;
  color: var(--text-color);
}
.bangumi-hero-sub {
  margin-top: 8px;
  font-size: 15.5px;
  line-height: 1.7;
  color: var(--text-color);
  opacity: 0.72;
}
.bangumi-hero-stats {
  margin-top: 18px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr)) minmax(0, 2fr);
  gap: 12px;
}
.bangumi-stat-card {
  padding: 12px 16px;
  border: 1px solid var(--nested-inner-card-border);
  border-radius: 18px;
  background: var(--nested-inner-card-bg);
  box-shadow: var(--nested-inner-card-shadow);
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.bangumi-stat-card span {
  font-size: 13px;
  color: var(--text-color);
  opacity: 0.62;
}
.bangumi-stat-card strong {
  font-size: 24px;
  color: var(--accent-text);
}
.bangumi-stat-wide {
  justify-content: center;
}

/* 通用进度条 */
.bangumi-bar {
  flex: 1;
  height: 8px;
  border-radius: 999px;
  background: rgba(122, 176, 230, 0.22);
  overflow: hidden;
}
.bangumi-bar i {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--accent-solid), #7cd6c0);
  transition: width 0.5s cubic-bezier(0.22, 1, 0.36, 1);
}
html.dark .bangumi-bar {
  background: rgba(148, 163, 184, 0.18);
}

/* 筛选 chips：收进一条半透明岛屿条，与参考站的胶囊筛选栏对齐 */
.bangumi-filter {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border: 1px solid var(--card-border);
  border-radius: 999px;
  background: var(--nested-outer-card-bg);
  box-shadow: var(--nested-outer-card-shadow);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  /* 翻页回滚时给悬浮顶栏留位 */
  scroll-margin-top: 86px;
}
.bangumi-chip {
  padding: 7px 18px;
  border: 1px solid var(--accent-border);
  border-radius: 999px;
  background: var(--card-bg);
  color: var(--accent-text);
  font-family: inherit;
  font-size: 14.5px;
  transition: transform 0.2s ease, background 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
}
.bangumi-chip:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 18px var(--accent-glow);
}
.bangumi-chip.active {
  background: var(--accent-solid);
  border-color: var(--accent-solid);
  color: #fff;
  box-shadow: 0 10px 22px var(--accent-glow);
}
.bangumi-calendar-link {
  margin-left: auto;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
}

/* 分页条：与筛选条同款岛屿胶囊 */
.bangumi-pager {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 10px 16px;
  align-self: center;
  border: 1px solid var(--card-border);
  border-radius: 999px;
  background: var(--nested-outer-card-bg);
  box-shadow: var(--nested-outer-card-shadow);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
}
.bangumi-pager .bangumi-chip:disabled {
  opacity: 0.4;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}
.bangumi-pager-num {
  min-width: 40px;
  padding: 7px 0;
  text-align: center;
}
.bangumi-pager-ellipsis {
  color: var(--text-color);
  opacity: 0.5;
  padding: 0 2px;
}
.bangumi-pager-info {
  margin-left: 6px;
  font-size: 13.5px;
  color: var(--text-color);
  opacity: 0.65;
  white-space: nowrap;
}

/* 去重提示（与参考站一致的半透明胶囊注释行） */
.bangumi-dedup-note {
  align-self: flex-start;
  margin: -8px 0 0;
  padding: 6px 16px;
  border: 1px solid var(--card-border);
  border-radius: 999px;
  background: var(--nested-middle-card-bg);
  font-size: 13.5px;
  color: var(--text-color);
  opacity: 0.72;
}

/* 状态区 */
.bangumi-state {
  padding: 56px 20px;
  text-align: center;
  font-size: 15.5px;
  color: var(--text-color);
  opacity: 0.66;
  border: 1px dashed var(--accent-border);
  border-radius: 24px;
  background: var(--nested-middle-card-bg);
}

/* 封面卡网格：大海报优先，对齐参考站的五列大图布局 */
.bangumi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(196px, 1fr));
  gap: 20px;
}
.bangumi-card {
  border: 1px solid var(--card-border);
  border-radius: 20px;
  background: var(--card-bg);
  box-shadow: var(--nested-middle-card-shadow);
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}
.bangumi-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 22px 44px var(--accent-glow), var(--nested-middle-card-shadow);
}

/* 2:3 封面与渐变占位 */
.bangumi-cover {
  position: relative;
  aspect-ratio: 2 / 3;
  overflow: hidden;
}
.bangumi-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.bangumi-cover-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.bangumi-cover-fallback span {
  font-size: 57px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.92);
  text-shadow: 0 4px 14px rgba(15, 23, 42, 0.28);
}
.bangumi-grad-0 { background: linear-gradient(150deg, #7fb4e8, #7cd6c0); }
.bangumi-grad-1 { background: linear-gradient(150deg, #f2a6c0, #9fb8f5); }
.bangumi-grad-2 { background: linear-gradient(150deg, #f5c98d, #ef9f9f); }
.bangumi-grad-3 { background: linear-gradient(150deg, #9d9ff0, #79c3ec); }
.bangumi-grad-4 { background: linear-gradient(150deg, #86d0a3, #b7d97f); }
html.dark .bangumi-grad-0 { background: linear-gradient(150deg, #3d5f80, #38695f); }
html.dark .bangumi-grad-1 { background: linear-gradient(150deg, #7a4d60, #4d5c85); }
html.dark .bangumi-grad-2 { background: linear-gradient(150deg, #7f6a48, #7a5252); }
html.dark .bangumi-grad-3 { background: linear-gradient(150deg, #52538a, #3d6580); }
html.dark .bangumi-grad-4 { background: linear-gradient(150deg, #44684f, #5c6d40); }

/* 状态角标：左上角，与参考站一致 */
.bangumi-status-badge {
  position: absolute;
  top: 10px;
  left: 10px;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 13px;
  color: #fff;
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.2);
}
.bangumi-status-badge.is-watching { background: rgba(63, 119, 181, 0.88); }
.bangumi-status-badge.is-done { background: rgba(16, 145, 118, 0.85); }
.bangumi-status-badge.is-wish { background: rgba(233, 138, 88, 0.88); }
.bangumi-status-badge.is-hold { background: rgba(148, 128, 92, 0.85); }
.bangumi-status-badge.is-drop { background: rgba(140, 92, 108, 0.85); }

/* 卡片信息区 */
.bangumi-card-body {
  padding: 12px 14px 14px;
  display: flex;
  flex-direction: column;
  gap: 7px;
}
.bangumi-card-title {
  font-size: 15.5px;
  font-weight: 700;
  color: var(--text-color);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.bangumi-card-origin {
  font-size: 12px;
  color: var(--text-color);
  opacity: 0.55;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.bangumi-progress-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.bangumi-progress-text {
  font-size: 12px;
  color: var(--text-color);
  opacity: 0.65;
  flex-shrink: 0;
}
.bangumi-meta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  min-height: 18px;
}
.bangumi-stars {
  font-size: 12px;
  letter-spacing: 1px;
  color: #f0a742;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.bangumi-stars b {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0;
  color: var(--text-color);
  opacity: 0.72;
}
.bangumi-stars.is-empty {
  color: var(--text-color);
  opacity: 0.4;
  font-size: 12px;
  letter-spacing: 0;
}
.bangumi-score-badge {
  flex-shrink: 0;
  padding: 2px 8px;
  border: 1px solid var(--accent-border);
  border-radius: 999px;
  font-size: 12px;
  color: var(--accent-text);
  background: var(--nested-inner-card-bg);
}

/* 简介摘要：两行截断，对齐参考站卡片底部描述 */
.bangumi-card-summary {
  margin: 0;
  font-size: 12.5px;
  line-height: 1.6;
  color: var(--text-color);
  opacity: 0.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 响应式 */
@media (max-width: 720px) {
  .bangumi-page {
    padding: 6px 14px 56px;
  }
  .bangumi-hero {
    padding: 24px 20px 20px;
  }
  .bangumi-hero-stats {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
  .bangumi-stat-wide {
    grid-column: 1 / -1;
  }
  .bangumi-grid {
    grid-template-columns: repeat(auto-fill, minmax(136px, 1fr));
    gap: 12px;
  }
}

/* ===== 移动端适配（≤768 / ≤480，只追加、不回归桌面） ===== */
@media (max-width: 768px) {
  .bangumi-page {
    padding: 6px 14px 56px;
  }
  .bangumi-hero {
    padding: 24px 20px 20px;
  }
  .bangumi-hero-stats {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
  .bangumi-stat-wide {
    grid-column: 1 / -1;
  }
  .bangumi-filter {
    border-radius: 24px;
    padding: 10px 12px;
  }
  .bangumi-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 12px;
  }
  .bangumi-chip {
    min-height: 40px;
    padding: 8px 16px;
  }
  .bangumi-card-summary {
    display: none;
  }
}
@media (max-width: 480px) {
  .bangumi-hero {
    padding: 20px 16px 18px;
  }
  .bangumi-hero-stats {
    gap: 8px;
  }
  .bangumi-stat-card {
    padding: 10px 12px;
  }
  .bangumi-stat-card strong {
    font-size: 20px;
  }
  .bangumi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .bangumi-card-body {
    padding: 10px 12px 12px;
  }
}
</style>
