<template>
  <section class="scp-panel ai-config-panel">
    <header class="scp-head">
      <div>
        <h2 class="scp-head-title">AI 配置</h2>
        <p class="scp-head-sub">配置站内 AI 的运行参数。保存后前台下一次对话立即使用新配置。</p>
      </div>
      <CxButton :disabled="saving || loading" @click="save">
        {{ saving ? '保存中…' : '保存' }}
      </CxButton>
    </header>

    <div v-if="loading" class="scp-card admin-state">加载中…</div>
    <div v-else-if="loadError" class="scp-card admin-state-error">
      <p>{{ loadError }}</p>
      <CxButton plain @click="load">重试</CxButton>
    </div>

    <template v-else>
      <div class="scp-card">
        <p class="scp-section-label">运行状态</p>
        <div class="ai-config-status-row">
          <div>
            <strong>启用站内 AI</strong>
            <p class="admin-field-tip">关闭时仍可使用站内文章检索降级，不会调用外部模型。</p>
          </div>
          <CxSwitch v-model="form.enabled" aria-label="启用站内 AI" />
        </div>
        <div class="ai-config-key-status" :class="{ configured: form.apiKeyConfigured }" role="status">
          <span class="ai-config-key-dot" aria-hidden="true"></span>
          <span>{{ form.apiKeyConfigured ? 'API Key 已配置' : 'API Key 未配置' }}</span>
        </div>
        <p class="admin-field-tip">API Key 仅通过后端环境变量 APP_AI_API_KEY 配置，后台不会读取、保存或显示密钥内容。</p>
      </div>

      <div class="scp-card">
        <p class="scp-section-label">模型服务</p>
        <div class="admin-field">
          <label class="admin-field-label" for="ai-base-url">兼容接口地址</label>
          <input id="ai-base-url" v-model.trim="form.baseUrl" class="admin-input" type="url" autocomplete="url" placeholder="https://api.deepseek.com/v1" />
          <p class="admin-field-tip">填写 OpenAI-compatible API 的基础地址，系统会自动追加 /chat/completions。</p>
        </div>
        <div class="admin-field">
          <label class="admin-field-label" for="ai-model">模型名称</label>
          <input id="ai-model" v-model.trim="form.model" class="admin-input" type="text" autocomplete="off" placeholder="deepseek-chat" />
        </div>
      </div>

      <div class="scp-card">
        <p class="scp-section-label">请求与上下文</p>
        <div class="scp-grid-2">
          <div class="admin-field">
            <label class="admin-field-label" for="ai-timeout">请求超时（秒）</label>
            <input id="ai-timeout" v-model.number="form.timeoutSeconds" class="admin-input" type="number" min="1" max="120" step="1" />
            <p class="admin-field-tip">范围 1-120 秒。</p>
          </div>
          <div class="admin-field">
            <label class="admin-field-label" for="ai-context">文章上下文数量</label>
            <input id="ai-context" v-model.number="form.maxContextArticles" class="admin-input" type="number" min="1" max="8" step="1" />
            <p class="admin-field-tip">每次对话最多检索 1-8 篇已发布文章。</p>
          </div>
        </div>
      </div>
    </template>
  </section>
</template>

<script setup>
import { inject, onMounted, ref } from 'vue'
import { adminApi } from '../../api/admin'
import CxButton from '../../components/cx/CxButton.vue'
import CxSwitch from '../../components/cx/CxSwitch.vue'

const toast = inject('adminToast', () => {})
const onUnauthorized = inject('adminUnauthorized', () => {})

const loading = ref(true)
const saving = ref(false)
const loadError = ref('')
const form = ref({
  enabled: false,
  baseUrl: '',
  model: '',
  timeoutSeconds: 20,
  maxContextArticles: 5,
  apiKeyConfigured: false
})

function fill(data) {
  form.value = {
    enabled: !!data?.enabled,
    baseUrl: data?.baseUrl || '',
    model: data?.model || '',
    timeoutSeconds: Number(data?.timeoutSeconds) || 20,
    maxContextArticles: Number(data?.maxContextArticles) || 5,
    apiKeyConfigured: !!data?.apiKeyConfigured
  }
}

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    fill(await adminApi.aiConfig.get())
  } catch (err) {
    if (err && err.unauthorized) {
      onUnauthorized()
      return
    }
    loadError.value = (err && err.message) || 'AI 配置加载失败'
  } finally {
    loading.value = false
  }
}

async function save() {
  saving.value = true
  try {
    const data = await adminApi.aiConfig.save({
      enabled: !!form.value.enabled,
      baseUrl: form.value.baseUrl,
      model: form.value.model,
      timeoutSeconds: Number(form.value.timeoutSeconds),
      maxContextArticles: Number(form.value.maxContextArticles)
    })
    fill(data)
    toast('AI 配置已保存，前台下一次对话立即生效')
  } catch (err) {
    if (err && err.unauthorized) {
      onUnauthorized()
      return
    }
    toast((err && err.message) || 'AI 配置保存失败', 'error')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style>
.ai-config-status-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border: 1px solid var(--adm-border-soft);
  border-radius: 14px;
  background: var(--adm-card-2);
}

.ai-config-status-row strong {
  display: block;
  font-size: 15px;
}

.ai-config-status-row .admin-field-tip {
  margin-bottom: 0;
}

.ai-config-key-status {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  margin-top: 14px;
  color: var(--adm-danger);
  font-size: 14px;
}

.ai-config-key-status.configured {
  color: var(--adm-success, #278a62);
}

.ai-config-key-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: currentColor;
  box-shadow: 0 0 0 4px color-mix(in srgb, currentColor 14%, transparent);
}

html.dark .ai-config-key-status.configured {
  color: #7ed7ae;
}

@media (max-width: 560px) {
  .ai-config-status-row {
    align-items: flex-start;
  }
}
</style>
