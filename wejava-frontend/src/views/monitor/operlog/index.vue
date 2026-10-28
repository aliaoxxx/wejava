<script setup lang="ts">
import { h } from 'vue'
import { ElMessage, ElMessageBox, ElTag } from 'element-plus'
import CrudPage from '@/components/Crud/CrudPage.vue'
import { useCrud } from '@/hooks/useCrud'
import { cleanOperLog } from '@/api/monitor'
import { operLogSchema } from './schema'

const crud = useCrud(operLogSchema)

// ---------- 日志详情 ----------
const detail = reactive({ visible: false, row: null as any })

function openDetail(row: any) {
  detail.row = row
  detail.visible = true
}

function pretty(text: string | null | undefined) {
  if (!text) return '-'
  try {
    return JSON.stringify(JSON.parse(text), null, 2)
  } catch {
    return text
  }
}

// 状态列用 tag 渲染
const statusColumn = operLogSchema.columns.find((c) => c.prop === 'status')
if (statusColumn) {
  statusColumn.render = (row: any) =>
    h(ElTag, { type: row.status === 0 ? 'success' : 'danger', size: 'small' }, () =>
      row.status === 0 ? '成功' : '失败'
    )
}

async function handleClean() {
  await ElMessageBox.confirm('确认清空全部操作日志？此操作不可恢复', '警告', { type: 'warning' })
  await cleanOperLog()
  ElMessage.success('已清空')
  await crud.fetchList()
}
</script>

<template>
  <CrudPage :crud="crud">
    <template #toolbar>
      <el-button v-hasPerm="'monitor:operlog:remove'" type="warning" @click="handleClean">清空日志</el-button>
    </template>

    <template #row-actions="{ row }">
      <el-button link type="primary" @click="openDetail(row)">详情</el-button>
    </template>
  </CrudPage>

  <el-dialog v-model="detail.visible" title="操作日志详情" width="760px" destroy-on-close>
    <el-descriptions v-if="detail.row" :column="2" border size="small">
      <el-descriptions-item label="模块">{{ detail.row.title }}</el-descriptions-item>
      <el-descriptions-item label="操作人">{{ detail.row.operName }}</el-descriptions-item>
      <el-descriptions-item label="请求方式">{{ detail.row.requestMethod }}</el-descriptions-item>
      <el-descriptions-item label="耗时">{{ detail.row.costTime }} ms</el-descriptions-item>
      <el-descriptions-item label="IP">{{ detail.row.operIp }}</el-descriptions-item>
      <el-descriptions-item label="状态">
        <el-tag :type="detail.row.status === 0 ? 'success' : 'danger'" size="small">
          {{ detail.row.status === 0 ? '成功' : '失败' }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="请求地址" :span="2">{{ detail.row.operUrl }}</el-descriptions-item>
      <el-descriptions-item label="方法" :span="2">{{ detail.row.method }}</el-descriptions-item>
    </el-descriptions>

    <div v-if="detail.row" class="detail-block">
      <div class="detail-label">请求参数</div>
      <pre class="detail-json">{{ pretty(detail.row.operParam) }}</pre>
      <div class="detail-label">返回结果</div>
      <pre class="detail-json">{{ pretty(detail.row.jsonResult) }}</pre>
      <template v-if="detail.row.errorMsg">
        <div class="detail-label">错误信息</div>
        <pre class="detail-json error">{{ detail.row.errorMsg }}</pre>
      </template>
    </div>
  </el-dialog>
</template>
<style scoped>
.detail-block {
  margin-top: 12px;
}
.detail-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin: 8px 0 4px;
}
.detail-json {
  margin: 0;
  padding: 10px;
  max-height: 220px;
  overflow: auto;
  background: var(--el-fill-color-light);
  border-radius: 4px;
  font-size: 12px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-all;
}
.detail-json.error {
  color: var(--el-color-danger);
}
</style>
