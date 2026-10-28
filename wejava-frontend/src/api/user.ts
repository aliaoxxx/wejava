import request from '@/utils/request'

export function listUser(params: any) {
  return request.get<any, any>('/system/user/list', { params })
}

export function getUser(userId: string | number) {
  return request.get<any, any>(`/system/user/${userId}`)
}

export function createUser(data: any) {
  return request.post<any, any>('/system/user', data)
}

export function updateUser(data: any) {
  return request.put<any, any>('/system/user', data)
}

export function removeUser(ids: string) {
  return request.delete<any, any>(`/system/user/${ids}`)
}

export function resetUserPassword(data: { userId: string; password: string }) {
  return request.put<any, any>('/system/user/reset-pwd', data)
}
