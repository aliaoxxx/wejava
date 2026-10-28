import request from '@/utils/request'

export function getUnreadCount() {
  return request.get<any, any>('/system/message/unread-count')
}

export function listMessage(params: any) {
  return request.get<any, any>('/system/message/list', { params })
}

export function getMessage(messageId: string | number) {
  return request.get<any, any>(`/system/message/${messageId}`)
}

export function markMessageRead(messageId: string | number) {
  return request.put<any, any>(`/system/message/${messageId}/read`)
}

export function markAllMessageRead() {
  return request.put<any, any>('/system/message/read-all')
}

/** 换取 SSE 一次性票据（30 秒有效） */
export function getSseTicket() {
  return request.post<any, any>('/auth/sse-ticket')
}

/** 后台发送消息（按范围投递） */
export function sendMessage(data: any) {
  return request.post<any, any>('/system/message/send', data)
}
