<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Connection, Setting, CircleCheckFilled, CircleCloseFilled, Cpu } from '@element-plus/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import AdminSubNav from '@/components/admin/AdminSubNav.vue'
import {
  getCozeConfig,
  saveCozeConfig,
  testCozeChatHealth,
  testCozeWorkflowHealth,
  testDeepSeekChatHealth,
  testDeepSeekSchedulingHealth
} from '@/api/coze'
import { useAiChatStore } from '@/stores/aiChat'
import { useChatStore } from '@/stores/chat'
import type { AiProvider, CozeConfig, CozeHealthCheckItem, CozeHealthResult } from '@/types'

const chatStore = useChatStore()
const aiChatStore = useAiChatStore()

const loading = ref(false)
const saving = ref(false)
const testing = ref(false)
const tokenEditing = ref(false)
const deepseekKeyEditing = ref(false)
const healthResult = ref<CozeHealthResult | null>(null)

const providerOptions: Array<{ label: string; value: AiProvider; desc: string }> = [
  { label: 'Coze 智能体', value: 'coze', desc: '使用 Coze Bot + Workflow' },
  { label: 'DeepSeek 大模型', value: 'deepseek', desc: '使用 DeepSeek API 直连' },
  { label: '自动（优先 Coze）', value: 'auto', desc: 'Coze 可用时用 Coze，否则 DeepSeek' }
]

const deepseekModelOptions = [
  { label: 'deepseek-v4-flash', value: 'deepseek-v4-flash', desc: '极速推理模型（DeepSeek-V4-Flash）' },
  { label: 'deepseek-v4-pro', value: 'deepseek-v4-pro', desc: '旗舰大模型（DeepSeek-V4-Pro）' }
]

function healthItemType(status?: string) {
  if (status === 'ok') return 'success'
  if (status === 'pending') return 'info'
  if (status === 'skipped') return 'info'
  return 'error'
}

function resolveOverallStatus(
  chat?: CozeHealthResult['chat'],
  workflow?: CozeHealthResult['workflow']
) {
  const chatStatus = chat?.status ?? 'error'
  const workflowStatus = workflow?.status ?? 'error'
  if (chatStatus === 'pending' || workflowStatus === 'pending') {
    return 'partial'
  }
  if (chatStatus !== 'ok') return 'error'
  if (workflowStatus === 'error') return 'partial'
  if (workflowStatus === 'ok') return 'ok'
  return 'partial'
}

function formatTestError(error: unknown) {
  if (error instanceof Error && error.message.includes('timeout')) {
    return '请求超时，请确认前端 dev 服务已重启且后端正常运行'
  }
  if (error instanceof Error) return error.message
  return '请求失败'
}

function createFailedHealthItem(message: string): CozeHealthCheckItem {
  return { status: 'error', message }
}

function buildHealthMessage(
  provider: AiProvider,
  chat?: CozeHealthResult['chat'],
  workflow?: CozeHealthResult['workflow']
) {
  if (provider === 'deepseek') {
    return `DeepSeek 对话：${chat?.status ?? 'unknown'}；DeepSeek 排产：${workflow?.status ?? 'unknown'}`
  }
  return `Bot 对话：${chat?.status ?? 'unknown'}；排产工作流：${workflow?.status ?? 'unknown'}`
}

function overallHealthType(result: CozeHealthResult) {
  if (result.status === 'ok') return 'success'
  if (result.status === 'partial' || result.status === 'mock') return 'warning'
  return 'error'
}

const meta = reactive({
  hasApiToken: false,
  apiTokenMasked: '',
  cozeConfigured: false,
  deepseekConfigured: false,
  activeConfigured: false,
  activeProvider: 'coze' as AiProvider,
  hasDeepseekApiKey: false,
  deepseekApiKeyMasked: '',
  updateTime: ''
})

const form = reactive({
  aiProvider: 'coze' as AiProvider,
  apiToken: '',
  botId: '',
  apiUrl: 'https://api.coze.cn/v3',
  workflowId: '',
  welcomeMessage: '',
  deepseekApiKey: '',
  deepseekApiUrl: 'https://api.deepseek.com',
  deepseekModel: 'deepseek-v4-flash',
  enabled: true
})

const providerLabel = computed(() => {
  return providerOptions.find((item) => item.value === form.aiProvider)?.label ?? form.aiProvider
})

const statusTitle = computed(() => {
  if (!form.enabled) return 'AI 功能已关闭'
  if (meta.activeConfigured) {
    return `${providerLabel.value} 已启用`
  }
  return '演示模式（当前引擎未配置）'
})

const statusDesc = computed(() => {
  if (!form.enabled) return '关闭后 AI 客服与智能排产将回退为本地演示逻辑。'
  if (meta.activeConfigured) {
    return `当前生效引擎：${meta.activeProvider === 'coze' ? 'Coze' : 'DeepSeek'}；最近更新：${meta.updateTime || '刚刚'}`
  }
  return '请配置所选 AI 引擎的凭证后保存，再进行连通性测试。'
})

async function loadConfig() {
  loading.value = true
  try {
    const data: CozeConfig = await getCozeConfig()
    form.aiProvider = data.aiProvider ?? 'coze'
    form.botId = data.botId ?? ''
    form.apiUrl = data.apiUrl || 'https://api.coze.cn/v3'
    form.workflowId = data.workflowId ?? ''
    form.welcomeMessage = data.welcomeMessage ?? ''
    form.deepseekApiUrl = data.deepseekApiUrl || 'https://api.deepseek.com'
    const loadedModel = data.deepseekModel?.trim()
    if (!loadedModel || loadedModel === 'deepseek-chat' || !deepseekModelOptions.some((item) => item.value === loadedModel)) {
      form.deepseekModel = 'deepseek-v4-flash'
    } else {
      form.deepseekModel = loadedModel
    }
    form.enabled = data.enabled ?? true
    form.apiToken = ''
    form.deepseekApiKey = ''
    tokenEditing.value = false
    deepseekKeyEditing.value = false

    meta.hasApiToken = Boolean(data.hasApiToken)
    meta.apiTokenMasked = data.apiTokenMasked ?? ''
    meta.cozeConfigured = Boolean(data.cozeConfigured ?? data.configured)
    meta.deepseekConfigured = Boolean(data.deepseekConfigured)
    meta.activeConfigured = Boolean(data.activeConfigured)
    meta.activeProvider = data.activeProvider ?? form.aiProvider
    meta.hasDeepseekApiKey = Boolean(data.hasDeepseekApiKey)
    meta.deepseekApiKeyMasked = data.deepseekApiKeyMasked ?? ''
    meta.updateTime = data.updateTime ? String(data.updateTime) : ''
  } catch {
    ElMessage.error('加载系统配置失败')
  } finally {
    loading.value = false
  }
}

function validateBeforeSave() {
  if (form.aiProvider === 'coze') {
    if (!form.botId.trim()) {
      ElMessage.warning('请填写 Coze Bot ID')
      return false
    }
    if (!form.apiUrl.trim()) {
      ElMessage.warning('请填写 Coze API 地址')
      return false
    }
    if (!meta.hasApiToken && !form.apiToken.trim()) {
      ElMessage.warning('请填写 Coze API Token')
      return false
    }
    return true
  }
  if (form.aiProvider === 'deepseek') {
    if (!form.deepseekApiUrl.trim()) {
      ElMessage.warning('请填写 DeepSeek API 地址')
      return false
    }
    if (!form.deepseekModel.trim()) {
      ElMessage.warning('请填写 DeepSeek 模型名称')
      return false
    }
    if (!meta.hasDeepseekApiKey && !form.deepseekApiKey.trim()) {
      ElMessage.warning('请填写 DeepSeek API Key')
      return false
    }
    return true
  }
  const hasCoze = meta.hasApiToken || form.apiToken.trim()
  const hasDeepSeek = meta.hasDeepseekApiKey || form.deepseekApiKey.trim()
  if (!hasCoze && !hasDeepSeek) {
    ElMessage.warning('自动模式下请至少配置 Coze 或 DeepSeek 其中一套凭证')
    return false
  }
  return true
}

async function handleSave() {
  if (!validateBeforeSave()) return

  saving.value = true
  try {
    await saveCozeConfig({
      aiProvider: form.aiProvider,
      apiToken: form.apiToken.trim() || undefined,
      botId: form.botId.trim() || undefined,
      apiUrl: form.apiUrl.trim() || undefined,
      workflowId: form.workflowId.trim() || undefined,
      welcomeMessage: form.welcomeMessage.trim() || undefined,
      deepseekApiKey: form.deepseekApiKey.trim() || undefined,
      deepseekApiUrl: form.deepseekApiUrl.trim() || undefined,
      deepseekModel: form.deepseekModel.trim() || undefined,
      enabled: form.enabled
    })
    ElMessage.success('系统配置已保存')
    healthResult.value = null
    await loadConfig()
    await Promise.all([chatStore.loadWelcomeMessage(), aiChatStore.loadWelcomeMessage()])
  } catch {
    ElMessage.error('保存失败，请检查填写内容')
  } finally {
    saving.value = false
  }
}

async function runHealthTask(
  label: string,
  task: () => Promise<CozeHealthCheckItem>,
  slot: 'chat' | 'workflow'
) {
  try {
    const result = await task()
    if (healthResult.value) {
      healthResult.value[slot] = result
      healthResult.value.message = buildHealthMessage(form.aiProvider, healthResult.value.chat, healthResult.value.workflow)
    }
    return result
  } catch (error) {
    console.error(`[AI Config] ${label} 测试失败`, error)
    const failed = createFailedHealthItem(`${label}失败：${formatTestError(error)}`)
    if (healthResult.value) {
      healthResult.value[slot] = failed
      healthResult.value.message = buildHealthMessage(form.aiProvider, healthResult.value.chat, healthResult.value.workflow)
    }
    return failed
  }
}

function resolveTestProvider(): AiProvider {
  if (form.aiProvider === 'deepseek' || form.aiProvider === 'coze') {
    return form.aiProvider
  }
  return meta.activeProvider === 'deepseek' ? 'deepseek' : 'coze'
}

async function handleTest() {
  testing.value = true
  const testProvider = resolveTestProvider()
  const isDeepSeek = testProvider === 'deepseek'
  const chatPendingLabel = isDeepSeek ? 'DeepSeek 对话测试中…' : 'Bot 对话测试中…'
  const workflowPendingLabel = isDeepSeek
    ? 'DeepSeek 排产测试中，约需 10～60 秒…'
    : '排产工作流测试中，约需 30～90 秒…'

  healthResult.value = {
    provider: testProvider,
    configured: meta.activeConfigured,
    enabled: form.enabled,
    apiUrl: isDeepSeek ? form.deepseekApiUrl : form.apiUrl,
    status: 'partial',
    message: '测试进行中…',
    chat: { status: 'pending', message: chatPendingLabel },
    workflow: { status: 'pending', message: workflowPendingLabel }
  }

  const chatTask = isDeepSeek ? () => testDeepSeekChatHealth() : () => testCozeChatHealth()
  const workflowTask = isDeepSeek ? () => testDeepSeekSchedulingHealth() : () => testCozeWorkflowHealth()

  try {
    const [chat, workflow] = await Promise.all([
      runHealthTask(isDeepSeek ? 'DeepSeek 对话' : 'Bot 对话', chatTask, 'chat'),
      runHealthTask(isDeepSeek ? 'DeepSeek 排产' : '排产工作流', workflowTask, 'workflow')
    ])
    if (!healthResult.value) return
    healthResult.value.status = resolveOverallStatus(chat, workflow)
    healthResult.value.message = buildHealthMessage(testProvider, chat, workflow)

    if (healthResult.value.status === 'ok') {
      ElMessage.success('连通性测试全部通过')
    } else if (chat?.status === 'ok' || workflow?.status === 'ok') {
      ElMessage.warning('部分测试通过，请查看下方详细结果')
    } else {
      ElMessage.error('连接测试失败，请查看下方详细结果')
    }
  } finally {
    testing.value = false
  }
}

function startTokenEdit() {
  tokenEditing.value = true
  form.apiToken = ''
}

function onTokenBlur() {
  if (meta.hasApiToken && !form.apiToken.trim()) {
    tokenEditing.value = false
  }
}

function startDeepSeekKeyEdit() {
  deepseekKeyEditing.value = true
  form.deepseekApiKey = ''
}

function onDeepSeekKeyBlur() {
  if (meta.hasDeepseekApiKey && !form.deepseekApiKey.trim()) {
    deepseekKeyEditing.value = false
  }
}

function workflowResultDescription(workflow: NonNullable<CozeHealthResult['workflow']>) {
  const parts = [workflow.message]
  if (workflow.summary) {
    parts.push(
      `返回条目：优先级 ${workflow.summary.priorities} 条，瓶颈 ${workflow.summary.bottlenecks} 条，派工 ${workflow.summary.dispatches} 条`
    )
  }
  return parts.join('；')
}

function chatResultTitle(chat: NonNullable<CozeHealthResult['chat']>) {
  const label = (healthResult.value?.provider ?? resolveTestProvider()) === 'deepseek' ? 'DeepSeek 对话' : 'Bot 对话'
  const statusText = chat.status === 'ok' ? '成功' : chat.status === 'pending' ? '测试中' : chat.status === 'skipped' ? '跳过' : '失败'
  return `${label}：${statusText}`
}

function workflowResultTitle(workflow: NonNullable<CozeHealthResult['workflow']>) {
  const label = (healthResult.value?.provider ?? resolveTestProvider()) === 'deepseek' ? 'DeepSeek 排产' : '排产工作流'
  const statusText = workflow.status === 'ok' ? '成功' : workflow.status === 'pending' ? '测试中' : workflow.status === 'skipped' ? '跳过' : '失败'
  return `${label}：${statusText}`
}

onMounted(loadConfig)
</script>

<template>
  <div class="view-page" v-loading="loading">
    <PageHeader title="系统配置" subtitle="管理 AI 引擎切换，以及 Coze 智能体 / DeepSeek 大模型对接配置。" />
    <AdminSubNav />

    <div class="config-container">
      <div class="status-banner" :class="meta.activeConfigured && form.enabled ? 'status-banner--ready' : 'status-banner--demo'">
        <div class="status-banner__left">
          <el-icon class="status-icon">
            <CircleCheckFilled v-if="meta.activeConfigured && form.enabled" />
            <CircleCloseFilled v-else />
          </el-icon>
          <div class="status-info">
            <div class="status-title">{{ statusTitle }}</div>
            <div class="status-desc">{{ statusDesc }}</div>
          </div>
        </div>
        <div class="status-banner__right" v-if="healthResult">
          <el-tag :type="overallHealthType(healthResult) === 'success' ? 'success' : overallHealthType(healthResult) === 'warning' ? 'warning' : 'danger'" effect="dark" class="result-tag">
            综合结果: {{ healthResult.status === 'ok' ? '全部通过' : healthResult.status === 'partial' ? '部分通过' : healthResult.status === 'mock' ? '未配置' : '失败' }}
          </el-tag>
        </div>
      </div>

      <el-form :model="form" label-position="top" class="custom-config-form">
        <el-card shadow="hover" class="section-card">
          <template #header>
            <div class="section-card-header">
              <el-icon><Cpu /></el-icon>
              <span>AI 引擎切换</span>
            </div>
          </template>

          <el-form-item label="当前使用的 AI 引擎">
            <el-radio-group v-model="form.aiProvider" class="provider-group">
              <el-radio v-for="item in providerOptions" :key="item.value" :value="item.value" border>
                <div class="provider-option">
                  <span class="provider-option__label">{{ item.label }}</span>
                  <span class="provider-option__desc">{{ item.desc }}</span>
                </div>
              </el-radio>
            </el-radio-group>
          </el-form-item>
        </el-card>

        <el-card v-show="form.aiProvider !== 'deepseek'" shadow="hover" class="section-card">
          <template #header>
            <div class="section-card-header">
              <el-icon><Connection /></el-icon>
              <span>Coze 智能体配置</span>
            </div>
          </template>

          <el-form-item label="API Token (Access Token)">
            <div class="token-field">
              <el-input
                v-if="meta.hasApiToken && !tokenEditing"
                :model-value="meta.apiTokenMasked"
                readonly
                class="custom-input custom-input--token-saved"
                @focus="startTokenEdit"
              >
                <template #suffix>
                  <el-button link class="token-change-btn" @click="startTokenEdit">更换 Token</el-button>
                </template>
              </el-input>
              <el-input
                v-else
                v-model="form.apiToken"
                type="password"
                show-password
                clearable
                autocomplete="new-password"
                :placeholder="meta.hasApiToken ? '请输入新 Access Token' : '请输入 Coze 个人访问令牌'"
                class="custom-input"
                @blur="onTokenBlur"
              />
            </div>
          </el-form-item>

          <div class="form-grid-2">
            <el-form-item label="Bot ID">
              <el-input v-model="form.botId" placeholder="Coze Bot ID" clearable class="custom-input" />
            </el-form-item>
            <el-form-item label="API 服务地址">
              <el-input v-model="form.apiUrl" placeholder="https://api.coze.cn/v3" clearable class="custom-input" />
            </el-form-item>
          </div>

          <el-form-item label="智能排产工作流 ID">
            <el-input
              v-model="form.workflowId"
              placeholder="Coze 模式下填写 Workflow ID；DeepSeek 模式可留空"
              clearable
              class="custom-input"
            />
          </el-form-item>
        </el-card>

        <el-card v-show="form.aiProvider !== 'coze'" shadow="hover" class="section-card">
          <template #header>
            <div class="section-card-header">
              <el-icon><Connection /></el-icon>
              <span>DeepSeek 大模型配置</span>
            </div>
          </template>

          <el-form-item label="API Key">
            <div class="token-field">
              <el-input
                v-if="meta.hasDeepseekApiKey && !deepseekKeyEditing"
                :model-value="meta.deepseekApiKeyMasked"
                readonly
                class="custom-input custom-input--token-saved"
                @focus="startDeepSeekKeyEdit"
              >
                <template #suffix>
                  <el-button link class="token-change-btn" @click="startDeepSeekKeyEdit">更换 Key</el-button>
                </template>
              </el-input>
              <el-input
                v-else
                v-model="form.deepseekApiKey"
                type="password"
                show-password
                clearable
                autocomplete="new-password"
                :placeholder="meta.hasDeepseekApiKey ? '请输入新 DeepSeek API Key' : '请输入 DeepSeek API Key'"
                class="custom-input"
                @blur="onDeepSeekKeyBlur"
              />
            </div>
          </el-form-item>

          <div class="form-grid-2">
            <el-form-item label="API 服务地址">
              <el-input v-model="form.deepseekApiUrl" placeholder="https://api.deepseek.com" clearable class="custom-input" />
            </el-form-item>
            <el-form-item label="模型名称">
              <el-select v-model="form.deepseekModel" placeholder="请选择模型" class="custom-input" style="width: 100%;">
                <el-option
                  v-for="opt in deepseekModelOptions"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                >
                  <div style="display: flex; justify-content: space-between; align-items: center; width: 100%;">
                    <span style="font-weight: 500;">{{ opt.label }}</span>
                    <span style="font-size: 12px; color: #94a3b8; margin-left: 12px;">{{ opt.desc }}</span>
                  </div>
                </el-option>
              </el-select>
            </el-form-item>
          </div>
        </el-card>

        <el-card shadow="hover" class="section-card">
          <template #header>
            <div class="section-card-header">
              <el-icon><Setting /></el-icon>
              <span>通用 AI 设置</span>
            </div>
          </template>

          <el-form-item label="智能助理欢迎语">
            <el-input
              v-model="form.welcomeMessage"
              type="textarea"
              :rows="3"
              placeholder="用户首次打开 AI 助手时展示的问候语..."
              class="custom-textarea"
            />
          </el-form-item>

          <div class="switch-row-card">
            <div class="switch-row-card__info">
              <span class="switch-row-card__title">启用 AI 增强功能</span>
              <span class="switch-row-card__desc">关闭后 AI 客服与智能排产回退为本地演示逻辑。</span>
            </div>
            <el-switch v-model="form.enabled" class="custom-switch" />
          </div>
        </el-card>

        <div class="form-actions-row">
          <el-button type="primary" :loading="saving" class="btn-submit" @click="handleSave">
            保存配置
          </el-button>
          <el-button :loading="testing" class="btn-test" @click="handleTest">
            {{ resolveTestProvider() === 'deepseek' ? '测试 DeepSeek 对话 / 排产' : '测试 Coze Bot / 工作流' }}
          </el-button>
          <span v-if="testing" class="test-hint">
            {{ resolveTestProvider() === 'deepseek' ? 'DeepSeek 对话与排产并行测试中…' : 'Bot 约 10 秒内出结果，工作流约 30～90 秒…' }}
          </span>
        </div>
      </el-form>

      <div v-if="healthResult" class="health-result-panel">
        <el-alert
          :type="overallHealthType(healthResult)"
          :title="healthResult.message"
          show-icon
          :closable="false"
          class="health-result-alert"
        />
        <el-alert
          v-if="healthResult.chat"
          :type="healthItemType(healthResult.chat.status)"
          :title="chatResultTitle(healthResult.chat)"
          :description="healthResult.chat.message"
          show-icon
          :closable="false"
          class="health-result-alert"
        />
        <el-alert
          v-if="healthResult.workflow"
          :type="healthItemType(healthResult.workflow.status)"
          :title="workflowResultTitle(healthResult.workflow)"
          :description="workflowResultDescription(healthResult.workflow)"
          show-icon
          :closable="false"
          class="health-result-alert"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.view-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.config-container {
  width: 100%;
  padding: 0 0 16px 0;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.status-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-radius: 12px;
  border: 1px solid;
  transition: all 0.3s ease;
}

.status-banner--ready {
  background: rgba(16, 185, 129, 0.05);
  border-color: rgba(16, 185, 129, 0.2);
}

.status-banner--demo {
  background: rgba(245, 158, 11, 0.05);
  border-color: rgba(245, 158, 11, 0.2);
}

.status-banner__left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.status-icon {
  font-size: 24px;
}

.status-banner--ready .status-icon {
  color: #10b981;
}

.status-banner--demo .status-icon {
  color: #f59e0b;
}

.status-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.status-title {
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
}

.status-desc {
  font-size: 12px;
  color: #64748b;
}

.result-tag {
  border-radius: 8px !important;
  font-weight: 600;
}

.custom-config-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.section-card {
  border-radius: 16px !important;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.02) !important;
  border: 1px solid #e2e8f0 !important;
}

.section-card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
}

.section-card-header .el-icon {
  color: #4f46e5;
  font-size: 18px;
}

.custom-config-form :deep(.el-form-item__label) {
  font-weight: 600;
  color: #1e293b;
  padding-bottom: 8px !important;
  font-size: 13px;
}

.provider-group {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  width: 100%;
}

.provider-group :deep(.el-radio) {
  height: auto;
  margin-right: 0;
  padding: 12px 18px;
  align-items: flex-start;
  border-radius: 12px !important;
  background-color: #f8fafc;
  border: 1px solid #e2e8f0 !important;
  transition: all 0.25s ease !important;
}

.provider-group :deep(.el-radio:hover) {
  border-color: #c7d2fe !important;
  background-color: #f1f5f9;
}

.provider-group :deep(.el-radio.is-checked) {
  background-color: rgba(79, 70, 229, 0.04) !important;
  border-color: #4f46e5 !important;
  box-shadow: 0 0 0 1px #4f46e5 !important;
}

.provider-option {
  display: flex;
  flex-direction: column;
  gap: 4px;
  white-space: normal;
}

.provider-option__label {
  font-weight: 600;
  color: #0f172a;
}

.provider-option__desc {
  font-size: 12px;
  color: #64748b;
  line-height: 1.4;
}

.form-grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  width: 100%;
}

.custom-input :deep(.el-input__wrapper),
.custom-input :deep(.el-select__wrapper) {
  border-radius: 20px !important;
  padding: 6px 18px !important;
  min-height: 40px !important;
  box-shadow: 0 0 0 1px #e2e8f0 inset !important;
  background-color: #f8fafc !important;
  transition: all 0.3s ease !important;
}

.custom-textarea :deep(.el-textarea__inner) {
  border-radius: 16px !important;
  padding: 12px 18px !important;
  box-shadow: 0 0 0 1px #e2e8f0 inset !important;
  background-color: #f8fafc !important;
  transition: all 0.3s ease !important;
}

.custom-input :deep(.el-input__wrapper.is-focus),
.custom-input :deep(.el-select__wrapper.is-focused),
.custom-textarea :deep(.el-textarea__inner:focus) {
  background-color: #fff !important;
  box-shadow: 0 0 0 1px #4f46e5 inset, 0 0 0 3px rgba(79, 70, 229, 0.15) !important;
}

.custom-input :deep(.el-input__inner),
.custom-input :deep(.el-select__selected-item) {
  font-size: 13px;
}

.token-field {
  width: 100%;
}

.token-change-btn {
  font-size: 12px !important;
  font-weight: 600 !important;
  color: #4f46e5 !important;
}

.switch-row-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  margin-top: 12px;
}

.switch-row-card__info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.switch-row-card__title {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
}

.switch-row-card__desc {
  font-size: 12px;
  color: #64748b;
}

.form-actions-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-top: 8px;
  flex-wrap: wrap;
}

.btn-submit {
  border-radius: 20px !important;
  padding: 10px 24px !important;
  height: auto !important;
  background: linear-gradient(135deg, #4f46e5 0%, #3b82f6 100%) !important;
  border: none !important;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.2) !important;
  font-weight: 600;
  font-size: 14px !important;
}

.btn-test {
  border-radius: 20px !important;
  padding: 10px 24px !important;
  height: auto !important;
  border: 1px solid #e2e8f0 !important;
  font-weight: 600;
  font-size: 14px !important;
}

.test-hint {
  font-size: 12px;
  color: #64748b;
}

.health-result-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.health-result-alert {
  border-radius: 12px !important;
}

@media (max-width: 960px) {
  .form-grid-2 {
    grid-template-columns: 1fr;
  }
}
</style>
