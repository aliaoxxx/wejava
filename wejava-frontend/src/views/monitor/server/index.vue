<script setup lang="ts">
import { getServerInfo } from '@/api/monitor'

const loading = ref(false)
const info = ref<any>({})

/** 后端把所有 Long 序列化成了字符串（防前端精度丢失），这里统一转数值 */
function num(value: any): number {
  const n = Number(value)
  return Number.isFinite(n) ? n : 0
}

function formatBytes(raw?: any) {
  const bytes = num(raw)
  if (bytes <= 0) return '-'
  const gb = bytes / 1024 / 1024 / 1024
  return gb >= 1 ? `${gb.toFixed(2)} GB` : `${(bytes / 1024 / 1024).toFixed(0)} MB`
}

function formatUptime(raw?: any) {
  const seconds = num(raw)
  if (seconds <= 0) return '-'
  const d = Math.floor(seconds / 86400)
  const h = Math.floor((seconds % 86400) / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  return `${d}天 ${h}小时 ${m}分`
}

function percent(rawUsed?: any, rawTotal?: any) {
  const used = num(rawUsed)
  const total = num(rawTotal)
  if (total <= 0) return 0
  return Math.min(100, Math.round((used / total) * 100))
}

async function fetchInfo() {
  loading.value = true
  try {
    const res = await getServerInfo()
    info.value = res.data ?? {}
  } finally {
    loading.value = false
  }
}

onMounted(fetchInfo)
</script>

<template>
  <div v-loading="loading">
    <el-row :gutter="16">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>服务器信息</template>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="操作系统">
              {{ info.osName }} {{ info.osArch }} {{ info.osVersion }}
            </el-descriptions-item>
            <el-descriptions-item label="CPU 核心数">{{ info.cpuCores }}</el-descriptions-item>
            <el-descriptions-item label="CPU 使用率">
              <el-progress :percentage="Math.round(num(info.cpuLoad) * 100)" :stroke-width="14" />
            </el-descriptions-item>
            <el-descriptions-item label="物理内存">
              {{ formatBytes(info.memUsed) }} / {{ formatBytes(info.memTotal) }}
            </el-descriptions-item>
            <el-descriptions-item label="磁盘">
              {{ formatBytes(num(info.diskTotal) - num(info.diskUsable)) }} /
              {{ formatBytes(info.diskTotal) }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card shadow="never">
          <template #header>JVM 信息</template>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="Java 版本">{{ info.javaVersion }}</el-descriptions-item>
            <el-descriptions-item label="虚拟机">{{ info.jvmName }}</el-descriptions-item>
            <el-descriptions-item label="运行时长">{{ formatUptime(info.uptimeSeconds) }}</el-descriptions-item>
            <el-descriptions-item label="堆内存">
              <el-progress :percentage="percent(info.jvmUsed, info.jvmMax)" :stroke-width="14" />
              {{ formatBytes(info.jvmUsed) }} / {{ formatBytes(info.jvmMax) }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" style="margin-top: 16px">
      <template #header>依赖服务</template>
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="数据库">{{ info.dbProduct || '-' }}</el-descriptions-item>
        <el-descriptions-item label="Redis 版本">{{ info.redisVersion || '-' }}</el-descriptions-item>
        <el-descriptions-item label="Redis 内存">{{ info.redisUsedMemory || '-' }}</el-descriptions-item>
        <el-descriptions-item label="磁盘路径">{{ info.diskPath || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>
