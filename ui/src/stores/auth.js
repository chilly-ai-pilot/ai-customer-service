import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))
  const userType = ref(localStorage.getItem('userType') || '')

  const isLoggedIn = computed(() => !!token.value)

  function setAuth(newToken, info, type) {
    token.value = newToken
    userInfo.value = info
    userType.value = type
    localStorage.setItem('token', newToken)
    localStorage.setItem('userInfo', JSON.stringify(info))
    localStorage.setItem('userType', type)
  }

  function clearAuth() {
    token.value = ''
    userInfo.value = null
    userType.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    localStorage.removeItem('userType')
  }

  return { token, userInfo, userType, isLoggedIn, setAuth, clearAuth }
})
