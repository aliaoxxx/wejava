import { http, type PageData } from '@/utils/http'
import type { RoleItem } from '@/types/system'

export function listRoleOptions() {
  return http.get<Record<string, unknown>[]>('/system/role/option-select')
}

export function listRole(params: Record<string, unknown>) {
  return http.get<PageData<RoleItem>>('/system/role/list', { params })
}

export function getRole(roleId: string | number) {
  return http.get<Record<string, unknown>>(`/system/role/${roleId}`)
}

export function createRole(data: Record<string, unknown>) {
  return http.post('/system/role', data)
}

export function updateRole(data: Record<string, unknown>) {
  return http.put('/system/role', data)
}

export function removeRole(ids: string) {
  return http.delete(`/system/role/${ids}`)
}

export function getRoleDeptIds(roleId: string | number) {
  return http.get<Array<string | number>>(`/system/role/${roleId}/dept-ids`)
}

export function updateRoleDataScope(data: Record<string, unknown>) {
  return http.put('/system/role/data-scope', data)
}
