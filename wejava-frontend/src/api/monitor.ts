import { http, type PageData } from '@/utils/http'

export function listOperLog(params: any) {
  return http.get<PageData>('/monitor/operlog/list', { params })
}
export function removeOperLog(ids: string) {
  return http.delete(`/monitor/operlog/${ids}`)
}
export function cleanOperLog() {
  return http.delete('/monitor/operlog/clean')
}

export function listOnline(params: any) {
  return http.get<PageData>('/monitor/online/list', { params })
}
export function kickOnline(jti: string) {
  return http.delete(`/monitor/online/${jti}`)
}

export function getServerInfo() {
  return http.get('/monitor/server')
}
