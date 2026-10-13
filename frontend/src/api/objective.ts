import { http } from './request'
import type {
  AnswerResult,
  ObjectiveQuestion,
  ObjectiveSubmissionRecord,
  PageResult,
} from '@/types'

export interface ObjectiveListParams {
  type?: string
  keyword?: string
  page?: number
  size?: number
}

export interface ObjectiveSubmissionListParams {
  /** 1 只看答对 / 0 只看答错 / 不传为全部 */
  correct?: 0 | 1
  page?: number
  size?: number
}

export const objectiveApi = {
  list: (params: ObjectiveListParams) =>
    http.get<PageResult<ObjectiveQuestion>>('/objective-questions', { params }),
  detail: (id: number | string) => http.get<ObjectiveQuestion>(`/objective-questions/${id}`),
  submit: (id: number | string, answer: string) =>
    http.post<AnswerResult>(`/objective-questions/${id}/submit`, { answer }),
  /** 当前用户已答对的客观题 id 列表。 */
  solved: () => http.get<number[]>('/objective-submissions/solved'),
  listSubmissions: (params?: ObjectiveSubmissionListParams) =>
    http.get<PageResult<ObjectiveSubmissionRecord>>('/objective-submissions', { params }),
}
