<script setup lang="ts">
import { onBeforeUnmount } from 'vue'
import { useToast } from '@/composables/useToast'

const { toasts, dismiss } = useToast()

// 宿主卸载时逐条移除提示，顺带清掉遗留的定时器。
onBeforeUnmount(() => {
  for (const item of toasts.value) dismiss(item.id)
})
</script>

<template>
  <TransitionGroup name="toast" tag="div" class="toast-host">
    <div v-for="item in toasts" :key="item.id" class="toast" :class="item.type">
      <span class="toast-message">{{ item.message }}</span>
      <button class="toast-close" type="button" aria-label="关闭" @click="dismiss(item.id)">
        ×
      </button>
    </div>
  </TransitionGroup>
</template>

<style scoped>
.toast-host {
  position: fixed;
  top: 74px;
  right: 24px;
  z-index: 200;
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: min(360px, calc(100vw - 48px));
  /* 容器本身不接收点击，避免空白处挡住页面；提示条自身再打开。 */
  pointer-events: none;
}
.toast {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  font-size: 14px;
  line-height: 1.5;
  border: 1px solid var(--line);
  border-radius: 0;
  box-shadow: var(--shadow-md);
  pointer-events: auto;
}
.toast.success {
  background: var(--success-bg);
  color: var(--success);
}
.toast.error {
  background: var(--danger-bg);
  color: var(--danger);
}
.toast.info {
  background: var(--bg);
  color: var(--ink-secondary);
}
.toast-message {
  flex: 1;
  min-width: 0;
  word-break: break-word;
}
.toast-close {
  flex-shrink: 0;
  padding: 0 2px;
  font-size: 16px;
  line-height: 1;
  color: inherit;
  opacity: 0.6;
  transition: opacity 0.15s ease;
}
.toast-close:hover {
  opacity: 1;
}
.toast-enter-active,
.toast-leave-active {
  transition:
    opacity 0.18s ease,
    transform 0.18s ease;
}
.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateX(12px);
}
.toast-move {
  transition: transform 0.18s ease;
}
</style>
