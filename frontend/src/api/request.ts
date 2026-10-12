import axios, { type AxiosInstance, type AxiosResponse } from 'axios'
import { useUserStore } from '@/stores/user'

export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
  timestamp: number
}

const request: AxiosInstance = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
})

// 请求拦截：附加 JWT
request.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

// 响应拦截：解包统一响应体
request.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const body = response.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 200) {
        return body.data as never
      }
      // 业务错误
      if (body.code === 4002) {
        const userStore = useUserStore()
        userStore.clearAuth()
      }
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return response.data as never
  },
  (error) => {
    return Promise.reject(error)
  },
)

export default request
