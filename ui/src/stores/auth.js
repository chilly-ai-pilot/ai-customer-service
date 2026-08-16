import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import router from '@/router'

// 原来只有一份 token/userInfo/userType，存在固定的 localStorage key 下，
// 用户号和商户号谁后登录就会把谁的登录态覆盖掉，没法同时保持登录。
// 这里按身份拆成两个独立的 store，各自用不同前缀的 key 存储，互不覆盖。
function createAuthStore(storeId, storagePrefix) {
  const tokenKey = `${storagePrefix}_token`
  const userInfoKey = `${storagePrefix}_userInfo`
  const userTypeKey = `${storagePrefix}_userType`

  return defineStore(storeId, () => {
    const token = ref(localStorage.getItem(tokenKey) || '')
    const userInfo = ref(JSON.parse(localStorage.getItem(userInfoKey) || 'null'))
    const userType = ref(localStorage.getItem(userTypeKey) || '')

    const isLoggedIn = computed(() => !!token.value)

    function setAuth(newToken, info, type) {
      token.value = newToken
      userInfo.value = info
      userType.value = type
      localStorage.setItem(tokenKey, newToken)
      localStorage.setItem(userInfoKey, JSON.stringify(info))
      localStorage.setItem(userTypeKey, type)
    }

    function clearAuth() {
      token.value = ''
      userInfo.value = null
      userType.value = ''
      localStorage.removeItem(tokenKey)
      localStorage.removeItem(userInfoKey)
      localStorage.removeItem(userTypeKey)
    }

    return { token, userInfo, userType, isLoggedIn, setAuth, clearAuth }
  })
}

// 用户号登录态，固定 userType = 'USER'
export const useUserAuthStore = createAuthStore('userAuth', 'user')
// 商户号登录态，固定 userType = 'TENANT'
export const useMerchantAuthStore = createAuthStore('merchantAuth', 'merchant')

// 当前生效的登录态：按当前路由处在 /user 还是 /merchant 分支来决定用哪一份，
// 这样组件、request 拦截器、WebSocket 里原来的 useAuthStore() 调用完全不用改，
// 各自页面天然只会用到自己那份 token，用户号和商户号可以同时保持登录。
// 注意：登录页 /login 不属于任何一个分支，两个表单要分别显式用
// useUserAuthStore / useMerchantAuthStore，不能用这个通用入口。
export function useAuthStore() {
  const path = router.currentRoute.value.path
  return path.startsWith('/merchant') ? useMerchantAuthStore() : useUserAuthStore()
}