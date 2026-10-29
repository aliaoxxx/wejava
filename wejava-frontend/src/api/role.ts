import { http, type PageData } from '@/utils/http'

export function listRoleOptions() {
  return http.get<Record<string, unknown>[]>('/system/role/option-select')
}

export function listRole(params: any) {
  return http.get<PageData>('/system/role/list', { params })
}

export function getRole(roleId: string | number) {
  return http.get<Record<string, unknown>>(`/system/role/${roleId}`)
}

export function createRole(data: any) {
  return http.post('/system/role', data)
}

export function updateRole(data: any) {
  return http.put('/system/role', data)
}

export function removeRole(ids: string) {
  return http.delete(`/system/role/${ids}`)
}

export function getRoleDeptIds(roleId: string | number) {
  return http.get<Array<string | number>>(`/system/role/${roleId}/dept-ids`)
}

export function updateRoleDataScope(data: any) {
  return http.put('/system/role/data-scope', data)
}
