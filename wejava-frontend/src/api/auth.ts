import { http } from '@/utils/http'
import type { BackendRoute, LoginVO, UserInfo } from '@/types/global'

export interface CaptchaVO {
  captchaKey: string
  captchaImage: string
}

export function getCaptcha() {
  return http.get<CaptchaVO>('/auth/captcha')
}

export function login(data: { username: string; password: string; captchaKey?: string; captchaCode?: string }) {
  return http.post<LoginVO>('/auth/login', data)
}

export function refreshToken(data: { refreshToken: string }) {
  return http.post<LoginVO>('/auth/refresh', data)
}

export function logout() {
  return http.post<void>('/auth/logout')
}

export function getMe() {
  return http.get<UserInfo>('/auth/me')
}

export function getRouters() {
  return http.get<BackendRoute[]>('/auth/routers')
}
