import { http } from './request'
import type { AuthResult, UserInfo } from '@/types'

export interface RegisterPayload {
  username: string
  password: string
  email?: string
}

export interface LoginPayload {
  username: string
  password: string
}

export const authApi = {
  register: (payload: RegisterPayload) => http.post<AuthResult>('/auth/register', payload),
  login: (payload: LoginPayload) => http.post<AuthResult>('/auth/login', payload),
  logout: () => http.post<void>('/auth/logout'),
  me: () => http.get<UserInfo>('/auth/me'),
}
