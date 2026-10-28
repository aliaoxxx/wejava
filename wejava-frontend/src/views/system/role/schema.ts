import type { CrudSchema, Option } from '@/types/crud'
import { createRole, getRole, listRole, removeRole, updateRole } from '@/api/role'

const STATUS: Option[] = [
  { label: '正常', value: 0 },
  { label: '停用', value: 1 }
]

export const roleSchema: CrudSchema = {
  key: '角色',
  rowKey: 'roleId',
  permissionPrefix: 'system:role',
  pagination: { pageSize: 10 },

  api: {
    list: listRole,
    detail: getRole,
    create: createRole,
    update: updateRole,
    remove: removeRole
  },

  search: [
    { prop: 'roleName', label: '角色名称', type: 'input' },
    { prop: 'roleKey', label: '角色标识', type: 'input' },
    { prop: 'status', label: '状态', type: 'select', options: STATUS }
  ],

  columns: [
    { prop: 'roleId', label: '角色ID', width: 180 },
    { prop: 'roleName', label: '角色名称', minWidth: 140 },
    { prop: 'roleKey', label: '角色标识', minWidth: 140 },
    { prop: 'roleSort', label: '排序', width: 80, align: 'center' },
    { prop: 'status', label: '状态', width: 90, dict: STATUS, align: 'center' },
    { prop: 'remark', label: '备注', minWidth: 160, showOverflowTooltip: true },
    { prop: 'createTime', label: '创建时间', width: 170, dateFormat: 'YYYY-MM-DD HH:mm:ss' }
  ],

  form: [
    { prop: 'roleName', label: '角色名称', type: 'input', required: true, span: 12 },
    { prop: 'roleKey', label: '角色标识', type: 'input', required: true, span: 12 },
    { prop: 'roleSort', label: '排序', type: 'number', defaultValue: 0, span: 12 },
    { prop: 'status', label: '状态', type: 'select', options: STATUS, defaultValue: 0, span: 12 },
    { prop: 'remark', label: '备注', type: 'textarea' }
  ]
}
