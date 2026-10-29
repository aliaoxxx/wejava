import { http } from '@/utils/http'

export function listDept(params?: any) {
  return http.get<Record<string, unknown>[]>('/system/dept/list', { params })
}

export function getDept(deptId: string | number) {
  return http.get<Record<string, unknown>>(`/system/dept/${deptId}`)
}

export function createDept(data: Record<string, unknown>) {
  return http.post('/system/dept', data)
}

export function updateDept(data: Record<string, unknown>) {
  return http.put('/system/dept', data)
}

export function removeDept(deptId: string | number) {
  return http.delete(`/system/dept/${deptId}`)
}

export function deptTreeSelect() {
  return http.get<Record<string, unknown>[]>('/system/dept/tree-select')
}
