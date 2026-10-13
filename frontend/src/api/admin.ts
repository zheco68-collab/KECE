import { http } from './request'
import type { PageResult } from '@/types'

export interface AdminUser {
  id: number
  username: string
  email: string | null
  roles: string[]
  status: number
  createdAt: string
}

export interface AdminUserListParams {
  keyword?: string
  role?: string
  status?: number
  page?: number
  size?: number
}

export const adminApi = {
  listUsers: (params: AdminUserListParams) =>
    http.get<PageResult<AdminUser>>('/admin/users', { params }),
  updateUserRoles: (id: number, roles: string[]) =>
    http.put<AdminUser>(`/admin/users/${id}/roles`, { roles }),
  updateUserStatus: (id: number, status: number) =>
    http.put<AdminUser>(`/admin/users/${id}/status`, { status }),
  resetUserPassword: (id: number, newPassword: string) =>
    http.put<void>(`/admin/users/${id}/password`, { newPassword }),
}
