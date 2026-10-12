import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { AuthResult, UserInfo } from '@/types'

const TOKEN_KEY = 'token'
const REFRESH_KEY = 'refreshToken'
const USER_KEY = 'userInfo'

function readStoredUser(): UserInfo | null {
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? (JSON.parse(raw) as UserInfo) : null
  } catch {
    return null
  }
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string | null>(localStorage.getItem(TOKEN_KEY))
  const refreshToken = ref<string | null>(localStorage.getItem(REFRESH_KEY))
  const userInfo = ref<UserInfo | null>(readStoredUser())

  const isLoggedIn = computed(() => !!token.value)

  function setToken(t: string) {
    token.value = t
    localStorage.setItem(TOKEN_KEY, t)
  }

  function setAuth(auth: AuthResult) {
    token.value = auth.token
    refreshToken.value = auth.refreshToken
    userInfo.value = auth.user
    localStorage.setItem(TOKEN_KEY, auth.token)
    localStorage.setItem(REFRESH_KEY, auth.refreshToken)
    localStorage.setItem(USER_KEY, JSON.stringify(auth.user))
  }

  function clearAuth() {
    token.value = null
    refreshToken.value = null
    userInfo.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(REFRESH_KEY)
    localStorage.removeItem(USER_KEY)
  }

  function hasRole(role: string): boolean {
    return !!userInfo.value?.roles?.includes(role)
  }

  return { token, refreshToken, userInfo, isLoggedIn, setToken, setAuth, clearAuth, hasRole }
})
