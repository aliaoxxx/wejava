import { http, type PageData } from '@/utils/http'

export function listNotice(params: any) {
  return http.get<PageData>('/system/notice/list', { params })
}
export function getNotice(noticeId: string | number) {
  return http.get<Record<string, unknown>>(`/system/notice/${noticeId}`)
}
export function createNotice(data: any) {
  return http.post('/system/notice', data)
}
export function updateNotice(data: any) {
  return http.put('/system/notice', data)
}
export function removeNotice(ids: string) {
  return http.delete(`/system/notice/${ids}`)
}
