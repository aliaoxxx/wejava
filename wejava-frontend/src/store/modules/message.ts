import { defineStore } from 'pinia'

export interface SiteMessage {
  id: string
  title: string
  content: string
  read: boolean
  createdAt: string
}

/**
 * 站内消息。当前只有前端占位，等后端消息接口就绪后，
 * 把 fetchMessages 换成真实请求即可，组件层不需要改动。
 */
export const useMessageStore = defineStore('message', () => {
  const messages = ref<SiteMessage[]>([])
  const loading = ref(false)

  const unreadCount = computed(() => messages.value.filter((item) => !item.read).length)

  async function fetchMessages() {
    loading.value = true
    try {
      // 后端接口就绪前保持空列表，避免伪造数据
      messages.value = []
    } finally {
      loading.value = false
    }
  }

  function markAllRead() {
    messages.value = messages.value.map((item) => ({ ...item, read: true }))
  }

  return { messages, loading, unreadCount, fetchMessages, markAllRead }
})
