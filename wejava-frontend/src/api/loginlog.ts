import request from '@/utils/request'

export function listLoginLog(params: any) {
  return request.get<any, any>('/monitor/loginlog/list', { params })
}
export function removeLoginLog(ids: string) {
  return request.delete<any, any>(`/monitor/loginlog/${ids}`)
}
export function cleanLoginLog() {
  return request.delete<any, any>('/monitor/loginlog/clean')
}
export function unlockAccount(username: string) {
  return request.put<any, any>(`/monitor/loginlog/unlock/${username}`)
}
