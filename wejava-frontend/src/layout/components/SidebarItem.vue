<script setup lang="ts">
import type { AppRoute } from '@/router/transform'
import { resolveIcon } from '@/utils/icon'

const props = defineProps<{ item: AppRoute; basePath?: string }>()

function iconOf(item: AppRoute): string {
  return resolveIcon(item.meta?.icon as string | undefined, item.children?.length ? 'Folder' : 'Document')
}

/** 只有一个可见子项时直接展示子项，避免出现「分组里套一项」的结构 */
const onlyChild = computed(() => {
  const visible = (props.item.children ?? []).filter((child) => !child.meta?.hidden)
  return visible.length === 1 ? visible[0] : null
})

function joinPath(parent?: string, child?: string) {
  const p = `${parent ?? ''}/${child ?? ''}`.replace(/\/+/g, '/')
  return p.length > 1 ? p.replace(/\/$/, '') : p
}
</script>

<template>
  <template v-if="!item.meta?.hidden">
    <el-menu-item v-if="onlyChild" :index="joinPath(item.path, onlyChild.path)">
      <el-icon><component :is="iconOf(onlyChild)" /></el-icon>
      <template #title>{{ onlyChild.meta?.title || item.meta?.title }}</template>
    </el-menu-item>

    <el-sub-menu v-else-if="item.children && item.children.length" :index="item.path">
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
