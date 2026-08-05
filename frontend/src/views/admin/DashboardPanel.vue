<template>
  <section class="dash">
    <div v-if="loading" class="admin-state">加载概览中…</div>
    <div v-else-if="error" class="admin-state">
      {{ error }}
      <p><button class="admin-btn admin-btn-ghost" @click="load">重试</button></p>
    </div>
    <template v-else>
      <!-- 统计卡网格 -->
      <div class="dash-stats">
        <div
          v-for="card in statCards"
          :key="card.label"
          class="dash-stat"
          :class="{ 'is-clickable': !!card.go }"
          :role="card.go ? 'button' : undefined"
          :tabindex="card.go ? 0 : -1"
          :title="card.go ? `前往「${card.label}」` : undefined"
          @click="card.go && emit('go', card.go)"
          @keydown.enter.prevent="card.go && emit('go', card.go)"
          @keydown.space.prevent="card.go && emit('go', card.go)"
        >
          <span class="dash-stat-icon"><SvgIcon :name="card.icon" size="20px" /></span>
          <div class="dash-stat-info">
            <p class="dash-stat-num">{{ card.value }}</p>
            <p class="dash-stat-label">{{ card.label }}</p>
            <p v-if="card.sub" class="dash-stat-sub">{{ card.sub }}</p>
          </div>
        </div>
      </div>

      <div class="dash-row">
        <!-- 快捷操作 -->
        <div class="dash-card">
          <h3 class="dash-card-title">快捷操作</h3>
          <div class="dash-quick-grid">
            <button v-for="a in quickActions" :key="a.label" class="dash-quick-btn" @click="a.run">
              <SvgIcon :name="a.icon" size="18px" />
              <span>{{ a.label }}</span>
            </button>
          </div>
        </div>

        <!-- 分类分布 -->
        <div class="dash-card">
          <h3 class="dash-card-title">分类分布</h3>
          <p v-if="!categories.length" class="dash-empty">还没有已发布文章的分类数据</p>
          <ul v-else class="dash-cat-list">
            <li v-for="c in categories" :key="c.name" class="dash-cat-item">
              <span class="dash-cat-name">{{ c.name }}</span>
              <span class="dash-cat-bar"><i :style="{ width: barWidth(c.count) }"></i></span>
              <span class="dash-cat-num">{{ c.count }}</span>
            </li>
          </ul>
        </div>
      </div>
    </template>
  </section>
</template>

<script setup>
import { computed, inject, onMounted, ref } from 'vue'
import { overview } from '../../api/admin'
import SvgIcon from '../../components/SvgIcon.vue'

const emit = defineEmits(['go'])

const toast = inject('adminToast', () => {})
const unauthorized = inject('adminUnauthorized', () => {})

const loading = ref(true)
const error = ref('')
const data = ref({})

async function load() {
  loading.value = true
  error.value = ''
  try {
    data.value = (await overview()) || {}
  } catch (err) {
    if (err && err.unauthorized) {
      unauthorized()
      return
    }
    error.value = (err && err.message) || '概览加载失败'
  } finally {
    loading.value = false
  }
}
onMounted(load)

const num = key => Number(data.value[key]) || 0

// 每张统计卡映射到对应的管理面板 key（go 为 null 表示仅展示不跳转）
const statCards = computed(() => [
  { label: '已发布文章', value: num('articleCount'), icon: 'common-articlePages', go: 'articles', sub: `草稿 ${num('draftCount')} 篇` },
  { label: '站点分类', value: num('categoryCount'), icon: 'common-archive', go: 'archive-categories' },
  { label: '标签数量', value: num('tagCount'), icon: 'common-icons', go: 'articles' },
  { label: '总浏览量', value: num('viewCount'), icon: 'common-web', go: null },
  { label: '番剧收录', value: num('bangumiCount'), icon: 'common-cat', go: 'bangumi-records' },
  { label: '工具站点', value: num('toolCount'), icon: 'common-tool', go: 'tool-sites' },
  { label: '音乐曲目', value: num('musicCount'), icon: 'common-music', go: 'musics' },
  { label: '首屏场景', value: num('carouselCount'), icon: 'common-component', go: 'scenes' },
  { label: '内容卡片', value: num('collapseCardCount'), icon: 'common-menu', go: 'collapse-cards' },
  { label: '时间线', value: num('timelineCount'), icon: 'common-timeline', go: 'timeline-events' }
])

const quickActions = [
  { label: '写新文章', icon: 'common-add', run: () => emit('go', 'articles', { create: true }) },
  { label: '首屏场景', icon: 'common-component', run: () => emit('go', 'scenes') },
  { label: '内容卡片', icon: 'common-menu', run: () => emit('go', 'collapse-cards') },
  { label: '个人介绍', icon: 'common-person', run: () => emit('go', 'team-members') },
  { label: '图片管理', icon: 'common-icons', run: () => emit('go', 'media') },
  { label: '音乐管理', icon: 'common-music', run: () => emit('go', 'musics') },
  { label: '工具站点', icon: 'common-tool', run: () => emit('go', 'tool-sites') },
  { label: '番剧管理', icon: 'common-open', run: () => emit('go', 'bangumi-records') },
  { label: '背景管理', icon: 'common-parallax', run: () => emit('go', 'media') },
  { label: 'AI 配置', icon: 'common-ai', run: () => toast('AI 配置敬请期待') }
]

const categories = computed(() => {
  const list = Array.isArray(data.value.categoryDistribution) ? data.value.categoryDistribution : []
  return [...list].sort((a, b) => (b.count || 0) - (a.count || 0))
})

const maxCount = computed(() => Math.max(1, ...categories.value.map(c => c.count || 0)))

function barWidth(count) {
  return `${Math.max(4, Math.round(((count || 0) / maxCount.value) * 100))}%`
}
</script>

<style scoped>
.dash-stats {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(196px, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}

.dash-stat {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 18px;
  background-color: var(--adm-card);
  border: 1px solid var(--adm-border-soft);
  border-radius: 20px;
  box-shadow: var(--adm-shadow);
  transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease, background-color 0.18s ease;
}
.dash-stat.is-clickable {
  cursor: pointer;
  user-select: none;
}
.dash-stat.is-clickable:hover {
  transform: translateY(-2px);
  border-color: var(--adm-accent-soft);
  background-color: color-mix(in srgb, var(--adm-accent-soft) 38%, var(--adm-card));
  box-shadow: var(--adm-shadow), 0 8px 20px var(--adm-accent-soft);
}
.dash-stat.is-clickable:focus-visible {
  outline: 2px solid var(--adm-accent);
  outline-offset: 2px;
}

.dash-stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  border-radius: 14px;
  background-color: var(--adm-accent-soft);
  color: var(--adm-accent-ink);
}

.dash-stat-info {
  min-width: 0;
}

.dash-stat-num {
  margin: 0;
  font-size: 29px;
  font-weight: bold;
  line-height: 1.2;
  color: var(--adm-text);
}

.dash-stat-label {
  margin: 2px 0 0;
  font-size: 15px;
  color: var(--adm-text-dim);
}

.dash-stat-sub {
  margin: 2px 0 0;
  font-size: 14px;
  color: var(--adm-text-faint);
}

.dash-row {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 14px;
  align-items: start;
}

@media (max-width: 980px) {
  .dash-row {
    grid-template-columns: 1fr;
  }
}

.dash-card {
  padding: 20px 22px;
  background-color: var(--adm-card);
  border: 1px solid var(--adm-border-soft);
  border-radius: 20px;
  box-shadow: var(--adm-shadow);
}

.dash-card-title {
  margin: 0 0 14px;
  font-size: 18.5px;
  color: var(--adm-text);
}

.dash-quick-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(128px, 1fr));
  gap: 10px;
}

.dash-quick-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 14px;
  border: 1px solid var(--adm-border-soft);
  border-radius: 14px;
  background-color: var(--adm-card-2);
  color: var(--adm-text-dim);
  font-family: inherit;
  font-size: 16px;
  cursor: pointer;
  transition: background-color 0.2s ease, color 0.2s ease, transform 0.15s ease;
}

.dash-quick-btn:hover {
  background-color: var(--adm-accent-soft);
  color: var(--adm-accent-ink);
  transform: translateY(-1px);
}

.dash-empty {
  margin: 0;
  padding: 20px 0;
  font-size: 16px;
  color: var(--adm-text-faint);
}

.dash-cat-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.dash-cat-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.dash-cat-name {
  flex-shrink: 0;
  width: 92px;
  font-size: 15.5px;
  color: var(--adm-text-dim);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dash-cat-bar {
  flex: 1;
  height: 10px;
  border-radius: 999px;
  background-color: var(--adm-border-soft);
  overflow: hidden;
}

.dash-cat-bar i {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--adm-accent) 0%, var(--adm-accent-strong) 100%);
  transition: width 0.4s ease;
}

.dash-cat-num {
  flex-shrink: 0;
  min-width: 26px;
  text-align: right;
  font-size: 15.5px;
  font-weight: bold;
  color: var(--adm-text);
}

/* ---------- 移动端适配（≤900px，追加） ---------- */

@media (max-width: 900px) {
  .dash-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
  }

  .dash-stat {
    gap: 10px;
    padding: 14px;
  }

  .dash-quick-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .dash-quick-btn {
    padding: 12px 10px;
  }

  .dash-card {
    padding: 16px;
  }

  /* 分类分布条：名称收窄，条形自适应剩余宽度 */
  .dash-cat-name {
    width: 72px;
  }
}

@media (max-width: 480px) {
  .dash-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .dash-stat-icon {
    width: 36px;
    height: 36px;
    border-radius: 10px;
  }

  .dash-stat-num {
    font-size: 22px;
  }

  .dash-stat-label {
    font-size: 13px;
  }

  .dash-stat-sub {
    font-size: 12.5px;
  }
}
</style>
