<template>
  <div class="user-chat-window-view">
    <ChatWindow
      :session-id="route.params.sessionId || ''"
      :goods-id="route.query.goodsId || ''"
      :ct-id="route.query.ctId || ''"
      :title="chatTitle"
      @session-created="handleSessionCreated"
    />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ChatWindow from '@/components/ChatWindow.vue'

const route = useRoute()
const router = useRouter()

// 聊天窗口标题：默认用父组件传的 title，等异步查到对方真实名字后自动替换
const chatTitle = computed(() => {
  const ctId = route.query.ctId
  const goodsId = route.query.goodsId
  if (ctId || goodsId) {
    const parts = []
    if (ctId) parts.push(`#${ctId}`)
    if (goodsId) parts.push(`商品#${goodsId}`)
    return parts.join(' - ')
  }
  return '会话详情'
})

/**
 * SESSION_CREATED 后把地址栏更新为带 sessionId 的路径。
 * 同一命名路由（UserChatWindow）只是替换 params，不会重新挂载组件，
 * WebSocket 连接得以保留。
 */
function handleSessionCreated(sessionId) {
  router.replace({ name: 'UserChatWindow', params: { sessionId } })
}
</script>

<style scoped>
.user-chat-window-view {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
</style>
