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
    <el-scrollbar class="tags-scroll">
      <div class="tags-inner">
        <div
          v-for="tag in tagsStore.visited"
          :key="tag.path"
          class="tag-item"
          :class="{ active: isActive(tag.path) }"
          @click="handleClick(tag.fullPath)"
        >
          <el-icon v-if="tag.icon" class="tag-icon"><component :is="tag.icon" /></el-icon>
          <span class="tag-title">{{ tag.title }}</span>
          <el-icon
            v-if="!tag.affix"
            class="tag-close"
            :class="{ always: isActive(tag.path) }"
            @click.stop="handleClose(tag.path)"
          >
            <Close />
          </el-icon>
        </div>
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
  gap: 8px;
  height: 40px;
  padding: 0 12px;
  background: var(--app-header-bg);
  border-bottom: 1px solid var(--app-border);
  transition: background-color 0.25s;
}

.tags-scroll {
  flex: 1 1 auto;
  min-width: 0;
}

.tags-inner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  white-space: nowrap;
}

.tag-item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 26px;
  padding: 0 9px;
  font-size: 12px;
  line-height: 1;
  cursor: pointer;
  border: 1px solid var(--app-border);
  border-radius: 6px;
  color: var(--el-text-color-regular);
  background: transparent;
  transition:
    color 0.15s,
    border-color 0.15s,
    background-color 0.15s;
}

.tag-item:hover {
  color: var(--el-color-primary);
  border-color: var(--el-color-primary-light-5);
}

.tag-item.active {
  color: var(--el-color-primary);
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  font-weight: 500;
}

.tag-icon {
  font-size: 13px;
}

.tag-close {
  font-size: 12px;
  border-radius: 50%;
  opacity: 0;
  transition:
    opacity 0.15s,
    background-color 0.15s;
}

.tag-item:hover .tag-close,
.tag-close.always {
  opacity: 1;
}

.tag-close:hover {
  color: #fff;
  background: var(--el-color-primary);
}

.tag-more {
  flex: 0 0 auto;
}
</style>
