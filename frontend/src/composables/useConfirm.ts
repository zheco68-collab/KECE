import { ref } from 'vue'
import type { Ref } from 'vue'

export interface ConfirmOptions {
  title: string
  message?: string
  /** 确认按钮文案，默认「确定」。 */
  confirmText?: string
  /** 取消按钮文案，默认「取消」。 */
  cancelText?: string
  /** 危险操作：确认按钮使用危险色。 */
  danger?: boolean
}

type ConfirmState = { visible: boolean; options: ConfirmOptions }

// 模块级单例：同一时刻只展示一个确认框，多组件调用共享状态。
const state = ref<ConfirmState | null>(null)
let resolver: ((ok: boolean) => void) | null = null

function resolve(ok: boolean) {
  const pending = resolver
  resolver = null
  state.value = null
  if (pending) pending(ok)
}

export function useConfirm(): {
  state: Ref<{ visible: boolean; options: ConfirmOptions } | null>
  confirm(options: ConfirmOptions): Promise<boolean>
  resolve(ok: boolean): void
} {
  return {
    state,
    confirm(options) {
      // 上一次还没结算就先按取消收尾，避免调用方的 Promise 一直悬挂。
      resolve(false)
      state.value = { visible: true, options }
      return new Promise<boolean>((r) => {
        resolver = r
      })
    },
    resolve,
  }
}
