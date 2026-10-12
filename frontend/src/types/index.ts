export type Role = 'USER' | 'TEACHER' | 'ADMIN'
export type Difficulty = 'EASY' | 'MEDIUM' | 'HARD'
export type ObjectiveType = 'SINGLE' | 'MULTIPLE' | 'FILL'
export type MatchType = 'EXACT' | 'IGNORE_CASE' | 'REGEX'

export interface UserInfo {
  id: number
  username: string
  email: string | null
  roles: string[]
}

export interface AuthResult {
  token: string
  refreshToken: string
  user: UserInfo
}

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  size: number
}

export interface TestCase {
  id: number
  input: string | null
  expectedOutput: string
  isSample: number
}

export interface Question {
  id: number
  title: string
  description: string
  difficulty: Difficulty
  timeLimit: number | null
  memoryLimit: number | null
  languages: string[]
  status: number
  createdBy: number
  sampleTestCases: TestCase[]
  createdAt: string
  updatedAt: string
}

export interface ObjectiveQuestion {
  id: number
  type: ObjectiveType
  content: string
  options: string[]
  answer: string | null
  matchType: MatchType | null
  score: number
  createdBy: number
  createdAt: string
  updatedAt: string
}

export interface AnswerResult {
  correct: boolean
  score: number
  fullScore: number
  correctAnswer: string
}
