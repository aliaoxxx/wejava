import request from '@/utils/request'

export function listNotice(params: any) {
  return request.get<any, any>('/system/notice/list', { params })
}
export function getNotice(noticeId: string | number) {
  return request.get<any, any>(`/system/notice/${noticeId}`)
}
export function createNotice(data: any) {
  return request.post<any, any>('/system/notice', data)
}
export function updateNotice(data: any) {
  return request.put<any, any>('/system/notice', data)
}
export function removeNotice(ids: string) {
  return request.delete<any, any>(`/system/notice/${ids}`)
}
