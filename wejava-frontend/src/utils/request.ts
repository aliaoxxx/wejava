import axios, { type AxiosRequestConfig, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { clearToken, getAccessToken, setToken } from '@/utils/auth'

const service = axios.create({
  baseURL: import.meta.env.VITE_APP_BASE_API,
  timeout: 15000
})

/** 刷新专用实例：不走拦截器，避免 401 递归 */
const refreshClient = axios.create({
  baseURL: import.meta.env.VITE_APP_BASE_API,
  timeout: 15000
})

let isRefreshing = false
let waitingQueue: Array<(token: string) => void> = []

service.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

service.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 200) return res
    ElMessage.error(res.msg || '操作失败')
    return Promise.reject(new Error(res.msg || 'error'))
  },
  async (error) => {
    const { response, config } = error
    if (!response) {
      ElMessage.error('网络异常，请稍后重试')
      return Promise.reject(error)
    }

    // 1005 过期 → 静默刷新；1006 失效 → 跳登录
    if (response.status === 401) {
      const code = response.data?.code
      const retryConfig = config as AxiosRequestConfig & { _retry?: boolean }

      if (code === 1005 && !retryConfig._retry) {
        retryConfig._retry = true
        const refreshToken = localStorage.getItem('wejava:refresh_token')

        if (refreshToken) {
          if (isRefreshing) {
            return new Promise((resolve) => {
              waitingQueue.push((token: string) => {
                retryConfig.headers = { ...retryConfig.headers, Authorization: `Bearer ${token}` }
                resolve(service(retryConfig))
              })
            })
          }
          isRefreshing = true
          try {
            const { data } = await refreshClient.post('/auth/refresh', { refreshToken })
            setToken(data.data.accessToken, data.data.refreshToken)
            waitingQueue.forEach((cb) => cb(data.data.accessToken))
            waitingQueue = []
            retryConfig.headers = { ...retryConfig.headers, Authorization: `Bearer ${data.data.accessToken}` }
            return service(retryConfig)
          } catch (e) {
            waitingQueue = []
            clearToken()
            await router.push('/login')
            return Promise.reject(e)
          } finally {
            isRefreshing = false
          }
        }
      }

      waitingQueue = []
      clearToken()
      await router.push('/login')
      return Promise.reject(error)
    }

    // 403 不跳登录页，否则死循环
    if (response.status === 403) {
      ElMessage.error('无访问权限')
      return Promise.reject(error)
    }

    ElMessage.error(response.data?.msg || `请求失败（${response.status}）`)
    return Promise.reject(error)
  }
)

export default service
