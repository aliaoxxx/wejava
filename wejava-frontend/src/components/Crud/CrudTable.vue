<script setup lang="ts">
import dayjs from 'dayjs'
import { hasPerm } from '@/utils/permission'
import type { Column, CrudSchema } from '@/types/crud'
import { dictOptions } from '@/hooks/useDict'

const props = defineProps<{
  schema: CrudSchema
  list: any[]
  loading: boolean
}>()

const emit = defineEmits<{
  selectionChange: [rows: any[]]
  edit: [row: any]
  remove: [row: any]
}>()

const prefix = props.schema.permissionPrefix
const canEdit = (row: any) =>
  props.schema.readonly ? false : !prefix ? true : hasPerm(`${prefix}:edit`)
const canRemove = (row: any) => (!prefix ? true : hasPerm(`${prefix}:remove`))

function display(row: any, col: Column) {
  const value = row[col.prop]
  if (value === null || value === undefined) return '-'
  if (col.dateFormat) return dayjs(value).format(col.dateFormat)
  return value
}

function dictLabel(row: any, col: Column) {
  const value = row[col.prop]
  return dictOptions(col.dict)?.find((item) => String(item.value) === String(value))?.label ?? value
}
</script>

<template>
  <el-table
    class="page-table"
    height="100%"
    :data="list"
    :loading="loading"
    :row-key="schema.rowKey"
    border
    stripe
    empty-text="暂无数据"
    @selection-change="emit('selectionChange', $event)"
  >
    <el-table-column type="selection" width="46" align="center" />

    <el-table-column
      v-for="col in schema.columns"
      :key="col.prop"
      :prop="col.prop"
      :label="col.label"
      :width="col.width"
      :min-width="col.minWidth"
      :align="col.align || 'left'"
      :fixed="col.fixed"
      :sortable="col.sortable"
      :show-overflow-tooltip="col.showOverflowTooltip"
    >
      <template #default="{ row, $index }">
        <component :is="() => col.render!(row, $index)" v-if="col.render" />
        <el-tag v-else-if="col.dict" size="small" :type="row[col.prop] === 0 ? 'success' : 'info'">
          {{ dictLabel(row, col) }}
        </el-tag>
        <span v-else>{{ display(row, col) }}</span>
      </template>
    </el-table-column>

    <el-table-column label="操作" width="200" fixed="right" align="center">
      <template #default="{ row }">
        <el-button v-if="canEdit(row)" link type="primary" @click="emit('edit', row)">修改</el-button>
        <el-button v-if="canRemove(row)" link type="danger" @click="emit('remove', row)">删除</el-button>
        <slot name="row-actions" :row="row" />
      </template>
    </el-table-column>
  </el-table>
</template>
