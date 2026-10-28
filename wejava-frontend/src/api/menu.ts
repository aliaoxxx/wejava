import request from '@/utils/request'

export function listMenu(params?: any) {
  return request.get<any, any>('/system/menu/list', { params })
}

export function getRoleMenuTree(roleId: string | number) {
  return request.get<any, any>(`/system/menu/role-tree/${roleId}`)
}

export function menuTreeSelect() {
  return request.get<any, any>('/system/menu/tree-select')
}

export function createMenu(data: any) {
  return request.post<any, any>('/system/menu', data)
}

export function updateMenu(data: any) {
  return request.put<any, any>('/system/menu', data)
}

export function removeMenu(menuId: string | number) {
  return request.delete<any, any>(`/system/menu/${menuId}`)
}
