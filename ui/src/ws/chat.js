import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

/** 服务端主动关闭连接时的自定义关闭码：表示"该账号已在别处登录" */
const CLOSE_CODE_REPLACED = 4001
/** 重连基础等待时间（毫秒），后续每次指数退避 */
const RECONNECT_BASE_DELAY = 1000
/** 最大重连次数，达到后停止重连 */
const RECONNECT_MAX_RETRIES = 5

/** 当前 WebSocket 连接实例 */
let ws = null
/** 重连定时器句柄 */
let reconnectTimer = null
/** 已尝试重连次数 */
let reconnectAttempts = 0

/** 回调函数（由调用方通过 initChatWs 注入） */
let onMessageCallback = null
let onOpenCallback = null
let onCloseCallback = null

/**
 * 根据当前登录身份构建 WS 连接 URL。
 * 用户端连接 /user/chat/{userId}，商户端连接 /commercialTenant/chat/{ctId}。
 */
function buildWsUrl() {
  const authStore = useAuthStore()
  const { userType, userInfo, token } = authStore
  if (userType === 'USER') {
    return `ws://localhost:8080/user/chat/${userInfo.id}?token=${token}`
  } else {
    return `ws://localhost:8080/commercialTenant/chat/${userInfo.id}?token=${token}`
  }
}

/**
 * 建立 WebSocket 连接。
 * 若已有连接（CONNECTING 或 OPEN 状态）则直接返回，不重复建立。
 */
function connect() {
  if (ws && (ws.readyState === WebSocket.CONNECTING || ws.readyState === WebSocket.OPEN)) {
    return
  }

  const url = buildWsUrl()
  ws = new WebSocket(url)

  // 连接建立成功：重置重连计数，清除定时器，通知调用方
  ws.onopen = () => {
    reconnectAttempts = 0
    clearTimeout(reconnectTimer)
    onOpenCallback?.()
  }

  // 收到消息：尝试 JSON 解析，解析成功则派发给 onMessageCallback。
  // 非 JSON 消息（如 WebSocket 原生 Pong 帧）直接忽略，不触发任何回调。
  ws.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data)
      onMessageCallback?.(data)
    } catch {
      // ignore non-json messages (e.g. pong frames)
    }
  }

  // 连接关闭：区分"被顶替"（code 4001）和其他原因
  ws.onclose = (event) => {
    if (event.code === CLOSE_CODE_REPLACED) {
      // 服务端通知：同一账号在别处登录，清除本地登录态并跳转登录页
      ElMessage.warning('该账号已在别处登录')
      useAuthStore().clearAuth()
      window.location.href = '/login'
      return
    }
    // 非顶替原因：通知调用方并触发自动重连
    onCloseCallback?.()
    scheduleReconnect()
  }

  // onerror 之后 always 跟着 onclose，不需要在 error 回调里做额外处理
  ws.onerror = () => {
    // onclose will follow; nothing extra needed here
  }
}

/**
 * 指数退避重连调度。
 * 每次等待 RECONNECT_BASE_DELAY * 2^attemptCount 毫秒后重新连接。
 * 达到最大重连次数后停止。
 */
function scheduleReconnect() {
  if (reconnectAttempts >= RECONNECT_MAX_RETRIES) return
  const delay = RECONNECT_BASE_DELAY * Math.pow(2, reconnectAttempts)
  reconnectAttempts++
  reconnectTimer = setTimeout(connect, delay)
}

/**
 * 初始化聊天 WebSocket：注册回调并建立连接。
 *
 * @param {Object} options
 * @param {Function} options.onMessage 收到消息回调
 * @param {Function} options.onOpen   连接建立回调
 * @param {Function} options.onClose  连接关闭回调
 */
export function initChatWs({ onMessage, onOpen, onClose }) {
  onMessageCallback = onMessage
  onOpenCallback = onOpen
  onCloseCallback = onClose
  connect()
}

/**
 * 通过 WebSocket 发送消息。
 *
 * @param {Object} payload 要发送的消息体
 * @returns {boolean} 是否发送成功
 */
export function sendMessage(payload) {
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify(payload))
    return true
  }
  return false
}

/**
 * 销毁 WebSocket 连接，清理所有状态。
 * 用于组件卸载（onUnmounted）时调用，防止组件销毁后仍有回调引用。
 */
export function destroyChatWs() {
  clearTimeout(reconnectTimer)
  reconnectAttempts = RECONNECT_MAX_RETRIES
  if (ws) {
    ws.onclose = null
    ws.close()
    ws = null
  }
  onMessageCallback = null
  onOpenCallback = null
  onCloseCallback = null
}
