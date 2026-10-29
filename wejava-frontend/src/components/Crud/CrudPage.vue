<script setup lang="ts">
import CrudSearch from './CrudSearch.vue'
import CrudTable from './CrudTable.vue'
import CrudForm from './CrudForm.vue'
import { hasPerm } from '@/utils/permission'
import { loadSchemaDicts } from '@/hooks/useDict'
import { useFillHeight } from '@/hooks/useFillHeight'

import type { CrudInstance } from '@/hooks/useCrud'

const props = defineProps<{ crud: CrudInstance }>()
const schema = props.crud.schema
const prefix: string | undefined = schema.permissionPrefix

const canAdd = computed(() => !prefix || hasPerm(`${prefix}:add`))

// 列和表单里用到的字典类型，进入页面时统一加载
onMounted(() => {
  loadSchemaDicts([...(schema.columns ?? []), ...(schema.form ?? [])] as any)
})
const formRef = ref()
const { wrapRef: tableWrapRef, height: tableHeight } = useFillHeight()

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
  <div class="fill-page">
  <!-- 搜索区独立成卡片 -->
  <el-card v-if="schema.search?.length" shadow="never" class="crud-search-card">
    <CrudSearch
      :schema="schema"
      :model="crud.searchModel"
      @search="crud.handleSearch"
      @reset="crud.handleReset"
    />
  </el-card>

  <el-card shadow="never" class="fill-card">
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

    <div ref="tableWrapRef" class="fill-table-wrap">
    <CrudTable
      :schema="schema"
      :list="crud.list"
      :loading="crud.loading"
      :table-height="tableHeight"
      @selection-change="crud.handleSelectionChange"
      @edit="crud.openEdit"
      @remove="(row: any) => crud.handleRemove([row])"
    >
      <template #row-actions="{ row }">
        <slot name="row-actions" :row="row" />
      </template>
    </CrudTable>
    </div>

    <el-pagination
      v-model:current-page="crud.query.pageNum"
      v-model:page-size="crud.query.pageSize"
      class="fill-pager"
      background
      layout="total, sizes, prev, pager, next, jumper"
      :total="crud.total"
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
  </div>
</template>

<style scoped>
.crud-search-card {
  margin-bottom: 12px;
}
.crud-toolbar {
  display: flex;
  gap: 8px;
  margin: 12px 0;
}

</style>
