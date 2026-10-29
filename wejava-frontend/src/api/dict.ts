import { http, type PageData } from '@/utils/http'

export function listDictType(params: any) {
  return http.get<PageData>('/system/dict/type/list', { params })
}
export function createDictType(data: any) {
  return http.post('/system/dict/type', data)
}
export function updateDictType(data: any) {
  return http.put('/system/dict/type', data)
}
export function removeDictType(ids: string) {
  return http.delete(`/system/dict/type/${ids}`)
}

export function listDictData(params: any) {
  return http.get<PageData>('/system/dict/data/list', { params })
}
export function createDictData(data: any) {
  return http.post('/system/dict/data', data)
}
export function updateDictData(data: any) {
  return http.put('/system/dict/data', data)
}
export function removeDictData(ids: string) {
  return http.delete(`/system/dict/data/${ids}`)
}
export function getDictByType(dictType: string) {
  return http.get<{ label: string; value: string | number }[]>(`/system/dict/data/type/${dictType}`)
}
