import { profileApi } from '@/api/profile'

const userCache = new Map<number, Promise<{ username: string; avatarUrl: string | null }>>()

/**
 * 获取用户信息缓存，根据 userId 异步拉取出题人用户名与头像，避免多次重复请求。
 */
export function fetchUserInfo(userId: number): Promise<{ username: string; avatarUrl: string | null }> {
  if (userCache.has(userId)) {
    return userCache.get(userId)!
  }
  const promise = profileApi
    .getProfile(userId)
    .then((p) => ({
      username: p.username,
      avatarUrl: p.avatarUrl || null,
    }))
    .catch(() => ({
      username: '教师',
      avatarUrl: null,
    }))
  userCache.set(userId, promise)
  return promise
}
