<template>
  <div class="merchant-chat-window-view">
    <ChatWindow
      :session-id="route.params.sessionId"
      :title="`会话 #${route.params.sessionId}`"
    />
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router'
import ChatWindow from '@/components/ChatWindow.vue'

const route = useRoute()
</script>

<style scoped>
.merchant-chat-window-view {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
</style>
