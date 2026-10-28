<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  createDictData,
  createDictType,
  listDictData,
  listDictType,
  removeDictData,
  removeDictType,
  updateDictData,
  updateDictType
} from '@/api/dict'

const types = ref<any[]>([])
const typeLoading = ref(false)
const selected = ref<any>(null)

const dataList = ref<any[]>([])
const dataLoading = ref(false)
const dataTotal = ref(0)
const dataQuery = reactive({ pageNum: 1, pageSize: 10, dictLabel: '' })

const typeFormRef = ref<FormInstance>()
const dataFormRef = ref<FormInstance>()

const typeDialog = reactive({ visible: false, mode: 'create', model: {} as Record<string, any> })
const dataDialog = reactive({ visible: false, mode: 'create', model: {} as Record<string, any> })

const typeRules: FormRules = {
  dictName: [{ required: true, message: '字典名称不能为空', trigger: 'blur' }],
  dictType: [{ required: true, message: '字典类型不能为空', trigger: 'blur' }]
}
const dataRules: FormRules = {
  dictLabel: [{ required: true, message: '字典标签不能为空', trigger: 'blur' }],
  dictValue: [{ required: true, message: '字典值不能为空', trigger: 'blur' }]
}

async function fetchTypes() {
  typeLoading.value = true
  try {
    const res = await listDictType({ pageNum: 1, pageSize: 100 })
    types.value = res.data?.list ?? []
    if (!selected.value && types.value.length) {
      selectType(types.value[0])
    }
  } finally {
    typeLoading.value = false
  }
}

function selectType(row: any) {
  selected.value = row
  dataQuery.pageNum = 1
  fetchData()
}

async function fetchData() {
  if (!selected.value) {
    dataList.value = []
    dataTotal.value = 0
    return
  }
  dataLoading.value = true
  try {
    const res = await listDictData({
      pageNum: dataQuery.pageNum,
      pageSize: dataQuery.pageSize,
      dictLabel: dataQuery.dictLabel,
      dictType: selected.value.dictType
    })
    dataList.value = res.data?.list ?? []
    dataTotal.value = res.data?.total ?? 0
  } finally {
    dataLoading.value = false
  }
}

// ---------- 字典类型 ----------
function openTypeCreate() {
  typeDialog.mode = 'create'
  typeDialog.model = { status: 0 }
  typeDialog.visible = true
}
function openTypeEdit(row: any) {
  typeDialog.mode = 'edit'
  typeDialog.model = { ...row }
  typeDialog.visible = true
}
async function submitType() {
  const valid = await typeFormRef.value?.validate().catch(() => false)
  if (!valid) return
  if (typeDialog.mode === 'create') {
    await createDictType(typeDialog.model)
  } else {
    await updateDictType(typeDialog.model)
  }
  ElMessage.success('保存成功')
  typeDialog.visible = false
  await fetchTypes()
  await fetchData()
}
async function removeType(row: any) {
  await ElMessageBox.confirm(`确认删除字典「${row.dictName}」？`, '提示', { type: 'warning' })
  await removeDictType(row.dictId)
  ElMessage.success('删除成功')
  if (selected.value?.dictId === row.dictId) selected.value = null
  await fetchTypes()
}

// ---------- 字典数据 ----------
function openDataCreate() {
  if (!selected.value) return ElMessage.warning('请先选择左侧字典类型')
  dataDialog.mode = 'create'
  dataDialog.model = { dictType: selected.value.dictType, dictSort: 1, status: 0, isDefault: 0 }
  dataDialog.visible = true
}
function openDataEdit(row: any) {
  dataDialog.mode = 'edit'
  dataDialog.model = { ...row }
  dataDialog.visible = true
}
async function submitData() {
  const valid = await dataFormRef.value?.validate().catch(() => false)
  if (!valid) return
  if (dataDialog.mode === 'create') {
    await createDictData(dataDialog.model)
  } else {
    await updateDictData(dataDialog.model)
  }
  ElMessage.success('保存成功')
  dataDialog.visible = false
  await fetchData()
}
async function removeData(row: any) {
  await ElMessageBox.confirm(`确认删除「${row.dictLabel}」？`, '提示', { type: 'warning' })
  await removeDictData(row.dictCode)
  ElMessage.success('删除成功')
  await fetchData()
}

onMounted(fetchTypes)
</script>

<template>
  <el-row :gutter="16">
    <el-col :span="8">
      <el-card shadow="never">
        <template #header>
          <div class="card-head">
            <span>字典类型</span>
            <el-button v-hasPerm="'system:dict:add'" size="small" type="primary" @click="openTypeCreate">
              新增
            </el-button>
          </div>
        </template>
        <el-table
          v-loading="typeLoading"
          :data="types"
          size="small"
          highlight-current-row
          @current-change="selectType"
        >
          <el-table-column prop="dictName" label="字典名称" min-width="100" />
          <el-table-column prop="dictType" label="类型" min-width="120" show-overflow-tooltip />
          <el-table-column label="操作" width="100" align="center">
            <template #default="{ row }">
              <el-button v-hasPerm="'system:dict:edit'" link type="primary" @click.stop="openTypeEdit(row)">
                改
              </el-button>
              <el-button v-hasPerm="'system:dict:remove'" link type="danger" @click.stop="removeType(row)">
                删
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </el-col>

    <el-col :span="16">
      <el-card shadow="never">
        <template #header>
          <div class="card-head">
            <span>字典数据{{ selected ? ` - ${selected.dictName}` : '' }}</span>
            <div class="head-actions">
              <el-input v-model="dataQuery.dictLabel" placeholder="标签搜索" size="small" style="width: 140px"
                clearable @keyup.enter="fetchData" />
              <el-button size="small" @click="fetchData">查询</el-button>
              <el-button v-hasPerm="'system:dict:add'" size="small" type="primary" @click="openDataCreate">
                新增
              </el-button>
            </div>
          </div>
        </template>

        <el-table v-loading="dataLoading" :data="dataList" size="small" border>
          <el-table-column prop="dictLabel" label="标签" min-width="120" />
          <el-table-column prop="dictValue" label="值" min-width="100" />
          <el-table-column prop="dictSort" label="排序" width="70" align="center" />
          <el-table-column prop="listClass" label="回显样式" width="110" />
          <el-table-column label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.status === 0 ? 'success' : 'info'">
                {{ row.status === 0 ? '正常' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" align="center">
            <template #default="{ row }">
              <el-button v-hasPerm="'system:dict:edit'" link type="primary" @click="openDataEdit(row)">修改</el-button>
              <el-button v-hasPerm="'system:dict:remove'" link type="danger" @click="removeData(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination class="pager" background layout="total, prev, pager, next" :total="dataTotal"
          :current-page="dataQuery.pageNum" :page-size="dataQuery.pageSize"
          @current-change="(p: number) => { dataQuery.pageNum = p; fetchData() }" />
      </el-card>
    </el-col>
  </el-row>

  <el-dialog v-model="typeDialog.visible" :title="typeDialog.mode === 'create' ? '新增字典类型' : '修改字典类型'"
    width="520px" destroy-on-close>
    <el-form ref="typeFormRef" :model="typeDialog.model" :rules="typeRules" label-width="90px">
      <el-form-item label="字典名称" prop="dictName">
        <el-input v-model="typeDialog.model.dictName" placeholder="如：用户性别" />
      </el-form-item>
      <el-form-item label="字典类型" prop="dictType">
        <el-input v-model="typeDialog.model.dictType" placeholder="如：sys_user_sex" :disabled="typeDialog.mode === 'edit'" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="typeDialog.model.status" style="width: 100%">
          <el-option label="正常" :value="0" />
          <el-option label="停用" :value="1" />
        </el-select>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="typeDialog.model.remark" type="textarea" :rows="2" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="typeDialog.visible = false">取消</el-button>
      <el-button type="primary" @click="submitType">确定</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="dataDialog.visible" :title="dataDialog.mode === 'create' ? '新增字典数据' : '修改字典数据'"
    width="520px" destroy-on-close>
    <el-form ref="dataFormRef" :model="dataDialog.model" :rules="dataRules" label-width="90px">
      <el-form-item label="字典类型">
        <el-input v-model="dataDialog.model.dictType" disabled />
      </el-form-item>
      <el-form-item label="数据标签" prop="dictLabel">
        <el-input v-model="dataDialog.model.dictLabel" />
      </el-form-item>
      <el-form-item label="数据值" prop="dictValue">
        <el-input v-model="dataDialog.model.dictValue" />
      </el-form-item>
      <el-form-item label="排序">
        <el-input-number v-model="dataDialog.model.dictSort" :min="0" style="width: 100%" />
      </el-form-item>
      <el-form-item label="回显样式">
        <el-input v-model="dataDialog.model.listClass" placeholder="primary / success / info / danger" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="dataDialog.model.status" style="width: 100%">
          <el-option label="正常" :value="0" />
          <el-option label="停用" :value="1" />
        </el-select>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="dataDialog.model.remark" type="textarea" :rows="2" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dataDialog.visible = false">取消</el-button>
      <el-button type="primary" @click="submitData">确定</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.head-actions {
  display: flex;
  gap: 6px;
  align-items: center;
}
.pager {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>
