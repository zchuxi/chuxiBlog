import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { menuGroups } from './adminMenu.js'

const read = relative => readFile(new URL(relative, import.meta.url), 'utf8')

test('管理端暴露 AI 配置读取与保存接口', async () => {
  const source = await read('../../api/admin.js')
  assert.match(source, /adminApi\.aiConfig\s*=\s*\{/)
  assert.match(source, /get:\s*\(\)\s*=>\s*http\.get\('\/admin\/ai\/config'\)/)
  assert.match(source, /save:\s*data\s*=>\s*http\.put\('\/admin\/ai\/config',\s*data\)/)
})

test('AI 配置加入后台菜单并挂载独立面板', async () => {
  const items = menuGroups.flatMap(group => group.items)
  const item = items.find(entry => entry.key === 'ai-config')
  assert.deepEqual(item, {
    key: 'ai-config',
    label: 'AI 配置',
    icon: 'common-ai',
    description: '配置 AI 服务与文章上下文'
  })

  const view = await read('./AdminView.vue')
  assert.match(view, /import AiConfigPanel from '\.\/AiConfigPanel\.vue'/)
  assert.match(view, /<AiConfigPanel v-else-if="currentKey === 'ai-config'"\s*\/>/)
})

test('仪表盘 AI 快捷操作直接进入配置面板', async () => {
  const source = await read('./DashboardPanel.vue')
  assert.match(source, /label:\s*'AI 配置'[\s\S]*emit\('go',\s*'ai-config'\)/)
  assert.doesNotMatch(source, /AI 配置敬请期待/)
})

test('AI 配置面板覆盖安全字段、状态反馈和授权失效处理', async () => {
  const source = await read('./AiConfigPanel.vue')

  for (const field of ['enabled', 'baseUrl', 'model', 'timeoutSeconds', 'maxContextArticles']) {
    assert.match(source, new RegExp(`form\\.${field}`), `缺少字段 ${field}`)
  }
  assert.match(source, /apiKeyConfigured/)
  assert.match(source, /API Key 仅通过后端环境变量 APP_AI_API_KEY 配置/)
  assert.doesNotMatch(source, /v-model[^>]*apiKey/i)

  assert.match(source, /v-if="loading"/)
  assert.match(source, /v-else-if="loadError"/)
  assert.match(source, /:disabled="saving \|\| loading"/)
  assert.match(source, /adminApi\.aiConfig\.get\(\)/)
  assert.match(source, /adminApi\.aiConfig\.save\(/)
  assert.match(source, /err\s*&&\s*err\.unauthorized/)
  assert.match(source, /onUnauthorized/)
})
