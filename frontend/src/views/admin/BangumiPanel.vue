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
import { inject, ref } from 'vue'
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
const panelKey = ref(0)

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
    try {
      // 首选 v0 搜索接口
      const res = await fetch('https://api.bgm.tv/v0/search/subjects', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ keyword: kw, filter: { type: [2] } })
      })
      if (!res.ok) throw new Error(`v0 搜索失败: ${res.status}`)
      const data = await res.json()
      list = data && Array.isArray(data.data) ? data.data : []
    } catch {
      // 降级：旧版搜索接口
      const res = await fetch(
        `https://api.bgm.tv/search/subject/${encodeURIComponent(kw)}?type=2&responseGroup=large&max_results=10`
      )
      if (!res.ok) throw new Error(`搜索失败: ${res.status}`)
      const data = await res.json()
      list = data && Array.isArray(data.list) ? data.list : []
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
    tags: item.tags.slice(0, 4)
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
    ).slice(0, 4)
  }
}

async function importItem(item) {
  importingId.value = item.id
  let usedFallback = false
  let record
  try {
    const res = await fetch(`https://api.bgm.tv/v0/subjects/${item.id}`)
    if (!res.ok) throw new Error(`详情获取失败: ${res.status}`)
    record = buildDetailRecord(item, await res.json())
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
.bgm-import-label {
  font-size: 13px;
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
  font-size: 16px;
  color: #fff;
  background: linear-gradient(150deg, #7fb4e8, #7cd6c0);
}
.bgm-result-info {
  flex: 1;
  min-width: 0;
}
.bgm-result-name {
  font-size: 13px;
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.bgm-result-sub {
  margin-top: 2px;
  font-size: 12px;
  opacity: 0.65;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.bgm-result-empty {
  margin-top: 12px;
  font-size: 13px;
  opacity: 0.65;
}
</style>
