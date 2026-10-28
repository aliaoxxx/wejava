<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useMessageStore } from '@/store/modules/message'

const messageStore = useMessageStore()
const router = useRouter()

const TYPE_LABEL: Record<number, string> = { 1: '公告', 2: '系统', 3: '消息' }
const TYPE_TAG: Record<number, string> = { 1: 'warning', 2: 'info', 3: 'primary' }

async function handleOpen() {
  await messageStore.fetchRecent()
}

async function openDetail(item: any) {
  if (item.readFlag === 0) {
    await messageStore.readOne(item.messageId)
  }
  if (item.link) {
    router.push(item.link)
  }
}

function goAll() {
  router.push('/message')
}

async function readAll() {
  await messageStore.readAll()
}
</script>

<template>
  <el-popover
    placement="bottom-end"
    :width="320"
    trigger="click"
    :show-after="0"
    :hide-after="0"
    @show="handleOpen"
  >
    <template #reference>
      <el-badge
        :value="messageStore.unreadCount"
        :hidden="messageStore.unreadCount === 0"
        :max="99"
        class="bell-badge"
      >
        <el-icon class="bell" :title="`未读消息 ${messageStore.unreadCount} 条`">
          <Bell />
        </el-icon>
      </el-badge>
    </template>

    <div class="msg-header">
      <span>消息通知</span>
      <el-button v-if="messageStore.unreadCount > 0" link type="primary" size="small" @click="readAll">
        全部已读
      </el-button>
    </div>

    <div v-loading="messageStore.loading">
      <div v-if="messageStore.recent.length" class="msg-list">
        <div v-for="item in messageStore.recent" :key="item.messageId" class="msg-item"
          :class="{ unread: item.readFlag === 0 }" @click="openDetail(item)">
          <div class="msg-title">
            <el-tag size="small" :type="(TYPE_TAG[item.msgType] as any) || 'info'">
              {{ TYPE_LABEL[item.msgType] || '消息' }}
            </el-tag>
            <span class="title-text">{{ item.title }}</span>
          </div>
          <div class="msg-content">{{ item.content }}</div>
          <div class="msg-time">{{ item.createTime }}</div>
        </div>
      </div>
      <el-empty v-else description="暂无新消息" :image-size="60" />
    </div>

    <div class="msg-footer">
      <el-button link type="primary" @click="goAll">查看全部</el-button>
    </div>
  </el-popover>
</template>

<style scoped>
.bell-badge {
  display: inline-flex;
  align-items: center;
}
.bell {
  cursor: pointer;
  font-size: 18px;
}
.msg-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 8px;
  margin-bottom: 4px;
  font-size: 13px;
  font-weight: 500;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.msg-list {
  max-height: 300px;
  overflow-y: auto;
}
.msg-item {
  padding: 8px 6px;
  border-radius: 4px;
  cursor: pointer;
  border-bottom: 1px dashed var(--el-border-color-lighter);
}
.msg-item:hover {
  background: var(--el-fill-color-light);
}
.msg-item.unread .title-text {
  font-weight: 600;
}
.msg-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--el-text-color-primary);
}
.title-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.msg-content {
  margin-top: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.msg-time {
  margin-top: 4px;
  font-size: 11px;
  color: var(--el-text-color-placeholder);
}
.msg-footer {
  margin-top: 8px;
  text-align: center;
  border-top: 1px solid var(--el-border-color-lighter);
  padding-top: 6px;
}
</style>
