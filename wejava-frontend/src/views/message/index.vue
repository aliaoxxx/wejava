<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { listMessage, markAllMessageRead, markMessageRead } from '@/api/message'
import { useMessageStore } from '@/store/modules/message'

const messageStore = useMessageStore()

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, msgType: undefined as number | undefined, readFlag: undefined as number | undefined })
const detail = reactive({ visible: false, row: null as any })

const TABS = [
  { label: '全部', value: undefined },
  { label: '公告', value: 1 },
  { label: '系统', value: 2 }
]

async function fetchList() {
  loading.value = true
  try {
    const res = await listMessage(query)
    list.value = res.data?.list ?? []
    total.value = res.data?.total ?? 0
  } finally {
    loading.value = false
  }
}

function switchType(value: any) {
  query.msgType = value
  query.pageNum = 1
  fetchList()
}

async function openDetail(row: any) {
  detail.row = row
  detail.visible = true
  if (row.readFlag === 0) {
    await markMessageRead(row.messageId)
    row.readFlag = 1
    await messageStore.refreshUnread()
  }
}

async function readAll() {
  await markAllMessageRead()
  ElMessage.success('已全部标记为已读')
  await messageStore.refreshUnread()
  await fetchList()
}

onMounted(fetchList)
</script>

<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-radio-group :model-value="query.msgType" @update:model-value="switchType">
        <el-radio-button v-for="tab in TABS" :key="String(tab.value)" :value="tab.value">
          {{ tab.label }}
        </el-radio-button>
      </el-radio-group>
      <div class="actions">
        <el-button @click="fetchList">刷新</el-button>
        <el-button type="primary" :disabled="messageStore.unreadCount === 0" @click="readAll">
          全部已读
        </el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="list" border>
      <el-table-column label="类型" width="90" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="row.msgType === 1 ? 'warning' : row.msgType === 2 ? 'info' : 'primary'">
            {{ row.msgType === 1 ? '公告' : row.msgType === 2 ? '系统' : '消息' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="标题" min-width="240">
        <template #default="{ row }">
          <span :class="{ unread: row.readFlag === 0 }">{{ row.title }}</span>
          <el-badge v-if="row.readFlag === 0" is-dot class="dot" />
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="时间" width="180" />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="row.readFlag === 0 ? 'danger' : 'info'">
            {{ row.readFlag === 0 ? '未读' : '已读' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination class="pager" background layout="total, prev, pager, next" :total="total"
      v-model:current-page="query.pageNum" :page-size="query.pageSize"
      @current-change="() => fetchList()" />
  </el-card>

  <el-dialog v-model="detail.visible" :title="detail.row?.title" width="640px">
    <div class="detail-time">{{ detail.row?.createTime }}</div>
    <div class="detail-content">{{ detail.row?.content }}</div>
  </el-dialog>
</template>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.actions {
  display: flex;
  gap: 8px;
}
.unread {
  font-weight: 600;
}
.dot {
  margin-left: 6px;
}
.pager {
  margin-top: 12px;
  justify-content: flex-end;
}
.detail-time {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 12px;
}
.detail-content {
  white-space: pre-wrap;
  line-height: 1.7;
}
</style>
