<template>
  <div class="bgm-panel">
    <!-- 从 Bangumi 导入工具条 -->
    <section class="admin-table-card bgm-import-card">
      <div class="bgm-import-row">
        <span class="bgm-import-label">从 Bangumi 导入</span>
        <input
          v-model.trim="keyword"
          class="admin-input bgm-import-input"
          type="text"
          placeholder="输入番剧名，如：葬送的芙莉莲"
          @keyup.enter="doSearch"
        />
        <button class="admin-btn" :disabled="searching" @click="doSearch">
          {{ searching ? '搜索中…' : '搜索' }}
        </button>
        <button class="admin-btn admin-btn-ghost" :disabled="cleaning" @click="cleanDuplicates">
          {{ cleaning ? '清理中…' : '清理重复' }}
        </button>
      </div>

      <!-- 用 Bangumi 访问令牌同步个人收藏（token 只存本地浏览器，不入库不入仓） -->
      <div class="bgm-import-row bgm-sync-row">
        <span class="bgm-import-label">同步我的收藏</span>
        <input
          v-model.trim="bgmToken"
          class="admin-input bgm-import-input"
          type="password"
          placeholder="粘贴 Bangumi 访问令牌（next.bgm.tv/demo/access-token 生成）"
        />
        <button class="admin-btn" :disabled="syncing" @click="syncCollections">
          {{ syncing ? syncTip || '同步中…' : '同步收藏' }}
        </button>
      </div>

      <ul v-if="results.length" class="bgm-result-list">
        <li v-for="item in results" :key="item.id" class="bgm-result-item">
          <img
            v-if="item.cover"
            class="bgm-result-cover"
            :src="item.cover"
            referrerpolicy="no-referrer"
            alt=""
            @error="item.cover = ''"
          />
          <div v-else class="bgm-result-cover bgm-result-cover-empty">番</div>
          <div class="bgm-result-info">
            <p class="bgm-result-name">{{ item.nameCn || item.name }}</p>
            <p class="bgm-result-sub">
              {{ item.name }} · {{ item.date ? item.date.slice(0, 4) : '年份未知' }}
              <template v-if="item.score != null"> · 评分 {{ item.score }}</template>
            </p>
          </div>
          <button class="admin-btn admin-btn-ghost" :disabled="importingId === item.id" @click="importItem(item)">
            {{ importingId === item.id ? '导入中…' : '导入' }}
          </button>
        </li>
      </ul>
      <p v-else-if="searched && !searching" class="bgm-result-empty">没有搜到相关条目，换个关键词试试</p>
    </section>

    <!-- 既有 CRUD 面板：导入成功后通过 key 重挂刷新列表 -->
    <ResourcePanel :key="panelKey" :schema="schema" />
  </div>
</template>

<script setup>
import { inject, onMounted, ref } from 'vue'
import { adminApi } from '../../api/admin'
import ResourcePanel from './ResourcePanel.vue'
import resourceSchemas from './resourceSchemas'

const schema = resourceSchemas.find(s => s.key === 'bangumi-records')

const toast = inject('adminToast')
const onUnauthorized = inject('adminUnauthorized')

const keyword = ref('')
const results = ref([])
const searching = ref(false)
const searched = ref(false)
const importingId = ref(null)
const cleaning = ref(false)
const panelKey = ref(0)

// Bangumi 访问令牌：仅存当前浏览器会话（sessionStorage），关闭页面即失效，避免长期驻留
// 直连 api.bgm.tv 的兜底请求超时：该域名在国内不稳定，没有超时会让
// 同步/搜索一直挂在 pending，按钮的 loading 态永远不回落。
const BGM_TIMEOUT_MS = 8000

const TOKEN_KEY = 'cx-bgm-token'
const bgmToken = ref(sessionStorage.getItem(TOKEN_KEY) || '')
const syncing = ref(false)
const syncTip = ref('')

// 已收录的 bgm 条目 id，用于搜索结果里标注与拦截重复导入
const existingSubjectIds = ref(new Set())

async function loadExisting() {
  try {
    const list = await adminApi['bangumi-records'].list()
    existingSubjectIds.value = new Set((list || []).map(r => Number(r.subjectId)).filter(Boolean))
  } catch (e) { console.warn('[番剧管理] 已有记录加载失败:', e) }
}

onMounted(loadExisting)

/* ===== 同步个人收藏（bgm v0 API，Bearer 认证） ===== */

// bgm 收藏类型 -> 本站五状态
const COLLECTION_STATUS = { 1: '想看', 2: '看过', 3: '在看', 4: '搁置', 5: '弃番' }

function authHeaders() {
  return { Authorization: `Bearer ${bgmToken.value}` }
}

/** 收藏条目 -> 新建 record（subject 为收藏接口附带的精简条目信息） */
function buildCollectionRecord(item) {
  const s = item.subject || {}
  const images = s.images || {}
  return {
    subjectId: item.subject_id,
    name: s.name || '',
    nameCn: s.name_cn || s.name || '',
    coverUrl: images.common || images.large || images.medium || '',
    totalEps: Number(s.eps) || 0,
    watchedEps: Number(item.ep_status) || 0,
    status: COLLECTION_STATUS[item.type] || '想看',
    rating: item.rate > 0 ? Number(item.rate) : null,
    score: s.score == null ? null : Number(s.score),
    airDate: s.date || '',
    rank: s.rank ? Number(s.rank) : null,
    summary: s.short_summary || '',
    tags: Array.isArray(item.tags) ? item.tags.slice(0, 4) : [],
    visible: true
  }
}

async function syncCollections() {
  if (!bgmToken.value) {
    toast && toast('先粘贴 Bangumi 访问令牌吧', 'error')
    return
  }
  syncing.value = true
  syncTip.value = ''
  try {
    sessionStorage.setItem(TOKEN_KEY, bgmToken.value)
    // 首选后端代理（服务器可达时无需浏览器代理）；失败降级浏览器直连
    let me = {}
    let items = []
    try {
      const result = await adminApi.bangumiSyncCollections(bgmToken.value)
      me = { username: result.username, nickname: result.nickname }
      items = Array.isArray(result.items) ? result.items : []
    } catch (e) {
      console.warn('[番剧管理] 后端同步不可用，降级浏览器直连:', e)
      // 1. 拿用户名
      const meRes = await fetch('https://api.bgm.tv/v0/me', {
        headers: authHeaders(),
        signal: AbortSignal.timeout(BGM_TIMEOUT_MS)
      })
      if (meRes.status === 401) throw new Error('令牌无效或已过期', { cause: e })
      if (!meRes.ok) throw new Error(`获取用户信息失败: ${meRes.status}`, { cause: e })
      me = await meRes.json()
      // 2. 分页拉全部动画收藏
      let offset = 0
      let total = Infinity
      while (offset < total && offset < 1000) {
        syncTip.value = `拉取中 ${items.length}…`
        const res = await fetch(
          `https://api.bgm.tv/v0/users/${encodeURIComponent(me.username)}/collections?subject_type=2&limit=50&offset=${offset}`,
          { headers: authHeaders(), signal: AbortSignal.timeout(BGM_TIMEOUT_MS) }
        )
        if (!res.ok) throw new Error(`获取收藏失败: ${res.status}`, { cause: e })
        const data = await res.json()
        total = Number(data.total) || 0
        const page = Array.isArray(data.data) ? data.data : []
        items.push(...page)
        if (!page.length) break
        offset += page.length
      }
    }
    if (!items.length) {
      toast && toast(`${me.nickname || me.username} 的收藏是空的，去 bgm 标几部吧`) 
      return
    }
    // 3. 新条目创建，已收录的按 bgm 收藏更新状态/进度/评分（后端 update 为整体替换，回传完整行）
    const list = (await adminApi['bangumi-records'].list()) || []
    const bySubject = new Map(list.filter(r => r.subjectId).map(r => [Number(r.subjectId), r]))
    let created = 0
    let updated = 0
    for (const [i, item] of items.entries()) {
      syncTip.value = `同步中 ${i + 1}/${items.length}`
      const exist = bySubject.get(Number(item.subject_id))
      if (!exist) {
        await adminApi['bangumi-records'].create(buildCollectionRecord(item))
        created += 1
      } else {
        const next = {
          ...exist,
          status: COLLECTION_STATUS[item.type] || exist.status,
          watchedEps: Number(item.ep_status) || 0,
          rating: item.rate > 0 ? Number(item.rate) : exist.rating
        }
        if (
          next.status !== exist.status ||
          next.watchedEps !== (exist.watchedEps || 0) ||
          next.rating !== exist.rating
        ) {
          await adminApi['bangumi-records'].update(exist.id, next)
          updated += 1
        }
      }
    }
    toast && toast(`同步完成：新增 ${created} 部，更新 ${updated} 部（共 ${items.length} 条收藏）`)
    await loadExisting()
    panelKey.value += 1
  } catch (err) {
    if (err && err.unauthorized) {
      onUnauthorized && onUnauthorized()
      return
    }
    toast && toast((err && err.message) || '同步失败，可能是网络或跨域限制', 'error')
  } finally {
    syncing.value = false
    syncTip.value = ''
  }
}

/** 清理重复：同一 subjectId 只保留最早收录的一条 */
async function cleanDuplicates() {
  cleaning.value = true
  try {
    const list = (await adminApi['bangumi-records'].list()) || []
    const seen = new Map()
    const extras = []
    for (const r of list) {
      const sid = Number(r.subjectId)
      if (!sid) continue
      if (seen.has(sid)) extras.push(r)
      else seen.set(sid, r)
    }
    if (!extras.length) {
      toast && toast('没有发现重复收录的番剧')
      return
    }
    if (!window.confirm(`发现 ${extras.length} 条按 bgm 条目重复的记录，确定删除吗？`)) return
    for (const r of extras) await adminApi['bangumi-records'].remove(r.id)
    toast && toast(`已清理 ${extras.length} 条重复记录`)
    await loadExisting()
    panelKey.value += 1
  } catch (err) {
    if (err && err.unauthorized) {
      onUnauthorized && onUnauthorized()
      return
    }
    toast && toast((err && err.message) || '清理失败', 'error')
  } finally {
    cleaning.value = false
  }
}

/** bgm v0 / 旧版搜索结果统一成一个形状 */
function normalize(item) {
  const images = item.images || {}
  return {
    id: item.id,
    name: item.name || '',
    nameCn: item.name_cn || '',
    cover: images.common || images.large || images.medium || images.grid || '',
    date: item.date || item.air_date || '',
    score:
      item.score != null
        ? item.score
        : item.rating && item.rating.score != null
          ? item.rating.score
          : null,
    eps: Number(item.eps) || Number(item.eps_count) || 0,
    summary: item.summary || '',
    tags: Array.isArray(item.tags) ? item.tags.map(t => t && t.name).filter(Boolean) : []
  }
}

async function doSearch() {
  const kw = keyword.value
  if (!kw) {
    toast && toast('先输入想搜索的番剧名吧', 'error')
    return
  }
  searching.value = true
  results.value = []
  searched.value = false
  try {
    let list = []
    // 首选后端缓存代理（无代理也能搜已缓存的关键词），失败降级浏览器直连
    try {
      const data = await adminApi.bangumiSearch(kw)
      if (!Array.isArray(data)) throw new Error('后端搜索返回空')
      list = data
    } catch (e) {
      console.warn('[番剧管理] 后端搜索不可用，降级浏览器直连:', e)
      try {
        // 首选 v0 搜索接口
        const res = await fetch('https://api.bgm.tv/v0/search/subjects', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ keyword: kw, filter: { type: [2] } }),
          signal: AbortSignal.timeout(BGM_TIMEOUT_MS)
        })
        if (!res.ok) throw new Error(`v0 搜索失败: ${res.status}`, { cause: e })
        const data = await res.json()
        list = data && Array.isArray(data.data) ? data.data : []
      } catch (e2) {
        console.warn('[番剧管理] v0搜索失败，降级旧接口:', e2)
        // 降级：旧版搜索接口
        const res = await fetch(
          `https://api.bgm.tv/search/subject/${encodeURIComponent(kw)}?type=2&responseGroup=large&max_results=10`,
          { signal: AbortSignal.timeout(BGM_TIMEOUT_MS) }
        )
        if (!res.ok) throw new Error(`搜索失败: ${res.status}`, { cause: e2 })
        const data = await res.json()
        list = data && Array.isArray(data.list) ? data.list : []
      }
    }
    results.value = list.slice(0, 10).map(normalize)
    searched.value = true
  } catch {
    toast && toast('连接 Bangumi 失败，可能是网络或跨域限制，请稍后再试', 'error')
  } finally {
    searching.value = false
  }
}

/** 搜索摘要兜底 record（详情拉取失败时使用） */
function buildFallbackRecord(item) {
  return {
    subjectId: item.id,
    name: item.name || item.nameCn,
    nameCn: item.nameCn || item.name,
    coverUrl: item.cover || '',
    totalEps: item.eps || 0,
    watchedEps: 0,
    status: '想看',
    rating: null,
    score: item.score == null ? null : Number(item.score),
    airDate: item.date || '',
    summary: item.summary || '',
    tags: item.tags.slice(0, 4),
    visible: true
  }
}

/** 用 /v0/subjects/{id} 全量详情组装 record */
function buildDetailRecord(item, s) {
  const images = s.images || {}
  const rating = s.rating || {}
  return {
    subjectId: s.id || item.id,
    name: s.name || item.name || item.nameCn,
    nameCn: s.name_cn || item.nameCn || s.name || '',
    coverUrl: images.common || images.large || images.medium || item.cover || '',
    totalEps: Number(s.eps) || Number(s.total_episodes) || item.eps || 0,
    watchedEps: 0,
    status: '想看',
    rating: null,
    score: rating.score == null ? null : Number(rating.score),
    airDate: s.date || item.date || '',
    platform: s.platform || '',
    rank: rating.rank ? Number(rating.rank) : null,
    ratingTotal: rating.total ? Number(rating.total) : null,
    summary: s.summary || item.summary || '',
    tags: (Array.isArray(s.tags) && s.tags.length
      ? s.tags.map(t => t && t.name).filter(Boolean)
      : item.tags
    ).slice(0, 4),
    visible: true
  }
}

async function importItem(item) {
  // 先查重：已收录的直接提示，省掉一次 bgm 详情请求
  if (existingSubjectIds.value.has(Number(item.id))) {
    toast && toast(`「${item.nameCn || item.name}」已经收录过了`, 'error')
    return
  }
  importingId.value = item.id
  let usedFallback = false
  let record
  try {
    // 走后端三层缓存详情接口（无代理也能导已缓存条目），失败降级用搜索摘要
    const data = await adminApi.bangumiSubject(item.id)
    if (data == null) throw new Error('详情获取失败')
    record = buildDetailRecord(item, data)
  } catch {
    usedFallback = true
    record = buildFallbackRecord(item)
  }
  try {
    await adminApi['bangumi-records'].create(record)
    if (usedFallback) {
      toast && toast(`详情拉取失败，已用搜索摘要导入「${item.nameCn || item.name}」`)
    } else {
      toast && toast(`已导入「${item.nameCn || item.name}」`)
    }
    existingSubjectIds.value.add(Number(item.id))
    panelKey.value += 1
  } catch (err) {
    if (err && err.unauthorized) {
      onUnauthorized && onUnauthorized()
      return
    }
    toast && toast((err && err.message) || '导入失败', 'error')
  } finally {
    importingId.value = null
  }
}
</script>

<style>
/* 番剧导入工具条（bgm- 前缀，复用 admin.css 基础控件） */
.bgm-panel {
  display: flex;
  flex-direction: column;
}
.bgm-import-card {
  padding: 14px 16px;
  margin-bottom: 16px;
}
.bgm-import-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.bgm-sync-row {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed var(--adm-border-soft, rgba(63, 119, 181, 0.15));
}
.bgm-import-label {
  font-size: 14.5px;
  font-weight: 700;
  white-space: nowrap;
}
.bgm-import-input {
  flex: 1;
  min-width: 200px;
}
.bgm-result-list {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 320px;
  overflow-y: auto;
}
.bgm-result-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 10px;
  border: 1px solid var(--input-border, rgba(178, 202, 238, 0.6));
  border-radius: 12px;
  background: var(--input-bg, rgba(255, 255, 255, 0.5));
}
.bgm-result-cover {
  flex-shrink: 0;
  width: 40px;
  height: 56px;
  border-radius: 8px;
  object-fit: cover;
}
.bgm-result-cover-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 17.5px;
  color: #fff;
  background: linear-gradient(150deg, #7fb4e8, #7cd6c0);
}
.bgm-result-info {
  flex: 1;
  min-width: 0;
}
.bgm-result-name {
  font-size: 14.5px;
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.bgm-result-sub {
  margin-top: 2px;
  font-size: 13px;
  opacity: 0.65;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.bgm-result-empty {
  margin-top: 12px;
  font-size: 14.5px;
  opacity: 0.65;
}

/* ---------- 移动端适配（≤900px，追加） ---------- */

@media (max-width: 900px) {
  /* 导入工具条：标签独占一行，输入框与搜索按钮同行伸缩 */
  .bgm-import-label {
    flex: 1 1 100%;
  }

  .bgm-import-input {
    min-width: 0;
    flex: 1 1 160px;
  }

  /* 结果列表：允许换行堆叠，导入按钮不挤压标题 */
  .bgm-result-item {
    flex-wrap: wrap;
    gap: 8px 12px;
  }

  .bgm-result-info {
    flex: 1 1 160px;
  }

  .bgm-result-item .admin-btn-ghost {
    margin-left: auto;
    padding: 8px 16px;
  }
}
</style>
