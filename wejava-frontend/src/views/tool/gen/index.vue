<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  downloadGenCode,
  getGenTable,
  importTables,
  listDbTables,
  listGenTable,
  previewGenCode,
  removeGenTable,
  updateGenTable,
  type DbTableItem,
  type GenColumnItem,
  type GenTableDetail,
  type GenTableItem
} from '@/api/gen'

const loading = ref(false)
const rows = ref<GenTableItem[]>([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, tableName: '', functionName: '' })

async function fetchList() {
  loading.value = true
  try {
    const res = await listGenTable(query)
    rows.value = res.data?.list ?? []
    total.value = res.data?.total ?? 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  fetchList()
}

function handleReset() {
  query.tableName = ''
  query.functionName = ''
  handleSearch()
}

/* ---------------- 导入表 ---------------- */
const importVisible = ref(false)
const importLoading = ref(false)
const dbTables = ref<DbTableItem[]>([])
const selectedTables = ref<string[]>([])

async function openImport() {
  importVisible.value = true
  selectedTables.value = []
  importLoading.value = true
  try {
    const res = await listDbTables()
    dbTables.value = res.data ?? []
  } finally {
    importLoading.value = false
  }
}

async function submitImport() {
  if (!selectedTables.value.length) {
    ElMessage.warning('请至少选择一张表')
    return
  }
  const count = (await importTables(selectedTables.value)).data ?? 0
  ElMessage.success(`已导入 ${count} 张表`)
  importVisible.value = false
  await fetchList()
}

/* ---------------- 编辑配置 ---------------- */
const editVisible = ref(false)
const editLoading = ref(false)
const saving = ref(false)
const editForm = reactive<GenTableDetail>({
  tableId: '',
  tableName: '',
  tableComment: '',
  className: '',
  moduleName: '',
  businessName: '',
  functionName: '',
  packageName: '',
  author: '',
  parentMenuId: undefined,
  columns: []
})

async function openEdit(row: GenTableItem) {
  editVisible.value = true
  editLoading.value = true
  try {
    const res = await getGenTable(row.tableId)
    Object.assign(editForm, res.data)
  } finally {
    editLoading.value = false
  }
}

async function submitEdit() {
  saving.value = true
  try {
    await updateGenTable(editForm.tableId, {
      tableName: editForm.tableName,
      tableComment: editForm.tableComment,
      className: editForm.className,
      moduleName: editForm.moduleName,
      businessName: editForm.businessName,
      functionName: editForm.functionName,
      packageName: editForm.packageName,
      author: editForm.author,
      parentMenuId: editForm.parentMenuId || null,
      columns: editForm.columns.map((col: GenColumnItem) => ({
        columnName: col.columnName,
        columnComment: col.columnComment,
        javaType: col.javaType,
        javaField: col.javaField,
        isRequired: col.isRequired,
        isInsert: col.isInsert,
        isEdit: col.isEdit,
        isList: col.isList,
        isQuery: col.isQuery,
        queryType: col.queryType,
        htmlType: col.htmlType,
        dictType: col.dictType,
        sort: col.sort
      }))
    })
    ElMessage.success('配置已保存')
    editVisible.value = false
    await fetchList()
  } finally {
    saving.value = false
  }
}

/* ---------------- 预览 ---------------- */
const previewVisible = ref(false)
const previewLoading = ref(false)
const previewFiles = ref<{ path: string; content: string }[]>([])
const activeFile = ref('')

async function openPreview(row: GenTableItem) {
  previewVisible.value = true
  previewLoading.value = true
  previewFiles.value = []
  try {
    const res = await previewGenCode(row.tableId)
    const data = res.data ?? {}
    previewFiles.value = Object.entries(data)
      .map(([path, content]) => ({ path, content }))
      .sort((a, b) => a.path.localeCompare(b.path))
    activeFile.value = previewFiles.value[0]?.path ?? ''
  } finally {
    previewLoading.value = false
  }
}

const activeContent = computed(
  () => previewFiles.value.find((item) => item.path === activeFile.value)?.content ?? ''
)

/* ---------------- 下载 / 删除 ---------------- */
async function handleDownload(row: GenTableItem) {
  try {
    await downloadGenCode(row.tableId, `${row.businessName || row.tableName}.zip`)
  } catch (e) {
    ElMessage.error((e as Error).message)
  }
}

async function handleRemove(row: GenTableItem) {
  await ElMessageBox.confirm(
    `删除「${row.functionName}」的生成配置？业务表本身不会被删除。`,
    '删除配置',
    { type: 'warning' }
  )
  await removeGenTable(row.tableId)
  ElMessage.success('已删除配置')
  await fetchList()
}

onMounted(fetchList)
</script>

<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>代码生成</span>
          <el-button v-hasPerm="'system:gen:import'" type="primary" @click="openImport">
            导入表
          </el-button>
        </div>
      </template>

      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="导入数据库表后会自动推断字段类型、必填与查询条件；生成的是普通代码，可直接修改。产物以 zip 下载，不会写入服务器文件。"
        class="hint"
      />

      <el-form :inline="true" class="search-form" @submit.prevent>
        <el-form-item label="表名">
          <el-input v-model="query.tableName" placeholder="请输入" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="功能名">
          <el-input v-model="query.functionName" placeholder="请输入" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="rows" border stripe>
        <el-table-column prop="tableName" label="表名" min-width="180" show-overflow-tooltip />
        <el-table-column prop="tableComment" label="表描述" min-width="160" show-overflow-tooltip />
        <el-table-column prop="className" label="实体类" min-width="140" />
        <el-table-column prop="functionName" label="功能名" min-width="120" />
        <el-table-column prop="moduleName" label="模块" width="100" />
        <el-table-column prop="createTime" label="导入时间" width="180" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openPreview(row as GenTableItem)">预览</el-button>
            <el-button
              v-hasPerm="'system:gen:edit'"
              link
              type="primary"
              @click="openEdit(row as GenTableItem)"
            >
              编辑
            </el-button>
            <el-button
              v-hasPerm="'system:gen:code'"
              link
              type="primary"
              @click="handleDownload(row as GenTableItem)"
            >
              下载
            </el-button>
            <el-button
              v-hasPerm="'system:gen:remove'"
              link
              type="danger"
              @click="handleRemove(row as GenTableItem)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        class="pager"
        @current-change="fetchList"
        @size-change="fetchList"
      />
    </el-card>

    <!-- 导入 -->
    <el-dialog v-model="importVisible" title="导入数据库表" width="720px" destroy-on-close>
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        title="已导入的表会显示为灰色。这里只读取表结构，不会修改任何业务表。"
        class="hint"
      />
      <el-table
        v-loading="importLoading"
        :data="dbTables"
        border
        max-height="420"
        @selection-change="(rows: DbTableItem[]) => (selectedTables = rows.map((r) => r.tableName))"
      >
        <el-table-column type="selection" width="50" :selectable="(row: DbTableItem) => !row.imported" />
        <el-table-column prop="tableName" label="表名" min-width="200" />
        <el-table-column prop="tableComment" label="表描述" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.imported ? 'info' : 'success'" size="small">
              {{ row.imported ? '已导入' : '可导入' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button type="primary" :loading="importLoading" @click="submitImport">
          导入选中（{{ selectedTables.length }}）
        </el-button>
      </template>
    </el-dialog>

    <!-- 编辑配置 -->
    <el-dialog v-model="editVisible" title="生成配置" width="1000px" top="6vh" destroy-on-close>
      <div v-loading="editLoading">
        <el-form :model="editForm" label-width="110px" class="edit-form">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="表名">
                <el-input :model-value="editForm.tableName" disabled />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="表描述">
                <el-input v-model="editForm.tableComment" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="实体类名">
                <el-input v-model="editForm.className" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="功能名">
                <el-input v-model="editForm.functionName" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="模块名">
                <el-input v-model="editForm.moduleName" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="业务名">
                <el-input v-model="editForm.businessName" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="包名">
                <el-input v-model="editForm.packageName" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="父菜单ID">
                <el-input v-model="editForm.parentMenuId" placeholder="留空则挂到顶级菜单" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>

        <el-table :data="editForm.columns" border size="small" max-height="380">
          <el-table-column prop="columnName" label="列名" width="150" fixed />
          <el-table-column prop="columnType" label="类型" width="120" />
          <el-table-column label="描述" width="150">
            <template #default="{ row }">
              <el-input v-model="row.columnComment" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="Java 类型" width="130">
            <template #default="{ row }">
              <el-select v-model="row.javaType" size="small">
                <el-option v-for="t in ['String', 'Long', 'Integer', 'Boolean', 'BigDecimal', 'Double', 'LocalDateTime', 'LocalDate']" :key="t" :label="t" :value="t" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="必填" width="60" align="center">
            <template #default="{ row }">
              <el-checkbox v-model="row.isRequired" :disabled="row.isPk" />
            </template>
          </el-table-column>
          <el-table-column label="列表" width="60" align="center">
            <template #default="{ row }"><el-checkbox v-model="row.isList" /></template>
          </el-table-column>
          <el-table-column label="查询" width="60" align="center">
            <template #default="{ row }"><el-checkbox v-model="row.isQuery" /></template>
          </el-table-column>
          <el-table-column label="表单" width="60" align="center">
            <template #default="{ row }">
              <el-checkbox v-model="row.isInsert" :disabled="row.isPk" />
            </template>
          </el-table-column>
          <el-table-column label="查询方式" width="110">
            <template #default="{ row }">
              <el-select v-model="row.queryType" size="small" :disabled="!row.isQuery">
                <el-option label="等值" value="EQ" />
                <el-option label="模糊" value="LIKE" />
                <el-option label="区间" value="BETWEEN" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="控件" width="120">
            <template #default="{ row }">
              <el-select v-model="row.htmlType" size="small">
                <el-option v-for="t in ['input', 'textarea', 'number', 'select', 'radio', 'switch', 'date', 'datetime', 'tree-select']" :key="t" :label="t" :value="t" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="字典" min-width="140">
            <template #default="{ row }">
              <el-input v-model="row.dictType" size="small" placeholder="如 sys_normal_disable" />
            </template>
          </el-table-column>
        </el-table>
      </div>

      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 预览 -->
    <el-dialog v-model="previewVisible" title="代码预览" width="1080px" top="5vh" destroy-on-close>
      <div v-loading="previewLoading" class="preview">
        <div class="file-list">
          <div
            v-for="file in previewFiles"
            :key="file.path"
            class="file-item"
            :class="{ active: file.path === activeFile }"
            @click="activeFile = file.path"
          >
            {{ file.path }}
          </div>
        </div>
        <pre class="code"><code>{{ activeContent }}</code></pre>
      </div>
      <template #footer>
        <el-button @click="previewVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.hint {
  margin-bottom: 12px;
}
.search-form {
  margin-bottom: 4px;
}
.pager {
  margin-top: 14px;
  justify-content: flex-end;
}
.edit-form {
  padding-top: 4px;
}
.preview {
  display: flex;
  gap: 12px;
  height: 62vh;
}
.file-list {
  width: 300px;
  overflow: auto;
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
  padding: 6px;
}
.file-item {
  font-size: 12px;
  line-height: 1.6;
  padding: 6px 8px;
  border-radius: 4px;
  cursor: pointer;
  word-break: break-all;
  color: var(--el-text-color-regular);
}
.file-item:hover {
  background: var(--el-fill-color-light);
}
.file-item.active {
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}
.code {
  flex: 1;
  margin: 0;
  overflow: auto;
  background: #0f172a;
  color: #d7e0f4;
  border-radius: 6px;
  padding: 14px 16px;
  font-size: 12.5px;
  line-height: 1.65;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
}
</style>
