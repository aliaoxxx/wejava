<script setup lang="ts">
import type { AppRoute } from '@/router/transform'

defineProps<{ item: AppRoute; basePath?: string }>()

/** 后端菜单里的图标名（RuoYi 风格）→ Element Plus 图标组件名 */
const ICON_MAP: Record<string, string> = {
  setting: 'Setting',
  user: 'User',
  peoples: 'UserFilled',
  'tree-table': 'Grid',
  tree: 'Share',
  post: 'Postcard',
  dict: 'Collection',
  edit: 'Edit',
  message: 'Message',
  monitor: 'Monitor',
  online: 'Connection',
  form: 'Document',
  logininfor: 'Key',
  server: 'Cpu'
}

function iconOf(item: AppRoute): string {
  const icon = item.meta?.icon as string | undefined
  if (icon && ICON_MAP[icon]) return ICON_MAP[icon]
  return item.children?.length ? 'Folder' : 'Document'
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
