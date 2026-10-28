<script setup lang="ts">
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { getAiConfig, updateAiConfig } from '@/api/aiConfig'

const formRef = ref<FormInstance>()
const loading = ref(false)
const saving = ref(false)
const masked = ref('')
const configured = ref(false)

const form = reactive({
  provider: 'deepseek',
  model: 'deepseek-chat',
  baseUrl: 'https://api.deepseek.com',
  apiKey: '',
  temperature: 0.7,
  maxTokens: 4096,
  enabled: 1
})

const rules: FormRules = {
  model: [{ required: true, message: '请选择或填写模型', trigger: 'change' }],
  baseUrl: [{ required: true, message: 'Base URL 不能为空', trigger: 'blur' }]
}

const MODELS = [
  { label: 'deepseek-chat（通用对话）', value: 'deepseek-chat' },
  { label: 'deepseek-reasoner（推理模型）', value: 'deepseek-reasoner' }
]

async function fetchConfig() {
  loading.value = true
  try {
    const res = await getAiConfig()
    const data = res.data ?? {}
    if (!data.aiConfigId) return
    form.provider = data.provider ?? 'deepseek'
    form.model = data.model ?? 'deepseek-chat'
    form.baseUrl = data.baseUrl ?? ''
    form.temperature = Number(data.temperature ?? 0.7)
    form.maxTokens = data.maxTokens ?? 4096
    form.enabled = data.enabled ?? 1
    masked.value = data.apiKeyMasked ?? ''
    configured.value = !!data.apiKeyConfigured
    form.apiKey = ''
  } finally {
    loading.value = false
  }
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await updateAiConfig({ ...form, apiKey: form.apiKey })
    ElMessage.success('AI 配置已保存')
    form.apiKey = ''
    await fetchConfig()
  } finally {
    saving.value = false
  }
}

onMounted(fetchConfig)
</script>

<template>
  <el-card v-loading="loading" shadow="never" class="config-card">
    <template #header>
      <div class="card-header">
        <span>AI 配置</span>
        <el-tag size="small" :type="configured ? 'success' : 'warning'">
          {{ configured ? '密钥已配置' : '密钥未配置' }}
        </el-tag>
      </div>
    </template>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="130px" class="config-form">
      <el-form-item label="服务商">
        <el-input :model-value="form.provider" disabled />
      </el-form-item>

      <el-form-item label="Base URL" prop="baseUrl">
        <el-input v-model="form.baseUrl" placeholder="https://api.deepseek.com" />
      </el-form-item>

      <el-form-item label="模型" prop="model">
        <el-select v-model="form.model" allow-create filterable style="width: 100%">
          <el-option v-for="item in MODELS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>

      <el-form-item label="API Key">
        <el-input
          v-model="form.apiKey"
          type="password"
          show-password
          :placeholder="configured ? `已配置 ${masked}，留空表示不修改` : '请输入 DeepSeek API Key'"
        />
        <div class="tip">留空则保留原密钥；保存后只以掩码显示，不会下发明文</div>
      </el-form-item>

      <el-form-item label="温度">
        <el-slider v-model="form.temperature" :min="0" :max="2" :step="0.1" show-input style="width: 60%" />
      </el-form-item>

      <el-form-item label="最大输出 token">
        <el-input-number v-model="form.maxTokens" :min="1" :max="32768" :step="512" />
      </el-form-item>

      <el-form-item label="启用">
        <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
        <el-button @click="fetchConfig">重置</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<style scoped>
.config-card {
  max-width: 760px;
}
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.config-form {
  padding-top: 8px;
}
.tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.6;
}
</style>
