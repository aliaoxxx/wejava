<script setup lang="ts">
import zhCn from 'element-plus/es/locale/lang/zh-cn'

const renderError = ref('')

onErrorCaptured((err) => {
  renderError.value = err instanceof Error ? `${err.message}\n\n${err.stack ?? ''}` : String(err)
  console.error('[WeJava] 页面渲染异常', err)
  return false
})

function reload() {
  renderError.value = ''
  window.location.reload()
}
</script>

<template>
  <el-config-provider :locale="zhCn">
    <div v-if="renderError" class="error-boundary">
      <h3>页面渲染出错</h3>
      <pre>{{ renderError }}</pre>
      <el-button type="primary" @click="reload">重新加载</el-button>
    </div>
    <router-view v-else />
  </el-config-provider>
</template>

<style scoped>
.error-boundary {
  padding: 24px;
  color: #f56c6c;
  font-family: monospace;
}
.error-boundary pre {
  white-space: pre-wrap;
  background: #fef0f0;
  padding: 12px;
  border-radius: 4px;
  max-height: 60vh;
  overflow: auto;
}
</style>
