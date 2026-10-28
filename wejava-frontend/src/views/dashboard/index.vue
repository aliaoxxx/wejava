<script setup lang="ts">
import { useUserStore } from '@/store/modules/user'
import { usePermissionStore } from '@/store/modules/permission'

const userStore = useUserStore()
const permissionStore = usePermissionStore()
</script>

<template>
  <div>
    <el-card>
      <template #header>欢迎回来，{{ userStore.userInfo?.nickname }}</template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="用户ID">{{ userStore.userInfo?.userId }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ userStore.userInfo?.username }}</el-descriptions-item>
        <el-descriptions-item label="角色">{{ userStore.roles.join(', ') }}</el-descriptions-item>
        <el-descriptions-item label="超级管理员">
          {{ userStore.userInfo?.superAdmin ? '是' : '否' }}
        </el-descriptions-item>
        <el-descriptions-item label="权限数量">{{ userStore.perms.length }}</el-descriptions-item>
        <el-descriptions-item label="可见菜单">{{ permissionStore.routes.length }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card style="margin-top: 16px">
      <template #header>当前权限（前 20 条）</template>
      <el-tag v-for="p in userStore.perms.slice(0, 20)" :key="p" class="perm-tag" type="info">
        {{ p }}
      </el-tag>
    </el-card>
  </div>
</template>

<style scoped>
.perm-tag {
  margin: 0 8px 8px 0;
}
</style>
