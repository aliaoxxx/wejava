<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()
</script>

<template>
  <el-card>
    <template #header>{{ route.meta.title || '页面' }}</template>
    <el-empty description="该页面将在 M3-3 用配置驱动的方式实现" />
  </el-card>
</template>
