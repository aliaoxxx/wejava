<script setup lang="ts">
import type { AppRoute } from '@/router/transform'
import { resolveIcon } from '@/utils/icon'

defineProps<{ item: AppRoute; basePath?: string }>()

function iconOf(item: AppRoute): string {
  return resolveIcon(item.meta?.icon as string | undefined, item.children?.length ? 'Folder' : 'Document')
}
</script>

<template>
  <template v-if="!item.meta?.hidden">
    <el-sub-menu v-if="item.children && item.children.length" :index="item.path">
      <template #title>
        <el-icon><component :is="iconOf(item)" /></el-icon>
        <span>{{ item.meta?.title }}</span>
      </template>
      <SidebarItem
        v-for="child in item.children"
        :key="child.path"
        :item="child"
        :base-path="`${item.path}/${child.path}`.replace('//', '/')"
      />
    </el-sub-menu>

    <el-menu-item v-else :index="basePath || item.path">
      <el-icon><component :is="iconOf(item)" /></el-icon>
      <template #title>{{ item.meta?.title }}</template>
    </el-menu-item>
  </template>
</template>
