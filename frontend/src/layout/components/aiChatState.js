export const MAX_MESSAGE_LENGTH = 2000
export const MAX_CONTEXT_MESSAGES = 16

export function clipMessageContent(content) {
  return String(content ?? '').trim().slice(0, MAX_MESSAGE_LENGTH)
}

export function appendMessage(state, role, content, extra = {}) {
  const clipped = clipMessageContent(content)
  if (!clipped) return null
  const message = { role, content: clipped, ...extra }
  state.messages.push(message)
  return message
}

export function buildChatPayload(messages) {
  const normalized = (messages || [])
    .filter(message => message && (message.role === 'user' || message.role === 'assistant'))
    .map(message => ({ role: message.role, content: clipMessageContent(message.content) }))
    .filter(message => message.content)

  return { messages: normalized.slice(-MAX_CONTEXT_MESSAGES) }
}

export function normalizeChatResponse(data) {
  const response = data || {}
  return {
    reply: clipMessageContent(response.reply || response.message),
    references: Array.isArray(response.references) ? response.references : [],
    degraded: Boolean(response.degraded)
  }
}

export function fallbackReply(references = []) {
  const titles = references
    .map(reference => clipMessageContent(reference && reference.title))
    .filter(Boolean)
  if (titles.length) return `AI 服务暂时不可用，你可以先看看站内文章：${titles.join('、')}`
  return '暂时无法连接 AI 服务，请稍后再试。'
}

export function resetChatState(state) {
  state.messages = []
  state.input = ''
  state.loading = false
  state.error = ''
}
