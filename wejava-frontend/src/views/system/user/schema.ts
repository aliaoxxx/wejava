import type { CrudSchema, Option } from '@/types/crud'
import { createUser, getUser, listUser, removeUser, updateUser } from '@/api/user'
import { listRoleOptions } from '@/api/role'
import { deptTreeSelect } from '@/api/dept'

const STATUS: Option[] = [
  { label: '正常', value: 0 },
  { label: '停用', value: 1 }
]

const SEX: Option[] = [
  { label: '男', value: '0' },
  { label: '女', value: '1' },
  { label: '未知', value: '2' }
]

/** 部门树 -> el-tree-select 需要的 { value, label, children } */
function toDeptTree(nodes: any[]): any[] {
  return (nodes ?? []).map((node) => ({
    value: node.deptId,
    label: node.deptName,
    children: toDeptTree(node.children ?? [])
  }))
}

export const userSchema: CrudSchema = {
  key: '用户',
  rowKey: 'userId',
  permissionPrefix: 'system:user',
  pagination: { pageSize: 10 },

  api: {
    list: listUser,
    detail: getUser,
    create: createUser,
    update: updateUser,
    remove: removeUser
  },

  search: [
    { prop: 'username', label: '用户名', type: 'input' },
    { prop: 'nickname', label: '昵称', type: 'input' },
    { prop: 'phone', label: '手机号', type: 'input' },
    { prop: 'status', label: '状态', type: 'select', options: STATUS }
  ],

  columns: [
    { prop: 'userId', label: '用户ID', width: 180 },
    { prop: 'username', label: '用户名', minWidth: 120 },
    { prop: 'nickname', label: '昵称', minWidth: 120 },
    { prop: 'deptName', label: '部门', minWidth: 120 },
    { prop: 'phone', label: '手机号', minWidth: 130 },
    { prop: 'status', label: '状态', width: 90, dict: STATUS, align: 'center' },
    { prop: 'createTime', label: '创建时间', width: 170, dateFormat: 'YYYY-MM-DD HH:mm:ss' }
  ],

  form: [
    {
      prop: 'deptId',
      label: '部门',
      type: 'tree-select',
      required: true,
      defaultValue: 100,
      loadOptions: async () => {
        const res = await deptTreeSelect()
        return toDeptTree(res.data ?? [])
      },
      span: 12
    },
    { prop: 'status', label: '状态', type: 'select', options: STATUS, defaultValue: 0, span: 12 },
    {
      prop: 'username',
      label: '用户名',
      type: 'input',
      required: true,
      placeholder: '字母、数字、下划线，3~30 位',
      disabled: (_model, mode) => mode === 'edit',
      span: 12
    },
    { prop: 'nickname', label: '昵称', type: 'input', required: true, span: 12 },
    {
      prop: 'password',
      label: '密码',
      type: 'password',
      required: true,
      only: 'create',
      placeholder: '6~20 位',
      span: 12
    },
    { prop: 'phone', label: '手机号', type: 'input', span: 12 },
    { prop: 'email', label: '邮箱', type: 'input', span: 12 },
    { prop: 'sex', label: '性别', type: 'select', options: SEX, defaultValue: '2', span: 12 },
    {
      prop: 'roleIds',
      label: '角色',
      type: 'select',
      multiple: true,
      loadOptions: async () => {
        const res = await listRoleOptions()
        return (res.data ?? []).map((item: any) => ({ label: item.roleName, value: item.roleId }))
      },
      span: 12
    },
    { prop: 'remark', label: '备注', type: 'textarea' }
  ],

  beforeSubmit: (model, mode) => {
    if (mode === 'edit') {
      // 用户名和密码不通过此接口修改
      delete model.username
      delete model.password
    }
    return model
  }
}
