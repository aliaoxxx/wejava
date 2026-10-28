import request from '@/utils/request'

export function getProfile() {
  return request.get<any, any>('/system/profile')
}

export function updateProfile(data: any) {
  return request.put<any, any>('/system/profile', data)
}

export function changePassword(data: any) {
  return request.put<any, any>('/system/profile/password', data)
}
