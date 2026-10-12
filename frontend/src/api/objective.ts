import { http } from './request'
import type { AnswerResult, ObjectiveQuestion, PageResult } from '@/types'

export interface ObjectiveListParams {
  type?: string
  keyword?: string
  page?: number
  size?: number
}

export const objectiveApi = {
  list: (params: ObjectiveListParams) =>
    http.get<PageResult<ObjectiveQuestion>>('/objective-questions', { params }),
  detail: (id: number | string) => http.get<ObjectiveQuestion>(`/objective-questions/${id}`),
  submit: (id: number | string, answer: string) =>
    http.post<AnswerResult>(`/objective-questions/${id}/submit`, { answer }),
}
