<template>
  <div class="chat-window">
    <div class="chat-header">
      <span class="chat-title">{{ displayTitle }}</span>
    </div>

    <!-- 消息列表 -->
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

    <!-- 输入区 -->
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
  /** 会话 ID（已有会话时传入，新建会话时为空字符串） */
  sessionId: { type: String, default: '' },
  /** 商品 ID（新建会话时从路由 query 传入） */
  goodsId: { type: [String, Number], default: '' },
  /** 商户 ID（新建会话时从路由 query 传入） */
  ctId: { type: [String, Number], default: '' },
  /** 窗口标题兜底文案 */
  title: { type: String, default: '咨询' }
})

/**
 * 新建会话成功后（SESSION_CREATED）把真实 sessionId 交给父组件去 router.replace 更新 URL。
 * 这样 ChatWindow 本身不感知具体路由路径，用户端/商户端均可复用。
 */
const emit = defineEmits(['session-created'])

const authStore = useAuthStore()
const messages = ref([])
const inputText = ref('')
const messageListRef = ref(null)
const loading = ref(false)
const currentSessionId = ref(props.sessionId)

/** 将 props 转为可响应的内部状态 */
const currentGoodsId = ref(parseOptionalNumber(props.goodsId))
const currentCtId = ref(parseOptionalNumber(props.ctId))

// 临时 ID 计数器（用于前端追踪本地发送中的消息）
let tempIdCounter = 0

/** 生成临时消息 ID */
function genTempId() {
  return `temp_${++tempIdCounter}_${Date.now()}`
}

/** 解析可选数字（props 可能是空字符串） */
function parseOptionalNumber(value) {
  if (value === '' || value === null || value === undefined) return null
  return Number(value)
}

/** 滚动到底部（等待下一次 DOM 更新完成后执行） */
function scrollToBottom() {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

/** 判断消息是否为当前用户发送 */
function isMyMessage(msg) {
  return msg.senderType === authStore.userType || msg.isMine
}

/** 按创建时间升序排列消息 */
function sortMessages() {
  messages.value = [...messages.value].sort((a, b) => {
    const aTime = a.createdAt ? new Date(a.createdAt).getTime() : 0
    const bTime = b.createdAt ? new Date(b.createdAt).getTime() : 0
    return aTime - bTime
  })
}

/** 把消息追加到列表并触发排序和滚动 */
function appendMessage(msg) {
  messages.value.push(msg)
  sortMessages()
  scrollToBottom()
}

// ---------------------------------------------------------------
// 历史消息加载
// ---------------------------------------------------------------

/**
 * 获取全部历史消息（一次性拉取，不做分页，适用于消息量较小的会话）。
 * 先用 pageSize=1 探出 totalElements，再拉全部。
 */
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

/** 首次进入时加载全部历史消息（不依赖 WS 连接） */
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

/**
 * 断线重连时增量合并历史消息：
 * 保留本地已有消息，只把本地最后一条消息之后的新消息追加进来，
 * 避免重复加载已展示的消息。
 */
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
      // 已有的消息跳过
      if (msg.id != null && existingIds.has(msg.id)) return false
      // 无 createdAt 的消息直接追加（防御性处理）
      if (!msg.createdAt) return true
      return new Date(msg.createdAt).getTime() > lastLocalTime
    })
    if (incremental.length === 0) return
    incremental.forEach((msg) => messages.value.push({ ...msg, isMine: isMyMessage(msg) }))
    sortMessages()
    scrollToBottom()
  } catch {
    // 增量拉取失败不影响已展示的消息，下次重连再重试
  }
}

// ---------------------------------------------------------------
// 消息已读
// ---------------------------------------------------------------

/**
 * 标记当前会话消息为已读。
 * 成功后派发全局事件，通知 SessionList 等组件刷新未读数。
 */
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

// ---------------------------------------------------------------
// 待确认消息队列（FIFO）
// ---------------------------------------------------------------

/**
 * 已发送但尚未收到服务端回执的消息队列（按发送顺序排队）。
 *
 * 后端消息里的 messageId 是服务端生成的唯一标识，
 * WS 发送协议本身不支持客户端回传"临时 ID"。
 * 因此用"先发先回"的 FIFO 顺序匹配 SUCCESS/SESSION_CREATED/ERROR 回执，
 * 而不是比较一个后端根本不会回传的 tempId。
 */
const pendingSendQueue = []

/**
 * 根据服务端回执更新本地消息状态。
 *
 * @param data   服务端回执数据
 * @param isError 是否为错误回执
 */
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

// ---------------------------------------------------------------
// WS 消息处理
// ---------------------------------------------------------------

/**
 * 处理从 WebSocket 收到的消息。
 *
 * 处理逻辑：
 * - SESSION_CREATED：建立会话，更新 sessionId，通知父组件更新 URL
 * - SUCCESS：更新待确认队列中对应消息的状态
 * - ERROR：标记消息发送失败，提示用户
 * - DELIVERED / RECEIVED：服务端内部流转状态，不做展示
 * - 其他：对方实时推来的消息（完整 MessageResponse），追加到列表
 */
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
  // 对方实时推来的消息
  if (data.senderType && data.senderType !== authStore.userType) {
    // 防止重复追加（服务端可能重复推送）
    if (data.id != null && messages.value.some((m) => m.id === data.id)) return
    appendMessage({ ...data, isMine: false })
  }
}

// ---------------------------------------------------------------
// 发送消息
// ---------------------------------------------------------------

/**
 * 追加一条本地发送消息并发送到 WebSocket。
 * WS 发送协议字段统一用 "content"。
 *
 * 流程：
 * 1. 生成临时 ID，构造待发送消息
 * 2. 追加到消息列表（state=SENDING）
 * 3. 清空输入框
 * 4. 调用 sendMessage，发送成功则进入待确认队列；失败则直接标记 ERROR
 */
function appendOutgoingMessage(text) {
  const tempId = genTempId()
  const payload = currentSessionId.value
      ? { sessionId: Number(currentSessionId.value), content: text }
      : { goodsId: currentGoodsId.value, ctId: currentCtId.value, content: text }

  appendMessage({
    tempId,
    content: text,
    senderType: authStore.userType,
    isMine: true,
    state: 'SENDING',
    createdAt: new Date().toISOString()
  })

  inputText.value = ''

  const sent = sendMessage(payload)
  if (sent) {
    pendingSendQueue.push(tempId)
  } else {
    // WebSocket 当前不可用，直接标记失败
    const target = messages.value.find((msg) => msg.tempId === tempId)
    if (target) target.state = 'ERROR'
    ElMessage.error('发送失败，请重试')
  }
}

/**
 * 处理发送按钮/回车事件。
 */
function handleSend() {
  const text = inputText.value.trim()
  if (!text) return

  // 新建会话场景：必须同时有 goodsId 和 ctId
  if (!currentSessionId.value && (currentGoodsId.value === null || currentCtId.value === null)) {
    ElMessage.error('缺少商品或商户信息，无法发送消息')
    return
  }

  appendOutgoingMessage(text)
}

// ---------------------------------------------------------------
// 标题解析
// ---------------------------------------------------------------

const partnerName = ref('')
const goodsName = ref('')

/** 显示标题：优先用异步查到的真实名字，兜底用父组件传的 title */
const displayTitle = computed(() => {
  const base = partnerName.value || props.title
  return goodsName.value ? `${base}（咨询商品：${goodsName.value}）` : base
})

/**
 * 会话详情缓存（避免同一 sessionId 被重复请求）。
 */
let sessionDetailPromise = null

/** 获取会话详情（带缓存） */
function fetchSessionDetail() {
  if (!currentSessionId.value) return Promise.resolve(null)
  if (!sessionDetailPromise) {
    sessionDetailPromise = sessionApi.detail(currentSessionId.value).catch(() => null)
  }
  return sessionDetailPromise
}

/**
 * 解析对方名称并更新标题。
 * 用户端：ctId 大多数情况从路由 query 直接拿到；从会话列表点进来时 query 里没有，
 * 需要先查一次会话详情。
 * 商户端：永远从 sessionId 进入，只能查会话详情获取 userId。
 */
async function resolvePartnerName() {
  try {
    if (authStore.userType === 'USER') {
      let ctId = currentCtId.value
      if (ctId == null && currentSessionId.value) {
        const detail = await fetchSessionDetail()
        ctId = detail?.ctId ?? null
      }
      if (ctId == null) return
      const name = await commercialTenantApi.name(ctId)
      if (name) partnerName.value = name
    } else if (authStore.userType === 'TENANT') {
      if (!currentSessionId.value) return
      const detail = await fetchSessionDetail()
      if (detail?.userId == null) return
      const name = await userApi.name(detail.userId)
      if (name) partnerName.value = name
    }
  } catch {
    // 查不到名字就用兜底标题，不影响聊天主流程
  }
}

/**
 * 解析商品名称。
 * 用户端从路由 query 直接拿到，商户端从会话详情查。
 */
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

// ---------------------------------------------------------------
// 新会话场景：检查历史会话
// ---------------------------------------------------------------

/**
 * 用户端点"咨询"进入时，可能之前就与该商户就同一商品聊过。
 * 主动查询自己的会话列表，若已有该 goodsId+ctId 的会话，直接复用其 sessionId
 * 去加载历史，不用等发消息才建立会话。
 */
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
    // 查询失败就当作没有历史会话，不影响正常使用
  }
}

// ---------------------------------------------------------------
// 生命周期
// ---------------------------------------------------------------

onMounted(async () => {
  // 检查是否已有历史会话（用户端新建入口场景）
  await resolveExistingSession()

  // 异步加载标题（不 await，不阻塞聊天主流程）
  resolvePartnerName()
  resolveGoodsName()

  // 历史消息通过 REST 直接加载，不依赖 WS 连接是否建立
  if (currentSessionId.value) {
    await loadHistory()
  }

  // 初始化 WebSocket
  initChatWs({
    onOpen: async () => {
      if (!currentSessionId.value) return
      // 首次加载已在上面完成，这里只处理断线重连场景的增量合并
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
  flex-shrink: 0;
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
  min-height: 0;
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
