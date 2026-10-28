const ACCESS_KEY = 'wejava:access_token'
const REFRESH_KEY = 'wejava:refresh_token'

export function getAccessToken(): string {
  return localStorage.getItem(ACCESS_KEY) || ''
}

export function getRefreshToken(): string {
  return localStorage.getItem(REFRESH_KEY) || ''
}

export function setToken(accessToken: string, refreshToken: string) {
  localStorage.setItem(ACCESS_KEY, accessToken)
  localStorage.setItem(REFRESH_KEY, refreshToken)
}

export function clearToken() {
  localStorage.removeItem(ACCESS_KEY)
  localStorage.removeItem(REFRESH_KEY)
}
