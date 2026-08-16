<template>
  <span>{{ timeText }}</span>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { formatRelativeTime } from '@/utils/dayjs'

const props = defineProps({
  date: { type: String, default: '' }
})

const timeText = ref('')
let timer = null

function refresh() {
  timeText.value = formatRelativeTime(props.date)
}

onMounted(() => {
  refresh()
  timer = setInterval(refresh, 60000)
})

onUnmounted(() => {
  clearInterval(timer)
})

watch(() => props.date, refresh)
</script>
