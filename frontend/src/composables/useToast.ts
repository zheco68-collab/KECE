import { ref } from 'vue'
import type { Ref } from 'vue'

export interface ToastItem {
  id: number
  type: 'success' | 'error' | 'info'
  message: string
}

/** 自动消失时长（毫秒）：错误提示留更久，方便看清原因。 */
const DURATION: Record<ToastItem['type'], number> = {
  success: 3000,
  info: 3000,
  error: 6000,
}

/** 同一时刻最多堆叠的条数，超出时丢弃最旧的。 */
const MAX_VISIBLE = 4

// 模块级单例：任意组件调用共享同一份列表与定时器。
const toasts = ref<ToastItem[]>([])
const timers = new Map<number, ReturnType<typeof setTimeout>>()
let seq = 0

function clearTimer(id: number) {
  const timer = timers.get(id)
  if (timer !== undefined) {
    clearTimeout(timer)
    timers.delete(id)
  }
}

function dismiss(id: number) {
  clearTimer(id)
  toasts.value = toasts.value.filter((item) => item.id !== id)
}

function push(type: ToastItem['type'], message: string) {
  const id = ++seq
  const next = [...toasts.value, { id, type, message }]
  while (next.length > MAX_VISIBLE) {
    const dropped = next.shift()
    if (dropped) clearTimer(dropped.id)
  }
  toasts.value = next
  timers.set(id, setTimeout(() => dismiss(id), DURATION[type]))
}

export function useToast(): {
  toasts: Ref<ToastItem[]>
  success(message: string): void
  error(message: string): void
  info(message: string): void
  dismiss(id: number): void
} {
  return {
    toasts,
    success: (message) => push('success', message),
    error: (message) => push('error', message),
    info: (message) => push('info', message),
    dismiss,
  }
}
