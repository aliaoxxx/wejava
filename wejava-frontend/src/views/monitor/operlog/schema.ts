import type { CrudSchema, Option } from '@/types/crud'
import { listOperLog, removeOperLog } from '@/api/monitor'

const BUSINESS: Option[] = [
  { label: '其它', value: 0 },
  { label: '新增', value: 1 },
  { label: '修改', value: 2 },
  { label: '删除', value: 3 },
  { label: '导出', value: 4 },
  { label: '导入', value: 5 }
]

const STATUS: Option[] = [
  { label: '成功', value: 0 },
  { label: '失败', value: 1 }
]

export const operLogSchema: CrudSchema = {
  key: '操作日志',
  rowKey: 'operId',
  permissionPrefix: 'monitor:operlog',
  readonly: true,
  pagination: { pageSize: 10 },

  api: {
    list: listOperLog,
    remove: removeOperLog
  },

  search: [
    {
      prop: 'timeRange',
      label: '操作时间',
      type: 'daterange',
      rangeKeys: ['beginTime', 'endTime']
    },
    { prop: 'title', label: '模块', type: 'input' },
    { prop: 'operName', label: '操作人', type: 'input' },
    { prop: 'businessType', label: '类型', type: 'select', options: BUSINESS },
    { prop: 'status', label: '状态', type: 'select', options: STATUS }
  ],

  columns: [
    { prop: 'operId', label: '日志ID', width: 180 },
    { prop: 'title', label: '模块', width: 120 },
    { prop: 'businessType', label: '类型', width: 90, dict: BUSINESS, align: 'center' },
    { prop: 'operName', label: '操作人', width: 110 },
    { prop: 'operIp', label: 'IP', width: 130 },
    { prop: 'requestMethod', label: '方法', width: 80, align: 'center' },
    { prop: 'operUrl', label: '请求地址', minWidth: 180, showOverflowTooltip: true },
    { prop: 'status', label: '状态', width: 90, dict: STATUS, align: 'center' },
    { prop: 'costTime', label: '耗时(ms)', width: 100, align: 'right' },
    { prop: 'operTime', label: '操作时间', width: 170, dateFormat: 'YYYY-MM-DD HH:mm:ss' }
  ]
}
