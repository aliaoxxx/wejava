<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { createDept, deptTreeSelect, listDept, removeDept, updateDept } from '@/api/dept'

const loading = ref(false)
const list = ref<any[]>([])
const saving = ref(false)
const formRef = ref<FormInstance>()
const parentOptions = ref<any[]>([])

const dialog = reactive({
  visible: false,
  mode: 'create' as 'create' | 'edit',
  title: '',
  model: {} as Record<string, any>
})

const rules: FormRules = {
  deptName: [{ required: true, message: '部门名称不能为空', trigger: 'blur' }],
  parentId: [{ required: true, message: '请选择上级部门', trigger: 'change' }]
}

async function fetchList() {
  loading.value = true
  try {
    const res = await listDept()
    list.value = res.data ?? []
  } finally {
    loading.value = false
  }
}

async function fetchParents() {
  const res = await deptTreeSelect()
  parentOptions.value = [{ deptId: 0, deptName: '顶级部门', children: res.data ?? [] }]
}

function openCreate(parent?: any) {
  dialog.mode = 'create'
  dialog.title = parent ? `新增子部门 - ${parent.deptName}` : '新增部门'
  dialog.model = { parentId: parent?.deptId ?? 0, orderNum: 1, status: 0 }
  dialog.visible = true
}

function openEdit(row: any) {
  dialog.mode = 'edit'
  dialog.title = `修改部门 - ${row.deptName}`
  dialog.model = { ...row }
  dialog.visible = true
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (dialog.mode === 'create') {
      await createDept(dialog.model)
      ElMessage.success('新增成功')
    } else {
      await updateDept(dialog.model)
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
  await ElMessageBox.confirm(`确认删除部门「${row.deptName}」？`, '提示', { type: 'warning' })
  await removeDept(row.deptId)
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
      <el-button v-hasPerm="'system:dept:add'" type="primary" @click="openCreate()">新增部门</el-button>
      <el-button @click="fetchList">刷新</el-button>
    </div>

    <el-table v-loading="loading" :data="list" row-key="deptId" border default-expand-all
      :tree-props="{ children: 'children' }">
      <el-table-column prop="deptName" label="部门名称" min-width="220" />
      <el-table-column prop="orderNum" label="排序" width="80" align="center" />
      <el-table-column prop="leader" label="负责人" width="120" />
      <el-table-column prop="phone" label="联系电话" width="140" />
      <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === 0 ? 'success' : 'info'">
            {{ row.status === 0 ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="190" fixed="right" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="openCreate(row)">新增</el-button>
          <el-button v-hasPerm="'system:dept:edit'" link type="primary" @click="openEdit(row)">修改</el-button>
          <el-button v-hasPerm="'system:dept:remove'" link type="danger" @click="handleRemove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="dialog.visible" :title="dialog.title" width="600px" destroy-on-close>
    <el-form ref="formRef" :model="dialog.model" :rules="rules" label-width="96px">
      <el-row :gutter="16">
        <el-col :span="24">
          <el-form-item label="上级部门" prop="parentId">
            <el-tree-select v-model="dialog.model.parentId" :data="parentOptions" node-key="deptId"
              :props="{ label: 'deptName', children: 'children' }" check-strictly default-expand-all
              style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="部门名称" prop="deptName">
            <el-input v-model="dialog.model.deptName" placeholder="如：研发部" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="显示排序">
            <el-input-number v-model="dialog.model.orderNum" :min="0" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="负责人">
            <el-input v-model="dialog.model.leader" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="联系电话">
            <el-input v-model="dialog.model.phone" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="邮箱">
            <el-input v-model="dialog.model.email" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="状态">
            <el-select v-model="dialog.model.status" style="width: 100%">
              <el-option label="正常" :value="0" />
              <el-option label="停用" :value="1" />
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
