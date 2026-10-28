import request from '@/utils/request'

export function listRoleOptions() {
  return request.get<any, any>('/system/role/option-select')
}

export function listRole(params: any) {
  return request.get<any, any>('/system/role/list', { params })
}

export function getRole(roleId: string | number) {
  return request.get<any, any>(`/system/role/${roleId}`)
}

export function createRole(data: any) {
  return request.post<any, any>('/system/role', data)
}

export function updateRole(data: any) {
  return request.put<any, any>('/system/role', data)
}

export function removeRole(ids: string) {
  return request.delete<any, any>(`/system/role/${ids}`)
}

export function getRoleDeptIds(roleId: string | number) {
  return request.get<any, any>(`/system/role/${roleId}/dept-ids`)
}

export function updateRoleDataScope(data: any) {
  return request.put<any, any>('/system/role/data-scope', data)
}
