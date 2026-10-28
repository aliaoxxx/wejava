import request from '@/utils/request'

export function getAiConfig() {
  return request.get<any, any>('/system/ai-config')
}

export function updateAiConfig(data: any) {
  return request.put<any, any>('/system/ai-config', data)
}
