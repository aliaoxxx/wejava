import request from '@/utils/request'

export function listOperLog(params: any) {
  return request.get<any, any>('/monitor/operlog/list', { params })
}
export function removeOperLog(ids: string) {
  return request.delete<any, any>(`/monitor/operlog/${ids}`)
}
export function cleanOperLog() {
  return request.delete<any, any>('/monitor/operlog/clean')
}

export function listOnline(params: any) {
  return request.get<any, any>('/monitor/online/list', { params })
}
export function kickOnline(jti: string) {
  return request.delete<any, any>(`/monitor/online/${jti}`)
}

export function getServerInfo() {
  return request.get<any, any>('/monitor/server')
}
