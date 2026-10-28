import { defineStore } from 'pinia'
import { getUnreadCount, listMessage, markAllMessageRead, markMessageRead } from '@/api/message'

export interface SiteMessage {
  messageId: string
  title: string
  content: string
  msgType: number
  level: number
  link?: string
  readFlag: number
  createTime: string
}

/** 站内消息：公告与系统消息统一收件箱 */
export const useMessageStore = defineStore('message', () => {
  const recent = ref<SiteMessage[]>([])
  const unreadCount = ref(0)
  const loading = ref(false)

  async function refreshUnread() {
    const res = await getUnreadCount()
    unreadCount.value = Number(res.data ?? 0)
  }

  async function fetchRecent() {
    loading.value = true
    try {
      const res = await listMessage({ pageNum: 1, pageSize: 5 })
      recent.value = res.data?.list ?? []
    } finally {
      loading.value = false
    }
  }

  async function readOne(messageId: string) {
    await markMessageRead(messageId)
    const item = recent.value.find((msg) => msg.messageId === messageId)
    if (item && item.readFlag === 0) {
      item.readFlag = 1
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    }
  }

  async function readAll() {
    await markAllMessageRead()
    recent.value = recent.value.map((msg) => ({ ...msg, readFlag: 1 }))
    unreadCount.value = 0
  }

  return { recent, unreadCount, loading, refreshUnread, fetchRecent, readOne, readAll }
})
