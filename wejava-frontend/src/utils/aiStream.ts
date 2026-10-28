import { getAccessToken } from '@/utils/auth'

interface ChatHandlers {
  onDelta: (text: string) => void
  onDone: (payload: any) => void
  onError: (message: string) => void
}

/**
 * AI 对话流式客户端。
 *
 * 与通知通道不同，这里必须用 fetch：EventSource 只支持 GET，
 * 而提问需要 POST 请求体。解析逻辑仍遵循 SSE 协议（event/data + 空行分隔）。
 */
export async function streamChat(
  payload: { sessionId?: string; content: string },
  handlers: ChatHandlers,
  signal?: AbortSignal
): Promise<void> {
  const baseUrl = import.meta.env.VITE_APP_BASE_API
  const response = await fetch(`${baseUrl}/ai/chat`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${getAccessToken()}`
    },
    body: JSON.stringify(payload),
    signal
  })

  if (!response.ok || !response.body) {
    // 非 200 一般是鉴权或限流，body 是普通 JSON
    const text = await response.text().catch(() => '')
    handlers.onError(parseError(text) || `请求失败（${response.status}）`)
    return
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })

    // SSE 帧以空行分隔，最后一段可能不完整，留在 buffer 里
    const frames = buffer.split('\n\n')
    buffer = frames.pop() ?? ''

    for (const frame of frames) {
      handleFrame(frame, handlers)
    }
  }
}

function handleFrame(frame: string, handlers: ChatHandlers) {
  let event = 'message'
  const dataLines: string[] = []

  frame.split('\n').forEach((line) => {
    if (line.startsWith(':')) return // 注释行（心跳）
    if (line.startsWith('event:')) event = line.slice(6).trim()
    else if (line.startsWith('data:')) dataLines.push(line.slice(5).trim())
  })

  if (!dataLines.length) return
  const raw = dataLines.join('\n')

  let payload: any = raw
  try {
    payload = JSON.parse(raw)
  } catch {
    // 非 JSON 直接当文本
  }

  if (event === 'delta') handlers.onDelta(payload?.text ?? '')
  else if (event === 'done') handlers.onDone(payload)
  else if (event === 'error') handlers.onError(payload?.msg ?? '对话失败')
}

function parseError(text: string) {
  try {
    return JSON.parse(text)?.msg
  } catch {
    return text.slice(0, 120)
  }
}
