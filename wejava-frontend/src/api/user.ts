import { http, type PageData } from '@/utils/http'

export function listUser(params: Record<string, unknown>) {
  return http.get<PageData>('/system/user/list', { params })
}

export function getUser(userId: string | number) {
  return http.get<Record<string, unknown>>(`/system/user/${userId}`)
}

export function createUser(data: Record<string, unknown>) {
  return http.post('/system/user', data)
}

export function updateUser(data: Record<string, unknown>) {
  return http.put('/system/user', data)
}

export function removeUser(ids: string) {
  return http.delete(`/system/user/${ids}`)
}

export function resetUserPassword(data: { userId: string; password: string }) {
  return http.put('/system/user/reset-pwd', data)
}
