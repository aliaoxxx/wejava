import { http } from '@/utils/http'

export interface AiConfigItem {
  aiConfigId: string
  configName?: string
  provider: string
  providerLabel?: string
  protocol?: string
  model: string
  baseUrl: string
  temperature: number
  maxTokens: number
  enabled: number
  isDefault?: boolean
  sort?: number
  remark?: string
  apiKeyMasked?: string
  apiKeyConfigured?: boolean
  updateBy?: string
  updateTime?: string
}

export interface AiProviderPreset {
  key: string
  label: string
  baseUrl: string
  consoleUrl?: string
}

export interface AiConfigPayload {
  provider?: string
  configName?: string
  model: string
  baseUrl: string
  apiKey?: string
  temperature?: number
  maxTokens?: number
  enabled?: number
  isDefault?: boolean
  remark?: string
}

export function listAiConfigs() {
  return http.get<AiConfigItem[]>('/system/ai-config/list')
}

export function listAiProviders() {
  return http.get<AiProviderPreset[]>('/system/ai-config/providers')
}

export function createAiConfig(data: AiConfigPayload) {
  return http.post<number>('/system/ai-config', data)
}

export function updateAiConfig(id: string, data: AiConfigPayload) {
  return http.put(`/system/ai-config/${id}`, data)
}

export function deleteAiConfig(id: string) {
  return http.delete(`/system/ai-config/${id}`)
}

export function setDefaultAiConfig(id: string) {
  return http.put(`/system/ai-config/${id}/default`)
}

export function listAiModels(id: string) {
  return http.get<string[]>(`/system/ai-config/${id}/models`)
}

export function testAiConfig(id: string) {
  return http.post<{ ok: boolean; costMs: number }>(`/system/ai-config/${id}/test`)
}
