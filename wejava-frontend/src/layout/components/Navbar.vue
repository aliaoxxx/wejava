<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '@/store/modules/app'
import { useUserStore } from '@/store/modules/user'
import ThemeSwitcher from './ThemeSwitcher.vue'
import NotificationBell from './NotificationBell.vue'

const appStore = useAppStore()
const userStore = useUserStore()
const route = useRoute()
const router = useRouter()

async function handleCommand(command: string) {
  if (command === 'profile') {
    await router.push('/profile')
    return
  }
  if (command === 'logout') {
    await userStore.logout()
    await router.push('/login')
  }
}
</script>

<template>
  <div class="left">
    <el-icon class="collapse-btn" @click="appStore.toggleSidebar()">
      <Fold v-if="!appStore.sidebarCollapsed" />
      <Expand v-else />
    </el-icon>
    <span class="page-title">{{ route.meta.title }}</span>
  </div>
  <div class="right">
    <NotificationBell />
    <ThemeSwitcher class="theme-slot" />
    <el-dropdown @command="handleCommand">
      <span class="user-info">
        {{ userStore.userInfo?.nickname || userStore.userInfo?.username || '未登录' }}
        <el-icon><ArrowDown /></el-icon>
      </span>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item command="profile">个人中心</el-dropdown-item>
          <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
  </div>
</template>

<style scoped>
.left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.collapse-btn {
  cursor: pointer;
  font-size: 18px;
}
.page-title {
  font-size: 15px;
  font-weight: 500;
}
.right {
  display: flex;
  align-items: center;
  gap: 16px;
}
.user-info {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
}
</style>
