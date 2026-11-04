import { http, type PageData } from '@/utils/http'

export function listContract(params: Record<string, unknown>) {
  return http.get<PageData>('/biz/contract/list', { params })
}

export function getContract(contractId: string | number) {
  return http.get<Record<string, unknown>>(`/biz/contract/${contractId}`)
}

export function createContract(data: Record<string, unknown>) {
  return http.post('/biz/contract', data)
}

export function updateContract(data: Record<string, unknown>) {
  return http.put('/biz/contract', data)
}

export function removeContract(ids: string) {
  return http.delete(`/biz/contract/${ids}`)
}
