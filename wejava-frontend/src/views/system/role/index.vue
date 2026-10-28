<script setup lang="ts">
import { ElMessage } from 'element-plus'
import CrudPage from '@/components/Crud/CrudPage.vue'
import { useCrud } from '@/hooks/useCrud'
import { getRole, getRoleDeptIds, updateRole, updateRoleDataScope } from '@/api/role'
import { getRoleMenuTree } from '@/api/menu'
import { deptTreeSelect } from '@/api/dept'
import { DATA_SCOPES, roleSchema } from './schema'

const crud = useCrud(roleSchema)

const menuDialog = reactive({
  visible: false,
  roleId: '' as string,
  roleName: '',
  tree: [] as any[],
  loading: false
})
const treeRef = ref()
const saving = ref(false)

// ---------- 数据权限 ----------
const scopeDialog = reactive({
  visible: false,
  roleId: '',
  roleName: '',
  dataScope: '1',
  deptIds: [] as string[],
  deptTree: [] as any[],
  loading: false
})
const deptTreeRef = ref()

async function openScopeDialog(row: any) {
  scopeDialog.visible = true
  scopeDialog.roleId = row.roleId
  scopeDialog.roleName = row.roleName
  scopeDialog.dataScope = row.dataScope || '1'
  scopeDialog.loading = true
  try {
    const tree = await deptTreeSelect()
    scopeDialog.deptTree = tree.data ?? []
    if (scopeDialog.dataScope === '2') {
      const ids = await getRoleDeptIds(row.roleId)
      scopeDialog.deptIds = (ids.data ?? []).map((id: any) => String(id))
      await nextTick()
      deptTreeRef.value?.setCheckedKeys(scopeDialog.deptIds)
    } else {
      scopeDialog.deptIds = []
    }
  } finally {
    scopeDialog.loading = false
  }
}

async function saveScope() {
  if (scopeDialog.dataScope === '2') {
    const checked = deptTreeRef.value?.getCheckedKeys() ?? []
    if (!checked.length) {
      ElMessage.warning('自定义数据权限至少要选一个部门')
      return
    }
    scopeDialog.deptIds = checked.map((id: any) => String(id))
  }
  saving.value = true
  try {
    await updateRoleDataScope({
      roleId: scopeDialog.roleId,
      dataScope: scopeDialog.dataScope,
      deptIds: scopeDialog.dataScope === '2' ? scopeDialog.deptIds : []
    })
    ElMessage.success('数据权限已保存，立即生效')
    scopeDialog.visible = false
    await crud.fetchList()
  } finally {
    saving.value = false
  }
}

async function openMenuDialog(row: any) {
  menuDialog.loading = true
  menuDialog.visible = true
  menuDialog.roleId = row.roleId
  menuDialog.roleName = row.roleName
  try {
    const res = await getRoleMenuTree(row.roleId)
    menuDialog.tree = res.data?.menus ?? []
    const checked = (res.data?.checkedKeys ?? []).map((id: any) => String(id))
    await nextTick()
    // 只设置叶子节点，父节点由 el-tree 自动推导半选状态
    const leafIds = collectLeafIds(menuDialog.tree)
    treeRef.value?.setCheckedKeys(checked.filter((id: string) => leafIds.has(id)))
  } finally {
    menuDialog.loading = false
  }
}

function collectLeafIds(nodes: any[], result = new Set<string>()): Set<string> {
  nodes.forEach((node) => {
    if (node.children?.length) {
      collectLeafIds(node.children, result)
    } else {
      result.add(String(node.menuId))
    }
  })
  return result
}

async function saveMenus() {
  saving.value = true
  try {
    const checked = treeRef.value?.getCheckedKeys() ?? []
    const halfChecked = treeRef.value?.getHalfCheckedKeys() ?? []
    const menuIds = [...checked, ...halfChecked].map((id: any) => String(id))

    const detail = await getRole(menuDialog.roleId)
    await updateRole({ ...detail.data, menuIds })

    ElMessage.success('菜单权限已保存，相关用户需重新登录生效')
    menuDialog.visible = false
    await crud.fetchList()
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <CrudPage :crud="crud">
    <template #row-actions="{ row }">
      <el-button
        v-hasPerm="'system:role:edit'"
        link
        type="primary"
        @click="openMenuDialog(row)"
      >
        分配菜单
      </el-button>
      <el-button
        v-hasPerm="'system:role:dataScope'"
        link
        type="primary"
        @click="openScopeDialog(row)"
      >
        数据权限
      </el-button>
    </template>
  </CrudPage>

  <el-dialog v-model="scopeDialog.visible" :title="`数据权限 - ${scopeDialog.roleName}`" width="520px">
    <el-form label-width="90px">
      <el-form-item label="数据范围">
        <el-radio-group v-model="scopeDialog.dataScope">
          <el-radio v-for="item in DATA_SCOPES" :key="item.value" :value="item.value">
            {{ item.label }}
          </el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="scopeDialog.dataScope === '2'" label="指定部门">
        <div v-loading="scopeDialog.loading" class="dept-tree">
          <el-tree
            ref="deptTreeRef"
            :data="scopeDialog.deptTree"
            show-checkbox
            node-key="deptId"
            default-expand-all
            :props="{ label: 'deptName', children: 'children' }"
          />
        </div>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="scopeDialog.visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="saveScope">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="menuDialog.visible" :title="`分配菜单 - ${menuDialog.roleName}`" width="480px">
    <el-tree
      ref="treeRef"
      v-loading="menuDialog.loading"
      :data="menuDialog.tree"
      show-checkbox
      node-key="menuId"
      default-expand-all
      :props="{ label: 'menuName', children: 'children' }"
      style="max-height: 420px; overflow: auto"
    />
    <template #footer>
      <el-button @click="menuDialog.visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="saveMenus">保存</el-button>
    </template>
  </el-dialog>
</template>
