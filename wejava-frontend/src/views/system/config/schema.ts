import type { CrudSchema, Option } from '@/types/crud'
import { createConfig, getConfig, listConfig, removeConfig, updateConfig } from '@/api/config'

const TYPE: Option[] = [
  { label: '自定义', value: 0 },
  { label: '系统内置', value: 1 }
]

export const configSchema: CrudSchema = {
  key: '参数',
  rowKey: 'configId',
  permissionPrefix: 'system:config',
  pagination: { pageSize: 10 },

  api: {
    list: listConfig,
    detail: getConfig,
    create: createConfig,
    update: updateConfig,
    remove: removeConfig
  },

  search: [
    { prop: 'configName', label: '参数名称', type: 'input' },
    { prop: 'configKey', label: '参数键名', type: 'input' },
    { prop: 'configType', label: '类型', type: 'select', options: TYPE }
  ],

  columns: [
    { prop: 'configId', label: '参数ID', width: 180 },
    { prop: 'configName', label: '参数名称', minWidth: 160 },
    { prop: 'configKey', label: '参数键名', minWidth: 180 },
    { prop: 'configValue', label: '参数键值', minWidth: 160, showOverflowTooltip: true },
    { prop: 'configType', label: '类型', width: 110, dict: TYPE, align: 'center' },
    { prop: 'remark', label: '备注', minWidth: 160, showOverflowTooltip: true },
    { prop: 'createTime', label: '创建时间', width: 170, dateFormat: 'YYYY-MM-DD HH:mm:ss' }
  ],

  form: [
    { prop: 'configName', label: '参数名称', type: 'input', required: true, span: 12 },
    { prop: 'configKey', label: '参数键名', type: 'input', required: true, span: 12 },
    { prop: 'configValue', label: '参数键值', type: 'input', required: true, span: 12 },
    { prop: 'configType', label: '类型', type: 'select', options: TYPE, defaultValue: 0, span: 12 },
    { prop: 'remark', label: '备注', type: 'textarea' }
  ]
}
