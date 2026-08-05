<template>
  <main class="tree-hole-page">
    <div class="tree-hole-dashboard">
      <!-- 弹幕流 -->
      <section class="tree-hole-dashboard-main">
        <div class="tree-hole-section-content tree-hole-dashboard-main-content">
          <CxSection eyebrow="Tree Hole - 弹幕流">
            <template #pill>让安慰以漂浮的方式互相看见</template>
            <template #action>
              <button class="cx-button cx-button--section is-round is-circle" type="button" @click="danmakuPaused = !danmakuPaused">
                <span class="cx-button__content"><SvgIcon :name="danmakuPaused ? 'music-play' : 'music-pause'" size="18px" /></span>
              </button>
            </template>
            <section class="tree-hole-danmaku-panel">
              <div class="tree-hole-danmaku-stage-card">
                <div class="tree-hole-danmaku-stage">
                  <div ref="stageRef" class="vue-danmaku tree-hole-danmaku-stage-inner">
                    <div class="danmus show" :class="{ paused: danmakuPaused }">
                      <div
                        v-for="d in danmus"
                        :key="d.key"
                        class="dm move"
                        :style="d.style"
                      >
                        <div class="tree-hole-danmaku-bubble tree-hole-danmaku-bubble-primary">
                          <span class="tree-hole-danmaku-bubble-mood">{{ d.mood }}</span>
                          <strong>{{ d.nickname }}</strong>
                          <p>{{ d.content }}</p>
                          <button class="tree-hole-danmaku-bubble-like" type="button" @click="like(d)">
                            <SvgIcon name="common-thumbUp" size="14px" class="tree-hole-danmaku-bubble-like-icon" />
                          </button>
                          <span>{{ d.likeCount }}</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              <form class="tree-hole-danmaku-composer" @submit.prevent="send">
                <div class="tree-hole-danmaku-mood-list">
                  <span
                    v-for="m in moods"
                    :key="m.label"
                    class="cx-tag cx-tag--small is-round tree-hole-danmaku-mood-item"
                    :class="[`cx-tag--${m.type}`, mood === m.label ? 'is-active' : 'is-plain']"
                    @click="mood = m.label"
                  >
                    <span class="cx-tag__content"><span class="cx-tag__prefix">#</span><span class="cx-tag__label">{{ m.label }}</span></span>
                  </span>
                </div>
                <div class="tree-hole-danmaku-composer-row">
                  <div class="tree-hole-danmaku-input-shell">
                    <div class="tree-hole-danmaku-input-icon"><SvgIcon name="common-tree" size="18px" /></div>
                    <div class="cx-input">
                      <input
                        v-model="draft"
                        class="cx-input__inner tree-hole-danmaku-input"
                        type="text"
                        maxlength="80"
                        :placeholder="thConfig?.placeholder || DEFAULT_PLACEHOLDER"
                      />
                    </div>
                    <button class="cx-button cx-button--primary is-circle" type="submit" :disabled="sending" :class="{ 'is-loading': sending }">
                      <span class="cx-button__content">
                        <SvgIcon v-if="sendSuccess" name="common-thumbUp" size="18px" />
                        <SvgIcon v-else name="common-send" size="18px" />
                      </span>
                    </button>
                  </div>
                </div>
                <div class="tree-hole-danmaku-composer-meta">
                  <span class="tree-hole-danmaku-composer-hint">{{ sendSuccess ? '✓ 已发布，请稍候再发' : '写下的话会化作弹幕飘过树洞。' }}</span>
                  <span class="tree-hole-danmaku-composer-count">{{ draft.length }}/80</span>
                </div>
              </form>
            </section>
          </CxSection>
        </div>
      </section>

      <!-- 疗愈书架 -->
      <section class="tree-hole-dashboard-secondary">
        <div class="tree-hole-section-content tree-hole-dashboard-secondary-content">
          <CxSection eyebrow="Comfort Shelf - 疗愈书架">
            <template #pill>把情绪落进更安静、更具体的日常片段里</template>
            <section class="tree-hole-healing-panel">
              <div class="tree-hole-healing-grid">
                <article
                  v-for="(c, i) in calledTexts"
                  :key="c.id"
                  v-reveal="60 + i * 80"
                  class="tree-hole-healing-card"
                  :class="`tree-hole-healing-card-${VARIANTS[i % VARIANTS.length]}`"
                >
                  <div class="tree-hole-healing-card-image-wrap">
                    <img class="tree-hole-healing-card-image" :src="c.imageUrl || landscape(i)" :alt="c.title" loading="lazy" />
                    <div class="tree-hole-healing-card-tag-row">
                      <span class="cx-tag cx-tag--primary cx-tag--small is-round tree-hole-healing-card-tag">
                        <span class="cx-tag__content"><span class="cx-tag__label">{{ c.tag }}</span></span>
                      </span>
                      <span class="cx-tag cx-tag--neutral cx-tag--small is-round is-plain tree-hole-healing-card-read-time">
                        <span class="cx-tag__content"><span class="cx-tag__label">{{ c.readTime }}</span></span>
                      </span>
                    </div>
                  </div>
                  <div class="tree-hole-healing-card-body">
                    <h3>{{ c.title }}</h3>
                    <p class="tree-hole-healing-card-content">{{ c.content }}</p>
                    <div class="tree-hole-healing-card-summary">
                      <span>一句总结</span>
                      <p>{{ c.summary }}</p>
                    </div>
                  </div>
                </article>
              </div>
            </section>
          </CxSection>
        </div>
      </section>
    </div>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import CxSection from '../components/CxSection.vue'
import SvgIcon from '../components/SvgIcon.vue'
import { api } from '../api'
import { ensureVisitorToken } from '../utils/visitorId'
import '../assets/css/tree-hole.css'

const VARIANTS = ['featured', 'portrait', 'note', 'wide', 'compact', 'balanced']
const MOOD_TYPE_MAP = { '轻声': 'primary', '鼓劲': 'success', '拥抱': 'warning', '放空': 'neutral' }
const DEFAULT_MOOD_OPTIONS = ['轻声', '鼓劲', '拥抱', '放空']
const DEFAULT_PLACEHOLDER = '写下一句此刻想被接住的话...'

const thConfig = ref(null)
const moods = computed(() => {
  const labels = thConfig.value?.moodOptions || DEFAULT_MOOD_OPTIONS
  return labels.map(label => ({ label, type: MOOD_TYPE_MAP[label] || 'neutral' }))
})

const stageRef = ref(null)
const danmus = ref([])
const danmakuPaused = ref(false)
const calledTexts = ref([])
const draft = ref('')
const mood = ref('轻声')
const sending = ref(false)
const sendSuccess = ref(false)
let sendCooldownTimer = null
let laneCursor = 0
let keySeq = 0
const MAX_DANMU_COUNT = 100

function landscape(i) {
  return `/image/bg/Landscape/${String((i % 13) + 1).padStart(2, '0')}.webp`
}

function addDanmu(b, initial = false) {
  const stage = stageRef.value
  const stageWidth = stage ? stage.clientWidth : 1200
  const stageHeight = stage ? stage.clientHeight : 420
  const lanes = Math.max(4, Math.floor(stageHeight / 58))
  const lane = laneCursor++ % lanes
  // 宽度要装下整块胶囊：mood+昵称+点赞约 150px，正文按字数估算；
  // 估窄了 flex 会把 overflow:hidden 的正文压成 0 宽（只显示昵称和点赞）
  const fixedW = 150 + String(b.nickname || '').length * 9
  const width = Math.min(360, Math.round(stageWidth * 0.7), fixedW + String(b.content || '').length * 15)
  const duration = 14 + Math.random() * 8
  danmus.value.push({
    key: `dm-${keySeq++}`,
    id: b.id,
    nickname: b.nickname,
    mood: b.mood || '轻声',
    content: b.content,
    likeCount: b.likeCount || 0,
    style: {
      top: `${lane * 58}px`,
      left: '100%',
      width: `${width}px`,
      opacity: 1,
      zIndex: 4,
      '--dm-scroll-width': `-${stageWidth + width + 80}px`,
      animationDuration: `${duration}s`,
      animationDelay: initial ? `${Math.random() * duration}s` : '0s',
      animationIterationCount: 'infinite'
    }
  })
  // 超出限制时移除最早的弹幕，防止 DOM 持续增长
  if (danmus.value.length > MAX_DANMU_COUNT) {
    danmus.value.shift()
  }
}

async function like(d) {
  if (!d.id) return
  try {
    // SEC-001：点赞前确保持有服务端签发的匿名身份；身份无效时刷新后重试一次
    if (!(await ensureVisitorToken())) {
      console.warn('[点赞] 未获取到匿名身份，已跳过')
      return
    }
    try {
      const updated = await api.likeBarrage(d.id)
      if (updated) d.likeCount = updated.likeCount
    } catch (err) {
      const msg = (err && err.message) || ''
      if (msg.includes('访客标识无效')) {
        const updated = await api.likeBarrage(d.id)
        if (updated) d.likeCount = updated.likeCount
      } else {
        throw err
      }
    }
  } catch (e) { console.warn('[点赞] 操作失败:', e) }
}

async function send() {
  const content = draft.value.trim()
  if (!content || sending.value) return
  sending.value = true
  draft.value = ''
  try {
    const saved = await api.addBarrage({ content, mood: mood.value })
    addDanmu(saved || { content, mood: mood.value, nickname: '树友-0001' })
    sendSuccess.value = true
    clearTimeout(sendCooldownTimer)
    sendCooldownTimer = setTimeout(() => {
      sendSuccess.value = false
      sending.value = false
    }, 3000)
  } catch (e) {
    sending.value = false
    addDanmu({ content, mood: mood.value, nickname: '树友-0001' })
    if (e?.message) alert(e.message)
  }
}

onMounted(async () => {
  try {
    const [barrages, texts] = await Promise.all([api.treeHoleBarrages(), api.calledTexts()])
    calledTexts.value = texts.records || []
    for (const b of barrages.records || []) addDanmu(b, true)
  } catch (e) { console.warn('[树洞] 加载失败:', e) }
  try {
    thConfig.value = await api.siteContent('treehole-config')
  } catch (e) { console.warn('[树洞] 配置加载失败:', e) }
})
</script>

<style>
/* 弹幕悬停暂停：方便阅读与点赞（原 CSS 里 .dm.move 是 running，这里悬停时覆盖） */
.tree-hole-danmaku-stage-inner .dm:hover {
  animation-play-state: paused !important;
  z-index: 9 !important;
}
.tree-hole-danmaku-stage-inner .dm:hover .tree-hole-danmaku-bubble {
  transform: scale(1.04);
  box-shadow: 0 10px 26px rgba(40, 70, 110, 0.28);
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}
</style>
