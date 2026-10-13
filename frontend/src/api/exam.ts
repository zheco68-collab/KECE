import { http } from './request';
import type { PageResult } from '@/types';

/** 试卷题目快照条目（与后端 ExamQuestionVO 对齐）。 */
export interface ExamQuestionItem {
  id: number;
  questionId: number;
  questionType: 'ALGO' | 'DB' | 'OBJECTIVE';
  title: string;
  score: number;
  sortOrder: number;
}

export interface Exam {
  id: number;
  title: string;
  description: string | null;
  durationMinutes: number;
  startTime: string | null;
  endTime: string | null;
  mode: 'ACM' | 'OI';
  status: number;
  sessionStatus: string | null;
  createdBy: number;
  questions: ExamQuestionItem[];
  createdAt: string;
  updatedAt: string;
}

/** 试卷创建/更新请求（组卷条目：题目引用 + 分值快照）。 */
export interface ExamWriteRequest {
  title: string;
  description?: string | null;
  durationMinutes: number;
  mode: 'ACM' | 'OI';
  status?: number;
  startTime?: string | null;
  questions: Array<{
    questionId: number;
    questionType: 'ALGO' | 'DB' | 'OBJECTIVE';
    score: number;
  }>;
}

export interface SessionEnter {
  id: number;
  examId: number;
  examTitle: string;
  mode: 'ACM' | 'OI';
  durationMinutes: number;
  startTime: string;
  endTime: string;
  status: string;
  remainingSeconds: number;
  questions: ExamSessionQuestion[];
}

/** 考试中单题：快照 + 实时题面（作答所需）。 */
export interface ExamSessionQuestion {
  id: number;
  questionId: number;
  questionType: 'ALGO' | 'DB' | 'OBJECTIVE';
  title: string;
  score: number;
  sortOrder: number;
  content: string | null;
  options: string[] | null;
  objectiveType: 'SINGLE' | 'MULTIPLE' | 'FILL' | null;
  schemaSql: string | null;
  timeLimitMs: number | null;
  memoryLimitMb: number | null;
}

/** 场次（公开列表与管理列表共用；状态含 UPCOMING/ONGOING/ENDED/CANCELLED）。 */
export interface ExamSessionInfo {
  id: number;
  examId: number;
  examTitle: string;
  startTime: string;
  endTime: string;
  durationMinutes: number;
  status: string;
  createdBy: number;
  createdAt: string;
}

/** ACM 单题实时提交的返回（submissionId 供轮询判题结果）。 */
export interface SingleSubmit {
  submissionId: number;
  questionType: 'ALGO' | 'DB';
  status: string;
}

export interface SubmitResult {
  examId: number;
  graded: number;
  queued: number;
}

export interface ExamSubmissionRow {
  examQuestionId: number;
  questionId: number;
  questionType: 'ALGO' | 'DB' | 'OBJECTIVE';
  title: string;
  score: number | null;
  fullScore: number;
  answer: string;
  language: string | null;
  status: string | null;
}

export interface RankRow {
  userId: number;
  username: string;
  solved: number;
  penaltyMinutes: number;
  totalScore: number;
  finished: number;
  fullScore: number;
}

export const examApi = {
  listExams: (params: { keyword?: string; page?: number; size?: number }) =>
    http.get<PageResult<Exam>>('/exams', { params }),
  listAdmin: (params: {
    keyword?: string;
    status?: number;
    createdBy?: number;
    page?: number;
    size?: number;
  }) => http.get<PageResult<Exam>>('/exams/admin', { params }),
  detail: (id: number) => http.get<Exam>(`/exams/${id}`),
  create: (body: ExamWriteRequest) => http.post<Exam>('/exams', body),
  update: (id: number, body: ExamWriteRequest) => http.put<Exam>(`/exams/${id}`, body),
  remove: (id: number) => http.delete<void>(`/exams/${id}`),
  listOpen: (params: { keyword?: string; page?: number; size?: number }) =>
    http.get<PageResult<ExamSessionInfo>>('/exams/open', { params }),
  enter: (examId: number) => http.get<SessionEnter>(`/exams/${examId}/enter`),
  saveAnswer: (
    examId: number,
    body: { examQuestionId: number; answer: string; language?: string },
  ) => http.put<void>(`/exams/${examId}/answers`, body),
  submitQuestion: (examId: number, examQuestionId: number) =>
    http.post<SingleSubmit>(`/exams/${examId}/questions/${examQuestionId}/submit`),
  submit: (examId: number) => http.post<SubmitResult>(`/exams/${examId}/submit`),
  results: (examId: number) => http.get<ExamSubmissionRow[]>(`/exams/${examId}/results`),
  ranking: (examId: number) => http.get<RankRow[]>(`/exams/${examId}/ranking`),
  cancel: (examId: number) => http.delete<void>(`/exams/${examId}/cancel`),
};
