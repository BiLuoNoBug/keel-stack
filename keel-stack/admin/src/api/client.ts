import axios, { type AxiosRequestConfig } from 'axios'
import { message } from 'ant-design-vue'

const TOKEN_KEY = 'keel_admin_token'

export const getToken = () => localStorage.getItem(TOKEN_KEY) || ''
export const setToken = (token: string) => localStorage.setItem(TOKEN_KEY, token)
export const clearToken = () => localStorage.removeItem(TOKEN_KEY)

const instance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 20000
})

instance.interceptors.request.use((config) => {
  const token = getToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

instance.interceptors.response.use(
  (res) => {
    const body = res.data
    // 后端统一格式 { code, message, data }，这里就地把 data 拆出来
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 0) return body.data
      message.error(body.message || '请求失败')
      return Promise.reject(new Error(body.message))
    }
    return body
  },
  (error) => {
    const status = error.response?.status
    const text = error.response?.data?.message || error.message || '网络异常'
    if (status === 401) {
      // token 过期：清掉本地登录态并回登录页，避免页面反复报错
      clearToken()
      if (!location.pathname.startsWith('/login')) {
        message.warning('登录已过期，请重新登录')
        location.replace('/login')
      }
    } else {
      message.error(text)
    }
    return Promise.reject(error)
  }
)

/** 拦截器已拆包，这里把返回类型补回来，调用方直接拿到 T 而不是 AxiosResponse */
export const http = instance as unknown as {
  get<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T>
  post<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>
  put<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>
  patch<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>
  delete<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T>
}
