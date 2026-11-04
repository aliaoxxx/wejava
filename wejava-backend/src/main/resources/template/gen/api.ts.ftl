import { http, type PageData } from '@/utils/http'

export function list${table.businessCap}(params: Record<string, unknown>) {
  return http.get<PageData>('${table.routePath}/list', { params })
}

export function get${table.businessCap}(${table.pkColumn.javaField}: string | number) {
  return http.get<Record<string, unknown>>(`${table.routePath}/${'$'}{${table.pkColumn.javaField}}`)
}

export function create${table.businessCap}(data: Record<string, unknown>) {
  return http.post('${table.routePath}', data)
}

export function update${table.businessCap}(data: Record<string, unknown>) {
  return http.put('${table.routePath}', data)
}

export function remove${table.businessCap}(ids: string) {
  return http.delete(`${table.routePath}/${'$'}{ids}`)
}
