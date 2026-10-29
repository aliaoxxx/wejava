<script setup lang="ts">
import CrudSearch from './CrudSearch.vue'
import CrudTable from './CrudTable.vue'
import CrudForm from './CrudForm.vue'
import { hasPerm } from '@/utils/permission'
import { loadSchemaDicts } from '@/hooks/useDict'

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
  <div class="page-container">
    <!-- 搜索区独立成卡片，高度自适应 -->
    <el-card v-if="schema.search?.length" shadow="never" class="page-search">
      <CrudSearch
        :schema="schema"
        :model="crud.searchModel"
        @search="crud.handleSearch"
        @reset="crud.handleReset"
      />
    </el-card>

    <!-- 主卡片：工具栏 / 表格 / 分页 三行，中间行由表格撑满并内部滚动 -->
    <el-card shadow="never" class="page-content">
      <div class="page-toolbar crud-toolbar">
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

      <div class="page-table-wrapper">
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
      </div>

      <el-pagination
        v-model:current-page="crud.query.pageNum"
        v-model:page-size="crud.query.pageSize"
        class="page-pagination"
        background
        layout="total, sizes, prev, pager, next, jumper"
        :total="crud.total"
        :page-sizes="[10, 20, 50, 100]"
        @current-change="onPageChange"
        @size-change="onSizeChange"
      />
    </el-card>

    <!-- 弹窗放在卡片外，避免占用卡片内容区的 grid 行 -->
    <CrudForm
      v-if="schema.form?.length"
      ref="formRef"
      :schema="schema"
      :dialog="crud.dialog"
      @submit="handleSubmit"
    />
  </div>
</template>
