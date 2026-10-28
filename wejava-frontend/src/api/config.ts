import request from '@/utils/request'

export function listConfig(params: any) {
  return request.get<any, any>('/system/config/list', { params })
}
export function getConfig(configId: string | number) {
  return request.get<any, any>(`/system/config/${configId}`)
}
export function createConfig(data: any) {
  return request.post<any, any>('/system/config', data)
}
export function updateConfig(data: any) {
  return request.put<any, any>('/system/config', data)
}
export function removeConfig(ids: string) {
  return request.delete<any, any>(`/system/config/${ids}`)
}
