<script setup lang="ts">
import { useMessageStore } from '@/store/modules/message'

const messageStore = useMessageStore()

function handleCommand(command: string) {
  if (command === 'readAll') {
    messageStore.markAllRead()
  }
}
</script>

<template>
  <el-popover placement="bottom-end" :width="300" trigger="click" :show-after="0" :hide-after="0">
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
      <el-button
        v-if="messageStore.unreadCount > 0"
        link
        type="primary"
        size="small"
        @click="handleCommand('readAll')"
      >
        全部已读
      </el-button>
    </div>

    <div v-if="messageStore.messages.length" class="msg-list">
      <div v-for="item in messageStore.messages" :key="item.id" class="msg-item">
        <div class="msg-title">{{ item.title }}</div>
        <div class="msg-content">{{ item.content }}</div>
        <div class="msg-time">{{ item.createdAt }}</div>
      </div>
    </div>
    <el-empty v-else description="暂无新消息" :image-size="60" />
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
  max-height: 280px;
  overflow-y: auto;
}
.msg-item {
  padding: 8px 0;
  border-bottom: 1px dashed var(--el-border-color-lighter);
}
.msg-item:last-child {
  border-bottom: none;
}
.msg-title {
  font-size: 13px;
  color: var(--el-text-color-primary);
}
.msg-content {
  margin-top: 2px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.msg-time {
  margin-top: 2px;
  font-size: 11px;
  color: var(--el-text-color-placeholder);
}
</style>
