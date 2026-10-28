<script setup lang="ts">
import { PRESETS, useThemeStore, type ThemeMode } from '@/store/modules/theme'

const themeStore = useThemeStore()

const MODES: Array<{ value: ThemeMode; label: string; icon: string }> = [
  { value: 'light', label: '亮色', icon: 'Sunny' },
  { value: 'dark', label: '暗色', icon: 'Moon' },
  { value: 'system', label: '跟随系统', icon: 'Monitor' }
]
</script>

<template>
  <el-popover
    placement="bottom-end"
    :width="260"
    trigger="click"
    :show-after="0"
    :hide-after="0"
    :persistent="false"
    :transition="themeStore.motion === 'none' ? 'none' : 'el-fade-in-linear'"
  >
    <template #reference>
      <el-icon class="theme-trigger" :title="`当前：${themeStore.isDark ? '暗色' : '亮色'}`">
        <Moon v-if="themeStore.isDark" />
        <Sunny v-else />
      </el-icon>
    </template>

    <div class="theme-panel">
      <div class="panel-label">明暗模式</div>
      <el-radio-group
        :model-value="themeStore.mode"
        size="small"
        @update:model-value="(v: any) => themeStore.setMode(v)"
      >
        <el-radio-button v-for="item in MODES" :key="item.value" :value="item.value">
          <el-icon><component :is="item.icon" /></el-icon>
          {{ item.label }}
        </el-radio-button>
      </el-radio-group>

      <div class="panel-label">界面动效</div>
      <el-radio-group
        :model-value="themeStore.motion"
        size="small"
        @update:model-value="(v: any) => themeStore.setMotion(v)"
      >
        <el-radio-button value="full">标准</el-radio-button>
        <el-radio-button value="fast">轻快</el-radio-button>
        <el-radio-button value="none">关闭</el-radio-button>
      </el-radio-group>

      <div class="panel-label">主题色</div>
      <div class="preset-list">
        <div
          v-for="item in PRESETS"
          :key="item.key"
          class="preset-item"
          :class="{ active: themeStore.preset === item.key }"
          @click="themeStore.setPreset(item.key)"
        >
          <span class="dot" :style="{ background: item.primary }" />
          <span class="preset-name">{{ item.label }}</span>
        </div>
      </div>
    </div>
  </el-popover>
</template>

<style scoped>
.theme-trigger {
  cursor: pointer;
  font-size: 18px;
}
.theme-panel {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.panel-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}
.preset-list {
  display: flex;
  gap: 12px;
}
.preset-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  padding: 4px;
  border-radius: 6px;
  border: 1px solid transparent;
}
.preset-item.active {
  border-color: var(--el-color-primary);
}
.dot {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  display: inline-block;
}
.preset-name {
  font-size: 12px;
  color: var(--el-text-color-regular);
}
</style>
