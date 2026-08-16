<template>
  <div class="chat-window">
    <div class="chat-header">
      <span class="chat-title">{{ title }}</span>
    </div>

    <div class="message-list" ref="messageListRef">
      <div v-if="loading" class="loading-tip">加载中...</div>
      <div v-else-if="messages.length === 0" class="empty-tip">暂无消息，开始对话吧</div>

      <div
        v-for="msg in messages"
        :key="msg.id || msg.messageId || msg.tempId"
        class="message-item"
        :class="msg.isMine ? 'mine' : 'theirs'"
      >
        <div class="message-bubble">
          <div class="message-content">{{ msg.content }}</div>
          <div class="message-meta">
            <span class="message-time">
              <RelativeTime v-if="msg.createdAt" :date="msg.createdAt" />
            </span>
            <span v-if="msg.isMine && msg.state === 'SENDING'" class="msg-status">发送中...</span>
            <span v-if="msg.isMine && msg.state === 'ERROR'" class="msg-status error">发送失败</span>
          </div>
        </div>
      </div>
    </div>

    <div class="chat-input-area">
      <el-input
        v-model="inputText"
        type="textarea"
        :rows="3"
        resize="none"
        placeholder="输入消息，回车发送"
        @keydown.enter.exact.prevent="handleSend"
      />
      <el-button type="primary" class="send-btn" @click="handleSend" :disabled="!inputText.trim()">
        发送
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, onUnmounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { sessionApi } from '@/api'
import { initChatWs, sendMessage, destroyChatWs } from '@/ws/chat'
import { useAuthStore } from '@/stores/auth'
import RelativeTime from './RelativeTime.vue'

const props = defineProps({
  sessionId: { type: String, default: '' },
  goodsId: { type: [String, Number], default: '' },
  ctId: { type: [String, Number], default: '' },
  title: { type: String, default: '咨询' }
})

const authStore = useAuthStore()
const messages = ref([])
const inputText = ref('')
const messageListRef = ref(null)
const loading = ref(false)
const currentSessionId = ref(props.sessionId)
const currentGoodsId = ref(props.goodsId === '' || props.goodsId === null || props.goodsId === undefined ? null : Number(props.goodsId))
const currentCtId = ref(props.ctId === '' || props.ctId === null || props.ctId === undefined ? null : Number(props.ctId))

let tempIdCounter = 0

function genTempId() {
  return `temp_${++tempIdCounter}_${Date.now()}`
}

function scrollToBottom() {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

function isMyMessage(msg) {
  const myType = authStore.userType
  return msg.senderType === myType || msg.isMine
}

function sortMessages() {
  messages.value = [...messages.value].sort((a, b) => {
    const aTime = a.createdAt ? new Date(a.createdAt).getTime() : 0
    const bTime = b.createdAt ? new Date(b.createdAt).getTime() : 0
    return aTime - bTime
  })
}

function appendMessage(msg) {
  const isMine = isMyMessage(msg)
  messages.value.push({ ...msg, isMine })
  sortMessages()
  scrollToBottom()
}

async function loadHistory() {
  if (!currentSessionId.value) return
  loading.value = true
  try {
    const data = await sessionApi.getMessages(currentSessionId.value, { pageNum: 1, pageSize: 50 })
    const history = (data.content || [])
      .slice()
      .sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime())

    history.forEach((msg) => {
      messages.value.push({ ...msg, isMine: isMyMessage(msg) })
    })
    sortMessages()
    scrollToBottom()
  } finally {
    loading.value = false
  }
}

async function markRead() {
  if (!currentSessionId.value) return
  try {
    await sessionApi.markRead(currentSessionId.value)
    window.dispatchEvent(new CustomEvent('chat:session-read', {
      detail: { sessionId: currentSessionId.value }
    }))
  } catch {
    // ignore
  }
}

function updateMessageStatus(data) {
  const messageId = data.messageId
  if (!messageId) return

  const target = messages.value.find((msg) => msg.messageId === messageId || msg.tempId === messageId)
  if (!target) return

  target.messageId = messageId
  target.state = 'SENT'
  if (data.sessionId) target.sessionId = data.sessionId
  if (data.timestamp) target.createdAt = data.timestamp
}

function handleMessage(data) {
  if (data.state === 'SESSION_CREATED') {
    currentSessionId.value = String(data.sessionId)
    appendMessage({
      messageId: data.messageId,
      sessionId: data.sessionId,
      content: data.content || '会话已建立',
      senderType: 'SYSTEM',
      createdAt: new Date().toISOString(),
      state: 'SESSION_CREATED'
    })
    updateMessageStatus(data)
    return
  }
  if (data.state === 'SUCCESS') {
    updateMessageStatus(data)
    return
  }
  if (data.state === 'ERROR') {
    appendMessage({ content: data.content || '发送失败，请重试', senderType: 'SYSTEM', state: 'ERROR' })
    ElMessage.error('发送失败，请重试')
    return
  }
  if (['DELIVERED', 'RECEIVED'].includes(data.state)) {
    return
  }
  if (data.senderType && !data.sessionId) {
    return
  }
  if (!data.content && !data.state) {
    return
  }
  appendMessage(data)
}

function handleSend() {
  const text = inputText.value.trim()
  if (!text) return

  if (currentGoodsId.value === null || currentCtId.value === null) {
    ElMessage.error('缺少商品或商户信息，无法发送消息')
    return
  }

  if (!currentSessionId.value) {
    const tempId = genTempId()
    const payload = {
      tempId,
      messageId: tempId,
      goodsId: currentGoodsId.value,
      ctId: currentCtId.value,
      content: text
    }
    appendMessage({ tempId: payload.tempId, messageId: payload.messageId, content: text, senderType: authStore.userType, isMine: true, state: 'SENDING', createdAt: new Date().toISOString() })
    inputText.value = ''
    sendMessage(payload)
  } else {
    const tempId = genTempId()
    const payload = {
      tempId,
      messageId: tempId,
      sessionId: Number(currentSessionId.value),
      goodsId: currentGoodsId.value,
      ctId: currentCtId.value,
      content: text
    }
    appendMessage({ tempId: payload.tempId, messageId: payload.messageId, content: text, senderType: authStore.userType, isMine: true, state: 'SENDING', createdAt: new Date().toISOString() })
    inputText.value = ''
    sendMessage(payload)
  }
}

onMounted(() => {
  initChatWs({
    onOpen: async () => {
      if (currentSessionId.value) {
        await loadHistory()
        await markRead()
      }
    },
    onMessage: handleMessage,
    onClose: () => {}
  })
})

onUnmounted(() => {
  destroyChatWs()
})
</script>

<style scoped>
.chat-window {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  height: 100%;
}

.chat-header {
  height: 50px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  background: #fff;
  border-bottom: 1px solid #eee;
}

.chat-title {
  font-size: 16px;
  font-weight: 500;
  color: #333;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #f5f5f5;
}

.loading-tip,
.empty-tip {
  text-align: center;
  color: #999;
  padding: 40px;
}

.message-item {
  display: flex;
  margin-bottom: 12px;
}

.message-item.mine {
  justify-content: flex-end;
}

.message-item.theirs {
  justify-content: flex-start;
}

.message-bubble {
  max-width: 70%;
  padding: 10px 14px;
  border-radius: 8px;
  word-break: break-word;
}

.mine .message-bubble {
  background: #95ec69;
  color: #333;
}

.theirs .message-bubble {
  background: #fff;
  color: #333;
  border: 1px solid #eee;
}

.message-content {
  font-size: 14px;
  line-height: 1.5;
}

.message-meta {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
  margin-top: 4px;
}

.message-time {
  font-size: 11px;
  color: #999;
}

.msg-status {
  font-size: 11px;
  color: #999;
}

.msg-status.error {
  color: #f56c6c;
}

.chat-input-area {
  flex-shrink: 0;
  padding: 12px 16px;
  background: #fff;
  border-top: 1px solid #eee;
  display: flex;
  gap: 10px;
  align-items: flex-end;
}

.chat-input-area .el-textarea {
  flex: 1;
}

.send-btn {
  flex-shrink: 0;
  height: 64px;
}
</style>
