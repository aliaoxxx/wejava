<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { streamChat } from '@/utils/aiStream'

interface ChatItem {
  role: 'user' | 'assistant'
  content: string
  pending?: boolean
}

const messages = ref<ChatItem[]>([])
const input = ref('')
const sending = ref(false)
const sessionId = ref<string>()
const listRef = ref<HTMLElement>()
let controller: AbortController | undefined

function scrollToBottom() {
  nextTick(() => {
    if (listRef.value) listRef.value.scrollTop = listRef.value.scrollHeight
  })
}

async function send() {
  const content = input.value.trim()
  if (!content || sending.value) return

  input.value = ''
  sending.value = true
  messages.value.push({ role: 'user', content })
  const reply = reactive<ChatItem>({ role: 'assistant', content: '', pending: true })
  messages.value.push(reply)
  scrollToBottom()

  controller = new AbortController()
  try {
    await streamChat(
      { sessionId: sessionId.value, content },
      {
        onDelta: (text) => {
          reply.content += text
          scrollToBottom()
        },
        onDone: (payload) => {
          reply.pending = false
          if (payload?.sessionId) sessionId.value = payload.sessionId
          if (payload?.completionTokens) {
            ElMessage.success(
              `完成：输出 ${payload.completionTokens} tokens，用时 ${payload.durationMs} ms`
            )
          }
        },
        onError: (message) => {
          reply.pending = false
          reply.content = reply.content || message
          ElMessage.error(message)
        }
      },
      controller.signal
    )
  } catch (e: any) {
    reply.pending = false
    if (e?.name !== 'AbortError') {
      reply.content = reply.content || '发送失败'
      ElMessage.error('发送失败')
    }
  } finally {
    sending.value = false
    controller = undefined
  }
}

function stop() {
  controller?.abort()
  sending.value = false
  const last = messages.value[messages.value.length - 1]
  if (last) last.pending = false
}

function newSession() {
  sessionId.value = undefined
  messages.value = []
}
</script>

<template>
  <el-card shadow="never" class="chat-card">
    <template #header>
      <div class="chat-header">
        <span>AI 助手</span>
        <div class="header-actions">
          <el-tag v-if="sessionId" size="small" type="info">会话 {{ sessionId }}</el-tag>
          <el-button size="small" @click="newSession">新对话</el-button>
        </div>
      </div>
    </template>

    <div ref="listRef" class="chat-list">
      <el-empty v-if="!messages.length" description="输入问题开始对话" />
      <div v-for="(item, index) in messages" :key="index" class="chat-row" :class="item.role">
        <div class="bubble">
          <span>{{ item.content }}</span>
          <span v-if="item.pending && item.role === 'assistant'" class="cursor">▍</span>
        </div>
      </div>
    </div>

    <div class="chat-input">
      <el-input
        v-model="input"
        type="textarea"
        :rows="3"
        resize="none"
        placeholder="输入问题，Enter 发送，Shift+Enter 换行"
        @keydown.enter.exact.prevent="send"
      />
      <div class="input-actions">
        <el-button v-if="sending" @click="stop">停止</el-button>
        <el-button type="primary" :loading="sending" @click="send">发送</el-button>
      </div>
    </div>
  </el-card>
</template>

<style scoped>
.chat-card {
  height: calc(100vh - 170px);
  display: flex;
  flex-direction: column;
}

/* 让卡片内容区变成弹性容器，否则底部按钮会溢出被裁掉 */
.chat-card :deep(.el-card__body) {
  flex: 1 1 auto;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.chat-list {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  padding: 8px 4px;
}
.chat-row {
  display: flex;
  margin-bottom: 12px;
}
.chat-row.user {
  justify-content: flex-end;
}
.bubble {
  max-width: 72%;
  padding: 8px 12px;
  border-radius: 8px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  background: var(--el-fill-color-light);
}
.chat-row.user .bubble {
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary-dark-2);
}
.cursor {
  animation: blink 1s step-start infinite;
}
@keyframes blink {
  50% {
    opacity: 0;
  }
}
.chat-input {
  border-top: 1px solid var(--el-border-color-lighter);
  padding-top: 10px;
}
.input-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}
</style>
