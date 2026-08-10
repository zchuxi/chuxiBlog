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
            <strong class="components-page__hero-meta-value">7 个示例区块</strong>
            <p class="components-page__hero-meta-description">覆盖 CX 核心组件：按钮、消息提示、气泡弹层、单选、开关、标签与区块容器。</p>
          </div>
        </div>
      </div>
      <div class="components-page__hero-aside">
        <div class="components-page__hero-summary">
          <span class="components-page__hero-summary-label">已收录组件</span>
          <strong class="components-page__hero-summary-value">7</strong>
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
              <CxButton type="primary">Primary</CxButton>
              <CxButton type="info" :color="infoBtnStyle">
                <SvgIcon name="common-icons" size="18px" />
                <span class="cx-button__label">Info Icon</span>
              </CxButton>
              <CxButton type="warning" plain>Warning Plain</CxButton>
              <CxButton type="danger" round>Danger Round</CxButton>
              <CxButton type="success" round plain>Success</CxButton>
              <CxButton type="primary" round circle plain loading disabled />
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
              <CxButton type="success" @click="messageRef.pushMessage('success', '操作成功：内容已保存')">Success</CxButton>
              <CxButton type="danger" plain @click="messageRef.pushMessage('error', '出错了：请稍后重试')">Error</CxButton>
              <CxButton type="primary" @click="messageRef.pushMessage('info', '提示：这是一条普通消息')">Info</CxButton>
              <CxButton type="primary" plain @click="messageRef.pushMessage('info', '这条消息不会自动关闭', 0)">Persistent</CxButton>
              <CxButton type="info" plain @click="messageRef.clearMessages()">Close All</CxButton>
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
              <CxPopover v-for="p in popovers" :key="p.label" :trigger="p.trigger">
                <CxButton :type="p.btnType" :plain="p.plain">{{ p.label }}</CxButton>
                <template #content="{ close }">
                  <div v-for="item in p.items" :key="item" class="cx-popover-item" @click="close">
                    <span class="cx-popover-item__content">{{ item }}</span>
                  </div>
                </template>
              </CxPopover>
            </div>
          </div>
        </section>

        <!-- CX-date-picker -->
        <section id="demo-date-picker" class="demo-panel">
          <header class="demo-panel-header">
            <SvgIcon name="common-icons" size="18px" />
            <span>CX-date-picker 示例</span>
          </header>
          <p class="demo-panel-description">自绘日历弹层，替代原生 date / datetime-local（原生弹层由浏览器绘制，不受站点主题与暗色模式控制）。周一为首列，取值格式与后端 LocalDateTime 契约一致。</p>
          <div class="demo-panel-body">
            <div class="radio-demo-stack">
              <div class="demo-card">
                <span class="demo-card__label">日期 + 时间</span>
                <div class="demo-card__control">
                  <CxDatePicker v-model="datetimeValue" type="datetime" />
                </div>
              </div>
              <div class="demo-card">
                <span class="demo-card__label">取值</span>
                <strong class="demo-card__value">{{ datetimeValue || '（空）' }}</strong>
              </div>
              <div class="demo-card">
                <span class="demo-card__label">仅日期</span>
                <div class="demo-card__control">
                  <CxDatePicker v-model="dateOnlyValue" type="date" />
                </div>
              </div>
              <div class="demo-card">
                <span class="demo-card__label">取值</span>
                <strong class="demo-card__value">{{ dateOnlyValue || '（空）' }}</strong>
              </div>
              <div class="demo-card">
                <span class="demo-card__label">禁用态</span>
                <div class="demo-card__control">
                  <CxDatePicker :model-value="'2026-03-17T10:20:30'" type="datetime" disabled />
                </div>
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
                <CxRadioGroup v-model="radioValue" class="demo-card__control">
                  <CxRadio v-for="r in radioOptions" :key="r.value" :value="r.value">{{ r.label }}</CxRadio>
                </CxRadioGroup>
              </div>
              <div class="demo-card">
                <span class="demo-card__label">当前选中</span>
                <strong class="demo-card__value">{{ radioValue }}</strong>
              </div>
              <div class="demo-card">
                <span class="demo-card__label">禁用态</span>
                <div class="demo-card__control">
                  <CxRadio value="disabled" disabled>禁用项</CxRadio>
                  <CxRadio value="standalone" :model-value="'standalone'">独立单选</CxRadio>
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
                <CxSwitch v-model="switchOn" :disabled="switchLocked" />
              </div>
              <div class="demo-card demo-card--row">
                <div>
                  <span class="demo-card__label">锁定演示开关</span>
                  <p class="demo-card__hint">开启后，上方主开关进入禁用状态。</p>
                </div>
                <CxSwitch v-model="switchLocked" />
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
                  <CxTag
                    v-for="t in tagThemes"
                    :key="t.label"
                    :type="t.type"
                    size="small"
                    round
                    :plain="t.plain"
                    prefix="#"
                  >{{ t.label }}</CxTag>
                </div>
              </div>
              <div class="demo-card">
                <div class="tag-demo-header">
                  <span class="demo-card__label">可关闭标签</span>
                  <CxButton type="info" plain @click="resetTags">重置</CxButton>
                </div>
                <div class="tag-demo-list">
                  <CxTag
                    v-for="(t, i) in closableTags"
                    :key="t"
                    type="primary"
                    size="small"
                    round
                    closable
                    @close="closableTags.splice(i, 1)"
                  >{{ t }}</CxTag>
                </div>
              </div>
            </div>
          </div>
        </section>

        <!-- CX-section -->
        <section id="demo-section" class="demo-panel">
          <header class="demo-panel-header">
            <SvgIcon name="common-component" size="18px" />
            <span>CX-section 示例</span>
          </header>
          <p class="demo-panel-description">页面分区容器：眉标、标题、标签 / 药丸 / 右侧操作插槽与内容主体。下方开关可实时切换各区块显隐，右侧操作按钮带事件反馈。</p>
          <div class="demo-panel-body">
            <div class="demo-card">
              <span class="demo-card__label">演示控制</span>
              <div class="section-demo-controls">
                <label class="section-demo-control__item">
                  <CxSwitch v-model="sectionShowTitle" />
                  <span>显示标题</span>
                </label>
                <label class="section-demo-control__item">
                  <CxSwitch v-model="sectionShowTags" />
                  <span>标签插槽</span>
                </label>
                <label class="section-demo-control__item">
                  <CxSwitch v-model="sectionShowPill" />
                  <span>药丸插槽</span>
                </label>
              </div>
            </div>
            <CxSection eyebrow="COMPONENTS" :title="sectionShowTitle ? '区块容器演示' : ''">
              <template v-if="sectionShowTags" #tags>
                <CxTag type="primary" size="small" round>标签插槽</CxTag>
                <CxTag type="success" size="small" round plain>多标签</CxTag>
              </template>
              <template v-if="sectionShowPill" #pill>
                <span>药丸信息：支持任意插槽内容</span>
              </template>
              <template #action>
                <CxButton type="primary" plain @click="messageRef.pushMessage('info', 'CX-section 事件：右侧操作按钮已触发')">右侧操作</CxButton>
              </template>
              <p class="demo-section-note">这里是 CxSection 的内容主体区域，可放置任意内容或组件。</p>
            </CxSection>
          </div>
        </section>
      </div>
    </section>

    <!-- 消息容器 -->
    <CxMessage ref="messageRef" />
  </main>
</template>

<script setup>
import { ref } from 'vue'
import '../assets/css/components-show.css'
import SvgIcon from '../components/SvgIcon.vue'
import CxSection from '../components/CxSection.vue'
import { CxButton, CxTag, CxSwitch, CxRadio, CxRadioGroup, CxMessage, CxPopover, CxDatePicker } from '../components/cx'

const menus = [
  { id: 'demo-button', name: 'CX-button', desc: '按钮组件' },
  { id: 'demo-message', name: 'CX-message', desc: '消息提示组件' },
  { id: 'demo-popover', name: 'CX-popover', desc: '气泡弹层组件' },
  { id: 'demo-radio', name: 'CX-radio', desc: '单选组件' },
  { id: 'demo-switch', name: 'CX-switch', desc: '开关组件' },
  { id: 'demo-tag', name: 'CX-tag', desc: '标签组件' },
  { id: 'demo-section', name: 'CX-section', desc: '区块容器组件' }
]

const activeMenu = ref('demo-button')

function goPanel(id) {
  activeMenu.value = id
  const el = document.getElementById(id)
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

/* message */
const messageRef = ref(null)

/* popover */
const popovers = [
  { label: '快捷操作', btnType: 'primary', trigger: 'click', items: ['新建草稿', '打开最近', '同步内容'] },
  { label: '悬停预览', btnType: 'info', plain: true, trigger: 'hover', items: ['预览一', '预览二'] },
  { label: '多步骤菜单', btnType: 'warning', plain: true, trigger: 'click', items: ['第一步', '第二步', '完成'] }
]

/* radio */
const radioOptions = [
  { label: '草稿', value: 'draft' },
  { label: '待审核', value: 'review' },
  { label: '已发布', value: 'published' }
]
const datetimeValue = ref('2026-03-17T10:20:30')
const dateOnlyValue = ref('2026-03-17')

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

/* button info style */
const infoBtnStyle = '--btn-text: #1f3f63; --btn-plain-text: #1f3f63; --btn-border: #b9d6ef; --btn-bg: #e8f2fb;'

/* cx-section demo 控制 */
const sectionShowTitle = ref(true)
const sectionShowTags = ref(true)
const sectionShowPill = ref(true)
</script>
