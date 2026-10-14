import { http } from './request'
import type {
  CreatePracticeRequest,
  PracticeDetail,
  PracticeMonitorData,
  PracticeObjectiveResult,
  PracticeSummary,
} from '@/types/practice'

export interface PracticeListPageResult {
  list: PracticeSummary[]
  total: number
  page: number
  size: number
}

export const practiceApi = {
  list: (params?: { keyword?: string; status?: string; page?: number; size?: number }) =>
    http.get<PracticeListPageResult>('/practice', { params }),

  listAdmin: (params?: { keyword?: string; status?: string; page?: number; size?: number }) =>
    http.get<PracticeListPageResult>('/practice/admin', { params }),

  create: (data: CreatePracticeRequest) =>
    http.post<PracticeSummary>('/practice', data),

  join: (accessCode: string) =>
    http.post<PracticeDetail>('/practice/join', { accessCode }),

  detail: (code: string) =>
    http.get<PracticeDetail>(`/practice/${code}`),

  submitObjective: (code: string, answers: Record<string | number, string>) =>
    http.post<PracticeObjectiveResult>(`/practice/${code}/objective-submit`, { answers }),

  recordPractical: (
    code: string,
    data: {
      practiceQuestionId: number
      submissionId?: number | null
      questionType: 'ALGO' | 'DB'
      status: string
      score: number
    },
  ) => http.post<void>(`/practice/${code}/practical-record`, data),

  submit: (code: string) =>
    http.post<void>(`/practice/${code}/submit`),

  monitor: (code: string) =>
    http.get<PracticeMonitorData>(`/practice/${code}/monitor`),

  end: (code: string) =>
    http.post<void>(`/practice/${code}/end`),

  extend: (code: string, extraMinutes: number = 5) =>
    http.post<void>(`/practice/${code}/extend`, { extraMinutes }),

  remove: (id: number) =>
    http.delete<void>(`/practice/${id}`),
}

