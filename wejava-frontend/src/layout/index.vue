<script setup lang="ts">
import { useMessageStore } from '@/store/modules/message'
import { createNotificationStream } from '@/utils/sse'

const messageStore = useMessageStore()

// SSE 实时通知：收到新消息事件后刷新未读数与列表
const stream = createNotificationStream({
  onConnected: () => messageStore.refreshUnread(),
  onMessage: () => {
    messageStore.refreshUnread()
    messageStore.fetchRecent()
  }
})

onMounted(() => {
  messageStore.refreshUnread()
  stream.connect()
})

onBeforeUnmount(() => stream.close())
import Sidebar from './components/Sidebar.vue'
import Navbar from './components/Navbar.vue'
import AppMain from './components/AppMain.vue'
import TagsView from './components/TagsView.vue'
import { useAppStore } from '@/store/modules/app'

const appStore = useAppStore()
</script>

<template>
  <el-container class="layout-container">
    <el-aside class="layout-aside" :width="appStore.sidebarCollapsed ? '64px' : '220px'">
      <Sidebar />
    </el-aside>
    <el-container>
      <el-header class="layout-header">
        <Navbar />
      </el-header>
      <TagsView />
      <el-main class="layout-main">
        <AppMain />
      </el-main>
    </el-container>
  </el-container>
</template>
