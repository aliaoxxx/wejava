import type { CrudSchema, Option } from '@/types/crud'
import { deptTreeSelect } from '@/api/dept'
import { listRoleOptions } from '@/api/role'
import { createNotice, getNotice, listNotice, removeNotice, updateNotice } from '@/api/notice'

const NOTICE_TYPE: Option[] = [
  { label: '通知', value: 1 },
  { label: '公告', value: 2 }
]
const TARGETS: Option[] = [
  { label: '全员', value: 1 },
  { label: '按角色', value: 2 },
  { label: '按部门', value: 3 }
]

/** 部门树拍平成带缩进的选项，交给 el-select 显示 */
function flattenDept(nodes: any[], depth = 0): Option[] {
  return (nodes ?? []).flatMap((node) => [
    { label: `${'　'.repeat(depth)}${node.deptName}`, value: node.deptId },
    ...flattenDept(node.children ?? [], depth + 1)
  ])
}

const STATUS: Option[] = [
  { label: '正常', value: 0 },
  { label: '关闭', value: 1 }
]

export const noticeSchema: CrudSchema = {
  key: '公告',
  rowKey: 'noticeId',
  permissionPrefix: 'system:notice',
  pagination: { pageSize: 10 },

  api: {
    list: listNotice,
    detail: getNotice,
    create: createNotice,
    update: updateNotice,
    remove: removeNotice
  },

  search: [
    { prop: 'noticeTitle', label: '标题', type: 'input' },
    { prop: 'noticeType', label: '类型', type: 'select', options: NOTICE_TYPE },
    { prop: 'status', label: '状态', type: 'select', options: STATUS }
  ],

  columns: [
    { prop: 'noticeId', label: '公告ID', width: 180 },
    { prop: 'noticeTitle', label: '标题', minWidth: 200 },
    { prop: 'noticeType', label: '类型', width: 100, dict: 'sys_notice_type', align: 'center' },
    { prop: 'status', label: '状态', width: 90, dict: 'sys_normal_disable', align: 'center' },
    { prop: 'createTime', label: '创建时间', width: 170, dateFormat: 'YYYY-MM-DD HH:mm:ss' }
  ],

  form: [
    { prop: 'noticeTitle', label: '标题', type: 'input', required: true },
    { prop: 'noticeType', label: '类型', type: 'select', dict: 'sys_notice_type', defaultValue: 1, span: 12 },
    { prop: 'status', label: '状态', type: 'select', dict: 'sys_normal_disable', defaultValue: 0, span: 12 },
    { prop: 'targetType', label: '接收范围', type: 'select', options: TARGETS, defaultValue: 1, span: 12 },
    {
      prop: 'targetIds',
      label: '接收对象',
      type: 'select',
      multiple: true,
      span: 12,
      dependsOn: 'targetType',
      visible: (model) => model.targetType === 2 || model.targetType === 3,
      loadOptions: async (model) => {
        if (model?.targetType === 2) {
          const res = await listRoleOptions()
          return (res.data ?? []).map((r: any) => ({ label: r.roleName, value: r.roleId }))
        }
        if (model?.targetType === 3) {
          const res = await deptTreeSelect()
          return flattenDept(res.data ?? [])
        }
        return []
      }
    },
    { prop: 'noticeContent', label: '内容', type: 'textarea', placeholder: '支持纯文本' }
  ]
}
