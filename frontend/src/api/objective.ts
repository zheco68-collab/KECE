import { http } from './request'
import type {
  AnswerResult,
  MatchType,
  ObjectiveQuestion,
  ObjectiveSubmissionRecord,
  ObjectiveType,
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

/** 客观题创建/更新请求。 */
export interface ObjectiveQuestionWriteRequest {
  type: ObjectiveType
  content: string
  /** 选项（单选/多选）。 */
  options?: string[]
  answer: string
  matchType?: MatchType
  score?: number
  /** 状态：1 公开 0 私有。 */
  status?: number
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
  create: (body: ObjectiveQuestionWriteRequest) =>
    http.post<ObjectiveQuestion>('/objective-questions', body),
  update: (id: number | string, body: ObjectiveQuestionWriteRequest) =>
    http.put<ObjectiveQuestion>(`/objective-questions/${id}`, body),
  remove: (id: number | string) => http.delete<void>(`/objective-questions/${id}`),
}
