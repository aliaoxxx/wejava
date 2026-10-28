<script setup lang="ts">
import type { CrudSchema } from '@/types/crud'

defineProps<{
  schema: CrudSchema
  model: Record<string, any>
}>()

const emit = defineEmits<{ search: []; reset: [] }>()
</script>

<template>
  <el-form :model="model" inline class="crud-search" @submit.prevent>
    <el-form-item v-for="field in schema.search" :key="field.prop" :label="field.label">
      <el-select
        v-if="field.type === 'select'"
        v-model="model[field.prop]"
        clearable
        placeholder="全部"
        style="width: 160px"
      >
        <el-option v-for="opt in field.options" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>

      <el-date-picker
        v-else-if="field.type === 'date'"
        v-model="model[field.prop]"
        type="date"
        value-format="YYYY-MM-DD HH:mm:ss"
        placeholder="选择日期"
        style="width: 180px"
      />

      <el-date-picker
        v-else-if="field.type === 'daterange'"
        v-model="model[field.prop]"
        type="datetimerange"
        value-format="YYYY-MM-DD HH:mm:ss"
        start-placeholder="开始"
        end-placeholder="结束"
        style="width: 340px"
      />

      <el-input
        v-else-if="field.type === 'number'"
        v-model.number="model[field.prop]"
        :placeholder="field.placeholder || '请输入'"
        clearable
        style="width: 140px"
      />

      <el-input
        v-else
        v-model="model[field.prop]"
        :placeholder="field.placeholder || '请输入'"
        clearable
        style="width: 180px"
        @keyup.enter="emit('search')"
      />
    </el-form-item>

    <el-form-item>
      <el-button type="primary" @click="emit('search')">查询</el-button>
      <el-button @click="emit('reset')">重置</el-button>
    </el-form-item>
  </el-form>
</template>

<style scoped>
.crud-search {
  margin-bottom: 4px;
}
</style>
