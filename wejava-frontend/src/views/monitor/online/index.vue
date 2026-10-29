<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { kickOnline, listOnline } from '@/api/monitor'

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10 })

async function fetchList() {
  loading.value = true
  try {
    const res = await listOnline(query)
    list.value = res.data?.list ?? []
    total.value = res.data?.total ?? 0
  } finally {
    loading.value = false
  }
}

async function handleKick(row: any) {
  await ElMessageBox.confirm(`确认强制「${row.username}」下线？`, '提示', { type: 'warning' })
  await kickOnline(row.jti)
  ElMessage.success('已强制下线')
  await fetchList()
}

onMounted(fetchList)
</script>

<template>
  <div class="page-container">
    <el-card shadow="never" class="page-content">
      <div class="page-toolbar">
        <el-button @click="fetchList">刷新</el-button>
        <span class="tip">在线用户 {{ total }} 人（含本机所有会话）</span>
      </div>

      <div class="page-table-wrapper">
        <el-table v-loading="loading" :data="list" border class="page-table" height="100%">
          <el-table-column prop="username" label="用户名" min-width="120" />
          <el-table-column prop="nickname" label="昵称" min-width="120" />
          <el-table-column prop="userId" label="用户ID" width="180" />
          <el-table-column prop="deptId" label="部门ID" width="180" />
          <el-table-column prop="lastActiveTime" label="最后活跃" width="180" />
          <el-table-column label="操作" width="120" align="center">
            <template #default="{ row }">
              <el-button v-hasPerm="'monitor:online:forceLogout'" link type="danger" @click="handleKick(row)">
                强制下线
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <el-pagination class="page-pagination" background layout="total, prev, pager, next" :total="total"
        v-model:current-page="query.pageNum" :page-size="query.pageSize"
        @current-change="() => fetchList()" />
    </el-card>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.tip {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.pager {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>
