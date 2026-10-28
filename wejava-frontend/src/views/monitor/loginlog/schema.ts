import type { CrudSchema, Option } from '@/types/crud'
import { listLoginLog, removeLoginLog } from '@/api/loginlog'

const STATUS: Option[] = [
  { label: '成功', value: 0 },
  { label: '失败', value: 1 }
]

export const loginLogSchema: CrudSchema = {
  key: '登录日志',
  rowKey: 'infoId',
  permissionPrefix: 'monitor:loginlog',
  readonly: true,
  pagination: { pageSize: 10 },

  api: {
    list: listLoginLog,
    remove: removeLoginLog
  },

  search: [
    { prop: 'username', label: '用户名', type: 'input' },
    { prop: 'ipaddr', label: 'IP', type: 'input' },
    { prop: 'status', label: '状态', type: 'select', options: STATUS }
  ],

  columns: [
    { prop: 'infoId', label: '日志ID', width: 180 },
    { prop: 'username', label: '用户名', width: 130 },
    { prop: 'ipaddr', label: 'IP 地址', width: 140 },
    { prop: 'loginLocation', label: '登录地点', width: 140 },
    { prop: 'browser', label: '浏览器', width: 120 },
    { prop: 'os', label: '操作系统', width: 120 },
    { prop: 'status', label: '状态', width: 90, dict: STATUS, align: 'center' },
    { prop: 'msg', label: '描述', minWidth: 160, showOverflowTooltip: true },
    { prop: 'loginTime', label: '登录时间', width: 170, dateFormat: 'YYYY-MM-DD HH:mm:ss' }
  ]
}
