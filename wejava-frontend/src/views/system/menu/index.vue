<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { createMenu, listMenu, menuTreeSelect, removeMenu, updateMenu } from '@/api/menu'

const loading = ref(false)
const list = ref<any[]>([])
const saving = ref(false)
const formRef = ref<FormInstance>()
const tableRef = ref()
/** 默认折叠：大菜单树一次全展开反而难找 */
const expandedAll = ref(false)

function toggleExpand() {
  expandedAll.value = !expandedAll.value
  const walk = (rows: any[]) => {
    rows.forEach((row) => {
      tableRef.value?.toggleRowExpansion(row, expandedAll.value)
      if (row.children?.length) walk(row.children)
    })
  }
  walk(list.value)
}
const parentOptions = ref<any[]>([])

const dialog = reactive({
  visible: false,
  mode: 'create' as 'create' | 'edit',
  title: '',
  model: {} as Record<string, any>
})

const TYPE_OPTIONS = [
  { label: '目录', value: 'M' },
  { label: '菜单', value: 'C' },
  { label: '按钮', value: 'F' }
]
const STATUS_OPTIONS = [
  { label: '正常', value: 0 },
  { label: '停用', value: 1 }
]

const rules: FormRules = {
  menuName: [{ required: true, message: '菜单名称不能为空', trigger: 'blur' }],
  menuType: [{ required: true, message: '请选择菜单类型', trigger: 'change' }]
}

async function fetchList() {
  loading.value = true
  try {
    const res = await listMenu()
    list.value = res.data ?? []
  } finally {
    loading.value = false
  }
}

async function fetchParents() {
  const res = await menuTreeSelect()
  parentOptions.value = [{ menuId: 0, menuName: '顶级菜单', children: res.data ?? [] }]
}

function openCreate(parent?: any) {
  dialog.mode = 'create'
  dialog.title = parent ? `新增子菜单 - ${parent.menuName}` : '新增菜单'
  dialog.model = {
    parentId: parent?.menuId ?? 0,
    menuType: 'C',
    orderNum: 1,
    status: 0,
    visible: 0,
    isFrame: 0,
    isCache: 0
  }
  dialog.visible = true
}

function openEdit(row: any) {
  dialog.mode = 'edit'
  dialog.title = `修改菜单 - ${row.menuName}`
  dialog.model = { ...row }
  dialog.visible = true
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  const model = dialog.model
  if (model.menuType === 'F' && !model.perms) return ElMessage.warning('按钮类型必须填写权限字符')
  if (model.menuType === 'C' && !model.component) return ElMessage.warning('菜单类型必须填写组件路径')
  if (model.menuId && model.parentId === model.menuId) return ElMessage.warning('上级菜单不能是自己')

  saving.value = true
  try {
    if (dialog.mode === 'create') {
      await createMenu(model)
      ElMessage.success('新增成功')
    } else {
      await updateMenu(model)
      ElMessage.success('修改成功')
    }
    dialog.visible = false
    await fetchList()
    await fetchParents()
  } finally {
    saving.value = false
  }
}

async function handleRemove(row: any) {
  await ElMessageBox.confirm(`确认删除菜单「${row.menuName}」？`, '提示', { type: 'warning' })
  await removeMenu(row.menuId)
  ElMessage.success('删除成功')
  await fetchList()
}

onMounted(() => {
  fetchList()
  fetchParents()
})
</script>

<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-button v-hasPerm="'system:menu:add'" type="primary" @click="openCreate()">新增菜单</el-button>
      <el-button @click="toggleExpand">{{ expandedAll ? '全部折叠' : '全部展开' }}</el-button>
      <el-button @click="fetchList">刷新</el-button>
    </div>

    <el-table ref="tableRef" v-loading="loading" :data="list" row-key="menuId" border
      :tree-props="{ children: 'children' }">
      <el-table-column prop="menuName" label="菜单名称" min-width="180" />
      <el-table-column label="类型" width="80" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="row.menuType === 'F' ? 'info' : row.menuType === 'M' ? 'warning' : 'success'">
            {{ TYPE_OPTIONS.find((t) => t.value === row.menuType)?.label }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="orderNum" label="排序" width="70" align="center" />
      <el-table-column prop="path" label="路由地址" min-width="110" />
      <el-table-column prop="component" label="组件路径" min-width="150" show-overflow-tooltip />
      <el-table-column prop="perms" label="权限字符" min-width="150" show-overflow-tooltip />
      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === 0 ? 'success' : 'info'">
            {{ row.status === 0 ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="190" fixed="right" align="center">
        <template #default="{ row }">
          <el-button v-if="row.menuType !== 'F'" link type="primary" @click="openCreate(row)">新增</el-button>
          <el-button v-hasPerm="'system:menu:edit'" link type="primary" @click="openEdit(row)">修改</el-button>
          <el-button v-hasPerm="'system:menu:remove'" link type="danger" @click="handleRemove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="dialog.visible" :title="dialog.title" width="640px" destroy-on-close>
    <el-form ref="formRef" :model="dialog.model" :rules="rules" label-width="96px">
      <el-row :gutter="16">
        <el-col :span="24">
          <el-form-item label="上级菜单">
            <el-tree-select v-model="dialog.model.parentId" :data="parentOptions" node-key="menuId"
              :props="{ label: 'menuName', children: 'children' }" check-strictly default-expand-all
              style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="菜单类型" prop="menuType">
            <el-radio-group v-model="dialog.model.menuType">
              <el-radio v-for="t in TYPE_OPTIONS" :key="t.value" :value="t.value">{{ t.label }}</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="菜单名称" prop="menuName">
            <el-input v-model="dialog.model.menuName" placeholder="如：用户管理" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="显示排序">
            <el-input-number v-model="dialog.model.orderNum" :min="0" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col v-if="dialog.model.menuType !== 'F'" :span="12">
          <el-form-item label="路由地址">
            <el-input v-model="dialog.model.path" placeholder="如：user" />
          </el-form-item>
        </el-col>
        <el-col v-if="dialog.model.menuType === 'C'" :span="12">
          <el-form-item label="组件路径">
            <el-input v-model="dialog.model.component" placeholder="如：system/user/index" />
          </el-form-item>
        </el-col>
        <el-col v-if="dialog.model.menuType !== 'M'" :span="12">
          <el-form-item label="权限字符">
            <el-input v-model="dialog.model.perms" placeholder="如：system:user:add" />
          </el-form-item>
        </el-col>
        <el-col v-if="dialog.model.menuType !== 'F'" :span="12">
          <el-form-item label="菜单图标">
            <el-input v-model="dialog.model.icon" placeholder="如：user / setting" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="状态">
            <el-select v-model="dialog.model.status" style="width: 100%">
              <el-option v-for="s in STATUS_OPTIONS" :key="s.value" :label="s.label" :value="s.value" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <template #footer>
      <el-button @click="dialog.visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">确定</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
</style>
