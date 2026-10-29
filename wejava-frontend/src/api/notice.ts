import { http, type PageData } from '@/utils/http'

export function listNotice(params: Record<string, unknown>) {
  return http.get<PageData>('/system/notice/list', { params })
}
export function getNotice(noticeId: string | number) {
  return http.get<Record<string, unknown>>(`/system/notice/${noticeId}`)
}
export function createNotice(data: Record<string, unknown>) {
  return http.post('/system/notice', data)
}
export function updateNotice(data: Record<string, unknown>) {
  return http.put('/system/notice', data)
}
export function removeNotice(ids: string) {
  return http.delete(`/system/notice/${ids}`)
}
