<template>
  <transition name="setting-dialog-fade">
    <div v-if="modelValue" class="setting-dialog">
      <div class="setting-dialog__mask" @click="$emit('update:modelValue', false)"></div>
      <div class="setting-dialog__card">
        <button class="setting-dialog__close" @click="$emit('update:modelValue', false)">
          <span class="setting-dialog__close-icon"><SvgIcon name="common-big-close" size="16px" /></span>
        </button>
        <div class="setting-dialog__header">
          <span class="setting-dialog__badge"><SvgIcon name="common-setting" size="16px" /></span>
          <div class="setting-dialog__title-row">
            <h3>偏好设置</h3>
            <p>主题、背景与小组件的偏好都在这里。</p>
          </div>
        </div>
        <div class="setting-panel">
          <!-- 外观主题 -->
          <section class="setting-section">
            <div class="setting-section__head">
              <span class="setting-section__icon"><SvgIcon :name="settings.isDark ? 'common-moon' : 'common-sun'" size="18px" /></span>
              <div>
                <h4>外观主题</h4>
                <p>切换亮色 / 暗色，亮色模式采用浅透明毛玻璃质感。</p>
              </div>
            </div>
            <div class="theme-pick-grid">
              <button
                type="button"
                class="theme-pick-card"
                :class="{ 'is-active': !settings.isDark }"
                @click="settings.setTheme('light')"
              >
                <span class="theme-pick-preview theme-pick-preview--light"><i></i></span>
                <span v-if="!settings.isDark" class="theme-pick-check"><SvgIcon name="common-sun" size="12px" /></span>
                <span class="theme-pick-name"><SvgIcon name="common-sun" size="15px" /> 亮色模式</span>
                <span class="theme-pick-desc">浅透明毛玻璃质感，明亮通透</span>
              </button>
              <button
                type="button"
                class="theme-pick-card"
                :class="{ 'is-active': settings.isDark }"
                @click="settings.setTheme('dark')"
              >
                <span class="theme-pick-preview theme-pick-preview--dark"><i></i></span>
                <span v-if="settings.isDark" class="theme-pick-check"><SvgIcon name="common-moon" size="12px" /></span>
                <span class="theme-pick-name"><SvgIcon name="common-moon" size="15px" /> 暗色模式</span>
                <span class="theme-pick-desc">深色沉浸背景，夜间更护眼</span>
              </button>
            </div>
          </section>

          <!-- 背景设置 -->
          <section class="setting-section">
            <div class="setting-section__head">
              <span class="setting-section__icon"><SvgIcon name="common-archive" size="18px" /></span>
              <div>
                <h4>背景设置</h4>
                <p>统一管理背景图、轮播切换与近远景效果。横屏与竖屏分别记忆选择。</p>
              </div>
            </div>
            <div class="background-mode-card">
              <div class="background-mode-card__header">
                <strong>背景模式</strong>
                <span>{{ settings.backgroundImageEnabled ? '已启用背景图' : '已关闭背景图' }}</span>
              </div>
              <div class="background-mode-card__group setting-radio-row">
                <label class="setting-radio" :class="{ 'is-checked': !settings.backgroundImageEnabled }">
                  <input
                    type="radio"
                    name="bg-mode"
                    :checked="!settings.backgroundImageEnabled"
                    @change="settings.update({ backgroundImageEnabled: false })"
                  />
                  <i class="setting-radio__dot"></i>
                  <span>无背景图</span>
                </label>
                <label class="setting-radio" :class="{ 'is-checked': settings.backgroundImageEnabled }">
                  <input
                    type="radio"
                    name="bg-mode"
                    :checked="settings.backgroundImageEnabled"
                    @change="settings.update({ backgroundImageEnabled: true })"
                  />
                  <i class="setting-radio__dot"></i>
                  <span>选择背景图</span>
                </label>
              </div>
            </div>
            <div class="background-rotation-card" :class="{ 'is-disabled': !settings.backgroundImageEnabled }">
              <div>
                <strong>背景轮播</strong>
                <p>开启后，背景会在图库中逐渐平滑切换图片；关闭时仅显示选中的单张背景。</p>
              </div>
              <button
                type="button"
                role="switch"
                aria-label="背景轮播"
                :aria-checked="settings.backgroundCarouselEnabled ? 'true' : 'false'"
                class="cx-switch"
                :class="{ 'is-checked': settings.backgroundCarouselEnabled }"
                :disabled="!settings.backgroundImageEnabled"
                @click="settings.update({ backgroundCarouselEnabled: !settings.backgroundCarouselEnabled })"
              >
                <span class="cx-switch__core"><span class="cx-switch__action"></span></span>
              </button>
            </div>
            <div class="gallery-card" :class="{ 'is-disabled': !settings.backgroundImageEnabled }">
              <div class="gallery-card__header">
                <strong>横屏背景</strong>
                <span>{{ settings.landscapeImages.length }} 张</span>
              </div>
              <div class="gallery-strip">
                <div
                  v-for="img in settings.landscapeImages"
                  :key="img"
                  class="gallery-item"
                  :class="{ 'is-active': settings.selectedLandscapeImage === img }"
                  @click="settings.backgroundImageEnabled && $emit('choose-background', img)"
                >
                  <img :src="img" alt="背景" loading="lazy" />
                  <div class="gallery-item__overlay"></div>
                  <span class="gallery-item__label">{{ img.split('/').pop() }}</span>
                </div>
              </div>
            </div>
          </section>

          <!-- 页面特效 -->
          <section class="setting-section">
            <div class="setting-section__head">
              <span class="setting-section__icon"><SvgIcon name="common-component" size="18px" /></span>
              <div>
                <h4>页面特效</h4>
                <p>氛围小组件，按喜好自由开关。</p>
              </div>
            </div>
            <div class="effect-card">
              <div class="effect-card__content">
                <span>樱花飘落</span>
                <button
                  type="button"
                  role="switch"
                  aria-label="樱花飘落"
                  :aria-checked="settings.sakuraEnabled ? 'true' : 'false'"
                  class="cx-switch"
                  :class="{ 'is-checked': settings.sakuraEnabled }"
                  @click="settings.update({ sakuraEnabled: !settings.sakuraEnabled })"
                >
                  <span class="cx-switch__core"><span class="cx-switch__action"></span></span>
                </button>
              </div>
              <div class="effect-card__content">
                <span>看板娘</span>
                <button
                  type="button"
                  role="switch"
                  aria-label="看板娘"
                  :aria-checked="settings.live2dEnabled ? 'true' : 'false'"
                  class="cx-switch"
                  :class="{ 'is-checked': settings.live2dEnabled }"
                  @click="settings.update({ live2dEnabled: !settings.live2dEnabled })"
                >
                  <span class="cx-switch__core"><span class="cx-switch__action"></span></span>
                </button>
              </div>
            </div>
          </section>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup>
import SvgIcon from '../../components/SvgIcon.vue'
import { useSettingsStore } from '../../stores/settings'

const settings = useSettingsStore()

defineProps({
  modelValue: { type: Boolean, default: false }
})

defineEmits(['update:modelValue', 'choose-background'])
</script>

<style scoped>
/* ===== 偏好设置弹窗覆盖 ===== */
.setting-dialog .setting-dialog__mask {
  background: rgba(0, 0, 0, 0.14);
}
html.dark .setting-dialog .setting-dialog__mask {
  background: rgba(0, 0, 0, 0.26);
}
.setting-dialog .setting-dialog__card {
  background: rgba(252, 253, 255, 0.5);
  backdrop-filter: blur(6px) saturate(1.08);
  -webkit-backdrop-filter: blur(6px) saturate(1.08);
}
html.dark .setting-dialog .setting-dialog__card {
  background: rgba(14, 16, 21, 0.52);
  backdrop-filter: none;
  -webkit-backdrop-filter: none;
}
.setting-dialog .setting-section {
  background: rgba(255, 255, 255, 0.34);
  box-shadow: none;
}
.setting-dialog .effect-card,
.setting-dialog .background-mode-card,
.setting-dialog .background-rotation-card,
.setting-dialog .gallery-card,
.setting-dialog .theme-pick-card {
  background: rgba(255, 255, 255, 0.32);
}
html.dark .setting-dialog .setting-section {
  background: rgba(22, 25, 32, 0.4);
  border-color: rgba(255, 255, 255, 0.09);
}
html.dark .setting-dialog .effect-card,
html.dark .setting-dialog .background-mode-card,
html.dark .setting-dialog .background-rotation-card,
html.dark .setting-dialog .gallery-card,
html.dark .setting-dialog .theme-pick-card {
  background: rgba(26, 29, 37, 0.38);
  border-color: rgba(255, 255, 255, 0.08);
}

.setting-dialog .effect-card {
  display: grid;
  gap: 16px;
}
.setting-dialog .effect-card__content {
  min-height: 44px;
}

.setting-section__head {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}
.setting-section__head h4 {
  margin: 0 0 4px;
  color: var(--text-color);
  font-size: 20px;
}
.setting-section__head p {
  margin: 0;
  color: var(--text-color);
  opacity: 0.62;
  font-size: 14px;
  line-height: 1.55;
}
.setting-section__icon {
  flex-shrink: 0;
  width: 38px;
  height: 38px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--topbar-border);
  border-radius: 12px;
  background: var(--action-btn-hover-bg);
  color: var(--accent-text, var(--text-color));
}

/* 外观主题：双预览卡 */
.theme-pick-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}
.theme-pick-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  padding: 14px;
  border: 1px solid var(--topbar-border);
  border-radius: 18px;
  background: var(--input-bg);
  text-align: left;
  cursor: pointer;
  transition: transform 0.24s ease, border-color 0.24s ease, box-shadow 0.24s ease;
}
.theme-pick-card:hover {
  transform: translateY(-2px);
  border-color: var(--accent-solid);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.12);
}
.theme-pick-card.is-active {
  border-color: #7ab0e6;
  box-shadow: 0 0 0 2px rgba(122, 176, 230, 0.35), 0 14px 28px rgba(0, 0, 0, 0.14);
}
.theme-pick-preview {
  position: relative;
  width: 100%;
  height: 84px;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid var(--topbar-border);
}
.theme-pick-preview i {
  position: absolute;
  left: 12%;
  bottom: 18%;
  width: 62%;
  height: 26%;
  border-radius: 8px;
}
.theme-pick-preview--light {
  background: linear-gradient(135deg, #dcebfb 0%, #f6fafe 55%, #ffffff 100%);
}
.theme-pick-preview--light i {
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 6px 16px rgba(120, 158, 202, 0.35);
}
.theme-pick-preview--dark {
  background: linear-gradient(135deg, #10151f 0%, #1a2333 55%, #0b0e14 100%);
}
.theme-pick-preview--dark i {
  background: rgba(38, 50, 70, 0.9);
  /* token-guard-ignore 暗色主题预览卡是刻意的暗色设计，不属于 html.dark 上下文 */
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08);
  left: auto;
  right: 10%;
}
.theme-pick-check {
  position: absolute;
  top: 22px;
  right: 22px;
  width: 24px;
  height: 24px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: linear-gradient(135deg, var(--accent-solid, #3b82f6), var(--accent-strong, #2563eb));
  color: #fff;
  box-shadow: 0 6px 14px rgba(59, 130, 246, 0.4);
}
.theme-pick-name {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
  color: var(--text-color);
  font-size: 16px;
  font-weight: 700;
}
.theme-pick-desc {
  color: var(--text-color);
  opacity: 0.6;
  font-size: 13.5px;
  line-height: 1.5;
}

/* 背景模式单选 */
.setting-radio-row {
  display: flex;
  align-items: center;
  gap: 26px;
}
.setting-radio {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: var(--text-color);
  font-size: 15px;
  opacity: 0.75;
  transition: opacity 0.2s ease;
}
.setting-radio input {
  position: absolute;
  opacity: 0;
  pointer-events: none;
}
.setting-radio__dot {
  width: 18px;
  height: 18px;
  border-radius: 999px;
  border: 2px solid var(--topbar-border);
  background: var(--input-bg);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}
.setting-radio__dot::after {
  content: '';
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: var(--accent-solid, #3b82f6);
  transform: scale(0);
  transition: transform 0.2s ease;
}
.setting-radio.is-checked {
  opacity: 1;
  font-weight: 700;
}
.setting-radio.is-checked .setting-radio__dot {
  border-color: var(--accent-solid, #3b82f6);
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.14);
}
.setting-radio.is-checked .setting-radio__dot::after {
  transform: scale(1);
}

/* 禁用态 */
.background-rotation-card.is-disabled,
.setting-dialog .gallery-card.is-disabled {
  pointer-events: none;
}

@media (max-width: 640px) {
  .theme-pick-grid {
    grid-template-columns: 1fr;
  }
  .setting-radio-row {
    gap: 18px;
    flex-wrap: wrap;
  }
}
</style>
