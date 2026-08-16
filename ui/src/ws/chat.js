import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const CLOSE_CODE_REPLACED = 4001
const RECONNECT_BASE_DELAY = 1000
const RECONNECT_MAX_RETRIES = 5

let ws = null
let reconnectTimer = null
let reconnectAttempts = 0
let onMessageCallback = null
let onOpenCallback = null
let onCloseCallback = null

function buildWsUrl() {
  const authStore = useAuthStore()
  const { userType, userInfo, token } = authStore
  if (userType === 'USER') {
    return `ws://localhost:8080/user/chat/${userInfo.id}?token=${token}`
  } else {
    return `ws://localhost:8080/commercialTenant/chat/${userInfo.id}?token=${token}`
  }
}

function connect() {
  if (ws && (ws.readyState === WebSocket.CONNECTING || ws.readyState === WebSocket.OPEN)) {
    return
  }
  const url = buildWsUrl()
  ws = new WebSocket(url)

  ws.onopen = () => {
    reconnectAttempts = 0
    clearTimeout(reconnectTimer)
    onOpenCallback?.()
  }

  ws.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data)
      onMessageCallback?.(data)
    } catch {
      // ignore non-json messages (e.g. pong frames)
    }
  }

  ws.onclose = (event) => {
    if (event.code === CLOSE_CODE_REPLACED) {
      ElMessage.warning('该账号已在别处登录')
      useAuthStore().clearAuth()
      window.location.href = '/login'
      return
    }
    onCloseCallback?.()
    scheduleReconnect()
  }

  ws.onerror = () => {
    // onclose will follow; nothing extra needed here
  }
}

function scheduleReconnect() {
  if (reconnectAttempts >= RECONNECT_MAX_RETRIES) return
  const delay = RECONNECT_BASE_DELAY * Math.pow(2, reconnectAttempts)
  reconnectAttempts++
  reconnectTimer = setTimeout(connect, delay)
}

export function initChatWs({ onMessage, onOpen, onClose }) {
  onMessageCallback = onMessage
  onOpenCallback = onOpen
  onCloseCallback = onClose
  connect()
}

export function sendMessage(payload) {
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify(payload))
    return true
  }
  return false
}

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
