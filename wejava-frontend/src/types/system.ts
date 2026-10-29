/** 系统模块的实体类型，供页面与 api 复用 */

export interface DictTypeItem extends Record<string, unknown> {
  dictId: string
  dictName: string
  dictType: string
  status: number
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface DictDataItem extends Record<string, unknown> {
  dictDataId: string
  dictType: string
  dictLabel: string
  dictValue: string
  dictSort: number
  listClass?: string
  status: number
  remark?: string
  createTime?: string
}

export interface RoleItem extends Record<string, unknown> {
  roleId: string
  roleName: string
  roleKey: string
  roleSort: number
  dataScope: number
  status: number
  remark?: string
  menuIds?: string[]
  deptIds?: string[]
  createTime?: string
}

/** 下拉/树节点（部门、菜单、角色选择器共用） */
export interface TreeNode {
  id: string
  label: string
  children?: TreeNode[]
}

export interface MenuItem {
  menuId: string
  parentId: string
  menuName: string
  menuType: string
  path?: string
  component?: string
  perms?: string
  icon?: string
  orderNum: number
  visible: number
  status: number
  children?: MenuItem[]
}
