import request from '@/utils/request'

export function listDictType(params: any) {
  return request.get<any, any>('/system/dict/type/list', { params })
}
export function createDictType(data: any) {
  return request.post<any, any>('/system/dict/type', data)
}
export function updateDictType(data: any) {
  return request.put<any, any>('/system/dict/type', data)
}
export function removeDictType(ids: string) {
  return request.delete<any, any>(`/system/dict/type/${ids}`)
}

export function listDictData(params: any) {
  return request.get<any, any>('/system/dict/data/list', { params })
}
export function createDictData(data: any) {
  return request.post<any, any>('/system/dict/data', data)
}
export function updateDictData(data: any) {
  return request.put<any, any>('/system/dict/data', data)
}
export function removeDictData(ids: string) {
  return request.delete<any, any>(`/system/dict/data/${ids}`)
}
export function getDictByType(dictType: string) {
  return request.get<any, any>(`/system/dict/data/type/${dictType}`)
}
