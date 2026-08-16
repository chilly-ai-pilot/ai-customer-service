import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'

/** 创建 axios 实例，baseURL 指向后端服务地址 */
const request = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 10000
})

/** 请求拦截器：从 authStore 注入 token */
request.interceptors.request.use((config) => {
  const authStore = useAuthStore()
  if (authStore.token) {
    config.headers.Authorization = authStore.token
  }
  return config
})

/** 响应拦截器：处理成功和错误，统一返回 data 或抛出错误 */
request.interceptors.response.use(
  (response) => {
    const res = response.data
    // 后端约定 code=0 表示成功
    if (res.code === 0) {
      return res.data
    }
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(res)
  },
  (error) => {
    // 401 未登录或 token 过期：清除登录态并跳转登录页
    if (error.response?.status === 401 || error.response?.data?.code === 10401) {
      const authStore = useAuthStore()
      authStore.clearAuth()
      router.push('/login')
    }
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default request
