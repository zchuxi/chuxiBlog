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
            <strong class="components-page__hero-meta-value">15 个示例区块</strong>
            <p class="components-page__hero-meta-description">覆盖 CX 基础组件、图标、全局顶栏、播放条、弹窗、搜索遮罩、AI 面板、特效画布与滚动展开指示条。</p>
          </div>
        </div>
      </div>
      <div class="components-page__hero-aside">
        <div class="components-page__hero-summary">
          <span class="components-page__hero-summary-label">已收录组件</span>
          <strong class="components-page__hero-summary-value">15</strong>
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
          <p class="demo-panel-description">页面分区容器：眉标、标题、标签 / 药丸 / 右侧操作插槽与内容主体。</p>
          <div class="demo-panel-body">
            <CxSection eyebrow="COMPONENTS" title="区块容器演示">
              <template #tags>
                <CxTag type="primary" size="small" round>标签插槽</CxTag>
                <CxTag type="success" size="small" round plain>多标签</CxTag>
              </template>
              <template #pill>
                <span>药丸信息：支持任意插槽内容</span>
              </template>
              <template #action>
                <CxButton type="primary" plain>右侧操作</CxButton>
              </template>
              <p class="demo-section-note">这里是 CxSection 的内容主体区域，可放置任意内容或组件。</p>
            </CxSection>
          </div>
        </section>

        <!-- SvgIcon 图标集 -->
        <section id="demo-icon" class="demo-panel">
          <header class="demo-panel-header">
            <SvgIcon name="common-icons" size="18px" />
            <span>SvgIcon 图标集</span>
          </header>
          <p class="demo-panel-description">基于 SVG Symbol 雪碧图的统一图标组件，共 {{ icons.length }} 个，点击图标可复制引用名。</p>
          <div class="demo-panel-body">
            <div class="icon-demo-grid">
              <div
                v-for="icon in icons"
                :key="icon"
                class="icon-demo-cell"
                title="点击复制引用名"
                @click="copyIcon(icon)"
              >
                <SvgIcon :name="icon" size="22px" />
                <span class="icon-demo-cell__name">{{ icon }}</span>
              </div>
            </div>
          </div>
        </section>

        <!-- TopBar -->
        <section id="demo-topbar" class="demo-panel">
          <header class="demo-panel-header">
            <SvgIcon name="common-menu" size="18px" />
            <span>TopBar 顶栏</span>
          </header>
          <p class="demo-panel-description">全局顶栏：导航、搜索、主题、AI、音乐、设置等入口事件全部上抛，下方可直接体验各事件回调。</p>
          <div class="demo-panel-body">
            <div class="topbar-demo-shell">
              <TopBar
                site-name="组件演示"
                @open-search="searchOpen = true"
                @toggle-theme="toggleThemeDemo"
                @toggle-ai="aiOpen = true"
                @toggle-music="musicBarOpen = true"
                @open-settings="settingsDialogOpen = true"
                @paw-toggle="messageRef.pushMessage('info', '顶栏事件：paw-toggle 已触发')"
                @scroll-to-top="messageRef.pushMessage('info', '顶栏事件：scroll-to-top 已触发')"
              />
            </div>
          </div>
        </section>

        <!-- MusicBar -->
        <section id="demo-music" class="demo-panel">
          <header class="demo-panel-header">
            <SvgIcon name="common-music" size="18px" />
            <span>MusicBar 播放条</span>
          </header>
          <p class="demo-panel-description">底部音乐播放条：Teleport 到 body 悬浮，支持播放列表、播放模式、倍速与音量控制。</p>
          <div class="demo-panel-body">
            <div class="demo-card demo-card--row">
              <div>
                <span class="demo-card__label">底部播放条</span>
                <p class="demo-card__hint">点击按钮从底部弹出播放条，曲目来自后端 /music 接口。</p>
              </div>
              <CxButton type="primary" @click="musicBarOpen = true">打开播放条</CxButton>
            </div>
          </div>
        </section>

        <!-- SettingsDialog -->
        <section id="demo-settings" class="demo-panel">
          <header class="demo-panel-header">
            <SvgIcon name="common-setting" size="18px" />
            <span>SettingsDialog 设置弹窗</span>
          </header>
          <p class="demo-panel-description">偏好设置弹窗：主题切换、背景模式、背景图库与特效开关，改动实时写入全局设置。</p>
          <div class="demo-panel-body">
            <div class="demo-card demo-card--row">
              <div>
                <span class="demo-card__label">设置弹窗</span>
                <p class="demo-card__hint">点击打开设置弹窗；选择背景图会触发 choose-background 事件。</p>
              </div>
              <CxButton type="primary" @click="settingsDialogOpen = true">打开设置</CxButton>
            </div>
          </div>
        </section>

        <!-- SearchOverlay -->
        <section id="demo-search" class="demo-panel">
          <header class="demo-panel-header">
            <SvgIcon name="common-search" size="18px" />
            <span>SearchOverlay 搜索遮罩</span>
          </header>
          <p class="demo-panel-description">全屏搜索遮罩：输入关键词请求文章搜索接口，点击结果可跳转文章详情。</p>
          <div class="demo-panel-body">
            <div class="demo-card demo-card--row">
              <div>
                <span class="demo-card__label">搜索遮罩</span>
                <p class="demo-card__hint">打开后输入关键词即可搜索站内文章。</p>
              </div>
              <CxButton type="primary" @click="searchOpen = true">打开搜索</CxButton>
            </div>
          </div>
        </section>

        <!-- AiChatPanel -->
        <section id="demo-ai" class="demo-panel">
          <header class="demo-panel-header">
            <SvgIcon name="common-ai" size="18px" />
            <span>AiChatPanel AI 面板</span>
          </header>
          <p class="demo-panel-description">AI 聊天浮层：底部输入发送消息、Esc 或点击遮罩关闭（当前为本地演示响应）。</p>
          <div class="demo-panel-body">
            <div class="demo-card demo-card--row">
              <div>
                <span class="demo-card__label">AI 聊天面板</span>
                <p class="demo-card__hint">点击打开 AI 浮层，输入消息体验交互。</p>
              </div>
              <CxButton type="primary" @click="aiOpen = true">打开 AI 面板</CxButton>
            </div>
          </div>
        </section>

        <!-- SakuraCanvas -->
        <section id="demo-sakura" class="demo-panel">
          <header class="demo-panel-header">
            <SvgIcon name="common-cat" size="18px" />
            <span>SakuraCanvas 樱花特效</span>
          </header>
          <p class="demo-panel-description">全屏 Canvas 飘落花瓣，与全局设置「特效 - 樱花」联动（由 settings.sakuraEnabled 控制）。</p>
          <div class="demo-panel-body">
            <div class="demo-card demo-card--row">
              <div>
                <span class="demo-card__label">樱花特效开关</span>
                <p class="demo-card__hint">开关与全局设置同步，开启后整站出现飘落花瓣。</p>
              </div>
              <CxSwitch v-model="settingsStore.sakuraEnabled" />
            </div>
          </div>
        </section>

        <!-- ScrollGrowLine -->
        <section id="demo-scroll-grow" class="demo-panel">
          <header class="demo-panel-header">
            <SvgIcon name="common-arrow" size="18px" />
            <span>ScrollGrowLine 示例</span>
          </header>
          <p class="demo-panel-description">SVG 进度条组件：随页面下滑从短逐渐变长，触底后固定最大长度。可用于阅读进度指示、章节标记等。</p>
          <div class="demo-panel-body">
            <div class="scroll-grow-demo">
              <div class="scroll-grow-demo__label">阅读进度</div>
              <ScrollGrowLine :min-width="80" :max-width="640" :height="8" class="scroll-grow-demo__line" />
              <div class="scroll-grow-demo__stack">
                <span class="scroll-grow-demo__chip">标签 A</span>
                <span class="scroll-grow-demo__chip">标签 B</span>
              </div>
              <p class="scroll-grow-demo__note">向下滚动整页查看效果：条形会随滚动从 80px 逐渐增长到 640px，触底后保持不变。</p>
            </div>
          </div>
        </section>
      </div>
    </section>

    <!-- 演示用浮层实例（Teleport 到 body 的组件） -->
    <MusicBar v-model:music-bar-open="musicBarOpen" />
    <SettingsDialog v-model="settingsDialogOpen" @choose-background="onChooseBackground" />
    <SearchOverlay v-model="searchOpen" />
    <AiChatPanel v-model="aiOpen" />
    <SakuraCanvas />

    <!-- 消息容器 -->
    <CxMessage ref="messageRef" />
  </main>
</template>

<script setup>
import { ref } from 'vue'
import '../assets/css/components-show.css'
import SvgIcon from '../components/SvgIcon.vue'
import CxSection from '../components/CxSection.vue'
import TopBar from '../layout/components/TopBar.vue'
import MusicBar from '../layout/components/MusicBar.vue'
import SettingsDialog from '../layout/components/SettingsDialog.vue'
import SearchOverlay from '../layout/components/SearchOverlay.vue'
import AiChatPanel from '../layout/components/AiChatPanel.vue'
import SakuraCanvas from '../layout/components/SakuraCanvas.vue'
import { useSettingsStore } from '../stores/settings'
import { CxButton, CxTag, CxSwitch, CxRadio, CxRadioGroup, CxMessage, CxPopover, ScrollGrowLine } from '../components/cx'

const menus = [
  { id: 'demo-button', name: 'CX-button', desc: '按钮组件' },
  { id: 'demo-message', name: 'CX-message', desc: '消息提示组件' },
  { id: 'demo-popover', name: 'CX-popover', desc: '气泡弹层组件' },
  { id: 'demo-radio', name: 'CX-radio', desc: '单选组件' },
  { id: 'demo-switch', name: 'CX-switch', desc: '开关组件' },
  { id: 'demo-tag', name: 'CX-tag', desc: '标签组件' },
  { id: 'demo-section', name: 'CX-section', desc: '区块容器组件' },
  { id: 'demo-icon', name: 'SvgIcon', desc: 'SVG 图标组件' },
  { id: 'demo-topbar', name: 'TopBar', desc: '全局顶栏' },
  { id: 'demo-music', name: 'MusicBar', desc: '底部播放条' },
  { id: 'demo-settings', name: 'SettingsDialog', desc: '设置弹窗' },
  { id: 'demo-search', name: 'SearchOverlay', desc: '搜索遮罩' },
  { id: 'demo-ai', name: 'AiChatPanel', desc: 'AI 聊天面板' },
  { id: 'demo-sakura', name: 'SakuraCanvas', desc: '樱花特效画布' },
  { id: 'demo-scroll-grow', name: 'ScrollGrowLine', desc: '滚动展开指示条' }
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

/* settings store（樱花开关等全局特效） */
const settingsStore = useSettingsStore()

/* svg 图标集（svg-sprite.svg 中的 symbol id，SvgIcon 的 name 不含 icon- 前缀） */
const icons = [
  'common-add', 'common-ai', 'common-archive', 'common-arrow', 'common-articlePages',
  'common-big-close', 'common-cat', 'common-chat', 'common-component', 'common-exchange',
  'common-expand-left', 'common-expand-right', 'common-hanger', 'common-history', 'common-home',
  'common-icons', 'common-loading', 'common-menu', 'common-moon', 'common-music',
  'common-open', 'common-parallax', 'common-paw', 'common-person', 'common-reset',
  'common-search', 'common-send', 'common-setting', 'common-sun', 'common-thumbUp',
  'common-timeline', 'common-tool', 'common-tree', 'common-web',
  'music-back', 'music-forward', 'music-list', 'music-lrc', 'music-next',
  'music-order', 'music-pause', 'music-play', 'music-pre', 'music-repeat',
  'music-repeatOne', 'music-shuffle'
]

async function copyIcon(name) {
  try {
    await navigator.clipboard.writeText(name)
    messageRef.value && messageRef.value.pushMessage('success', `已复制引用名：${name}`)
  } catch {
    messageRef.value && messageRef.value.pushMessage('info', `图标引用名：${name}`)
  }
}

/* 顶栏事件演示 */
function toggleThemeDemo() {
  settingsStore.setTheme(settingsStore.isDark ? 'light' : 'dark')
  messageRef.value && messageRef.value.pushMessage('info', `顶栏事件：toggle-theme → ${settingsStore.isDark ? '暗色' : '亮色'}`)
}

/* 设置弹窗 choose-background 事件演示（全局背景切换由 LayoutView 处理，这里仅提示） */
function onChooseBackground(img) {
  messageRef.value && messageRef.value.pushMessage('success', `已选择背景：${img}`)
}

/* 布局层浮层开关 */
const musicBarOpen = ref(false)
const settingsDialogOpen = ref(false)
const searchOpen = ref(false)
const aiOpen = ref(false)
</script>
