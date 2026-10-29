import { http } from '@/utils/http'

export function getProfile() {
  return http.get('/system/profile')
}

export function updateProfile(data: any) {
  return http.put('/system/profile', data)
}

export function changePassword(data: any) {
  return http.put('/system/profile/password', data)
}
