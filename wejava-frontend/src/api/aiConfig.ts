import { http } from '@/utils/http'

export interface AiConfigItem {
  aiConfigId: string
  provider: string
  model: string
  baseUrl: string
  temperature: number
  maxTokens: number
  enabled: number
  apiKeyMasked?: string
  apiKeyConfigured?: boolean
  updateBy?: string
  updateTime?: string
}

export function getAiConfig() {
  return http.get<AiConfigItem>('/system/ai-config')
}

export function updateAiConfig(data: any) {
  return http.put('/system/ai-config', data)
}
