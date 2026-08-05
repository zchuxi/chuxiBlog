<template>
  <main class="article-page">
    <section class="article-page-shell">
      <!-- 文章信息 -->
      <section v-reveal="0" class="timeline-section-container article-hero-section-container">
        <div class="timeline-section-container-header">
          <div class="timeline-section-container-header-main">
            <div class="timeline-section-container-header-title-row">
              <p class="timeline-section-container-header-eyebrow">Article Overview - 文章信息</p>
              <div class="timeline-section-container-header-tag-list">
                <span
                  v-for="t in article?.tags || []"
                  :key="t"
                  class="cx-tag cx-tag--section cx-tag--small is-round is-plain article-hero-section-header-tag"
                >
                  <span class="cx-tag__content"><span class="cx-tag__prefix">#</span><span class="cx-tag__label">{{ t }}</span></span>
                </span>
              </div>
            </div>
          </div>
          <div class="timeline-section-container-header-right-action">
            <button class="cx-button cx-button--section is-round is-circle" type="button">
              <span class="cx-button__content"><SvgIcon name="common-ai" size="18px" /></span>
            </button>
          </div>
        </div>
        <div class="timeline-section-container-content">
          <header class="article-hero-section">
            <div class="article-hero-section-backdrop"></div>
            <div class="article-hero-section-main">
              <div class="article-hero-section-meta">
                <span class="article-hero-section-category">{{ article?.categoryName }}</span>
                <span class="article-hero-section-date">更新于 {{ updateDate }}</span>
                <span class="article-hero-section-date">全文约 {{ wordCount }} 字</span>
              </div>
              <h1 class="article-hero-section-title">{{ article?.title }}</h1>
              <p class="article-hero-section-summary">{{ article?.summary }}</p>
            </div>
            <div class="article-hero-section-cover-panel">
              <div
                class="article-hero-section-cover"
                :style="{ backgroundImage: `linear-gradient(rgba(29, 46, 84, 0.08), rgba(29, 46, 84, 0.24)), url(${JSON.stringify(cover)})` }"
              ></div>
            </div>
          </header>
        </div>
      </section>

      <!-- 正文 + 目录 -->
      <section class="timeline-section-container article-content-section">
        <div class="timeline-section-container-header">
          <div class="timeline-section-container-header-main">
            <div class="timeline-section-container-header-title-row">
              <p class="timeline-section-container-header-eyebrow">Article Content - 正文</p>
            </div>
          </div>
        </div>
        <div class="timeline-section-container-content">
          <div class="article-content-layout">
            <article class="article-content-main-card">
              <div class="article-content-main-preview">
                <div :id="`article-preview-${articleId}`" class="md-editor md-editor-previewOnly">
                  <div
                    :id="`article-preview-${articleId}-preview`"
                    ref="previewRef"
                    class="md-editor-preview default-theme md-editor-scrn"
                    v-html="renderedHtml"
                  ></div>
                </div>
              </div>
            </article>
            <section class="timeline-section-container article-catalog-card">
              <div class="timeline-section-container-header">
                <div class="timeline-section-container-header-main">
                  <div class="timeline-section-container-header-title-row">
                    <p class="timeline-section-container-header-eyebrow">Article Catalog - 目录导航</p>
                  </div>
                </div>
              </div>
              <div class="timeline-section-container-content">
                <div class="article-catalog-card-panel">
                  <div class="article-catalog-card-scroll">
                    <nav class="article-catalog-card-nav">
                      <button
                        v-for="h in headings"
                        :key="h.id"
                        type="button"
                        class="article-catalog-card-link"
                        :class="{ 'is-active': activeHeading === h.id }"
                        :style="{ '--article-catalog-card-indent': `${(h.level - 1) * 14}px` }"
                        @click="scrollToHeading(h.id)"
                      >
                        <div class="cx-popover-wrapper">
                          <div class="cx-popover-trigger">
                            <span class="article-catalog-card-link-text">{{ h.text }}</span>
                          </div>
                        </div>
                      </button>
                    </nav>
                  </div>
                </div>
              </div>
            </section>
          </div>
        </div>
      </section>

      <!-- 前后篇导航 -->
      <section class="article-nav-section" v-if="prevArticle || nextArticle">
        <div class="article-nav">
          <router-link v-if="prevArticle" :to="`/article/${prevArticle.id}`" class="article-nav-item article-nav-prev">
            <span class="article-nav-label">上一篇</span>
            <span class="article-nav-title">{{ prevArticle.title }}</span>
          </router-link>
          <div v-else class="article-nav-spacer"></div>
          <router-link v-if="nextArticle" :to="`/article/${nextArticle.id}`" class="article-nav-item article-nav-next">
            <span class="article-nav-label">下一篇</span>
            <span class="article-nav-title">{{ nextArticle.title }}</span>
          </router-link>
          <div v-else class="article-nav-spacer"></div>
        </div>
      </section>

      <!-- 评论区 -->
      <section v-reveal="140" class="timeline-section-container article-comments-section">
        <div class="timeline-section-container-header">
          <div class="timeline-section-container-header-main">
            <div class="timeline-section-container-header-title-row">
              <p class="timeline-section-container-header-eyebrow">Comments - 评论区</p>
            </div>
          </div>
        </div>
        <div class="timeline-section-container-content">
          <div class="article-comments-panel">
            <header class="article-comments-header">
              <div class="article-comments-header-title-group">
                <div class="article-comments-header-label">
                  <SvgIcon name="common-chat" size="16px" />
                  <span>Comments</span>
                </div>
                <h2 class="article-comments-header-title">评论区</h2>
                <p class="article-comments-header-copy">欢迎留下你的想法，游客可直接评论。</p>
              </div>
              <div class="article-comments-header-count">{{ commentTotal }} 条主评论<span v-if="comments.length < commentTotal">（已加载 {{ comments.length }} 条）</span></div>
            </header>
            <form class="article-comments-composer" @submit.prevent="submitComment">
              <div class="article-comments-composer-head">
                  <div class="article-comments-composer-user">
                  <div class="article-comments-composer-avatar"><span>游</span></div>
                  <div class="article-comments-composer-user-meta">
                    <strong class="article-comments-composer-user-name">游客模式</strong>
                    <span class="article-comments-composer-user-hint">发表评论后将以「游客」身份显示。</span>
                  </div>
                </div>
              </div>
              <div class="article-comments-composer-textarea">
                <div class="cx-input is-textarea">
                  <textarea
                    v-model="commentDraft"
                    class="cx-input__inner cx-input__textarea"
                    maxlength="500"
                    placeholder="说点什么吧…（游客可直接评论）"
                  ></textarea>
                </div>
              </div>
              <div class="article-comments-composer-toolbar">
                <div class="article-comments-composer-toolbar-left">
                  <div class="cx-popover-wrapper">
                    <div class="cx-popover-trigger">
                      <button class="article-comments-toolbar-button" type="button">
                        <SvgIcon name="common-chat" size="14px" />
                        <span>表情</span>
                      </button>
                    </div>
                  </div>
                  <span class="article-comments-composer-toolbar-hint">支持点赞与楼中楼回复（演示站点未开放，可正常发表）。</span>
                </div>
                <div class="article-comments-composer-toolbar-right">
                  <span class="article-comments-composer-count">{{ commentDraft.length }}/500</span>
                  <button class="cx-button cx-button--primary" type="submit" :disabled="commentSubmitting" :class="{ 'is-loading': commentSubmitting }">
                    <span class="cx-button__content">
                      <SvgIcon name="common-send" size="18px" />
                      <span v-if="commentSuccess" class="cx-button__label" style="margin-left:4px">✓ 已发布</span>
                    </span>
                    <span v-if="!commentSuccess" class="cx-button__label">发表评论</span>
                  </button>
                </div>
              </div>
            </form>
            <div v-if="!comments.length" class="article-comments-state">还没有人留言，来做第一个留下回声的人吧。</div>
            <div v-else class="article-comments-list">
              <article v-for="c in comments" :key="c.id" class="article-comments-card">
                <div class="article-comments-card-avatar"><span>{{ (c.nickname || '游')[0] }}</span></div>
                <div class="article-comments-card-main">
                  <div class="article-comments-card-content">
                    <div class="article-comments-card-headline">
                      <div class="article-comments-card-author-group">
                        <span class="article-comments-card-author">{{ c.nickname }}</span>
                        <span class="article-comments-card-time">{{ (c.createdAt || '').replace('T', ' ').slice(0, 16) }}</span>
                      </div>
                    </div>
                    <p class="article-comments-card-text">{{ c.content }}</p>
                    <div class="article-comments-card-actions">
                      <button
                        type="button"
                        class="article-comments-action-button"
                        :class="{ 'is-liked': c.liked }"
                        :aria-pressed="Boolean(c.liked)"
                        @click="likeComment(c)"
                      >
                        <SvgIcon name="common-thumbUp" size="14px" />
                        <span>{{ c.likeCount || 0 }}</span>
                      </button>
                    </div>
                  </div>
                </div>
              </article>
            </div>
            <button
              v-if="comments.length < commentTotal"
              type="button"
              class="article-comments-load-more"
              :disabled="commentLoadingMore"
              @click="loadMoreComments"
            >
              {{ commentLoadingMore ? '加载中…' : '加载更多评论' }}
            </button>
          </div>
        </div>
      </section>
    </section>
  </main>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import SvgIcon from '../components/SvgIcon.vue'
import { api } from '../api'
import { renderMarkdown } from '../utils/markdown'
import { coverOf } from '../utils/display'
import { ensureVisitorToken } from '../utils/visitorId'
import '../assets/css/article.css'
import '../assets/css/preview.css'

const route = useRoute()
const articleId = computed(() => route.params.id)

const article = ref(null)
const prevArticle = ref(null)
const nextArticle = ref(null)
const comments = ref([])
const commentTotal = ref(0)
const commentPage = ref(1)
const commentLoadingMore = ref(false)

async function loadComments(page = 1) {
  try {
    const data = await api.articleComments(articleId.value, page, 20)
    const records = (data && data.records) || []
    commentPage.value = page
    commentTotal.value = Number((data && data.total) || 0)
    comments.value = page === 1 ? records : [...comments.value, ...records]
  } catch (e) {
    console.warn('[评论] 加载失败:', e)
    if (page === 1) { comments.value = []; commentTotal.value = 0 }
  }
}

async function loadMoreComments() {
  if (commentLoadingMore.value) return
  commentLoadingMore.value = true
  try {
    await loadComments(commentPage.value + 1)
  } finally {
    commentLoadingMore.value = false
  }
}
const renderedHtml = ref('')
const headings = ref([])
const activeHeading = ref('')
const previewRef = ref(null)
const commentDraft = ref('')
const commentSubmitting = ref(false)
const commentSuccess = ref(false)
let commentCooldownTimer = null

const cover = computed(() => coverOf(article.value || {}, Number(articleId.value) || 0))
const updateDate = computed(() => {
  const d = article.value?.updatedAt || article.value?.createdAt
  return d ? String(d).slice(0, 10).replaceAll('-', '/') : ''
})
const wordCount = computed(() => {
  const len = (article.value?.content || '').replace(/\s/g, '').length
  return Math.round(len / 50) * 50 || len
})

let headingObserver = null
let jsonLdScript = null

async function load() {
  try {
    const data = await api.articleDetail(articleId.value)
    article.value = data.article
    prevArticle.value = data.prev || null
    nextArticle.value = data.next || null
    const { html, headings: hs } = renderMarkdown(article.value.content)
    renderedHtml.value = html
    headings.value = hs
    activeHeading.value = hs.length ? hs[0].id : ''
    await nextTick()
    observeHeadings()
    injectJsonLd()
  } catch (e) { console.warn('[文章] 加载失败:', e) }
  try {
    await loadComments(1)
  } catch (e) { console.warn('[评论] 加载失败:', e) }
}

function observeHeadings() {
  if (headingObserver) headingObserver.disconnect()
  const root = previewRef.value
  if (!root) return
  headingObserver = new IntersectionObserver(entries => {
    for (const e of entries) {
      if (e.isIntersecting) activeHeading.value = e.target.id
    }
  }, { rootMargin: '-80px 0px -70% 0px' })
  root.querySelectorAll('h1, h2, h3, h4').forEach(h => headingObserver.observe(h))
}

function scrollToHeading(id) {
  activeHeading.value = id
  const el = document.getElementById(id)
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

async function submitComment() {
  const content = commentDraft.value.trim()
  if (!content || commentSubmitting.value) return
  commentSubmitting.value = true
  try {
    await api.addComment(articleId.value, { content, nickname: '游客' })
    commentDraft.value = ''
    await loadComments(1)
    commentSuccess.value = true
    clearTimeout(commentCooldownTimer)
    commentCooldownTimer = setTimeout(() => {
      commentSuccess.value = false
      commentSubmitting.value = false
    }, 3000)
  } catch (e) {
    commentSubmitting.value = false
    alert(e?.message || '评论提交失败，请稍后再试')
  }
}

async function likeComment(c) {
  try {
    // SEC-001：点赞前确保持有服务端签发的匿名身份；身份无效时刷新后重试一次
    if (!(await ensureVisitorToken())) {
      console.warn('[点赞] 未获取到匿名身份，已跳过')
      return
    }
    try {
      const updated = await api.likeComment(c.id)
      if (updated) Object.assign(c, updated)
    } catch (err) {
      const msg = (err && err.message) || ''
      if (msg.includes('访客标识无效')) {
        // 拦截器已把响应头下发的 token 存入本地，重试一次
        const updated = await api.likeComment(c.id)
        if (updated) Object.assign(c, updated)
      } else {
        throw err
      }
    }
  } catch (e) { console.warn('[点赞] 操作失败:', e) }
}

watch(articleId, load)

onMounted(load)

function injectJsonLd() {
  if (jsonLdScript) {
    jsonLdScript.remove()
    jsonLdScript = null
  }
  if (!article.value) return
  const jsonLd = {
    '@context': 'https://schema.org',
    '@type': 'BlogPosting',
    headline: article.value.title,
    description: article.value.summary,
    datePublished: article.value.createdAt,
    dateModified: article.value.updatedAt,
    author: { '@type': 'Person', name: '初曦' }
  }
  jsonLdScript = document.createElement('script')
  jsonLdScript.type = 'application/ld+json'
  jsonLdScript.textContent = JSON.stringify(jsonLd)
  document.head.appendChild(jsonLdScript)
}

onBeforeUnmount(() => {
  if (headingObserver) headingObserver.disconnect()
  if (jsonLdScript) jsonLdScript.remove()
})
</script>
