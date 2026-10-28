/// <reference types="vite/client" />

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    icon?: string
    hidden?: boolean
    cache?: boolean
  }
}

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

interface ImportMetaEnv {
  readonly VITE_APP_TITLE: string
  readonly VITE_APP_BASE_API: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

/** 统一返回体 */
export interface R<T = unknown> {
  code: number
  msg: string
  data: T
  traceId: string
}

/** 分页返回 */
export interface PageResult<T> {
  total: number
  list: T[]
}

/** 登录返回 */
export interface LoginVO {
  accessToken: string
  refreshToken: string
  expiresIn: number
  tokenType: string
}

export interface UserInfo {
  userId: string
  username: string
  nickname: string
  deptId: string | null
  superAdmin: boolean
  roles: string[]
  perms: string[]
}

/** 后端返回的路由节点 */
export interface BackendRoute {
  name: string
  path: string
  component: string | null
  redirect?: string
  meta: {
    title: string
    icon?: string | null
    hidden?: boolean
    cache?: boolean
  }
  children?: BackendRoute[]
}
