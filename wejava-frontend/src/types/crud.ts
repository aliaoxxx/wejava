import type { VNode } from 'vue'

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

export interface SearchField {
  prop: string
  label: string
  type: 'input' | 'select' | 'date' | 'daterange' | 'number'
  placeholder?: string
  options?: Option[]
  span?: number
}

export interface Column<T = any> {
  prop: string
  label: string
  width?: number | string
  minWidth?: number | string
  align?: 'left' | 'center' | 'right'
  fixed?: 'left' | 'right'
  sortable?: boolean
  /** 直接给选项，按 el-tag 渲染 */
  dict?: Option[]
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
  rules?: any[]
  options?: Option[]
  /** 异步加载选项（如角色列表） */
  loadOptions?: () => Promise<Option[]>
  /** select 是否多选 */
  multiple?: boolean
  placeholder?: string
  span?: number
  only?: CrudMode
  visible?: (model: any) => boolean
  disabled?: (model: any, mode: CrudMode) => boolean
  tip?: string
  defaultValue?: any
}

export interface CrudApi {
  list: (params: any) => Promise<any>
  detail?: (id: string | number) => Promise<any>
  create?: (data: any) => Promise<any>
  update?: (data: any) => Promise<any>
  remove?: (ids: string) => Promise<any>
}

export interface CrudSchema<T = any> {
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
  beforeSubmit?: (model: any, mode: CrudMode) => any | false | Promise<any | false>
}
