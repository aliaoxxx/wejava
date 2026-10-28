import { defineStore } from 'pinia'
import { getMe, login as loginApi, logout as logoutApi } from '@/api/auth'
import { clearToken, getAccessToken, setToken } from '@/utils/auth'
import type { UserInfo } from '@/types/global'

export const useUserStore = defineStore('user', () => {
  const userInfo = ref<UserInfo | null>(null)
  const loaded = ref(false)

  const roles = computed(() => userInfo.value?.roles ?? [])
  const perms = computed(() => userInfo.value?.perms ?? [])

  function isLoggedIn() {
    return !!getAccessToken()
  }

  async function login(form: { username: string; password: string; captchaKey: string; captchaCode: string }) {
    const { data } = await loginApi(form)
    setToken(data.accessToken, data.refreshToken)
  }

  async function fetchInfo() {
    const { data } = await getMe()
    userInfo.value = data
    loaded.value = true
  }

  async function logout() {
    try {
      await logoutApi()
    } finally {
      await reset()
    }
  }

  async function reset() {
    clearToken()
    userInfo.value = null
    loaded.value = false
    const { usePermissionStore } = await import('@/store/modules/permission')
    usePermissionStore().reset()
  }

  return { userInfo, loaded, roles, perms, isLoggedIn, login, fetchInfo, logout, reset }
})
