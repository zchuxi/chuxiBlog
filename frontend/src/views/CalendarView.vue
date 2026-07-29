<template>
  <main class="calendar-page">
    <div class="calendar-shell">
      <!-- 页头岛 -->
      <section v-reveal="0" class="calendar-hero">
        <p class="calendar-hero-eyebrow">Broadcast Calendar</p>
        <h1 class="calendar-hero-title">{{ calendarConfig?.title || DEFAULT_CALENDAR.title }}</h1>
        <p class="calendar-hero-sub">{{ calendarConfig?.subtitle || DEFAULT_CALENDAR.subtitle }}</p>
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

      <!-- 管理员追番提示弹窗：fixed 居中偏上 -->
      <Transition name="calendar-notice-fade">
        <div v-if="noticeVisible && notice" class="calendar-notice-modal">
          <div class="calendar-notice-modal-content">
            <span class="calendar-notice-modal-text">{{ notice }}</span>
            <router-link v-if="showNoticeLink" to="/bangumi" class="calendar-notice-modal-link">前往番剧记录</router-link>
          </div>
        </div>
      </Transition>

      <!-- 当日放送网格：key 含星期，切页签时卡片重新挂载触发错峰入场动效 -->
      <div v-if="!loading && days.length" class="calendar-grid">
        <div
          v-for="(item, i) in activeItems"
          :key="activeDay + '-' + item.id"
          class="calendar-card"
          :style="{ animationDelay: Math.min(i, 18) * 0.045 + 's' }"
        >
          <a
            class="calendar-card-inner"
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
            <p class="calendar-card-name" :title="item.nameCn || item.name">{{ item.nameCn || item.name }}</p>
          </a>
          <button
            v-if="isAdmin && !item.localId"
            type="button"
            class="calendar-import-btn"
            :disabled="importingId === item.id"
            @click.stop="importSubject(item)"
          >
            {{ importingId === item.id ? '追番中…' : '＋ 追番' }}
          </button>
        </div>
      </div>
    </div>
  </main>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api'
import { adminApi, getToken, clearToken } from '../api/admin'

const DEFAULT_CALENDAR = { title: '每日放送', subtitle: '查看今日播出的番剧时间表，不再错过任何一集。' }
const calendarConfig = ref(null)

const router = useRouter()

const days = ref([])
const activeDay = ref(1)
const loading = ref(true)
const broken = ref(new Set())
// 本站 subjectId -> 本地记录 id，用于「已收录」标记与跳转本站详情
const mine = ref(new Map())
// 已确认管理员（本地有 token 且后端 /api/auth/me 校验通过）才显示追番按钮
const isAdmin = ref(false)
const importingId = ref(null)
const notice = ref('')
const noticeVisible = ref(false)
const noticeType = ref('') // 'success' | 'error' | 'info'
let noticeTimer = null
let cleanupTimer = null

const NOTICE_FADE_DELAY = 200  // 替换通知时的淡出等待
const NOTICE_LEAVE_DURATION = 350  // leave 动画时长（与 CSS 0.35s 对应）

const showNoticeLink = computed(() => noticeType.value === 'success')

function flashNotice(msg) {
  const hasExisting = !!notice.value
  noticeVisible.value = false
  if (noticeTimer) clearTimeout(noticeTimer)
  if (cleanupTimer) clearTimeout(cleanupTimer)
  // 短暂延迟让淡出完成后再设置新消息并淡入
  setTimeout(() => {
    // 根据消息内容判断通知类型
    if (msg.startsWith('已追番')) {
      noticeType.value = 'success'
    } else if (msg.includes('失败') || msg.includes('过期')) {
      noticeType.value = 'error'
    } else {
      noticeType.value = 'info'
    }
    notice.value = msg
    noticeVisible.value = true
    noticeTimer = setTimeout(() => {
      noticeVisible.value = false
      cleanupTimer = setTimeout(() => { notice.value = '' }, NOTICE_LEAVE_DURATION)
    }, 4000)
  }, hasExisting ? NOTICE_FADE_DELAY : 0)
}

// bgm 的 weekday.id：1=周一 … 7=周日；JS getDay()：0=周日
const todayId = (() => {
  const d = new Date().getDay()
  return d === 0 ? 7 : d
})()

const activeItems = computed(() => {
  const day = days.value.find(d => d.weekdayId === activeDay.value)
  if (!day) return []
  // localId 从 mine 响应式注入，导入后「已收录」徽标即时更新
  return day.items.map(it => ({ ...it, localId: mine.value.get(Number(it.id)) || null }))
})

// Bangumi 图片地址常为 http://lain.bgm.tv/...，在 HTTPS 页面会触发 Mixed Content 警告，统一升级为 https
function toHttps(url) {
  return typeof url === 'string' ? url.replace(/^http:\/\//i, 'https://') : ''
}

function normalizeDay(raw) {
  return {
    weekdayId: raw.weekday?.id ?? 0,
    weekdayCn: raw.weekday?.cn || raw.weekday?.ja || '?',
    items: (raw.items || []).map(it => ({
      id: it.id,
      name: it.name || '',
      nameCn: it.name_cn || '',
      cover: toHttps((it.images && (it.images.common || it.images.large || it.images.grid)) || ''),
      score: it.rating && it.rating.score ? Number(it.rating.score) : null
    }))
  }
}

// 确认管理员身份：本地有 token 且后端 /api/auth/me 校验通过才视为已确认。
// 失败（无 token / 过期）则隐藏追番按钮，避免越权入口暴露。
async function confirmAdmin() {
  if (!getToken()) {
    isAdmin.value = false
    return
  }
  try {
    await adminApi.me()
    isAdmin.value = true
  } catch {
    clearToken()
    isAdmin.value = false
  }
}

// 追番：把日历上的 Bangumi 条目写入本站「番剧记录」（状态默认「想看」）。
// 复用后台已有权限的 adminApi，后端 AdminAuthInterceptor 会再次校验管理员身份。
async function importSubject(item) {
  if (importingId.value === item.id || item.localId) return
  importingId.value = item.id
  try {
    const res = await fetch(`https://api.bgm.tv/v0/subjects/${item.id}`)
    if (!res.ok) throw new Error(`详情获取失败: ${res.status}`)
    const s = await res.json()
    const images = s.images || {}
    const rating = s.rating || {}
    const record = {
      subjectId: s.id || item.id,
      name: s.name || item.name || item.nameCn,
      nameCn: s.name_cn || item.nameCn || s.name || '',
      coverUrl: toHttps(images.common || images.large || images.medium || item.cover || ''),
      totalEps: Number(s.eps) || Number(s.total_episodes) || 0,
      watchedEps: 0,
      status: '想看',
      rating: null,
      score: rating.score == null ? null : Number(rating.score),
      airDate: s.date || '',
      platform: s.platform || '',
      rank: rating.rank ? Number(rating.rank) : null,
      ratingTotal: rating.total ? Number(rating.total) : null,
      summary: s.summary || '',
      tags: Array.isArray(s.tags) ? s.tags.map(t => t && t.name).filter(Boolean).slice(0, 4) : [],
      visible: true
    }
    const created = await adminApi['bangumi-records'].create(record)
    const newId = created && created.id
    mine.value = new Map(mine.value).set(Number(item.id), newId || true)
    flashNotice(`已追番「${record.nameCn || record.name}」，可到「番剧记录」查看`)
  } catch (err) {
    if (err && err.unauthorized) {
      clearToken()
      isAdmin.value = false
      flashNotice('登录已过期，请到后台重新登录')
      return
    }
    flashNotice((err && err.message) || '追番失败，请稍后再试')
  } finally {
    importingId.value = null
  }
}

onBeforeUnmount(() => {
  if (noticeTimer) clearTimeout(noticeTimer)
  if (cleanupTimer) clearTimeout(cleanupTimer)
})

onMounted(async () => {
  // 先确认管理员身份（决定追番按钮是否可见），失败不阻塞放送表
  confirmAdmin()
  // 先拉本站记录做「已收录」映射，失败不阻塞放送表
  try {
    const list = (await api.bangumiRecords()) || []
    mine.value = new Map(list.filter(r => r.subjectId).map(r => [Number(r.subjectId), r.id]))
  } catch {
    mine.value = new Map()
  }
  try {
    calendarConfig.value = await api.siteContent('calendar-hero')
  } catch { /* 使用默认值 */ }
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
  /* 切换星期时的错峰入场：淡入 + 上浮 + 微缩放回正；delay 由行内样式按序号递增 */
  animation: calendar-card-in 0.5s cubic-bezier(0.22, 1, 0.36, 1) both;
}
@keyframes calendar-card-in {
  from {
    opacity: 0;
    transform: translateY(18px) scale(0.96);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}
@media (prefers-reduced-motion: reduce) {
  .calendar-card {
    animation: none;
  }
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
  /* line-clamp 元素自身带 padding 时，被裁行会透进底部 padding 区域露出半行，
     改用 margin 留白避开这个坑 */
  margin: 10px 12px 8px;
  font-size: 14px;
  line-height: 1.45;
  color: var(--text-color);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  /* 固定两行高，长短标题卡片底部对齐 */
  min-height: calc(14px * 1.45 * 2);
}

/* 追番按钮 + 提示条（确认管理员后可见） */
.calendar-card { position: relative; }
.calendar-card-inner {
  display: block;
  text-decoration: none;
  color: inherit;
}
/* 按钮作为卡片底部独立一行（不在文本区同行挤占空间） */
.calendar-import-btn {
  display: block;
  width: calc(100% - 24px);
  margin: 0 12px 12px;
  padding: 7px 13px;
  border: 1px solid var(--accent-solid, #7cd6c0);
  border-radius: 999px;
  background: var(--accent-solid, #7cd6c0);
  color: #fff;
  font-family: inherit;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  text-align: center;
  box-shadow: 0 4px 12px var(--accent-glow, rgba(124, 214, 192, 0.4));
  transition: transform 0.2s ease, box-shadow 0.2s ease, opacity 0.2s ease;
}
.calendar-import-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 10px 22px var(--accent-glow, rgba(124, 214, 192, 0.5));
}
.calendar-import-btn:disabled {
  opacity: 0.6;
  cursor: default;
}
/* 追番提示弹窗 */
.calendar-notice-modal {
  position: fixed;
  top: 80px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 100;
  padding: 16px 28px;
  background: rgba(30, 40, 60, 0.85);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.calendar-notice-modal-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.calendar-notice-modal-text {
  font-size: 14px;
  color: #e0e0e0;
}

.calendar-notice-modal-link {
  color: var(--theme-color, #7eb8da);
  text-decoration: none;
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
  transition: color 0.2s ease;
}

.calendar-notice-modal-link:hover {
  color: var(--theme-color-hover, #a0d0ea);
}

.calendar-notice-fade-enter-active {
  animation: calendar-notice-in 0.3s ease;
}

.calendar-notice-fade-leave-active {
  animation: calendar-notice-out 0.35s ease;
}

@keyframes calendar-notice-in {
  from { opacity: 0; transform: translateX(-50%) translateY(-12px); }
  to { opacity: 1; transform: translateX(-50%) translateY(0); }
}

@keyframes calendar-notice-out {
  from { opacity: 1; transform: translateX(-50%) translateY(0); }
  to { opacity: 0; transform: translateX(-50%) translateY(-12px); }
}

@media (prefers-reduced-motion: reduce) {
  .calendar-notice-fade-enter-active,
  .calendar-notice-fade-leave-active {
    animation-duration: 0.01ms;
  }
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
