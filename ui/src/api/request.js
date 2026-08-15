import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.request.use((config) => {
  const authStore = useAuthStore()
  if (authStore.token) {
    config.headers.Authorization = authStore.token
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 0) {
      return res.data
    }
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(res)
  },
  (error) => {
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
