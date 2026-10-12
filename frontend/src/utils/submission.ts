import type { SubmissionStatus } from '@/types'

/** 各评测状态的缩写（节点/徽章展示用）。 */
export const STATUS_ABBR: Record<SubmissionStatus, string> = {
  PENDING: 'PD',
  COMPILING: 'CP',
  JUDGING: 'JD',
  AI_REVIEWING: 'AI',
  ACCEPTED: 'AC',
  WRONG_ANSWER: 'WA',
  TIME_LIMIT_EXCEEDED: 'TLE',
  MEMORY_LIMIT_EXCEEDED: 'MLE',
  RUNTIME_ERROR: 'RE',
  COMPILE_ERROR: 'CE',
  SYSTEM_ERROR: 'SE',
}

/** 语言显示名。 */
export const LANGUAGE_LABELS: Record<string, string> = {
  cpp: 'C++',
  java: 'Java',
  python: 'Python',
}

/** 状态对应的配色基调（用于节点方块与状态徽章）。 */
export function statusTone(status: SubmissionStatus): string {
  switch (status) {
    case 'ACCEPTED':
      return 'ac'
    case 'WRONG_ANSWER':
      return 'wa'
    case 'TIME_LIMIT_EXCEEDED':
      return 'tle'
    case 'MEMORY_LIMIT_EXCEEDED':
      return 'mle'
    case 'RUNTIME_ERROR':
      return 're'
    case 'COMPILE_ERROR':
      return 'ce'
    case 'SYSTEM_ERROR':
      return 'se'
    default:
      return 'run'
  }
}
