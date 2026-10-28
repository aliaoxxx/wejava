<script setup lang="ts">
import { ElMessage } from 'element-plus'
import CrudPage from '@/components/Crud/CrudPage.vue'
import { useCrud } from '@/hooks/useCrud'
import { getRole, updateRole } from '@/api/role'
import { getRoleMenuTree } from '@/api/menu'
import { roleSchema } from './schema'

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
    </template>
  </CrudPage>

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
