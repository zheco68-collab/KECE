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

/**
 * 内存读数格式化：后端按 KB 上报。
 * 不足 1MB 保留 KB，其余折算为 MB（保留一位小数，整数值去掉小数位），避免节点方块上出现五位数。
 */
export function formatMemory(kb: number | null | undefined): string {
  if (kb == null) return '-'
  if (kb < 1024) return `${kb}KB`
  const mb = kb / 1024
  if (mb >= 1000) return `${Math.round(mb)}MB`
  return `${mb.toFixed(1).replace(/\.0$/, '')}MB`
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
