<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  createAiConfig,
  deleteAiConfig,
  listAiConfigs,
  listAiModels,
  listAiProviders,
  setDefaultAiConfig,
  testAiConfig,
  updateAiConfig,
  type AiConfigItem,
  type AiProviderPreset
} from '@/api/aiConfig'

const loading = ref(false)
const saving = ref(false)
const testingId = ref<string>()
const rows = ref<AiConfigItem[]>([])
const providers = ref<AiProviderPreset[]>([])

const dialogVisible = ref(false)
const editingId = ref<string>()
const formRef = ref<FormInstance>()
const modelOptions = ref<string[]>([])
const maskingKey = ref('')

const form = reactive({
  provider: 'deepseek',
  configName: '',
  baseUrl: '',
  model: '',
  apiKey: '',
  temperature: 0.7,
  maxTokens: 4096,
  enabled: 1,
  isDefault: false,
  remark: ''
})

const rules: FormRules = {
  provider: [{ required: true, message: '请选择服务商', trigger: 'change' }],
  baseUrl: [{ required: true, message: 'Base URL 不能为空', trigger: 'blur' }],
  model: [{ required: true, message: '请填写模型', trigger: 'change' }]
}

function currentProvider() {
  return providers.value.find((item) => item.key === form.provider)
}

function onProviderChange(key: string) {
  const preset = providers.value.find((item) => item.key === key)
  // 自定义的地址由用户自己填，切换其他厂商时带出预设地址
  if (preset?.baseUrl) {
    form.baseUrl = preset.baseUrl
  }
  modelOptions.value = []
}

async function fetchList() {
  loading.value = true
  try {
    const res = await listAiConfigs()
    rows.value = res.data ?? []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = undefined
  maskingKey.value = ''
  modelOptions.value = []
  Object.assign(form, {
    provider: 'deepseek',
    configName: '',
    baseUrl: providers.value.find((p) => p.key === 'deepseek')?.baseUrl ?? '',
    model: '',
    apiKey: '',
    temperature: 0.7,
    maxTokens: 4096,
    enabled: 1,
    isDefault: false,
    remark: ''
  })
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

function openEdit(row: AiConfigItem) {
  editingId.value = row.aiConfigId
  maskingKey.value = row.apiKeyMasked ?? ''
  modelOptions.value = row.model ? [row.model] : []
  Object.assign(form, {
    provider: row.provider || 'custom',
    configName: row.configName ?? '',
    baseUrl: row.baseUrl,
    model: row.model,
    apiKey: '',
    temperature: Number(row.temperature ?? 0.7),
    maxTokens: row.maxTokens ?? 4096,
    enabled: row.enabled ?? 1,
    isDefault: !!row.isDefault,
    remark: row.remark ?? ''
  })
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

async function fetchModels() {
  if (!editingId.value) {
    ElMessage.warning('先保存端点，再拉取模型列表')
    return
  }
  try {
    const res = await listAiModels(editingId.value)
    modelOptions.value = res.data ?? []
    ElMessage.success(modelOptions.value.length ? `拉到 ${modelOptions.value.length} 个模型` : '该厂商没有返回模型列表，请手工填写')
  } catch (e) {
    ElMessage.error((e as Error).message)
  }
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload = { ...form, apiKey: form.apiKey || undefined }
    if (editingId.value) {
      await updateAiConfig(editingId.value, payload)
      ElMessage.success('已保存')
    } else {
      await createAiConfig(payload)
      ElMessage.success('已新增')
    }
    dialogVisible.value = false
    await fetchList()
  } catch (e) {
    ElMessage.error((e as Error).message)
  } finally {
    saving.value = false
  }
}

async function setDefault(id: string) {
  await setDefaultAiConfig(id)
  ElMessage.success('已设为默认端点')
  await fetchList()
}

async function runTest(row: AiConfigItem) {
  testingId.value = row.aiConfigId
  try {
    const res = await testAiConfig(row.aiConfigId)
    ElMessage.success(`连接正常，耗时 ${res.data?.costMs ?? '-'} ms`)
  } catch (e) {
    ElMessage.error((e as Error).message)
  } finally {
    testingId.value = undefined
  }
}

async function remove(row: AiConfigItem) {
  await ElMessageBox.confirm(`确定删除「${row.configName || row.model}」？`, '删除端点', { type: 'warning' })
  await deleteAiConfig(row.aiConfigId)
  ElMessage.success('已删除')
  await fetchList()
}

onMounted(async () => {
  const res = await listAiProviders()
  providers.value = res.data ?? []
  await fetchList()
})
</script>

<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>AI 端点</span>
          <el-button type="primary" @click="openCreate">新增端点</el-button>
        </div>
      </template>

      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="一个端点 = 一个服务商 + 模型 + 密钥。聊天默认使用标记为「默认」的那条，用户也可以在 AI 助手页切换。"
        class="hint"
      />

      <el-table v-loading="loading" :data="rows" border stripe>
        <el-table-column prop="configName" label="名称" min-width="140" show-overflow-tooltip />
        <el-table-column label="服务商" width="120">
          <template #default="{ row }">{{ row.providerLabel || row.provider }}</template>
        </el-table-column>
        <el-table-column prop="model" label="模型" min-width="150" show-overflow-tooltip />
        <el-table-column prop="baseUrl" label="Base URL" min-width="220" show-overflow-tooltip />
        <el-table-column label="密钥" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.apiKeyConfigured ? 'success' : 'warning'">
              {{ row.apiKeyConfigured ? '已配置' : '未配置' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="默认" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.isDefault" size="small" type="primary">默认</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="row.enabled === 1 ? 'success' : 'info'">
              {{ row.enabled === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row as AiConfigItem)">编辑</el-button>
            <el-button link type="primary" :disabled="row.isDefault" @click="setDefault(row.aiConfigId)">
              设为默认
            </el-button>
            <el-button link type="primary" :loading="testingId === row.aiConfigId" @click="runTest(row as AiConfigItem)">
              测试
            </el-button>
            <el-button link type="danger" :disabled="row.isDefault" @click="remove(row as AiConfigItem)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑端点' : '新增端点'" width="620px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="服务商" prop="provider">
          <el-select v-model="form.provider" style="width: 100%" @change="onProviderChange">
            <el-option v-for="p in providers" :key="p.key" :label="p.label" :value="p.key" />
          </el-select>
          <div v-if="currentProvider()?.consoleUrl" class="tip">
            申请密钥：<el-link type="primary" :href="currentProvider()?.consoleUrl" target="_blank">
              {{ currentProvider()?.consoleUrl }}
            </el-link>
          </div>
        </el-form-item>

        <el-form-item label="显示名称">
          <el-input v-model="form.configName" placeholder="留空则自动生成，如「智谱 GLM glm-5.3」" />
        </el-form-item>

        <el-form-item label="Base URL" prop="baseUrl">
          <el-input v-model="form.baseUrl" placeholder="填到版本号为止，如 https://open.bigmodel.cn/api/paas/v4" />
        </el-form-item>

        <el-form-item label="模型" prop="model">
          <el-select v-model="form.model" allow-create filterable style="width: 100%">
            <el-option v-for="m in modelOptions" :key="m" :label="m" :value="m" />
          </el-select>
          <div class="tip">
            模型名变化很快，建议按厂商文档填写；保存后可点「拉取模型列表」自动获取
            <el-button link type="primary" :disabled="!editingId" @click="fetchModels">拉取模型列表</el-button>
          </div>
        </el-form-item>

        <el-form-item label="API Key">
          <el-input
            v-model="form.apiKey"
            type="password"
            show-password
            :placeholder="maskingKey ? `已配置 ${maskingKey}，留空表示不修改` : '请输入 API Key'"
          />
          <div class="tip">留空则保留原密钥；保存后只以掩码显示，不会下发明文</div>
        </el-form-item>

        <el-form-item label="温度">
          <el-slider v-model="form.temperature" :min="0" :max="2" :step="0.1" show-input style="width: 70%" />
          <div class="tip">智谱等部分厂商的取值范围是 0–1</div>
        </el-form-item>

        <el-form-item label="最大输出 token">
          <el-input-number v-model="form.maxTokens" :min="1" :max="32768" :step="512" />
        </el-form-item>

        <el-form-item label="启用">
          <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>

        <el-form-item label="设为默认">
          <el-switch v-model="form.isDefault" />
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="form.remark" placeholder="可选，例如厂商文档地址" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.hint {
  margin-bottom: 12px;
}
.tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.6;
}
</style>
