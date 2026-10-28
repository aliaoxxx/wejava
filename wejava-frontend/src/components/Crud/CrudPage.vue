<script setup lang="ts">
import CrudSearch from './CrudSearch.vue'
import CrudTable from './CrudTable.vue'
import CrudForm from './CrudForm.vue'
import { hasPerm } from '@/utils/permission'

import type { CrudInstance } from '@/hooks/useCrud'

const props = defineProps<{ crud: CrudInstance }>()
const schema = props.crud.schema
const prefix: string | undefined = schema.permissionPrefix

const canAdd = computed(() => !prefix || hasPerm(`${prefix}:add`))
const formRef = ref()

async function handleSubmit() {
  const ok = await formRef.value?.validate()
  if (!ok) return
  await props.crud.submit()
}
const canRemove = computed(() => !prefix || hasPerm(`${prefix}:remove`))

function onPageChange(page: number) {
  props.crud.query.pageNum = page
  props.crud.fetchList()
}

function onSizeChange(size: number) {
  props.crud.query.pageSize = size
  props.crud.query.pageNum = 1
  props.crud.fetchList()
}
</script>

<template>
  <el-card shadow="never">
    <CrudSearch
      v-if="schema.search?.length"
      :schema="schema"
      :model="crud.searchModel"
      @search="crud.handleSearch"
      @reset="crud.handleReset"
    />

    <div class="crud-toolbar">
      <el-button v-if="canAdd && !schema.readonly" type="primary" @click="crud.openCreate()">新增</el-button>
      <el-button
        v-if="canRemove"
        type="danger"
        :disabled="!crud.selected.length"
        @click="crud.handleRemove(crud.selected)"
      >
        批量删除
      </el-button>
      <slot name="toolbar" />
    </div>

    <CrudTable
      :schema="schema"
      :list="crud.list"
      :loading="crud.loading"
      @selection-change="crud.handleSelectionChange"
      @edit="crud.openEdit"
      @remove="(row: any) => crud.handleRemove([row])"
    >
      <template #row-actions="{ row }">
        <slot name="row-actions" :row="row" />
      </template>
    </CrudTable>

    <el-pagination
      class="crud-pagination"
      background
      layout="total, sizes, prev, pager, next, jumper"
      :total="crud.total"
      :current-page="crud.query.pageNum"
      :page-size="crud.query.pageSize"
      :page-sizes="[10, 20, 50, 100]"
      @current-change="onPageChange"
      @size-change="onSizeChange"
    />

    <CrudForm
      v-if="schema.form?.length"
      ref="formRef"
      :schema="schema"
      :dialog="crud.dialog"
      @submit="handleSubmit"
    />
  </el-card>
</template>

<style scoped>
.crud-toolbar {
  display: flex;
  gap: 8px;
  margin: 12px 0;
}
.crud-pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
