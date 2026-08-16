<template>
  <span>{{ timeText }}</span>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { formatRelativeTime } from '@/utils/dayjs'

const props = defineProps({
  /** ISO 日期字符串或 Date 对象 */
  date: { type: String, default: '' }
})

const timeText = ref('')
let timer = null

// 刷新相对时间文本
function refresh() {
  timeText.value = formatRelativeTime(props.date)
}

onMounted(() => {
  refresh()
  // 每分钟刷新一次，保证"刚刚"、"N分钟前"等动态文本实时更新
  timer = setInterval(refresh, 60000)
})

onUnmounted(() => {
  clearInterval(timer)
})

// date prop 变化时重新计算
watch(() => props.date, refresh)
</script>
