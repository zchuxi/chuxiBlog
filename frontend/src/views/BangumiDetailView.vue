<template>
  <main class="bangumi-detail-page">
    <div class="bangumi-detail-shell">
      <!-- 返回入口 -->
      <button class="bangumi-detail-back" type="button" @click="router.push('/bangumi')">
        ← 返回番剧记录
      </button>

      <div v-if="loading" class="bangumi-detail-state">正在翻开这一页追番小本本…</div>
      <div v-else-if="!record" class="bangumi-detail-state">没有找到这条番剧记录，它可能被收进箱底啦。</div>

      <template v-else>
        <!-- HERO 岛 -->
        <section v-reveal="0" class="bangumi-detail-hero">
          <div class="bangumi-detail-cover-box">
            <img
              v-if="coverSrc && !coverBroken"
              :src="coverSrc"
              :alt="title"
              referrerpolicy="no-referrer"
              @error="coverBroken = true"
            />
            <div v-else class="bangumi-detail-cover-fallback" :class="gradClass(record)">
              <span>{{ initialOf(record) }}</span>
            </div>
          </div>

          <div class="bangumi-detail-head">
            <span class="bangumi-detail-status" :class="statusClass(record.status)">{{ record.status }}</span>
            <h1 class="bangumi-detail-title">{{ title }}</h1>
            <p v-if="origin && origin !== title" class="bangumi-detail-origin">{{ origin }}</p>

            <div class="bangumi-detail-meta">
              <span v-if="airDate" class="bangumi-detail-fact">放送 {{ airDate }}</span>
              <span v-if="platform" class="bangumi-detail-fact">{{ platform }}</span>
              <span v-if="totalEpsDisplay" class="bangumi-detail-fact">全 {{ totalEpsDisplay }} 话</span>
              <span v-if="rank" class="bangumi-detail-rank">bgm 排名 #{{ rank }}</span>
            </div>

            <div class="bangumi-detail-scores">
              <div v-if="bgmScore != null" class="bangumi-detail-score-big">
                <strong>{{ bgmScore.toFixed(1) }}</strong>
                <span>bgm 均分<template v-if="bgmTotal"> · {{ bgmTotal }} 人评</template></span>
              </div>
              <div class="bangumi-detail-my-rating">
                <span v-if="record.rating != null" class="bangumi-detail-stars">{{ '★'.repeat(record.rating) }}</span>
                <span class="bangumi-detail-fact">{{ record.rating != null ? `我打 ${record.rating}/10` : '还没打分' }}</span>
              </div>
            </div>

            <div class="bangumi-detail-progress">
              <div class="bangumi-detail-bar"><i :style="{ width: percent + '%' }"></i></div>
              <span class="bangumi-detail-progress-text">
                {{ record.watchedEps || 0 }} / {{ totalEpsDisplay || '?' }} 话 · {{ percent }}%
              </span>
            </div>

            <a
              class="bangumi-detail-link"
              :href="`https://bgm.tv/subject/${record.subjectId}`"
              target="_blank"
              rel="noopener"
            >
              去 Bangumi 看看 →
            </a>
          </div>
        </section>

        <!-- 我的记录岛 -->
        <section v-reveal="60" class="bangumi-detail-island">
          <h2 class="bangumi-detail-island-title">我的记录</h2>
          <div class="bangumi-detail-record-grid">
            <article><span>状态</span><strong>{{ record.status || '—' }}</strong></article>
            <article><span>进度</span><strong>{{ record.watchedEps || 0 }} / {{ record.totalEps || '?' }} 话</strong></article>
            <article><span>个人评分</span><strong>{{ record.rating != null ? record.rating + ' / 10' : '未评分' }}</strong></article>
            <article>
              <span>收录 / 更新</span>
              <strong class="is-small">{{ fmtDate(record.createdAt) }} 收录<br />{{ fmtDate(record.updatedAt) }} 更新</strong>
            </article>
          </div>
        </section>

        <!-- 简介岛 -->
        <section v-if="summaryText || tagList.length" v-reveal="80" class="bangumi-detail-island">
          <h2 class="bangumi-detail-island-title">简介</h2>
          <p v-if="summaryText" class="bangumi-detail-summary">{{ summaryText }}</p>
          <div v-if="tagList.length" class="bangumi-detail-tags">
            <span v-for="t in tagList" :key="t" class="bangumi-detail-tag"># {{ t }}</span>
          </div>
        </section>

        <!-- 评分分布岛（在线数据） -->
        <section v-if="ratingDist" v-reveal="100" class="bangumi-detail-island">
          <h2 class="bangumi-detail-island-title">评分分布</h2>
          <div class="bangumi-detail-rating-flex">
            <div class="bangumi-detail-dist">
              <div
                v-for="row in ratingDist"
                :key="row.score"
                class="bangumi-detail-dist-row"
                :class="{ 'is-top': row.top }"
              >
                <span class="bangumi-detail-dist-label">{{ row.score }}</span>
                <div class="bangumi-detail-dist-bar"><i :style="{ width: row.percent + '%' }"></i></div>
                <span class="bangumi-detail-dist-count">{{ row.count }}<em v-if="row.top">最高</em></span>
              </div>
            </div>
            <div v-if="collectionBox" class="bangumi-detail-collect">
              <article v-for="c in collectionBox" :key="c.label">
                <span>{{ c.label }}</span>
                <strong>{{ c.value }}</strong>
              </article>
            </div>
          </div>
        </section>

        <!-- 角色 & 声优岛（在线数据） -->
        <section v-if="charactersShown.length" v-reveal="120" class="bangumi-detail-island">
          <h2 class="bangumi-detail-island-title">角色 &amp; 声优</h2>
          <div class="bangumi-detail-char-grid">
            <article v-for="c in charactersShown" :key="c.id" class="bangumi-detail-char-card">
              <div class="bangumi-detail-char-avatar">
                <img
                  v-if="c.img && !brokenImgs.has('c' + c.id)"
                  :src="c.img"
                  :alt="c.name"
                  referrerpolicy="no-referrer"
                  loading="lazy"
                  @error="markBroken('c' + c.id)"
                />
                <span v-else>{{ c.name.charAt(0) }}</span>
              </div>
              <div class="bangumi-detail-char-info">
                <p class="bangumi-detail-char-name">
                  {{ c.name }}<em v-if="c.relation">{{ c.relation }}</em>
                </p>
                <p v-if="c.cv" class="bangumi-detail-char-cv">CV {{ c.cv }}</p>
              </div>
            </article>
          </div>
        </section>

        <!-- 分集岛（在线数据） -->
        <section v-if="episodeCells.length" v-reveal="140" class="bangumi-detail-island">
          <h2 class="bangumi-detail-island-title">分集</h2>
          <p v-if="episodesTotal > 100" class="bangumi-detail-note">共 {{ episodesTotal }} 话，仅显示前 100 话</p>
          <div class="bangumi-detail-ep-grid">
            <span
              v-for="e in episodeCells"
              :key="e.id"
              class="bangumi-detail-ep"
              :class="{ 'is-watched': e.watched }"
              :title="e.title"
            >
              {{ e.sort }}
            </span>
          </div>
        </section>

        <!-- STAFF 岛（在线 infobox） -->
        <section v-if="staffRows.length" v-reveal="160" class="bangumi-detail-island">
          <h2 class="bangumi-detail-island-title">STAFF</h2>
          <dl class="bangumi-detail-staff">
            <div v-for="s in staffRows" :key="s.key" class="bangumi-detail-staff-row">
              <dt>{{ s.key }}</dt>
              <dd>{{ s.value }}</dd>
            </div>
          </dl>
        </section>

        <!-- 更多记录 -->
        <section v-if="others.length" v-reveal="180" class="bangumi-detail-island">
          <h2 class="bangumi-detail-island-title">更多记录</h2>
          <div class="bangumi-detail-more">
            <article
              v-for="r in others"
              :key="r.id"
              class="bangumi-detail-more-card"
              @click="router.push('/bangumi/' + r.id)"
            >
              <div class="bangumi-detail-more-cover">
                <img
                  v-if="r.coverUrl && !brokenImgs.has('m' + r.id)"
                  :src="r.coverUrl"
                  :alt="r.nameCn || r.name"
                  referrerpolicy="no-referrer"
                  loading="lazy"
                  @error="markBroken('m' + r.id)"
                />
                <div v-else class="bangumi-detail-cover-fallback" :class="gradClass(r)">
                  <span>{{ initialOf(r) }}</span>
                </div>
              </div>
              <p class="bangumi-detail-more-name">{{ r.nameCn || r.name }}</p>
            </article>
          </div>
        </section>
      </template>
    </div>
  </main>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../api'

const route = useRoute()
const router = useRouter()

const GRAD_COUNT = 5
const BGM_API = 'https://api.bgm.tv'
// STAFF 岛按此优先级挑 infobox 常见键
const STAFF_KEYS = ['原作', '导演', '系列构成', '脚本', '人物设定', '音乐', '动画制作', '总作画监督', '美术监督', '音响监督', '角色原案', '制片人']

const loading = ref(true)
const record = ref(null)
const subject = ref(null)
const episodes = ref([])
const episodesTotal = ref(0)
const characters = ref([])
const others = ref([])
const coverBroken = ref(false)
const brokenImgs = ref(new Set())

const title = computed(() => record.value?.nameCn || record.value?.name || '未知番剧')
const origin = computed(() => record.value?.name || '')
const coverSrc = computed(() => subject.value?.images?.large || record.value?.coverUrl || '')
const airDate = computed(() => subject.value?.date || record.value?.airDate || '')
const platform = computed(() => subject.value?.platform || record.value?.platform || '')
const rank = computed(() => subject.value?.rating?.rank || record.value?.rank || null)
const totalEpsDisplay = computed(
  () => subject.value?.total_episodes || subject.value?.eps || record.value?.totalEps || 0
)

const bgmScore = computed(() => {
  const online = subject.value?.rating?.score
  if (online != null && Number(online) > 0) return Number(online)
  const local = record.value?.score
  return local != null && Number(local) > 0 ? Number(local) : null
})
const bgmTotal = computed(() => subject.value?.rating?.total || record.value?.ratingTotal || 0)

const percent = computed(() => {
  const total = Number(totalEpsDisplay.value)
  const watched = Number(record.value?.watchedEps || 0)
  return total > 0 ? Math.min(100, Math.round((watched / total) * 100)) : 0
})

const summaryText = computed(() => (subject.value?.summary || '').trim() || record.value?.summary || '')

const tagList = computed(() => {
  const online = subject.value?.tags
  if (Array.isArray(online) && online.length) return online.map(t => t?.name).filter(Boolean).slice(0, 10)
  const raw = record.value?.tags
  if (Array.isArray(raw)) return raw.slice(0, 10)
  if (typeof raw === 'string' && raw.trim()) return raw.split(',').map(s => s.trim()).filter(Boolean).slice(0, 10)
  return []
})

// 1-10 分分布，标出最高档
const ratingDist = computed(() => {
  const count = subject.value?.rating?.count
  if (!count) return null
  const rows = []
  let max = 0
  for (let s = 10; s >= 1; s--) {
    const c = Number(count[String(s)] || 0)
    if (c > max) max = c
    rows.push({ score: s, count: c })
  }
  if (max <= 0) return null
  rows.forEach(r => {
    r.percent = Math.round((r.count / max) * 100)
    r.top = r.count === max
  })
  return rows
})

const collectionBox = computed(() => {
  const c = subject.value?.collection
  if (!c) return null
  return [
    { label: '想看', value: c.wish ?? 0 },
    { label: '在看', value: c.doing ?? 0 },
    { label: '看完', value: c.collect ?? 0 },
    { label: '搁置', value: c.on_hold ?? 0 },
    { label: '抛弃', value: c.dropped ?? 0 }
  ]
})

// 主角优先，最多 12 个
const charactersShown = computed(() => {
  const list = Array.isArray(characters.value) ? characters.value : []
  const weight = r => (r === '主角' ? 0 : r === '配角' ? 1 : 2)
  return [...list]
    .sort((a, b) => weight(a?.relation) - weight(b?.relation))
    .slice(0, 12)
    .map(c => ({
      id: c.id,
      name: c.name || '?',
      relation: c.relation || '',
      img: c.images?.grid || c.images?.medium || '',
      cv: (c.actors || []).map(a => a?.name).filter(Boolean).join(' / ')
    }))
})

const episodeCells = computed(() => {
  const watched = Number(record.value?.watchedEps || 0)
  return episodes.value.slice(0, 100).map((e, i) => {
    const no = e.ep ?? e.sort
    return {
      id: e.id ?? `ep-${i}`,
      sort: e.sort ?? no ?? i + 1,
      watched: no != null && Number(no) > 0 && Number(no) <= watched,
      title: `第${e.sort ?? no}话 ${e.name_cn || e.name || ''}${e.airdate ? ' / ' + e.airdate : ''}`.trim()
    }
  })
})

// infobox value 兼容字符串与 [{v}] 数组
const staffRows = computed(() => {
  const box = subject.value?.infobox
  if (!Array.isArray(box)) return []
  const norm = v => (Array.isArray(v) ? v.map(x => x?.v || '').filter(Boolean).join('、') : v || '')
  const rows = []
  for (const key of STAFF_KEYS) {
    const item = box.find(i => i?.key === key)
    if (!item) continue
    const value = norm(item.value)
    if (!value) continue
    rows.push({ key, value })
    if (rows.length >= 8) break
  }
  return rows
})

const initialOf = r => (r?.nameCn || r?.name || '?').trim().charAt(0)
const gradClass = r => `bangumi-detail-grad-${Number(r?.id || 0) % GRAD_COUNT}`
const fmtDate = v => (v ? String(v).slice(0, 10) : '—')

function statusClass(status) {
  const s = status === '看完' ? '看过' : status
  if (s === '在看') return 'is-watching'
  if (s === '看过') return 'is-done'
  if (s === '搁置') return 'is-hold'
  if (s === '弃番') return 'is-drop'
  return 'is-wish'
}

function markBroken(key) {
  brokenImgs.value = new Set(brokenImgs.value).add(key)
}

async function bgmFetch(path) {
  const res = await fetch(`${BGM_API}${path}`)
  if (!res.ok) throw new Error(`bgm ${res.status}`)
  return res.json()
}

async function load(id) {
  loading.value = true
  record.value = null
  subject.value = null
  episodes.value = []
  episodesTotal.value = 0
  characters.value = []
  coverBroken.value = false
  brokenImgs.value = new Set()

  try {
    record.value = await api.bangumiDetail(id)
  } catch (e) {
    console.warn('[番剧] 详情加载失败:', e)
  } finally {
    loading.value = false
  }

  // 底部更多记录（本地列表，失败静默）
  try {
    const list = (await api.bangumiRecords()) || []
    others.value = list.filter(r => String(r.id) !== String(id)).slice(0, 12)
  } catch (e) {
    console.warn('[番剧] 更多记录加载失败:', e)
    others.value = []
  }

  // 在线增强：任一失败对应区块降级隐藏
  const sid = record.value?.subjectId
  if (!sid) return
  const [s, eps, chars] = await Promise.allSettled([
    bgmFetch(`/v0/subjects/${sid}`),
    bgmFetch(`/v0/episodes?subject_id=${sid}&type=0&limit=100&offset=0`),
    bgmFetch(`/v0/subjects/${sid}/characters`)
  ])
  if (s.status === 'fulfilled' && s.value) subject.value = s.value
  if (eps.status === 'fulfilled' && Array.isArray(eps.value?.data)) {
    episodes.value = eps.value.data
    episodesTotal.value = Number(eps.value.total || eps.value.data.length)
  }
  if (chars.status === 'fulfilled' && Array.isArray(chars.value)) characters.value = chars.value
}

onMounted(() => load(route.params.id))

watch(
  () => route.params.id,
  id => {
    if (!id) return
    window.scrollTo({ top: 0 })
    load(id)
  }
)
</script>

<style>
/* ===== 番剧详情页（bangumi-detail- 前缀，非 scoped） ===== */
.bangumi-detail-page {
  min-height: 100%;
  /* 顶栏悬浮覆盖，全局已留位，页面不再叠加大留白 */
  padding: 10px 20px 72px;
}
.bangumi-detail-shell {
  max-width: 1080px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 返回按钮 */
.bangumi-detail-back {
  align-self: flex-start;
  padding: 8px 20px;
  border: 1px solid var(--accent-border);
  border-radius: 999px;
  background: var(--card-bg);
  color: var(--accent-text);
  font-family: inherit;
  font-size: 14.5px;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.bangumi-detail-back:hover {
  transform: translateX(-3px);
  box-shadow: 0 8px 18px var(--accent-glow);
}

/* 状态区 */
.bangumi-detail-state {
  padding: 56px 20px;
  text-align: center;
  font-size: 15.5px;
  color: var(--text-color);
  opacity: 0.66;
  border: 1px dashed var(--accent-border);
  border-radius: 24px;
  background: var(--nested-middle-card-bg);
}

/* HERO 岛 */
.bangumi-detail-hero {
  display: flex;
  gap: 28px;
  padding: 28px;
  border: 1px solid var(--card-border);
  border-radius: 28px;
  background: var(--nested-outer-card-bg);
  box-shadow: var(--nested-outer-card-shadow);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
}
.bangumi-detail-cover-box {
  flex-shrink: 0;
  width: 232px;
  aspect-ratio: 2 / 3;
  border-radius: 18px;
  overflow: hidden;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.24);
  align-self: flex-start;
}
.bangumi-detail-cover-box img,
.bangumi-detail-more-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.bangumi-detail-cover-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.bangumi-detail-cover-fallback span {
  font-size: 62px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.92);
  text-shadow: 0 4px 14px rgba(15, 23, 42, 0.28);
}
.bangumi-detail-grad-0 { background: linear-gradient(150deg, #7fb4e8, #7cd6c0); }
.bangumi-detail-grad-1 { background: linear-gradient(150deg, #f2a6c0, #9fb8f5); }
.bangumi-detail-grad-2 { background: linear-gradient(150deg, #f5c98d, #ef9f9f); }
.bangumi-detail-grad-3 { background: linear-gradient(150deg, #9d9ff0, #79c3ec); }
.bangumi-detail-grad-4 { background: linear-gradient(150deg, #86d0a3, #b7d97f); }
html.dark .bangumi-detail-grad-0 { background: linear-gradient(150deg, #3d5f80, #38695f); }
html.dark .bangumi-detail-grad-1 { background: linear-gradient(150deg, #7a4d60, #4d5c85); }
html.dark .bangumi-detail-grad-2 { background: linear-gradient(150deg, #7f6a48, #7a5252); }
html.dark .bangumi-detail-grad-3 { background: linear-gradient(150deg, #52538a, #3d6580); }
html.dark .bangumi-detail-grad-4 { background: linear-gradient(150deg, #44684f, #5c6d40); }

.bangumi-detail-head {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.bangumi-detail-status {
  align-self: flex-start;
  padding: 3px 12px;
  border-radius: 999px;
  font-size: 13px;
  color: #fff;
}
.bangumi-detail-status.is-watching { background: rgba(63, 119, 181, 0.88); }
.bangumi-detail-status.is-done { background: rgba(16, 145, 118, 0.85); }
.bangumi-detail-status.is-wish { background: rgba(233, 138, 88, 0.88); }
.bangumi-detail-status.is-hold { background: rgba(148, 128, 92, 0.85); }
.bangumi-detail-status.is-drop { background: rgba(140, 92, 108, 0.85); }
.bangumi-detail-title {
  font-size: clamp(26px, 4.2vw, 35px);
  font-weight: 700;
  line-height: 1.3;
  color: var(--text-color);
}
.bangumi-detail-origin {
  font-size: 15.5px;
  color: var(--text-color);
  opacity: 0.6;
}
.bangumi-detail-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 14px;
}
.bangumi-detail-fact {
  font-size: 14.5px;
  color: var(--text-color);
  opacity: 0.72;
}
.bangumi-detail-rank {
  padding: 2px 10px;
  border: 1px solid var(--accent-border);
  border-radius: 999px;
  font-size: 13px;
  color: var(--accent-text);
  background: var(--nested-inner-card-bg);
}

/* 评分块 */
.bangumi-detail-scores {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px 26px;
}
.bangumi-detail-score-big {
  display: flex;
  align-items: baseline;
  gap: 10px;
}
.bangumi-detail-score-big strong {
  font-size: 44px;
  font-weight: 700;
  color: var(--accent-text);
  line-height: 1;
}
.bangumi-detail-score-big span {
  font-size: 13px;
  color: var(--text-color);
  opacity: 0.62;
}
.bangumi-detail-my-rating {
  display: flex;
  align-items: center;
  gap: 8px;
}
.bangumi-detail-stars {
  font-size: 15.5px;
  letter-spacing: 1px;
  color: #f0a742;
  white-space: nowrap;
}

/* 进度条 */
.bangumi-detail-progress {
  display: flex;
  align-items: center;
  gap: 12px;
}
.bangumi-detail-bar {
  flex: 1;
  height: 10px;
  border-radius: 999px;
  background: rgba(122, 176, 230, 0.22);
  overflow: hidden;
}
html.dark .bangumi-detail-bar {
  background: rgba(148, 163, 184, 0.18);
}
.bangumi-detail-bar i {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--accent-solid), #7cd6c0);
  transition: width 0.5s cubic-bezier(0.22, 1, 0.36, 1);
}
.bangumi-detail-progress-text {
  flex-shrink: 0;
  font-size: 13px;
  color: var(--text-color);
  opacity: 0.68;
}

/* 外链按钮 */
.bangumi-detail-link {
  margin-top: auto;
  align-self: flex-start;
  padding: 9px 20px;
  border-radius: 999px;
  background: var(--accent-solid);
  color: #fff;
  font-size: 14.5px;
  box-shadow: 0 10px 24px var(--accent-glow);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.bangumi-detail-link:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 30px var(--accent-glow);
}

/* 通用岛屿 */
.bangumi-detail-island {
  padding: 24px 28px 26px;
  border: 1px solid var(--card-border);
  border-radius: 26px;
  background: var(--nested-outer-card-bg);
  box-shadow: var(--nested-outer-card-shadow);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
}
.bangumi-detail-island-title {
  margin-bottom: 16px;
  font-size: 20px;
  font-weight: 700;
  color: var(--text-color);
  display: flex;
  align-items: center;
  gap: 10px;
}
.bangumi-detail-island-title::before {
  content: '';
  width: 6px;
  height: 20px;
  border-radius: 4px;
  background: linear-gradient(180deg, var(--accent-solid), #7cd6c0);
}
.bangumi-detail-note {
  margin: -8px 0 12px;
  font-size: 13px;
  color: var(--text-color);
  opacity: 0.58;
}

/* 我的记录四格 */
.bangumi-detail-record-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}
.bangumi-detail-record-grid article {
  padding: 14px 16px;
  border: 1px solid var(--nested-inner-card-border);
  border-radius: 18px;
  background: var(--nested-inner-card-bg);
  box-shadow: var(--nested-inner-card-shadow);
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.bangumi-detail-record-grid span {
  font-size: 13px;
  color: var(--text-color);
  opacity: 0.62;
}
.bangumi-detail-record-grid strong {
  font-size: 20px;
  color: var(--accent-text);
}
.bangumi-detail-record-grid strong.is-small {
  font-size: 13px;
  line-height: 1.7;
  font-weight: 600;
}

/* 简介 */
.bangumi-detail-summary {
  font-size: 14.5px;
  line-height: 1.9;
  color: var(--text-color);
  opacity: 0.82;
  white-space: pre-line;
}
.bangumi-detail-tags {
  margin-top: 14px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.bangumi-detail-tag {
  padding: 3px 10px;
  border: 1px solid var(--accent-border);
  border-radius: 999px;
  font-size: 13px;
  color: var(--accent-text);
  background: var(--nested-inner-card-bg);
}

/* 评分分布 + 收藏盒子 */
.bangumi-detail-rating-flex {
  display: flex;
  gap: 26px;
  align-items: flex-start;
}
.bangumi-detail-dist {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.bangumi-detail-dist-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.bangumi-detail-dist-label {
  width: 22px;
  flex-shrink: 0;
  text-align: right;
  font-size: 13px;
  color: var(--text-color);
  opacity: 0.66;
}
.bangumi-detail-dist-row.is-top .bangumi-detail-dist-label {
  font-weight: 700;
  color: var(--accent-text);
  opacity: 1;
}
.bangumi-detail-dist-bar {
  flex: 1;
  height: 12px;
  border-radius: 999px;
  background: rgba(122, 176, 230, 0.16);
  overflow: hidden;
}
html.dark .bangumi-detail-dist-bar {
  background: rgba(148, 163, 184, 0.14);
}
.bangumi-detail-dist-bar i {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: rgba(122, 176, 230, 0.65);
}
.bangumi-detail-dist-row.is-top .bangumi-detail-dist-bar i {
  background: linear-gradient(90deg, var(--accent-solid), #7cd6c0);
}
.bangumi-detail-dist-count {
  width: 74px;
  flex-shrink: 0;
  font-size: 12px;
  color: var(--text-color);
  opacity: 0.62;
  display: flex;
  align-items: center;
  gap: 6px;
}
.bangumi-detail-dist-count em {
  font-style: normal;
  padding: 1px 7px;
  border-radius: 999px;
  font-size: 11px;
  color: #fff;
  background: var(--accent-solid);
}
.bangumi-detail-collect {
  flex-shrink: 0;
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
}
.bangumi-detail-collect article {
  min-width: 64px;
  padding: 12px 10px;
  border: 1px solid var(--nested-inner-card-border);
  border-radius: 16px;
  background: var(--nested-inner-card-bg);
  box-shadow: var(--nested-inner-card-shadow);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}
.bangumi-detail-collect span {
  font-size: 12px;
  color: var(--text-color);
  opacity: 0.62;
}
.bangumi-detail-collect strong {
  font-size: 17.5px;
  color: var(--accent-text);
}

/* 角色 & 声优 */
.bangumi-detail-char-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
}
.bangumi-detail-char-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border: 1px solid var(--nested-inner-card-border);
  border-radius: 16px;
  background: var(--nested-inner-card-bg);
  box-shadow: var(--nested-inner-card-shadow);
}
.bangumi-detail-char-avatar {
  flex-shrink: 0;
  width: 46px;
  height: 46px;
  border-radius: 50%;
  overflow: hidden;
  background: rgba(122, 176, 230, 0.28);
  display: flex;
  align-items: center;
  justify-content: center;
}
.bangumi-detail-char-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.bangumi-detail-char-avatar span {
  font-size: 20px;
  font-weight: 700;
  color: var(--accent-text);
}
.bangumi-detail-char-info {
  min-width: 0;
}
.bangumi-detail-char-name {
  font-size: 14.5px;
  font-weight: 700;
  color: var(--text-color);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.bangumi-detail-char-name em {
  font-style: normal;
  margin-left: 6px;
  padding: 1px 7px;
  border: 1px solid var(--accent-border);
  border-radius: 999px;
  font-size: 11px;
  font-weight: 400;
  color: var(--accent-text);
}
.bangumi-detail-char-cv {
  margin-top: 3px;
  font-size: 12px;
  color: var(--text-color);
  opacity: 0.6;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 分集格子 */
.bangumi-detail-ep-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(42px, 1fr));
  gap: 8px;
}
.bangumi-detail-ep {
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--accent-border);
  border-radius: 10px;
  font-size: 13px;
  color: var(--accent-text);
  background: var(--card-bg);
  cursor: default;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}
.bangumi-detail-ep:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 14px var(--accent-glow);
}
.bangumi-detail-ep.is-watched {
  background: var(--accent-solid);
  border-color: var(--accent-solid);
  color: #fff;
}

/* STAFF 双栏 */
.bangumi-detail-staff {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 26px;
}
.bangumi-detail-staff-row {
  display: flex;
  gap: 12px;
  padding: 8px 4px;
  border-bottom: 1px dashed var(--nested-inner-card-border);
  font-size: 14.5px;
}
.bangumi-detail-staff-row dt {
  flex-shrink: 0;
  width: 76px;
  color: var(--text-color);
  opacity: 0.58;
}
.bangumi-detail-staff-row dd {
  flex: 1;
  min-width: 0;
  color: var(--text-color);
  opacity: 0.85;
}

/* 更多记录横向小卡 */
.bangumi-detail-more {
  display: flex;
  gap: 14px;
  overflow-x: auto;
  padding-bottom: 8px;
  scrollbar-width: thin;
}
.bangumi-detail-more-card {
  flex-shrink: 0;
  width: 118px;
  cursor: pointer;
  transition: transform 0.2s ease;
}
.bangumi-detail-more-card:hover {
  transform: translateY(-4px);
}
.bangumi-detail-more-cover {
  aspect-ratio: 2 / 3;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: var(--nested-middle-card-shadow);
}
.bangumi-detail-more-cover .bangumi-detail-cover-fallback span {
  font-size: 35px;
}
.bangumi-detail-more-name {
  margin-top: 8px;
  font-size: 13px;
  color: var(--text-color);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 响应式 */
@media (max-width: 720px) {
  .bangumi-detail-page {
    padding: 6px 14px 56px;
  }
  .bangumi-detail-hero {
    flex-direction: column;
    padding: 22px 20px;
  }
  .bangumi-detail-cover-box {
    width: min(58vw, 220px);
    align-self: center;
  }
  .bangumi-detail-record-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .bangumi-detail-rating-flex {
    flex-direction: column;
  }
  .bangumi-detail-collect {
    width: 100%;
  }
  .bangumi-detail-staff {
    grid-template-columns: minmax(0, 1fr);
  }
  .bangumi-detail-char-grid {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  }
}

/* ===== 移动端适配（≤768 / ≤480，只追加、不回归桌面） ===== */
@media (max-width: 768px) {
  .bangumi-detail-page {
    padding: 6px 14px 56px;
  }
  /* HERO 岛上下堆叠，封面居中限宽 */
  .bangumi-detail-hero {
    flex-direction: column;
    padding: 22px 18px;
    gap: 18px;
  }
  .bangumi-detail-cover-box {
    width: min(56vw, 200px);
    align-self: center;
  }
  .bangumi-detail-meta {
    gap: 6px 10px;
  }
  .bangumi-detail-scores {
    gap: 10px 18px;
  }
  .bangumi-detail-island {
    padding: 20px 18px 22px;
  }
  .bangumi-detail-back {
    min-height: 40px;
  }
  .bangumi-detail-link {
    min-height: 40px;
    display: inline-flex;
    align-items: center;
  }
  .bangumi-detail-record-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  /* 评分分布与收藏盒子单列堆叠 */
  .bangumi-detail-rating-flex {
    flex-direction: column;
    gap: 18px;
  }
  .bangumi-detail-collect {
    width: 100%;
    grid-template-columns: repeat(5, minmax(0, 1fr));
  }
  .bangumi-detail-collect article {
    min-width: 0;
    padding: 10px 6px;
  }
  .bangumi-detail-char-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
  .bangumi-detail-char-card {
    padding: 10px 12px;
    gap: 10px;
  }
  .bangumi-detail-ep-grid {
    grid-template-columns: repeat(auto-fill, minmax(34px, 1fr));
    gap: 6px;
  }
  .bangumi-detail-staff {
    grid-template-columns: minmax(0, 1fr);
  }
  /* 更多记录：容器自身横滑，不影响页面 */
  .bangumi-detail-more {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }
  .bangumi-detail-more-card {
    width: 104px;
  }
}
@media (max-width: 480px) {
  .bangumi-detail-score-big strong {
    font-size: 35px;
  }
  .bangumi-detail-dist-count {
    width: 60px;
  }
  .bangumi-detail-collect {
    gap: 6px;
  }
  .bangumi-detail-collect strong {
    font-size: 15.5px;
  }
  .bangumi-detail-record-grid strong {
    font-size: 17.5px;
  }
  .bangumi-detail-char-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .bangumi-detail-char-card {
    padding: 8px 10px;
    gap: 8px;
  }
  .bangumi-detail-char-avatar {
    width: 40px;
    height: 40px;
  }
}
</style>
