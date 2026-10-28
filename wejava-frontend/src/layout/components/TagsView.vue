<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { useTagsViewStore } from '@/store/modules/tagsView'

const tagsStore = useTagsViewStore()
const route = useRoute()
const router = useRouter()

function isActive(path: string) {
  return route.path === path
}

function handleClick(path: string) {
  if (route.path !== path) router.push(path)
}

function handleClose(path: string) {
  const index = tagsStore.visited.findIndex((item) => item.path === path)
  tagsStore.delView(path)
  if (isActive(path)) {
    const next = tagsStore.visited[index - 1] ?? tagsStore.visited[index] ?? tagsStore.visited[0]
    router.push(next?.fullPath ?? '/dashboard')
  }
}

function handleCommand(command: string) {
  if (command === 'others') {
    tagsStore.delOthers(route.path)
  } else if (command === 'all') {
    tagsStore.delAll()
    router.push('/dashboard')
  }
}
</script>

<template>
  <div class="tags-view">
    <el-scrollbar>
      <div class="tags-inner">
        <el-tag
          v-for="tag in tagsStore.visited"
          :key="tag.path"
          :effect="isActive(tag.path) ? 'dark' : 'plain'"
          :closable="!tag.affix"
          size="small"
          class="tag-item"
          @click="handleClick(tag.fullPath)"
          @close="handleClose(tag.path)"
        >
          {{ tag.title }}
        </el-tag>
      </div>
    </el-scrollbar>

    <el-dropdown trigger="click" @command="handleCommand">
      <el-button size="small" text class="tag-more">
        标签<el-icon><ArrowDown /></el-icon>
      </el-button>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item command="others">关闭其他</el-dropdown-item>
          <el-dropdown-item command="all">关闭全部</el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
  </div>
</template>

<style scoped>
.tags-view {
  display: flex;
  align-items: center;
  height: 34px;
  padding: 0 8px;
  background: var(--app-header-bg);
  border-bottom: 1px solid var(--app-border);
  transition: background-color 0.25s;
}
.tags-inner {
  display: flex;
  gap: 6px;
  padding: 4px 0;
  white-space: nowrap;
}
.tag-item {
  cursor: pointer;
}
.tag-more {
  margin-left: 8px;
}
</style>
