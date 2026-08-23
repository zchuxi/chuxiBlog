import assert from 'node:assert/strict'
import test from 'node:test'

import {
  MAX_MESSAGE_LENGTH,
  MAX_CONTEXT_MESSAGES,
  appendMessage,
  buildChatPayload,
  normalizeChatResponse,
  fallbackReply,
  resetChatState
} from './aiChatState.js'

test('appendMessage trims and clips message content', () => {
  const state = { messages: [] }
  appendMessage(state, 'user', `  ${'x'.repeat(MAX_MESSAGE_LENGTH + 20)}  `)
  assert.equal(state.messages.length, 1)
  assert.equal(state.messages[0].content.length, MAX_MESSAGE_LENGTH)
  assert.equal(state.messages[0].role, 'user')
})

test('buildChatPayload keeps only the latest eight conversation rounds', () => {
  const messages = Array.from({ length: MAX_CONTEXT_MESSAGES + 4 }, (_, i) => ({
    role: i % 2 ? 'assistant' : 'user',
    content: `m${i}`
  }))
  const payload = buildChatPayload(messages)
  assert.equal(payload.messages.length, MAX_CONTEXT_MESSAGES)
  assert.deepEqual(payload.messages[0], { role: 'user', content: 'm4' })
  assert.deepEqual(payload.messages.at(-1), { role: 'assistant', content: `m${MAX_CONTEXT_MESSAGES + 3}` })
})

test('normalizeChatResponse supports reply and article references', () => {
  assert.deepEqual(normalizeChatResponse({ reply: '回答', references: [{ id: 1, title: '文章' }] }), {
    reply: '回答', references: [{ id: 1, title: '文章' }], degraded: false
  })
})

test('fallbackReply explains that the answer is a local search fallback', () => {
  assert.match(fallbackReply([{ title: '春日随笔' }]), /春日随笔/)
  assert.match(fallbackReply([]), /暂时无法连接 AI 服务/)
})

test('resetChatState clears messages, input and loading state', () => {
  const state = { messages: [{ role: 'user', content: 'x' }], input: 'y', loading: true, error: 'e' }
  resetChatState(state)
  assert.deepEqual(state, { messages: [], input: '', loading: false, error: '' })
})
