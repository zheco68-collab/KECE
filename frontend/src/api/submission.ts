import { http } from './request'
import type { PageResult, Submission, SubmissionRecord } from '@/types'

export interface SubmitPayload {
  questionId: number
  contestId?: number
  language: string
  codeContent: Record<string, string>
}

export interface SubmissionListParams {
  status?: string
  page?: number
  size?: number
}

export const submissionApi = {
  submit: (payload: SubmitPayload) => http.post<Submission>('/submissions', payload),
  get: (id: number | string) => http.get<Submission>(`/submissions/${id}`),
  list: (params?: SubmissionListParams) =>
    http.get<PageResult<SubmissionRecord>>('/submissions', { params }),
}
