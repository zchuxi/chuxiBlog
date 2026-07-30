<template>
  <Teleport to="body">
    <transition name="ai-chat-modal-fade">
      <div v-if="modelValue" class="ai-chat-modal-mask" @click.self="$emit('update:modelValue', false)">
        <div class="ai-chat-modal" role="dialog" aria-label="AI 助手">
          <div class="ai-chat-toolbar">
            <div class="ai-chat-toolbar__left">
              <span class="ai-chat-toolbar__spark">✦</span>
              <span class="ai-chat-toolbar__title">AI 助手</span>
            </div>
            <div class="ai-chat-toolbar__actions">
              <span class="ai-chat-toolbar__icon-action" title="重新开始" @click="aiMessages = []">
                <SvgIcon name="common-reset" size="18px" />
              </span>
              <span class="ai-chat-toolbar__icon-action" title="关闭" @click="$emit('update:modelValue', false)">
                <SvgIcon name="common-big-close" size="18px" />
              </span>
            </div>
          </div>
          <div class="ai-chat-message-list">
            <div v-if="!aiMessages.length" class="ai-chat-message ai-chat-message--assistant">
              <div class="ai-chat-message__body">
                <div class="ai-chat-message__content">
                  <p class="ai-chat-message__text">你好呀，我是站点助手 ✦ 试着输入一个关键词，我会帮你在文章里找找看。</p>
                </div>
              </div>
            </div>
            <div
              v-for="(m, i) in aiMessages"
              :key="i"
              class="ai-chat-message"
              :class="`ai-chat-message--${m.role}`"
            >
              <div class="ai-chat-message__body">
                <div class="ai-chat-message__content">
                  <p class="ai-chat-message__text">{{ m.content }}</p>
                </div>
              </div>
            </div>
          </div>
          <div class="ai-chat-input-wrapper">
            <div class="ai-chat-input-row">
              <input
                v-model="aiInput"
                class="ai-chat-input-row__field"
                type="text"
                placeholder="输入消息，Enter 发送"
                @keydown.enter.exact.prevent="sendAiMessage"
              />
              <button type="button" class="ai-chat-input-row__send" @click="sendAiMessage">
                <SvgIcon name="common-send" size="17px" />
              </button>
            </div>
          </div>
        </div>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
import { ref, watch, onBeforeUnmount } from 'vue'
import SvgIcon from '../../components/SvgIcon.vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue'])

const aiInput = ref('')
const aiMessages = ref([])

function onAiKeydown(e) { if (e.key === 'Escape') emit('update:modelValue', false) }

watch(() => props.modelValue, open => {
  if (open) document.addEventListener('keydown', onAiKeydown)
  else document.removeEventListener('keydown', onAiKeydown)
})

function sendAiMessage() {
  const text = aiInput.value.trim()
  if (!text) return
  aiMessages.value.push({ role: 'user', content: text })
  aiInput.value = ''
  setTimeout(() => {
    aiMessages.value.push({ role: 'assistant', content: '演示环境：AI 助手暂未接入模型服务，先陪你聊到这里啦。' })
  }, 400)
}

onBeforeUnmount(() => {
  document.removeEventListener('keydown', onAiKeydown)
})
</script>
