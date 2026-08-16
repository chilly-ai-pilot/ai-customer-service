<template>
  <div class="user-chat-window-view">
    <ChatWindow
      :session-id="route.params.sessionId || ''"
      :goods-id="route.query.goodsId || ''"
      :ct-id="route.query.ctId || ''"
      :title="chatTitle"
    />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import ChatWindow from '@/components/ChatWindow.vue'

const route = useRoute()

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
</script>

<style scoped>
.user-chat-window-view {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
</style>
