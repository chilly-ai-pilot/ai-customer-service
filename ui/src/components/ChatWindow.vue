<template>
  <div class="chat-window">
    <div class="chat-header">
      <span class="chat-title">{{ displayTitle }}</span>
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
import { ref, computed, nextTick, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { sessionApi, userApi, commercialTenantApi, goodsApi } from '@/api'
import { initChatWs, sendMessage, destroyChatWs } from '@/ws/chat'
import { useAuthStore } from '@/stores/auth'
import RelativeTime from './RelativeTime.vue'

const props = defineProps({
  sessionId: { type: String, default: '' },
  goodsId: { type: [String, Number], default: '' },
  ctId: { type: [String, Number], default: '' },
  title: { type: String, default: '咨询' }
})

// 新建会话成功后（SESSION_CREATED）把真实 sessionId 交给父组件去 router.replace 更新 URL，
// ChatWindow 本身不感知具体路由路径，方便用户端/商户端复用。
const emit = defineEmits(['session-created'])

const authStore = useAuthStore()
const messages = ref([])
const inputText = ref('')
const messageListRef = ref(null)
const loading = ref(false)
const currentSessionId = ref(props.sessionId)

const parseOptionalNumber = (value) => {
  if (value === '' || value === null || value === undefined) return null
  return Number(value)
}

const currentGoodsId = ref(parseOptionalNumber(props.goodsId))
const currentCtId = ref(parseOptionalNumber(props.ctId))

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
  return msg.senderType === authStore.userType || msg.isMine
}

function sortMessages() {
  messages.value = [...messages.value].sort((a, b) => {
    const aTime = a.createdAt ? new Date(a.createdAt).getTime() : 0
    const bTime = b.createdAt ? new Date(b.createdAt).getTime() : 0
    return aTime - bTime
  })
}

// 后端消息列表接口只支持 pageNum/pageSize 分页，没有"某时间点之后"的增量查询参数，
// 这里先用 pageSize=1 探出 totalElements，再一次性把全部消息取回（按 createdAt 升序），
// 供初次加载 / 断线重连增量比对使用。会话消息量一般不大，这个取舍是可接受的。
async function fetchAllHistoryMessages() {
  if (!currentSessionId.value) return []
  const peek = await sessionApi.getMessages(currentSessionId.value, { pageNum: 1, pageSize: 1 })
  const total = peek.totalElements || 0
  if (total === 0) return []
  const data = await sessionApi.getMessages(currentSessionId.value, { pageNum: 1, pageSize: total })
  return (data.content || [])
      .slice()
      .sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime())
}

// 首次进入（本地没有任何消息）：直接加载全部历史
async function loadHistory() {
  if (!currentSessionId.value) return
  loading.value = true
  try {
    const history = await fetchAllHistoryMessages()
    messages.value = history.map((msg) => ({ ...msg, isMine: isMyMessage(msg) }))
    scrollToBottom()
  } finally {
    loading.value = false
  }
}

// 断线重连成功：保留本地已加载的消息不动，只把本地最后一条消息 createdAt 之后
// （且本地还没有的）消息拼接到列表底部
async function mergeIncrementalHistory() {
  if (!currentSessionId.value || messages.value.length === 0) return
  try {
    const history = await fetchAllHistoryMessages()
    const lastLocalTime = Math.max(
        0,
        ...messages.value.filter((m) => m.createdAt).map((m) => new Date(m.createdAt).getTime())
    )
    const existingIds = new Set(messages.value.filter((m) => m.id != null).map((m) => m.id))
    const incremental = history.filter((msg) => {
      if (msg.id != null && existingIds.has(msg.id)) return false
      if (!msg.createdAt) return true
      return new Date(msg.createdAt).getTime() > lastLocalTime
    })
    if (incremental.length === 0) return
    incremental.forEach((msg) => messages.value.push({ ...msg, isMine: isMyMessage(msg) }))
    sortMessages()
    scrollToBottom()
  } catch {
    // 增量拉取失败不影响已展示的消息，下次重连再重试，不清空本地内容
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

// 待确认的本地发送消息，按发送顺序排队。后端消息里的 messageId 是服务端生成的
// 唯一标识，WS 发送协议本身不支持客户端回传一个"临时 ID"（文档给的发送格式里
// 没有这个字段），所以这里用"先发先回"的 FIFO 顺序去匹配 SUCCESS/SESSION_CREATED/
// ERROR 回执，而不是去比较一个后端根本不会回传的 tempId —— 避免消息永远卡在"发送中"。
const pendingSendQueue = []

function resolvePendingSend(data, isError) {
  const tempId = pendingSendQueue.shift()
  if (!tempId) return
  const target = messages.value.find((msg) => msg.tempId === tempId)
  if (!target) return

  if (isError) {
    target.state = 'ERROR'
    return
  }
  target.state = 'SENT'
  if (data?.id != null) target.id = data.id
  if (data?.messageId) target.messageId = data.messageId
  if (data?.sessionId != null) target.sessionId = data.sessionId
  if (data?.createdAt) target.createdAt = data.createdAt
}

function handleMessage(data) {
  if (data.state === 'SESSION_CREATED') {
    const sessionId = String(data.sessionId)
    currentSessionId.value = sessionId
    resolvePendingSend(data, false)
    emit('session-created', sessionId)
    return
  }
  if (data.state === 'SUCCESS') {
    resolvePendingSend(data, false)
    return
  }
  if (data.state === 'ERROR') {
    resolvePendingSend(data, true)
    ElMessage.error('发送失败，请重试')
    return
  }
  if (['DELIVERED', 'RECEIVED'].includes(data.state)) {
    return
  }
  // 对方实时推来的消息（完整的 MessageResponse，没有 state 字段）
  if (data.senderType && data.senderType !== authStore.userType) {
    if (data.id != null && messages.value.some((m) => m.id === data.id)) return
    messages.value.push({ ...data, isMine: false })
    sortMessages()
    scrollToBottom()
  }
}

function appendOutgoingMessage(text) {
  const tempId = genTempId()
  // WS 发送协议字段统一用 "content"（后端字段名），不再用旧的 "message" 别名
  const payload = currentSessionId.value
      ? { sessionId: Number(currentSessionId.value), content: text }
      : { goodsId: currentGoodsId.value, ctId: currentCtId.value, content: text }

  messages.value.push({
    tempId,
    content: text,
    senderType: authStore.userType,
    isMine: true,
    state: 'SENDING',
    createdAt: new Date().toISOString()
  })
  sortMessages()
  scrollToBottom()

  inputText.value = ''

  const sent = sendMessage(payload)
  if (sent) {
    pendingSendQueue.push(tempId)
  } else {
    // WebSocket 当前不可用，直接标记失败，不进队列等一个不会来的回执
    const target = messages.value.find((msg) => msg.tempId === tempId)
    if (target) target.state = 'ERROR'
    ElMessage.error('发送失败，请重试')
  }
}

function handleSend() {
  const text = inputText.value.trim()
  if (!text) return

  if (!currentSessionId.value && (currentGoodsId.value === null || currentCtId.value === null)) {
    ElMessage.error('缺少商品或商户信息，无法发送消息')
    return
  }

  appendOutgoingMessage(text)
}

// 聊天框标题：默认用父组件传的 title（"#ctId - 商品#xxx" 这类兜底文案），
// 等异步查到对方真实名字/商品名字后覆盖掉，不阻塞页面渲染，名字来了再"悄悄换掉"标题
const partnerName = ref('')
const goodsName = ref('')

const displayTitle = computed(() => {
  const base = partnerName.value || props.title
  return goodsName.value ? `${base}（咨询商品：${goodsName.value}）` : base
})

// 会话详情有多处（对方名字、商品名字）都可能需要用到，这里做一次缓存，
// 避免同一个 sessionId 被重复请求 /session/{id} 接口
let sessionDetailPromise = null
function fetchSessionDetail() {
  if (!currentSessionId.value) return Promise.resolve(null)
  if (!sessionDetailPromise) {
    sessionDetailPromise = sessionApi.detail(currentSessionId.value).catch(() => null)
  }
  return sessionDetailPromise
}

async function resolvePartnerName() {
  try {
    if (authStore.userType === 'USER') {
      // 用户看到的对方是商户：ctId 大多数情况下从路由 query 直接拿到；
      // 从收件箱列表点进来的场景 query 里没有 ctId，只能先查一次会话详情
      let ctId = currentCtId.value
      if (ctId == null && currentSessionId.value) {
        const detail = await fetchSessionDetail()
        ctId = detail?.ctId ?? null
      }
      if (ctId == null) return
      const name = await commercialTenantApi.name(ctId)
      if (name) partnerName.value = name
    } else if (authStore.userType === 'TENANT') {
      // 商户看到的对方是用户：商户端聊天窗口永远是从 sessionId 进来的，
      // 没有直接拿到 userId 的入口，只能查一次会话详情
      if (!currentSessionId.value) return
      const detail = await fetchSessionDetail()
      if (detail?.userId == null) return
      const name = await userApi.name(detail.userId)
      if (name) partnerName.value = name
    }
  } catch {
    // 查不到名字就继续用兜底的 title，不影响聊天主流程
  }
}

// 咨询的商品名字：用户端从路由 query 里的 goodsId 直接拿到；
// 商户端聊天窗口只有 sessionId，没有 goodsId，需要先查一次会话详情
async function resolveGoodsName() {
  try {
    let goodsId = currentGoodsId.value
    if (goodsId == null && currentSessionId.value) {
      const detail = await fetchSessionDetail()
      goodsId = detail?.goodsId ?? null
    }
    if (goodsId == null) return
    const goods = await goodsApi.detail({ id: goodsId })
    if (goods?.name) goodsName.value = goods.name
  } catch {
    // 查不到商品名就继续不展示，不影响聊天主流程
  }
}

// 用户端点"咨询"进入时通常只带 goodsId/ctId、不带 sessionId（新建会话的入口），
// 但如果之前已经跟这个商户就这个商品聊过，其实是同一个会话，只是前端还不知道
// 它的 sessionId（后端要等第一条消息发出去、走 SESSION_CREATED/SUCCESS 才会告诉
// 前端）。这里主动查一下用户自己的会话列表，按 ctId+goodsId 找是否已有会话，
// 有的话直接复用它的 sessionId 去加载历史，不用非要等用户发消息才能看到历史。
async function resolveExistingSession() {
  if (currentSessionId.value) return
  if (authStore.userType !== 'USER') return
  if (currentGoodsId.value == null || currentCtId.value == null) return

  try {
    const peek = await sessionApi.listByUser({ pageNum: 1, pageSize: 1 })
    const total = peek.totalElements || 0
    if (total === 0) return
    const data = await sessionApi.listByUser({ pageNum: 1, pageSize: total })
    const existing = (data.content || []).find(
        (s) => Number(s.ctId) === currentCtId.value && Number(s.goodsId) === currentGoodsId.value
    )
    if (existing) {
      currentSessionId.value = String(existing.id)
      emit('session-created', currentSessionId.value)
    }
  } catch {
    // 查询失败就当作没有历史会话，走原来"发消息时才新建会话"的路径，不影响正常使用
  }
}

onMounted(async () => {
  await resolveExistingSession()

  // 不 await：名字加载慢/失败都不该卡住聊天主流程，标题先用兜底文案，
  // 名字回来了再自动替换（partnerName/goodsName 是响应式的，模板会自动更新）
  resolvePartnerName()
  resolveGoodsName()

  // 历史消息通过 REST 直接加载，不依赖 WebSocket 是否连上——纯查看历史时
  // 不应该被 WS 握手延迟/重连退避卡住，甚至连不上就一直看不到历史。
  if (currentSessionId.value) {
    await loadHistory()
  }

  initChatWs({
    onOpen: async () => {
      if (!currentSessionId.value) return
      // 首次加载已经在上面做过了，这里只处理"断线重连"场景的增量补齐
      if (messages.value.length > 0) {
        await mergeIncrementalHistory()
      }
      await markRead()
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