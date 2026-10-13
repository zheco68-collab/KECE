import { http } from './request'
import type {
  DbQuestion,
  DbSubmissionRecord,
  PageResult,
  Submission,
  SubmissionStatus,
} from '@/types'

export interface DbQuestionListParams {
  type?: string
  keyword?: string
  page?: number
  size?: number
}

export interface DbSubmissionListParams {
  status?: SubmissionStatus | ''
  page?: number
  size?: number
}

/** 做题侧接口。出题/改题的接口（POST/PUT/DELETE /db-questions）待管理页面再做。 */
export const databaseApi = {
  list: (params: DbQuestionListParams) =>
    http.get<PageResult<DbQuestion>>('/db-questions', { params }),
  detail: (id: number | string) => http.get<DbQuestion>(`/db-questions/${id}`),
  submit: (id: number | string, sql: string) =>
    http.post<Submission>(`/db-questions/${id}/submit`, { sql }),
  listSubmissions: (params?: DbSubmissionListParams) =>
    http.get<PageResult<DbSubmissionRecord>>('/db-submissions', { params }),
  getSubmission: (id: number | string) => http.get<Submission>(`/db-submissions/${id}`),
  /** 当前用户已通过的数据库题目 id 列表。 */
  solved: () => http.get<number[]>('/db-submissions/solved'),
}
