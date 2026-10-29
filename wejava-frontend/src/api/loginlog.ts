import { http, type PageData } from '@/utils/http'

export function listLoginLog(params: any) {
  return http.get<PageData>('/monitor/loginlog/list', { params })
}
export function removeLoginLog(ids: string) {
  return http.delete(`/monitor/loginlog/${ids}`)
}
export function cleanLoginLog() {
  return http.delete('/monitor/loginlog/clean')
}
export function unlockAccount(username: string) {
  return http.put(`/monitor/loginlog/unlock/${username}`)
}
