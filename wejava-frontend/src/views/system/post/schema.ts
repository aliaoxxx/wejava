import type { CrudSchema, Option } from '@/types/crud'
import { createPost, getPost, listPost, removePost, updatePost } from '@/api/post'

const STATUS: Option[] = [
  { label: '正常', value: 0 },
  { label: '停用', value: 1 }
]

export const postSchema: CrudSchema = {
  key: '岗位',
  rowKey: 'postId',
  permissionPrefix: 'system:post',
  pagination: { pageSize: 10 },

  api: {
    list: listPost,
    detail: getPost,
    create: createPost,
    update: updatePost,
    remove: removePost
  },

  search: [
    { prop: 'postCode', label: '岗位编码', type: 'input' },
    { prop: 'postName', label: '岗位名称', type: 'input' },
    { prop: 'status', label: '状态', type: 'select', options: STATUS }
  ],

  columns: [
    { prop: 'postId', label: '岗位ID', width: 180 },
    { prop: 'postCode', label: '岗位编码', minWidth: 120 },
    { prop: 'postName', label: '岗位名称', minWidth: 140 },
    { prop: 'postSort', label: '排序', width: 80, align: 'center' },
    { prop: 'status', label: '状态', width: 90, dict: STATUS, align: 'center' },
    { prop: 'remark', label: '备注', minWidth: 160, showOverflowTooltip: true },
    { prop: 'createTime', label: '创建时间', width: 170, dateFormat: 'YYYY-MM-DD HH:mm:ss' }
  ],

  form: [
    { prop: 'postCode', label: '岗位编码', type: 'input', required: true, placeholder: '字母、数字、下划线', span: 12 },
    { prop: 'postName', label: '岗位名称', type: 'input', required: true, span: 12 },
    { prop: 'postSort', label: '显示排序', type: 'number', defaultValue: 0, span: 12 },
    { prop: 'status', label: '状态', type: 'select', options: STATUS, defaultValue: 0, span: 12 },
    { prop: 'remark', label: '备注', type: 'textarea' }
  ]
}
