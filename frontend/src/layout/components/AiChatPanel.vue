<template>
  <Teleport to="body">
    <transition name="ai-chat-modal-fade">
      <div v-if="modelValue" class="ai-chat-modal-mask" @click.self="close">
        <div class="ai-chat-modal" role="dialog" aria-label="AI 助手" :aria-busy="loading">
          <div class="ai-chat-toolbar">
            <div class="ai-chat-toolbar__left">
              <span class="ai-chat-toolbar__spark">✦</span>
              <span class="ai-chat-toolbar__title">AI 助手</span>
            </div>
            <div class="ai-chat-toolbar__actions">
              <button type="button" class="ai-chat-toolbar__icon-action" title="重新开始" aria-label="重新开始" :disabled="loading" @click="reset">
                <SvgIcon name="common-reset" size="18px" />
              </button>
              <button type="button" class="ai-chat-toolbar__icon-action" title="关闭" aria-label="关闭" @click="close">
                <SvgIcon name="common-big-close" size="18px" />
              </button>
            </div>
          </div>

          <div ref="messageList" class="ai-chat-message-list" aria-live="polite" aria-atomic="false">
            <div v-if="!messages.length" class="ai-chat-message ai-chat-message--assistant">
              <div class="ai-chat-message__body"><div class="ai-chat-message__content">
                <p class="ai-chat-message__text">你好呀，我是站点助手 ✦ 试着输入一个关键词，我会帮你在文章里找找看。</p>
              </div></div>
            </div>
            <div v-for="(message, index) in messages" :key="`${index}-${message.role}`" class="ai-chat-message" :class="`ai-chat-message--${message.role}`">
              <div class="ai-chat-message__body"><div class="ai-chat-message__content">
                <p class="ai-chat-message__text">{{ message.content }}</p>
                <div v-if="message.references?.length" class="ai-chat-message__references" aria-label="引用文章">
                  <a v-for="reference in message.references" :key="reference.id || reference.title" class="ai-chat-message__reference" :href="reference.id ? `/article/${reference.id}` : '#'">
                    {{ reference.title || '站内文章' }}
                  </a>
                </div>
              </div></div>
            </div>
            <div v-if="loading" class="ai-chat-message ai-chat-message--assistant" aria-label="AI 正在思考">
              <div class="ai-chat-message__body"><div class="ai-chat-message__content"><p class="ai-chat-message__text">正在思考...</p></div></div>
            </div>
          </div>

          <div class="ai-chat-input-wrapper">
            <div class="ai-chat-input-row">
              <textarea v-model="input" ref="inputField" class="ai-chat-input-row__field" rows="1" placeholder="输入消息，Enter 发送" aria-label="发送给 AI 助手的消息" :disabled="loading" @keydown="onInputKeydown" />
              <button type="button" class="ai-chat-input-row__send" :disabled="loading || !input.trim()" :aria-label="loading ? '发送中' : '发送消息'" @click="sendAiMessage">
                <SvgIcon name="common-send" size="17px" />
              </button>
            </div>
            <p v-if="error" class="ai-chat-error" role="status">{{ error }}</p>
          </div>
        </div>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'
import SvgIcon from '../../components/SvgIcon.vue'
import { api } from '../../api/index.js'
import { appendMessage, buildChatPayload, fallbackReply, normalizeChatResponse } from './aiChatState.js'

const props = defineProps({ modelValue: { type: Boolean, default: false } })
const emit = defineEmits(['update:modelValue'])
const input = ref('')
const messages = ref([])
const loading = ref(false)
const error = ref('')
const messageList = ref(null)
const inputField = ref(null)

function close() { emit('update:modelValue', false) }
function reset() {
  messages.value = []
  input.value = ''
  error.value = ''
}
function scrollToBottom() {
  nextTick(() => {
    if (messageList.value) messageList.value.scrollTop = messageList.value.scrollHeight
  })
}
function onInputKeydown(event) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    sendAiMessage()
  }
}
async function sendAiMessage() {
  if (loading.value || !input.value.trim()) return
  appendMessage({ messages: messages.value }, 'user', input.value)
  input.value = ''
  error.value = ''
  loading.value = true
  scrollToBottom()
  try {
    const response = normalizeChatResponse(await api.aiChat(buildChatPayload(messages.value).messages))
    appendMessage({ messages: messages.value }, 'assistant', response.reply || fallbackReply(response.references), {
      references: response.references,
      degraded: response.degraded
    })
  } catch {
    error.value = 'AI 服务暂时不可用，已切换为站内检索提示。'
    appendMessage({ messages: messages.value }, 'assistant', fallbackReply(), { degraded: true })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}
function onAiKeydown(event) { if (event.key === 'Escape') close() }
watch(() => props.modelValue, open => {
  if (open) {
    document.addEventListener('keydown', onAiKeydown)
    nextTick(() => inputField.value?.focus())
  } else document.removeEventListener('keydown', onAiKeydown)
})
onBeforeUnmount(() => document.removeEventListener('keydown', onAiKeydown))
</script>

<style scoped>
.ai-chat-input-row__field {
  height: 72px;
  min-height: 44px;
  padding-top: 10px;
  padding-bottom: 10px;
  border-radius: 16px;
  resize: vertical;
  line-height: 1.5;
}

.ai-chat-message__references {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.ai-chat-message__reference {
  color: var(--accent-text);
  font-size: 12px;
  text-decoration: none;
}

.ai-chat-message__reference:hover {
  text-decoration: underline;
}

.ai-chat-error {
  margin: 6px 4px 0;
  color: var(--danger-color, #d14d5d);
  font-size: 12px;
}

.ai-chat-toolbar__icon-action:disabled,
.ai-chat-input-row__send:disabled {
  cursor: not-allowed;
  opacity: .45;
}
</style>
