import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import router from '@/router'

/**
 * 认证状态管理。
 *
 * 设计说明：
 * 用户端和商户端分别维护独立的登录态（token 存在不同的 localStorage key 下），
 * 互不覆盖，允许同时保持登录。
 *
 * useAuthStore()：通用入口，根据当前路由路径自动判断身份类型。
 * useUserAuthStore()：用户端专用（登录页使用）
 * useMerchantAuthStore()：商户端专用（登录页使用）
 */

/**
 * 创建认证 Store 工厂。
 *
 * @param {string} storeId   Pinia store ID
 * @param {string} storagePrefix localStorage key 前缀
 */
function createAuthStore(storeId, storagePrefix) {
  const tokenKey = `${storagePrefix}_token`
  const userInfoKey = `${storagePrefix}_userInfo`
  const userTypeKey = `${storagePrefix}_userType`

  return defineStore(storeId, () => {
    const token = ref(localStorage.getItem(tokenKey) || '')
    const userInfo = ref(JSON.parse(localStorage.getItem(userInfoKey) || 'null'))
    const userType = ref(localStorage.getItem(userTypeKey) || '')

    /** 是否已登录 */
    const isLoggedIn = computed(() => !!token.value)

    /**
     * 保存登录态到响应式变量和 localStorage。
     *
     * @param {string} newToken  新 token
     * @param {Object} info      用户信息
     * @param {string} type     身份类型（'USER' 或 'TENANT'）
     */
    function setAuth(newToken, info, type) {
      token.value = newToken
      userInfo.value = info
      userType.value = type
      localStorage.setItem(tokenKey, newToken)
      localStorage.setItem(userInfoKey, JSON.stringify(info))
      localStorage.setItem(userTypeKey, type)
    }

    /** 清除登录态 */
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

/** 用户端登录态，固定 userType = 'USER' */
export const useUserAuthStore = createAuthStore('userAuth', 'user')

/** 商户端登录态，固定 userType = 'TENANT' */
export const useMerchantAuthStore = createAuthStore('merchantAuth', 'merchant')

/**
 * 通用认证 Store：根据当前路由路径判断身份类型并返回对应的 Store 实例。
 * 供业务页面（Layout、View、组件）使用，无需关心当前在哪个分支。
 *
 * 注意：登录页 /login 不属于 /user 或 /merchant 任何一个分支，
 * 两个表单必须分别显式调用 useUserAuthStore / useMerchantAuthStore，
 * 不能用这个自动判断入口。
 */
export function useAuthStore() {
  const path = router.currentRoute.value.path
  return path.startsWith('/merchant') ? useMerchantAuthStore() : useUserAuthStore()
}
