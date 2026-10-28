import request from '@/utils/request'
import type { BackendRoute, LoginVO, R, UserInfo } from '@/types/global'

export interface CaptchaVO {
  captchaKey: string
  captchaImage: string
}

export function getCaptcha() {
  return request.get<any, R<CaptchaVO>>('/auth/captcha')
}

export function login(data: { username: string; password: string; captchaKey?: string; captchaCode?: string }) {
  return request.post<any, R<LoginVO>>('/auth/login', data)
}

export function refreshToken(data: { refreshToken: string }) {
  return request.post<any, R<LoginVO>>('/auth/refresh', data)
}

export function logout() {
  return request.post<any, R<void>>('/auth/logout')
}

export function getMe() {
  return request.get<any, R<UserInfo>>('/auth/me')
}

export function getRouters() {
  return request.get<any, R<BackendRoute[]>>('/auth/routers')
}
