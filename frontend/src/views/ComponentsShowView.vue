<template>
  <main class="components-page">
    <!-- 页面 HERO -->
    <section class="components-page__hero">
      <div class="components-page__hero-main">
        <div class="components-page__hero-content">
          <p class="components-page__hero-eyebrow">ChuXi Components Showcase</p>
          <h1 class="components-page__hero-title">组件展示页</h1>
          <p class="components-page__hero-description">将 CX 组件拆成独立演示单元，侧边菜单快速定位，内容区统一复用卡片结构，便于后续持续扩展新的组件示例。</p>
        </div>
        <div class="components-page__hero-meta-list">
          <div class="components-page__hero-meta-card">
            <span class="components-page__hero-meta-label">展示节奏</span>
            <strong class="components-page__hero-meta-value">Sidebar + Demo Card</strong>
            <p class="components-page__hero-meta-description">左侧导航定位，右侧逐块查看每个组件的交互与状态。</p>
          </div>
          <div class="components-page__hero-meta-card">
            <span class="components-page__hero-meta-label">当前收录</span>
            <strong class="components-page__hero-meta-value">6 个示例区块</strong>
            <p class="components-page__hero-meta-description">覆盖按钮、消息、弹层、单选、开关与标签等常用基础组件。</p>
          </div>
        </div>
      </div>
      <div class="components-page__hero-aside">
        <div class="components-page__hero-summary">
          <span class="components-page__hero-summary-label">已收录组件</span>
          <strong class="components-page__hero-summary-value">6</strong>
          <span class="components-page__hero-summary-unit">Sections</span>
        </div>
        <div class="components-page__hero-chip-list">
          <span v-for="m in menus" :key="m.id" class="components-page__hero-chip">{{ m.name }}</span>
        </div>
      </div>
    </section>

    <!-- 内容区 -->
    <section class="components-page__content">
      <aside class="demo-sidebar">
        <h2 class="demo-sidebar__title">组件菜单</h2>
        <ul class="demo-sidebar__menu">
          <li v-for="m in menus" :key="m.id">
            <button
              type="button"
              class="demo-sidebar__menu-btn"
              :class="{ 'is-active': activeMenu === m.id }"
              @click="goPanel(m.id)"
            >
              <span class="demo-sidebar__menu-btn-name">{{ m.name }}</span>
              <span class="demo-sidebar__menu-btn-desc">{{ m.desc }}</span>
            </button>
          </li>
        </ul>
      </aside>
      <div class="demo-main">
        <!-- CX-button -->
        <section id="demo-button" class="demo-panel">
          <header class="demo-panel-header">
            <SvgIcon name="common-icons" size="18px" />
            <span>CX-button 示例</span>
          </header>
          <p class="demo-panel-description">支持类型、圆角、图标、加载态与自定义颜色。</p>
          <div class="demo-panel-body">
            <div class="button-demo-list">
              <button class="lx-button lx-button--primary" type="button"><span class="lx-button__content"><span class="lx-button__label">Primary</span></span></button>
              <button class="lx-button lx-button--info" type="button" style="--btn-text: #1f3f63; --btn-plain-text: #1f3f63; --btn-border: #b9d6ef; --btn-bg: #e8f2fb;">
                <span class="lx-button__content"><SvgIcon name="common-icons" size="18px" /></span>
                <span class="lx-button__label">Info Icon</span>
              </button>
              <button class="lx-button lx-button--warning is-plain" type="button"><span class="lx-button__content"><span class="lx-button__label">Warning Plain</span></span></button>
              <button class="lx-button lx-button--danger is-round" type="button"><span class="lx-button__content"><span class="lx-button__label">Danger Round</span></span></button>
              <button class="lx-button lx-button--success is-round is-plain" type="button"><span class="lx-button__content"><span class="lx-button__label">Success</span></span></button>
              <button class="lx-button lx-button--primary is-round is-circle is-plain is-loading is-disabled" type="button" disabled>
                <span class="lx-button__content"><SvgIcon name="common-loading" size="18px" class="lx-button__loading" /></span>
              </button>
            </div>
          </div>
        </section>

        <!-- CX-message -->
        <section id="demo-message" class="demo-panel">
          <header class="demo-panel-header">
            <SvgIcon name="common-icons" size="18px" />
            <span>CX-message 示例</span>
          </header>
          <p class="demo-panel-description">支持 success / info / error 类型、自动关闭与手动清空。</p>
          <div class="demo-panel-body">
            <div class="message-demo-list">
              <button class="lx-button lx-button--success" type="button" @click="pushMessage('success', '操作成功：内容已保存')"><span class="lx-button__content"><span class="lx-button__label">Success</span></span></button>
              <button class="lx-button lx-button--danger is-plain" type="button" @click="pushMessage('error', '出错了：请稍后重试')"><span class="lx-button__content"><span class="lx-button__label">Error</span></span></button>
              <button class="lx-button lx-button--primary" type="button" @click="pushMessage('info', '提示：这是一条普通消息')"><span class="lx-button__content"><span class="lx-button__label">Info</span></span></button>
              <button class="lx-button lx-button--primary is-plain" type="button" @click="pushMessage('info', '这条消息不会自动关闭', 0)"><span class="lx-button__content"><span class="lx-button__label">Persistent</span></span></button>
              <button class="lx-button lx-button--info is-plain" type="button" @click="messages = []"><span class="lx-button__content"><span class="lx-button__label">Close All</span></span></button>
            </div>
          </div>
        </section>

        <!-- CX-popover -->
        <section id="demo-popover" class="demo-panel">
          <header class="demo-panel-header">
            <SvgIcon name="common-icons" size="18px" />
            <span>CX-popover 示例</span>
          </header>
          <p class="demo-panel-description">支持 click / hover 触发、菜单项单独控制关闭行为，且全局同一时刻仅展示一个弹层。</p>
          <div class="demo-panel-body">
            <div class="popover-demo-list">
              <div v-for="(p, i) in popovers" :key="p.label" class="lx-popover-wrapper">
                <div class="lx-popover-trigger">
                  <button class="lx-button" :class="p.btnClass" type="button" @click="activePopover = activePopover === i ? -1 : i">
                    <span class="lx-button__content"><span class="lx-button__label">{{ p.label }}</span></span>
                  </button>
                </div>
                <transition name="lx-popover-fade">
                  <div v-if="activePopover === i" class="lx-popover">
                    <div v-for="item in p.items" :key="item" class="lx-popover-item" @click="activePopover = -1">
                      <span class="lx-popover-item__content">{{ item }}</span>
                    </div>
                  </div>
                </transition>
              </div>
            </div>
          </div>
        </section>

        <!-- CX-radio -->
        <section id="demo-radio" class="demo-panel">
          <header class="demo-panel-header">
            <SvgIcon name="common-icons" size="18px" />
            <span>CX-radio / CX-radio-group 示例</span>
          </header>
          <p class="demo-panel-description">支持单独使用、分组联动、命名与禁用态控制。</p>
          <div class="demo-panel-body">
            <div class="radio-demo-stack">
              <div class="demo-card">
                <span class="demo-card__label">内容状态</span>
                <div class="lx-radio-group demo-card__control">
                  <label
                    v-for="r in radioOptions"
                    :key="r.value"
                    class="lx-radio"
                    :class="{ 'is-checked': radioValue === r.value }"
                  >
                    <input class="lx-radio__input" type="radio" :value="r.value" v-model="radioValue" />
                    <span class="lx-radio__icon"><span class="lx-radio__inner"></span></span>
                    <span class="lx-radio__label">{{ r.label }}</span>
                  </label>
                </div>
              </div>
              <div class="demo-card">
                <span class="demo-card__label">当前选中</span>
                <strong class="demo-card__value">{{ radioValue }}</strong>
              </div>
              <div class="demo-card">
                <span class="demo-card__label">禁用态</span>
                <div class="demo-card__control">
                  <label class="lx-radio is-disabled">
                    <input class="lx-radio__input" type="radio" disabled />
                    <span class="lx-radio__icon"><span class="lx-radio__inner"></span></span>
                    <span class="lx-radio__label">禁用项</span>
                  </label>
                  <label class="lx-radio is-checked">
                    <input class="lx-radio__input" type="radio" checked />
                    <span class="lx-radio__icon"><span class="lx-radio__inner"></span></span>
                    <span class="lx-radio__label">独立单选</span>
                  </label>
                </div>
              </div>
            </div>
          </div>
        </section>

        <!-- CX-switch -->
        <section id="demo-switch" class="demo-panel">
          <header class="demo-panel-header">
            <SvgIcon name="common-icons" size="18px" />
            <span>CX-switch 示例</span>
          </header>
          <p class="demo-panel-description">支持 v-model 双向绑定、变更通知与禁用态切换。</p>
          <div class="demo-panel-body">
            <div class="switch-demo-stack">
              <div class="demo-card demo-card--row">
                <div>
                  <span class="demo-card__label">消息提醒</span>
                  <p class="demo-card__hint">模拟常见的设置面板开关场景。</p>
                </div>
                <button
                  class="lx-switch"
                  :class="{ 'is-checked': switchOn, 'is-disabled': switchLocked }"
                  type="button"
                  :disabled="switchLocked"
                  @click="switchOn = !switchOn"
                >
                  <span class="lx-switch__core"><span class="lx-switch__action"></span></span>
                </button>
              </div>
              <div class="demo-card demo-card--row">
                <div>
                  <span class="demo-card__label">锁定演示开关</span>
                  <p class="demo-card__hint">开启后，上方主开关进入禁用状态。</p>
                </div>
                <button class="lx-switch" :class="{ 'is-checked': switchLocked }" type="button" @click="switchLocked = !switchLocked">
                  <span class="lx-switch__core"><span class="lx-switch__action"></span></span>
                </button>
              </div>
              <div class="demo-card">
                <span class="demo-card__label">当前状态</span>
                <strong class="demo-card__value">{{ switchOn ? '已开启' : '已关闭' }}</strong>
              </div>
            </div>
          </div>
        </section>

        <!-- CX-tag -->
        <section id="demo-tag" class="demo-panel">
          <header class="demo-panel-header">
            <SvgIcon name="common-component" size="18px" />
            <span>CX-tag 示例</span>
          </header>
          <p class="demo-panel-description">支持多种主题、尺寸、圆角、图标、禁用态、可关闭交互、自定义前缀与配色，适合状态标注与元信息展示。</p>
          <div class="demo-panel-body">
            <div class="tag-demo-stack">
              <div class="demo-card">
                <span class="demo-card__label">主题与形态</span>
                <div class="tag-demo-list">
                  <span v-for="t in tagThemes" :key="t.label" class="lx-tag lx-tag--small is-round" :class="[`lx-tag--${t.type}`, { 'is-plain': t.plain }]">
                    <span class="lx-tag__content"><span class="lx-tag__prefix">#</span><span class="lx-tag__label">{{ t.label }}</span></span>
                  </span>
                </div>
              </div>
              <div class="demo-card">
                <div class="tag-demo-header">
                  <span class="demo-card__label">可关闭标签</span>
                  <button class="lx-button lx-button--info is-plain" type="button" @click="resetTags"><span class="lx-button__content"><span class="lx-button__label">重置</span></span></button>
                </div>
                <div class="tag-demo-list">
                  <span v-for="(t, i) in closableTags" :key="t" class="lx-tag lx-tag--primary lx-tag--small is-round">
                    <span class="lx-tag__content"><span class="lx-tag__label">{{ t }}</span></span>
                    <span class="lx-tag__close" @click="closableTags.splice(i, 1)">×</span>
                  </span>
                </div>
              </div>
            </div>
          </div>
        </section>
      </div>
    </section>

    <!-- 消息容器 -->
    <div class="lx-message-container">
      <transition-group name="lx-message-slide">
        <div v-for="m in messages" :key="m.id" class="lx-message-item" :class="`lx-message-${m.type}`">
          <span>{{ m.text }}</span>
        </div>
      </transition-group>
    </div>
  </main>
</template>

<script setup>
import { ref } from 'vue'
import SvgIcon from '../components/SvgIcon.vue'

const menus = [
  { id: 'demo-button', name: 'CX-button', desc: '按钮组件' },
  { id: 'demo-message', name: 'CX-message', desc: '消息提示组件' },
  { id: 'demo-popover', name: 'CX-popover', desc: '气泡弹层组件' },
  { id: 'demo-radio', name: 'CX-radio', desc: '单选组件' },
  { id: 'demo-switch', name: 'CX-switch', desc: '开关组件' },
  { id: 'demo-tag', name: 'CX-tag', desc: '标签组件' }
]

const activeMenu = ref('demo-button')

function goPanel(id) {
  activeMenu.value = id
  const el = document.getElementById(id)
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

/* message */
const messages = ref([])
let msgSeq = 0
function pushMessage(type, text, duration = 2600) {
  const id = ++msgSeq
  messages.value.push({ id, type, text })
  if (duration > 0) {
    setTimeout(() => {
      messages.value = messages.value.filter(m => m.id !== id)
    }, duration)
  }
}

/* popover */
const activePopover = ref(-1)
const popovers = [
  { label: '快捷操作', btnClass: 'lx-button--primary', items: ['新建草稿', '打开最近', '同步内容'] },
  { label: '悬停预览', btnClass: 'lx-button--info is-plain', items: ['预览一', '预览二'] },
  { label: '多步骤菜单', btnClass: 'lx-button--warning is-plain', items: ['第一步', '第二步', '完成'] }
]

/* radio */
const radioOptions = [
  { label: '草稿', value: 'draft' },
  { label: '待审核', value: 'review' },
  { label: '已发布', value: 'published' }
]
const radioValue = ref('review')

/* switch */
const switchOn = ref(true)
const switchLocked = ref(false)

/* tag */
const tagThemes = [
  { label: 'Primary', type: 'primary' },
  { label: 'Success', type: 'success' },
  { label: 'Warning', type: 'warning' },
  { label: 'Danger', type: 'danger' },
  { label: 'Neutral', type: 'neutral', plain: true },
  { label: 'Section', type: 'section', plain: true }
]
const DEFAULT_TAGS = ['Vue3', 'SpringBoot', 'MySQL', 'Vite']
const closableTags = ref([...DEFAULT_TAGS])
function resetTags() {
  closableTags.value = [...DEFAULT_TAGS]
}
</script>
