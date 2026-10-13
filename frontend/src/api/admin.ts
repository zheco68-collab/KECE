import { http } from './request'
import type { DbQuestion, ObjectiveQuestion, PageResult, Question } from '@/types'

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

export interface AdminQuestionListParams {
  keyword?: string
  difficulty?: string
  status?: number
  createdBy?: number
  page?: number
  size?: number
}

export interface AdminDbQuestionListParams {
  type?: string
  keyword?: string
  status?: number
  createdBy?: number
  page?: number
  size?: number
}

export interface AdminObjectiveListParams {
  type?: string
  keyword?: string
  status?: number
  createdBy?: number
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
  listQuestions: (params: AdminQuestionListParams) =>
    http.get<PageResult<Question>>('/admin/questions', { params }),
  listDbQuestions: (params: AdminDbQuestionListParams) =>
    http.get<PageResult<DbQuestion>>('/admin/db-questions', { params }),
  listObjectiveQuestions: (params: AdminObjectiveListParams) =>
    http.get<PageResult<ObjectiveQuestion>>('/admin/objective-questions', { params }),
}
