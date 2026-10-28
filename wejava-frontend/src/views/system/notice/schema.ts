import type { CrudSchema, Option } from '@/types/crud'
import { createNotice, getNotice, listNotice, removeNotice, updateNotice } from '@/api/notice'

const NOTICE_TYPE: Option[] = [
  { label: '通知', value: 1 },
  { label: '公告', value: 2 }
]
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
    { prop: 'noticeType', label: '类型', width: 100, dict: NOTICE_TYPE, align: 'center' },
    { prop: 'status', label: '状态', width: 90, dict: STATUS, align: 'center' },
    { prop: 'createTime', label: '创建时间', width: 170, dateFormat: 'YYYY-MM-DD HH:mm:ss' }
  ],

  form: [
    { prop: 'noticeTitle', label: '标题', type: 'input', required: true },
    { prop: 'noticeType', label: '类型', type: 'select', options: NOTICE_TYPE, defaultValue: 1, span: 12 },
    { prop: 'status', label: '状态', type: 'select', options: STATUS, defaultValue: 0, span: 12 },
    { prop: 'noticeContent', label: '内容', type: 'textarea', placeholder: '支持纯文本' }
  ]
}
