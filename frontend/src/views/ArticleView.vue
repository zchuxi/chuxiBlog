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
                <p class="article-comments-header-copy">登录后可评论、回复与点赞。</p>
              </div>
              <div class="article-comments-header-count">{{ comments.length }} 条主评论 / {{ comments.length }} 条全部评论</div>
            </header>
            <form class="article-comments-composer" @submit.prevent="submitComment">
              <div class="article-comments-composer-head">
                <div class="article-comments-composer-user">
                  <div class="article-comments-composer-avatar"><span>游</span></div>
                  <div class="article-comments-composer-user-meta">
                    <strong class="article-comments-composer-user-name">游客模式</strong>
                    <span class="article-comments-composer-user-hint">登录后发送的评论会显示在这里。</span>
                  </div>
                </div>
              </div>
              <div class="article-comments-composer-textarea">
                <div class="cx-input is-textarea">
                  <textarea
                    v-model="commentDraft"
                    class="cx-input__inner cx-input__textarea"
                    maxlength="500"
                    placeholder="登录后才能参与评论与点赞"
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
                  <span class="article-comments-composer-toolbar-hint">二级评论已开启，可直接回复主评论或回复楼中楼。</span>
                </div>
                <div class="article-comments-composer-toolbar-right">
                  <span class="article-comments-composer-count">{{ commentDraft.length }}/500</span>
                  <button class="cx-button cx-button--primary" type="submit">
                    <span class="cx-button__content"><SvgIcon name="common-send" size="18px" /></span>
                    <span class="cx-button__label">发表评论</span>
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
                      <button type="button" class="article-comments-action-button" @click="likeComment(c)">
                        <SvgIcon name="common-thumbUp" size="14px" />
                        <span>{{ c.likeCount || 0 }}</span>
                      </button>
                    </div>
                  </div>
                </div>
              </article>
            </div>
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

const route = useRoute()
const articleId = computed(() => route.params.id)

const article = ref(null)
const comments = ref([])
const renderedHtml = ref('')
const headings = ref([])
const activeHeading = ref('')
const previewRef = ref(null)
const commentDraft = ref('')

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

async function load() {
  try {
    article.value = await api.articleDetail(articleId.value)
    const { html, headings: hs } = renderMarkdown(article.value.content)
    renderedHtml.value = html
    headings.value = hs
    activeHeading.value = hs.length ? hs[0].id : ''
    await nextTick()
    observeHeadings()
  } catch { /* 文章不存在 */ }
  try {
    comments.value = await api.articleComments(articleId.value) || []
  } catch { comments.value = [] }
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
  if (!content) return
  try {
    await api.addComment(articleId.value, { content, nickname: '游客' })
    commentDraft.value = ''
    comments.value = await api.articleComments(articleId.value) || []
  } catch { /* 忽略 */ }
}

async function likeComment(c) {
  try {
    const updated = await api.likeComment(c.id)
    if (updated) Object.assign(c, updated)
  } catch { /* 忽略 */ }
}

watch(articleId, load)

onMounted(load)

onBeforeUnmount(() => {
  if (headingObserver) headingObserver.disconnect()
})
</script>
