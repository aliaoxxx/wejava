import type { VNode } from 'vue'
import type { FormItemRule } from 'element-plus'

/** 一行表格数据。字段由各业务模块自行约定，这里只约束"是对象" */
export type CrudRow = Record<string, unknown>

export type FieldType =
  | 'input'
  | 'textarea'
  | 'password'
  | 'number'
  | 'select'
  | 'radio'
  | 'switch'
  | 'date'
  | 'datetime'
  | 'daterange'
  | 'tree-select'

export interface Option {
  label: string
  value: string | number
}

export type CrudMode = 'create' | 'edit'

/** 后端统一响应体 */
export interface ApiResult<T = unknown> {
  code: number
  msg: string
  data: T
  traceId?: string
}

/** 分页响应体 */
export interface PageData<T = CrudRow> {
  total: number
  list: T[]
}

export interface SearchField {
  prop: string
  label: string
  type: 'input' | 'select' | 'date' | 'daterange' | 'number'
  placeholder?: string
  options?: Option[]
  /** 字典类型，优先级高于 options */
  dict?: Option[] | string
  /** daterange 拆成的两个后端参数名，如 ['beginTime', 'endTime'] */
  rangeKeys?: [string, string]
  span?: number
}

export interface Column<T = CrudRow> {
  prop: string
  label: string
  width?: number | string
  minWidth?: number | string
  align?: 'left' | 'center' | 'right'
  fixed?: 'left' | 'right'
  sortable?: boolean
  /** 选项数组，或字典类型字符串（运行时从 /system/dict/data/type/{type} 加载） */
  dict?: Option[] | string
  /** 日期格式化，如 YYYY-MM-DD HH:mm:ss */
  dateFormat?: string
  /** 完全自定义渲染 */
  render?: (row: T, index: number) => VNode | string
  showOverflowTooltip?: boolean
}

export interface FormField {
  prop: string
  label: string
  type: FieldType
  required?: boolean
  rules?: FormItemRule[]
  options?: Option[]
  /** 字典类型，优先级高于 options */
  dict?: Option[] | string
  /** 异步加载选项（如角色列表） */
  /** 动态加载选项；入参为当前表单模型，便于按其他字段联动 */
  loadOptions?: (model?: CrudRow) => Promise<Option[]>
  /** 依赖的字段名，变化时重新加载选项并清空已选 */
  dependsOn?: string
  /** select 是否多选 */
  multiple?: boolean
  placeholder?: string
  span?: number
  only?: CrudMode
  visible?: (model: CrudRow) => boolean
  disabled?: (model: CrudRow, mode: CrudMode) => boolean
  tip?: string
  defaultValue?: unknown
}

export interface CrudApi {
  list: (params: CrudRow) => Promise<ApiResult<PageData>>
  detail?: (id: string | number) => Promise<ApiResult<CrudRow>>
  create?: (data: CrudRow) => Promise<ApiResult<unknown>>
  update?: (data: CrudRow) => Promise<ApiResult<unknown>>
  remove?: (ids: string) => Promise<ApiResult<unknown>>
}

export interface CrudSchema<T = CrudRow> {
  /** 唯一标识 */
  key: string
  api: CrudApi
  rowKey: string
  /** 权限前缀，如 system:user → 自动拼 :add */
  permissionPrefix?: string
  /** 只读页面：不显示新增和修改，仅保留删除 */
  readonly?: boolean
  search?: SearchField[]
  columns: Column<T>[]
  form?: FormField[]
  dialog?: { width?: string; labelWidth?: string }
  pagination?: { pageSize?: number }
  /** 提交前改写数据；返回 false 阻止提交 */
  beforeSubmit?: (model: CrudRow, mode: CrudMode) => CrudRow | false | Promise<CrudRow | false>
}
