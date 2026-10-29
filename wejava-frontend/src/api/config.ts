import { http, type PageData } from '@/utils/http'

export function listConfig(params: any) {
  return http.get<PageData>('/system/config/list', { params })
}
export function getConfig(configId: string | number): Promise<import('@/utils/http').ApiResult<Record<string, unknown>>> {
  return http.get(`/system/config/${configId}`)
}
export function createConfig(data: any) {
  return http.post('/system/config', data)
}
export function updateConfig(data: any) {
  return http.put('/system/config', data)
}
export function removeConfig(ids: string) {
  return http.delete(`/system/config/${ids}`)
}
