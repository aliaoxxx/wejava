import { http, type PageData } from '@/utils/http'
import type { DictDataItem, DictTypeItem } from '@/types/system'

export function listDictType(params: Record<string, unknown>) {
  return http.get<PageData<DictTypeItem>>('/system/dict/type/list', { params })
}
export function createDictType(data: Record<string, unknown>) {
  return http.post('/system/dict/type', data)
}
export function updateDictType(data: Record<string, unknown>) {
  return http.put('/system/dict/type', data)
}
export function removeDictType(ids: string) {
  return http.delete(`/system/dict/type/${ids}`)
}

export function listDictData(params: Record<string, unknown>) {
  return http.get<PageData<DictDataItem>>('/system/dict/data/list', { params })
}
export function createDictData(data: Record<string, unknown>) {
  return http.post('/system/dict/data', data)
}
export function updateDictData(data: Record<string, unknown>) {
  return http.put('/system/dict/data', data)
}
export function removeDictData(ids: string) {
  return http.delete(`/system/dict/data/${ids}`)
}
export function getDictByType(dictType: string) {
  return http.get<{ label: string; value: string | number }[]>(`/system/dict/data/type/${dictType}`)
}
