<template>
  <div class="user-chat-view">
    <div class="page-header">
      <h2>我的咨询</h2>
    </div>
    <SessionList type="USER" />
  </div>
</template>

<script setup>
import SessionList from '@/components/SessionList.vue'
</script>

<style scoped>
.user-chat-view {
  height: 100%;
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.page-header {
  padding: 20px 20px 0;
}

.page-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: bold;
  color: #333;
}
</style>
