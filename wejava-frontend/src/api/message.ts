import { http, type PageData } from '@/utils/http'

export function getUnreadCount() {
  return http.get('/system/message/unread-count')
}

export function listMessage(params: Record<string, unknown>) {
  return http.get<PageData<Record<string, unknown>>>('/system/message/list', { params })
}

export function getMessage(messageId: string | number) {
  return http.get(`/system/message/${messageId}`)
}

export function markMessageRead(messageId: string | number) {
  return http.put(`/system/message/${messageId}/read`)
}

export function markAllMessageRead() {
  return http.put('/system/message/read-all')
}

/** 换取 SSE 一次性票据（30 秒有效） */
export function getSseTicket() {
  return http.post<{ ticket: string }>('/auth/sse-ticket')
}

/** 后台发送消息（按范围投递） */
export function sendMessage(data: any) {
  return http.post('/system/message/send', data)
}
