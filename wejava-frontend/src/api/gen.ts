import { http, type PageData } from '@/utils/http'

export interface GenTableItem {
  tableId: string
  tableName: string
  tableComment?: string
  className: string
  moduleName: string
  businessName: string
  functionName: string
  packageName: string
  author?: string
  parentMenuId?: string
  createTime?: string
}

export interface GenColumnItem {
  columnId: string
  columnName: string
  columnType: string
  columnComment?: string
  javaType: string
  javaField: string
  isPk: boolean
  isIncrement: boolean
  isRequired: boolean
  isInsert: boolean
  isEdit: boolean
  isList: boolean
  isQuery: boolean
  queryType?: string
  htmlType?: string
  dictType?: string
  sort?: number
}

export interface GenTableDetail extends GenTableItem {
  columns: GenColumnItem[]
}

export interface DbTableItem {
  tableName: string
  tableComment?: string
  rowCount?: number
  imported: boolean
}

export function listGenTable(params: Record<string, unknown>) {
  return http.get<PageData<GenTableItem>>('/system/gen/table/list', { params })
}

export function listDbTables() {
  return http.get<DbTableItem[]>('/system/gen/table/db/list')
}

export function importTables(tableNames: string[]) {
  return http.post<number>('/system/gen/table/import', tableNames)
}

export function getGenTable(tableId: string) {
  return http.get<GenTableDetail>(`/system/gen/table/${tableId}`)
}

export function updateGenTable(tableId: string, data: Record<string, unknown>) {
  return http.put(`/system/gen/table/${tableId}`, data)
}

export function removeGenTable(tableIds: string) {
  return http.delete(`/system/gen/table/${tableIds}`)
}

export interface GenTemplateItem {
  templateName: string
  overridden: boolean
  contentLength: number
  updateTime?: string
}

export function listGenTemplates() {
  return http.get<GenTemplateItem[]>('/system/gen/table/template/list')
}

export function getGenTemplate(name: string) {
  return http.get<string>(`/system/gen/table/template/${name}`)
}

export function saveGenTemplate(name: string, content: string) {
  return http.put(`/system/gen/table/template/${name}`, { content })
}

export function resetGenTemplate(name: string) {
  return http.delete(`/system/gen/table/template/${name}`)
}

export function previewGenCode(tableId: string) {
  return http.get<Record<string, string>>(`/system/gen/table/preview/${tableId}`)
}

/** 下载 zip：走原生 fetch，避免响应拦截器把二进制当成 JSON 解析 */
async function fetchZip(url: string, init: RequestInit, fileName: string) {
  const token = localStorage.getItem('wejava:access_token') ?? ''
  const response = await fetch(url, {
    ...init,
    headers: { ...(init.headers ?? {}), Authorization: `Bearer ${token}` }
  })
  if (!response.ok) {
    throw new Error(`下载失败：HTTP ${response.status}`)
  }
  const blob = await response.blob()
  const objectUrl = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = objectUrl
  link.download = fileName
  link.click()
  URL.revokeObjectURL(objectUrl)
}

export async function downloadGenCode(tableId: string, fileName = `wejava-code-${tableId}.zip`) {
  await fetchZip(`${import.meta.env.VITE_APP_BASE_API}/system/gen/table/download/${tableId}`, {}, fileName)
}

export async function batchDownloadGenCode(tableIds: string[]) {
  await fetchZip(
    `${import.meta.env.VITE_APP_BASE_API}/system/gen/table/batch-download`,
    { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(tableIds) },
    'wejava-code-batch.zip'
  )
}
