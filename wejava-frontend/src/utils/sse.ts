import { getSseTicket } from '@/api/message'

interface StreamHandlers {
  onMessage?: (payload: any) => void
  onUnread?: (payload: any) => void
  onConnected?: () => void
  onError?: (message: string) => void
}

/**
 * 通知通道 SSE 客户端。
 *
 * 遵循 SSE 协议：服务端用 event 名区分事件类型、data 为 JSON、空行结束。
 * 断线后指数退避重连（EventSource 自带重连，但票据是一次性的，
 * 必须重新换票，所以这里自己控制重连节奏）。
 */
export function createNotificationStream(handlers: StreamHandlers) {
  let source: EventSource | null = null
  let retryTimer: ReturnType<typeof setTimeout> | undefined
  let attempts = 0
  let stopped = false

  const baseUrl = import.meta.env.VITE_APP_BASE_API

  function scheduleReconnect() {
    if (stopped) return
    attempts += 1
    const delay = Math.min(30_000, 1000 * 2 ** Math.min(attempts, 5))
    retryTimer = setTimeout(connect, delay)
  }

  async function connect() {
    if (stopped) return
    try {
      const res = await getSseTicket()
      const ticket = res.data?.ticket
      if (!ticket) {
        scheduleReconnect()
        return
      }
      source = new EventSource(`${baseUrl}/stream/notifications?ticket=${ticket}`)

      source.addEventListener('connected', () => {
        attempts = 0
        handlers.onConnected?.()
      })
      source.addEventListener('message', (event) => {
        handlers.onMessage?.(safeParse((event as MessageEvent).data))
      })
      source.addEventListener('unread', (event) => {
        handlers.onUnread?.(safeParse((event as MessageEvent).data))
      })
      source.addEventListener('error', () => {
        handlers.onError?.('通知连接已断开')
      })
      source.onerror = () => {
        source?.close()
        source = null
        scheduleReconnect()
      }
    } catch {
      scheduleReconnect()
    }
  }

  function safeParse(raw: string) {
    try {
      return JSON.parse(raw)
    } catch {
      return raw
    }
  }

  function close() {
    stopped = true
    if (retryTimer) clearTimeout(retryTimer)
    source?.close()
    source = null
  }

  return { connect, close }
}
