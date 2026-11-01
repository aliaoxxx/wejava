<script setup lang="ts">
import logoMark from '@/assets/logo-mark.svg'
import SidebarItem from './SidebarItem.vue'
import { usePermissionStore } from '@/store/modules/permission'
import { useAppStore } from '@/store/modules/app'

const permissionStore = usePermissionStore()
const appStore = useAppStore()
</script>

<template>
  <div class="sidebar">
    <div class="logo">
      <img class="logo-mark" :src="logoMark" alt="WeJavaAdmin" />
      <span v-show="!appStore.sidebarCollapsed" class="logo-text">WeJava Admin</span>
    </div>
    <el-scrollbar class="menu-scroll">
      <el-menu
        :default-active="$route.path"
        :collapse="appStore.sidebarCollapsed"
        :collapse-transition="false"
        unique-opened
        router
      >
        <el-menu-item index="/dashboard">
          <el-icon><HomeFilled /></el-icon>
          <template #title>首页</template>
        </el-menu-item>
        <SidebarItem v-for="route in permissionStore.routes" :key="route.path" :item="route" />
      </el-menu>
    </el-scrollbar>
  </div>
</template>
