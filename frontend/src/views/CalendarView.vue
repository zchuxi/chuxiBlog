<template>
  <main class="calendar-page">
    <div class="calendar-shell">
      <!-- 页头岛 -->
      <section v-reveal="0" class="calendar-hero">
        <p class="calendar-hero-eyebrow">Broadcast Calendar</p>
        <h1 class="calendar-hero-title">每日放送</h1>
        <p class="calendar-hero-sub">来自 Bangumi 的一周放送时间表，已收录进追番小本本的会亮起标记。</p>
        <button class="calendar-back" type="button" @click="router.push('/bangumi')">← 返回番剧记录</button>
      </section>

      <!-- 星期切换 chips -->
      <div v-if="days.length" class="calendar-tabs">
        <button
          v-for="d in days"
          :key="d.weekdayId"
          type="button"
          class="calendar-tab"
          :class="{ active: activeDay === d.weekdayId, 'is-today': d.weekdayId === todayId }"
          @click="activeDay = d.weekdayId"
        >
          {{ d.weekdayCn }}<em v-if="d.weekdayId === todayId">今天</em>
        </button>
      </div>

      <div v-if="loading" class="calendar-state">正在向 Bangumi 打听这周播什么…</div>
      <div v-else-if="!days.length" class="calendar-state">放送表暂时拿不到，可能是网络原因，稍后再来看看吧。</div>

      <!-- 当日放送网格 -->
      <div v-else class="calendar-grid">
        <a
          v-for="item in activeItems"
          :key="item.id"
          class="calendar-card"
          :href="item.localId ? undefined : `https://bgm.tv/subject/${item.id}`"
          :target="item.localId ? undefined : '_blank'"
          rel="noopener"
          @click="item.localId && router.push('/bangumi/' + item.localId)"
        >
          <div class="calendar-cover">
            <img
              v-if="item.cover && !broken.has(item.id)"
              :src="item.cover"
              :alt="item.nameCn || item.name"
              referrerpolicy="no-referrer"
              loading="lazy"
              @error="broken = new Set(broken).add(item.id)"
            />
            <div v-else class="calendar-cover-fallback"><span>{{ (item.nameCn || item.name || '?').charAt(0) }}</span></div>
            <span v-if="item.localId" class="calendar-mine-badge">已收录</span>
            <span v-if="item.score" class="calendar-score-badge">{{ item.score.toFixed(1) }}</span>
          </div>
          <p class="calendar-card-name">{{ item.nameCn || item.name }}</p>
        </a>
      </div>
    </div>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api'

const router = useRouter()

const days = ref([])
const activeDay = ref(1)
const loading = ref(true)
const broken = ref(new Set())
// 本站 subjectId -> 本地记录 id，用于「已收录」标记与跳转本站详情
const mine = ref(new Map())

// bgm 的 weekday.id：1=周一 … 7=周日；JS getDay()：0=周日
const todayId = (() => {
  const d = new Date().getDay()
  return d === 0 ? 7 : d
})()

const activeItems = computed(() => {
  const day = days.value.find(d => d.weekdayId === activeDay.value)
  return day ? day.items : []
})

function normalizeDay(raw) {
  return {
    weekdayId: raw.weekday?.id ?? 0,
    weekdayCn: raw.weekday?.cn || raw.weekday?.ja || '?',
    items: (raw.items || []).map(it => ({
      id: it.id,
      name: it.name || '',
      nameCn: it.name_cn || '',
      cover: (it.images && (it.images.common || it.images.large || it.images.grid)) || '',
      score: it.rating && it.rating.score ? Number(it.rating.score) : null,
      localId: mine.value.get(Number(it.id)) || null
    }))
  }
}

onMounted(async () => {
  // 先拉本站记录做「已收录」映射，失败不阻塞放送表
  try {
    const list = (await api.bangumiRecords()) || []
    mine.value = new Map(list.filter(r => r.subjectId).map(r => [Number(r.subjectId), r.id]))
  } catch {
    mine.value = new Map()
  }
  try {
    const res = await fetch('https://api.bgm.tv/calendar')
    if (!res.ok) throw new Error(String(res.status))
    const data = await res.json()
    days.value = (Array.isArray(data) ? data : []).map(normalizeDay).filter(d => d.weekdayId)
    activeDay.value = days.value.some(d => d.weekdayId === todayId) ? todayId : days.value[0]?.weekdayId || 1
  } catch {
    days.value = []
  } finally {
    loading.value = false
  }
})
</script>

<style>
/* ===== 每日放送页（calendar- 前缀，非 scoped） ===== */
.calendar-page {
  min-height: 100%;
  /* 顶栏悬浮覆盖，全局已留位，页面不再叠加大留白 */
  padding: 10px 20px 72px;
}
.calendar-shell {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.calendar-hero {
  position: relative;
  padding: 30px 32px 26px;
  border: 1px solid var(--card-border);
  border-radius: 28px;
  background: var(--nested-outer-card-bg);
  box-shadow: var(--nested-outer-card-shadow);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
}
.calendar-hero-eyebrow {
  font-size: 13px;
  letter-spacing: 0.26em;
  text-transform: uppercase;
  color: var(--accent-text);
  opacity: 0.75;
}
.calendar-hero-title {
  margin-top: 6px;
  font-size: clamp(29px, 4.6vw, 37px);
  font-weight: 700;
  color: var(--text-color);
}
.calendar-hero-sub {
  margin-top: 8px;
  font-size: 15.5px;
  line-height: 1.7;
  color: var(--text-color);
  opacity: 0.72;
}
.calendar-back {
  margin-top: 14px;
  padding: 7px 16px;
  border: 1px solid var(--accent-border);
  border-radius: 999px;
  background: var(--card-bg);
  color: var(--accent-text);
  font-family: inherit;
  font-size: 14.5px;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.calendar-back:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 18px var(--accent-glow);
}

.calendar-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.calendar-tab {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 18px;
  border: 1px solid var(--accent-border);
  border-radius: 999px;
  background: var(--card-bg);
  color: var(--accent-text);
  font-family: inherit;
  font-size: 14.5px;
  cursor: pointer;
  transition: transform 0.2s ease, background 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
}
.calendar-tab em {
  font-style: normal;
  font-size: 12px;
  padding: 1px 8px;
  border-radius: 999px;
  background: rgba(233, 138, 88, 0.16);
  color: #d97742;
}
.calendar-tab:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 18px var(--accent-glow);
}
.calendar-tab.active {
  background: var(--accent-solid);
  border-color: var(--accent-solid);
  color: #fff;
  box-shadow: 0 10px 22px var(--accent-glow);
}
.calendar-tab.active em {
  background: rgba(255, 255, 255, 0.24);
  color: #fff;
}

.calendar-state {
  padding: 56px 20px;
  text-align: center;
  font-size: 15.5px;
  color: var(--text-color);
  opacity: 0.66;
  border: 1px dashed var(--accent-border);
  border-radius: 24px;
  background: var(--nested-middle-card-bg);
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(148px, 1fr));
  gap: 16px;
}
.calendar-card {
  display: block;
  border: 1px solid var(--card-border);
  border-radius: 18px;
  background: var(--card-bg);
  box-shadow: var(--nested-middle-card-shadow);
  overflow: hidden;
  cursor: pointer;
  text-decoration: none;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}
.calendar-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 18px 36px var(--accent-glow), var(--nested-middle-card-shadow);
}
.calendar-cover {
  position: relative;
  aspect-ratio: 2 / 3;
  overflow: hidden;
}
.calendar-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.calendar-cover-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(150deg, rgba(122, 176, 230, 0.4), rgba(124, 214, 192, 0.36));
  color: #fff;
  font-size: 32px;
  font-weight: 700;
}
.calendar-mine-badge {
  position: absolute;
  top: 8px;
  left: 8px;
  padding: 3px 10px;
  border-radius: 999px;
  background: rgba(16, 145, 118, 0.88);
  color: #fff;
  font-size: 12px;
}
.calendar-score-badge {
  position: absolute;
  right: 8px;
  bottom: 8px;
  padding: 2px 9px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.62);
  color: #ffd479;
  font-size: 12.5px;
  font-weight: 700;
}
.calendar-card-name {
  padding: 10px 12px 12px;
  font-size: 14px;
  line-height: 1.45;
  color: var(--text-color);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 移动端 */
@media (max-width: 768px) {
  .calendar-page {
    padding: 6px 14px 56px;
  }
  .calendar-hero {
    padding: 24px 20px 20px;
  }
  .calendar-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 12px;
  }
}
@media (max-width: 480px) {
  .calendar-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .calendar-tab {
    padding: 7px 14px;
  }
}
</style>
