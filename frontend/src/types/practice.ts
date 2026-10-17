export interface PracticeQuestionItem {
  questionId: number
  questionType: 'OBJECTIVE' | 'ALGO' | 'DB'
  title?: string
  score?: number
  sortOrder?: number
}

export interface CreatePracticeRequest {
  title: string
  description?: string
  durationMinutes: number
  questions: PracticeQuestionItem[]
}

export interface PracticeSummary {
  id: number
  title: string
  description?: string
  accessCode?: string | null
  durationMinutes: number
  status: 'ACTIVE' | 'ENDED'
  teacherId: number
  teacherName: string
  memberCount: number
  questionCount: number
  objectiveCount: number
  practicalCount: number
  startTime: string
  endTime?: string
  myJoined: boolean
  myScore?: number
  mySubmitted?: boolean
  createdAt: string
}

export interface PracticeObjectiveQuestion {
  practiceQuestionId: number
  questionId: number
  type: 'SINGLE' | 'MULTIPLE' | 'FILL' | 'JUDGE'
  content: string
  options: string[]
  score: number
  sortOrder: number
  userAnswer?: string
  userScore?: number
  isCorrect?: boolean
  answered?: boolean
}

export interface PracticePracticalQuestion {
  practiceQuestionId: number
  questionId: number
  questionType: 'ALGO' | 'DB'
  title: string
  score: number
  sortOrder: number
  difficulty: string
  timeLimitMs: number
  memoryLimitKb: number
  languages: string[]
  userStatus: 'ACCEPTED' | 'WRONG_ANSWER' | 'PENDING' | 'UNATTEMPTED' | 'PARTIAL' | string
  userScore: number
  submissionId?: number
}

export interface PracticeDetail {
  id: number
  title: string
  description?: string
  accessCode: string
  durationMinutes: number
  status: 'ACTIVE' | 'ENDED'
  teacherId: number
  teacherName: string
  memberCount: number
  startTime: string
  endTime?: string
  remainingSeconds?: number
  myJoined: boolean
  myScore: number
  mySolvedCount: number
  mySubmitted: boolean
  objectiveQuestions: PracticeObjectiveQuestion[]
  practicalQuestions: PracticePracticalQuestion[]
}

export interface PracticeObjectiveItemResult {
  practiceQuestionId: number
  questionId: number
  correct: boolean
  score: number
  fullScore: number
  userAnswer: string
  correctAnswer?: string
}

export interface PracticeObjectiveResult {
  totalScore: number
  fullScore: number
  correctCount: number
  totalCount: number
  items: PracticeObjectiveItemResult[]
}

export interface QuestionStatItem {
  practiceQuestionId: number
  questionId: number
  questionType: string
  title: string
  score: number
  submitCount: number
  passCount: number
  passRate: number
}

export interface RecentActivityItem {
  id: number
  username: string
  nickname: string
  questionTitle: string
  questionType: string
  status: string
  score: number
  createdAt: string
}

export interface LeaderboardItem {
  rank: number
  userId: number
  username: string
  nickname: string
  solvedCount: number
  totalScore: number
  submitted: boolean
  joinedAt: string
}

export interface PracticeMonitorData {
  practiceId: number
  title: string
  accessCode: string
  durationMinutes: number
  status: 'ACTIVE' | 'ENDED'
  remainingSeconds?: number
  joinedCount: number
  submittedCount: number
  totalSubmissions: number
  avgScore: number
  perfectScoreCount: number
  questionStats: QuestionStatItem[]
  recentActivities: RecentActivityItem[]
  leaderboard: LeaderboardItem[]
}

