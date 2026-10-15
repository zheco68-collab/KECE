/**
 * 复制文本到剪贴板
 * 兼容处理：
 * 1. 优先使用 navigator.clipboard（在 https 或 localhost 下可用）
 * 2. 降级使用 document.execCommand('copy')（在 http / 局域网 IP / 远端 IP 访问等非安全上下文下可用）
 */
export async function copyToClipboard(text: string): Promise<boolean> {
  if (!text) return false

  // 1. 现代 Clipboard API
  if (typeof navigator !== 'undefined' && navigator.clipboard && typeof navigator.clipboard.writeText === 'function') {
    try {
      await navigator.clipboard.writeText(text)
      return true
    } catch {
      // 权限受限或非安全域降级到 execCommand
    }
  }

  // 2. 传统兼容模式（降级兼容 HTTP / 非 localhost 场景）
  try {
    const textarea = document.createElement('textarea')
    textarea.value = text
    textarea.style.position = 'fixed'
    textarea.style.left = '-9999px'
    textarea.style.top = '-9999px'
    textarea.style.opacity = '0'
    textarea.setAttribute('readonly', '')
    document.body.appendChild(textarea)
    textarea.focus()
    textarea.select()
    const successful = document.execCommand('copy')
    document.body.removeChild(textarea)
    return successful
  } catch (err) {
    console.error('Clipboard copy failed:', err)
    return false
  }
}
