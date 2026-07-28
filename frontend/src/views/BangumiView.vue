<template>
  <main class="bangumi-page">
    <div class="bangumi-shell">
      <!-- 顶部 hero 岛 -->
      <section v-reveal="0" class="bangumi-hero">
        <p class="bangumi-hero-eyebrow">Bangumi Tracker</p>
        <h1 class="bangumi-hero-title">番剧记录</h1>
        <p class="bangumi-hero-sub">追番进度、个人打分与一句话观后感，都摊在这张小桌上慢慢看。</p>
        <div class="bangumi-hero-stats">
          <article class="bangumi-stat-card"><span>在看</span><strong>{{ countBy('在看') }}</strong></article>
          <article class="bangumi-stat-card"><span>看完</span><strong>{{ countBy('看完') }}</strong></article>
          <article class="bangumi-stat-card"><span>想看</span><strong>{{ countBy('想看') }}</strong></article>
          <article class="bangumi-stat-card bangumi-stat-wide">
            <span>总集进度 · {{ watchedTotal }} / {{ epsTotal }} 话</span>
            <div class="bangumi-bar"><i :style="{ width: totalPercent + '%' }"></i></div>
          </article>
        </div>
      </section>

      <!-- 状态筛选 chips -->
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
      </div>

      <!-- 列表 -->
      <div v-if="loading" class="bangumi-state">追番小本本翻页中…</div>
      <div v-else-if="filtered.length === 0" class="bangumi-state">
        这里还空空的，等一部让人心动的番剧住进来吧 ✧
      </div>
      <div v-else class="bangumi-grid">
        <article
          v-for="(r, i) in filtered"
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
            <span class="bangumi-status-badge" :class="statusClass(r.status)">{{ r.status }}</span>
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
                {{ '★'.repeat(r.rating) }}
              </span>
              <span v-else class="bangumi-stars is-empty">未评分</span>
              <span v-if="r.score != null" class="bangumi-score-badge">bgm {{ fmtScore(r.score) }}</span>
            </div>
          </div>
        </article>
      </div>
    </div>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api'

const FILTERS = ['全部', '在看', '看完', '想看']
const GRAD_COUNT = 5

const router = useRouter()
const records = ref([])
const loading = ref(true)
const activeFilter = ref('全部')
// 加载失败的封面 id 集合，回退到渐变占位
const broken = ref(new Set())

const filtered = computed(() =>
  activeFilter.value === '全部' ? records.value : records.value.filter(r => r.status === activeFilter.value)
)

const watchedTotal = computed(() => records.value.reduce((s, r) => s + (r.watchedEps || 0), 0))
const epsTotal = computed(() => records.value.reduce((s, r) => s + (r.totalEps || 0), 0))
const totalPercent = computed(() => (epsTotal.value > 0 ? Math.min(100, Math.round((watchedTotal.value / epsTotal.value) * 100)) : 0))

const countBy = s => records.value.filter(r => r.status === s).length
const hasCover = r => !!r.coverUrl && !broken.value.has(r.id)
const initialOf = r => (r.nameCn || r.name || '?').trim().charAt(0)
const gradClass = r => `bangumi-grad-${Number(r.id || 0) % GRAD_COUNT}`
const percentOf = r => (r.totalEps > 0 ? Math.min(100, Math.round(((r.watchedEps || 0) / r.totalEps) * 100)) : 0)
const fmtScore = v => Number(v).toFixed(1)

function statusClass(status) {
  if (status === '在看') return 'is-watching'
  if (status === '看完') return 'is-done'
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
})
</script>

<style>
/* ===== 番剧记录页（bangumi- 前缀，非 scoped） ===== */
.bangumi-page {
  min-height: 100%;
  padding: 96px 20px 72px;
}
.bangumi-shell {
  max-width: 1080px;
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
  font-size: 12px;
  letter-spacing: 0.26em;
  text-transform: uppercase;
  color: var(--accent-text);
  opacity: 0.75;
}
.bangumi-hero-title {
  margin-top: 6px;
  font-size: clamp(26px, 4.6vw, 34px);
  font-weight: 700;
  color: var(--text-color);
}
.bangumi-hero-sub {
  margin-top: 8px;
  font-size: 14px;
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
  font-size: 12px;
  color: var(--text-color);
  opacity: 0.62;
}
.bangumi-stat-card strong {
  font-size: 22px;
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

/* 筛选 chips */
.bangumi-filter {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.bangumi-chip {
  padding: 7px 18px;
  border: 1px solid var(--accent-border);
  border-radius: 999px;
  background: var(--card-bg);
  color: var(--accent-text);
  font-family: inherit;
  font-size: 13px;
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

/* 状态区 */
.bangumi-state {
  padding: 56px 20px;
  text-align: center;
  font-size: 14px;
  color: var(--text-color);
  opacity: 0.66;
  border: 1px dashed var(--accent-border);
  border-radius: 24px;
  background: var(--nested-middle-card-bg);
}

/* 封面卡网格 */
.bangumi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(168px, 1fr));
  gap: 18px;
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
  font-size: 52px;
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

/* 状态角标 */
.bangumi-status-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 12px;
  color: #fff;
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.2);
}
.bangumi-status-badge.is-watching { background: rgba(63, 119, 181, 0.88); }
.bangumi-status-badge.is-done { background: rgba(16, 145, 118, 0.85); }
.bangumi-status-badge.is-wish { background: rgba(233, 138, 88, 0.88); }

/* 卡片信息区 */
.bangumi-card-body {
  padding: 12px 14px 14px;
  display: flex;
  flex-direction: column;
  gap: 7px;
}
.bangumi-card-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-color);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.bangumi-card-origin {
  font-size: 11px;
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
  font-size: 11px;
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
  font-size: 10px;
  letter-spacing: 1px;
  color: #f0a742;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.bangumi-stars.is-empty {
  color: var(--text-color);
  opacity: 0.4;
  font-size: 11px;
  letter-spacing: 0;
}
.bangumi-score-badge {
  flex-shrink: 0;
  padding: 2px 8px;
  border: 1px solid var(--accent-border);
  border-radius: 999px;
  font-size: 11px;
  color: var(--accent-text);
  background: var(--nested-inner-card-bg);
}

/* 响应式 */
@media (max-width: 720px) {
  .bangumi-page {
    padding: 84px 14px 56px;
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
</style>
