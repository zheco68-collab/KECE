import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'
import { useUserStore } from './user'

describe('user store', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
  })

  it('初始状态无 token 与用户信息', () => {
    const store = useUserStore()
    expect(store.token).toBeNull()
    expect(store.userInfo).toBeNull()
  })

  it('setToken 写入并同步 localStorage', () => {
    const store = useUserStore()
    store.setToken('test-token')
    expect(store.token).toBe('test-token')
    expect(localStorage.getItem('token')).toBe('test-token')
  })

  it('clearAuth 清除 token 与用户信息', () => {
    const store = useUserStore()
    store.setToken('test-token')
    store.clearAuth()
    expect(store.token).toBeNull()
    expect(store.userInfo).toBeNull()
    expect(localStorage.getItem('token')).toBeNull()
  })
})
