import type { AxiosRequestConfig } from 'axios'
import request from './request'

/** 后端统一响应体 */
export interface ApiResult<T = unknown> {
  code: number
  msg: string
  data: T
  traceId?: string
}

/** 分页响应体 */
export interface PageData<T = Record<string, unknown>> {
  total: number
  list: T[]
}

/**
 * 类型化的请求封装。
 * <p>
 * 业务 api 文件直接用 http.get&lt;PageData&lt;XxxItem&gt;&gt;(...)，
 * 不必再写 request.get&lt;any, any&gt; 这种双泛型。
 */
export const http = {
  get: <T = unknown>(url: string, config?: AxiosRequestConfig) =>
    request.get<unknown, ApiResult<T>>(url, config),

  post: <T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig) =>
    request.post<unknown, ApiResult<T>>(url, data, config),

  put: <T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig) =>
    request.put<unknown, ApiResult<T>>(url, data, config),

  delete: <T = unknown>(url: string, config?: AxiosRequestConfig) =>
    request.delete<unknown, ApiResult<T>>(url, config)
}
