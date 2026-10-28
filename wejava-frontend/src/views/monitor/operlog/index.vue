<script setup lang="ts">
import { h } from 'vue'
import { ElMessage, ElMessageBox, ElTag } from 'element-plus'
import CrudPage from '@/components/Crud/CrudPage.vue'
import { useCrud } from '@/hooks/useCrud'
import { cleanOperLog } from '@/api/monitor'
import { operLogSchema } from './schema'

const crud = useCrud(operLogSchema)

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
  </CrudPage>
</template>
