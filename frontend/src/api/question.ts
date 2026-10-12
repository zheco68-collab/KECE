import { http } from './request'
import type { PageResult, Question } from '@/types'

export interface QuestionListParams {
  keyword?: string
  difficulty?: string
  page?: number
  size?: number
}

export const questionApi = {
  list: (params: QuestionListParams) => http.get<PageResult<Question>>('/questions', { params }),
  detail: (id: number | string) => http.get<Question>(`/questions/${id}`),
}
