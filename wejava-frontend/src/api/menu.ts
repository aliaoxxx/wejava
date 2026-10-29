import { http } from '@/utils/http'

export function listMenu(params?: any) {
  return http.get<Record<string, unknown>[]>('/system/menu/list', { params })
}

export function getRoleMenuTree(roleId: string | number) {
  return http.get<{ menus: unknown[]; checkedKeys: unknown[] }>(`/system/menu/role-tree/${roleId}`)
}

export function menuTreeSelect() {
  return http.get<Record<string, unknown>[]>('/system/menu/tree-select')
}

export function createMenu(data: any) {
  return http.post('/system/menu', data)
}

export function updateMenu(data: any) {
  return http.put('/system/menu', data)
}

export function removeMenu(menuId: string | number) {
  return http.delete(`/system/menu/${menuId}`)
}
