import request from '@/utils/request'

export function listDept(params?: any) {
  return request.get<any, any>('/system/dept/list', { params })
}

export function getDept(deptId: string | number) {
  return request.get<any, any>(`/system/dept/${deptId}`)
}

export function createDept(data: any) {
  return request.post<any, any>('/system/dept', data)
}

export function updateDept(data: any) {
  return request.put<any, any>('/system/dept', data)
}

export function removeDept(deptId: string | number) {
  return request.delete<any, any>(`/system/dept/${deptId}`)
}

export function deptTreeSelect() {
  return request.get<any, any>('/system/dept/tree-select')
}
