<template>
  <transition name="layout-article-search-panel">
    <div v-if="modelValue" class="layout-article-search-overlay">
      <div class="layout-article-search-overlay-mask" @click="close"></div>
      <div class="layout-article-search-panel">
        <div class="layout-article-search-header">
          <div class="layout-article-search-input-shell">
            <span class="layout-article-search-input-icon"><SvgIcon name="common-search" size="18px" /></span>
            <input
              ref="searchInputRef"
              v-model="searchKeyword"
              class="layout-article-search-input"
              placeholder="搜索文章标题、摘要或标签..."
              @keydown.enter="doSearch"
            />
            <button v-if="searchKeyword" class="layout-article-search-clear-btn" @click="searchKeyword = ''; searchResult = null">
              <SvgIcon name="common-big-close" size="14px" />
            </button>
          </div>
          <button class="layout-article-search-close-btn" @click="close">
            <SvgIcon name="common-big-close" size="16px" />
          </button>
        </div>
        <div class="layout-article-search-body">
          <div v-if="searchLoading" class="layout-article-search-loading-state">
            <span class="layout-article-search-loading-icon"><SvgIcon name="common-loading" size="22px" /></span>
            <p>正在搜索…</p>
          </div>
          <div v-else-if="searchResult && !searchResult.records.length" class="layout-article-search-empty-state">
            <p class="layout-article-search-empty-title">没有找到相关内容</p>
            <p class="layout-article-search-empty-text">换个关键词再试试吧。</p>
          </div>
          <template v-else-if="searchResult">
            <div class="layout-article-search-result-head">
              <span class="layout-article-search-result-title">搜索结果</span>
              <span class="layout-article-search-result-count">共 {{ searchResult.total }} 篇</span>
            </div>
            <div class="layout-article-search-grid">
              <article
                v-for="a in searchResult.records"
                :key="a.id"
                class="layout-article-search-card"
                @click="openArticle(a.id)"
              >
                <div class="layout-article-search-card-frame">
                  <div class="layout-article-search-card-media">
                    <img class="layout-article-search-card-image" :src="coverOf(a)" :alt="a.title" />
                  </div>
                  <div class="layout-article-search-card-content">
                    <div class="layout-article-search-card-copy">
                      <div class="layout-article-search-card-meta">
                        <span class="cx-tag cx-tag--primary cx-tag--small is-round">
                          <span class="cx-tag__content"><span class="cx-tag__label">{{ a.categoryName }}</span></span>
                        </span>
                        <span class="layout-article-search-card-date">{{ (a.updatedAt || '').slice(0, 10) }}</span>
                      </div>
                      <h3 class="layout-article-search-card-title">{{ a.title }}</h3>
                      <p class="layout-article-search-card-summary">{{ a.summary }}</p>
                    </div>
                    <div class="layout-article-search-card-footer">
                      <div class="layout-article-search-card-tag-list">
                        <span v-for="t in a.tags" :key="t" class="cx-tag cx-tag--primary cx-tag--small is-round">
                          <span class="cx-tag__content"><span class="cx-tag__prefix">#</span><span class="cx-tag__label">{{ t }}</span></span>
                        </span>
                      </div>
                      <button class="layout-article-search-card-action" type="button">打开全文</button>
                    </div>
                  </div>
                </div>
              </article>
            </div>
          </template>
          <div v-else class="layout-article-search-empty-state">
            <p class="layout-article-search-empty-title">输入关键词开始搜索</p>
            <p class="layout-article-search-empty-text">支持文章标题、摘要与标签。</p>
          </div>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import SvgIcon from '../../components/SvgIcon.vue'
import { api } from '../../api'

const props = defineProps({
  modelValue: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue'])

const router = useRouter()
const searchKeyword = ref('')
const searchLoading = ref(false)
const searchResult = ref(null)
const searchInputRef = ref(null)

watch(() => props.modelValue, open => {
  if (open) nextTick(() => searchInputRef.value && searchInputRef.value.focus())
})

async function doSearch() {
  const kw = searchKeyword.value.trim()
  if (!kw) return
  searchLoading.value = true
  try {
    searchResult.value = await api.searchArticles(kw, 1, 12)
  } catch (e) {
    console.warn('[搜索] 查询失败:', e)
    searchResult.value = { records: [], total: 0 }
  } finally {
    searchLoading.value = false
  }
}

function close() {
  emit('update:modelValue', false)
}

function openArticle(id) {
  close()
  router.push(`/article/${id}`)
}

const FALLBACK_COVERS = ['/image/bg/Landscape/01.webp', '/image/bg/Landscape/05.webp', '/image/bg/Landscape/08.webp', '/image/bg/Landscape/10.webp', '/image/bg/Landscape/12.webp', '/image/bg/Landscape/13.webp']
function coverOf(a) {
  return a.coverUrl || FALLBACK_COVERS[a.id % FALLBACK_COVERS.length]
}
</script>
