<script setup lang="ts">
import { onBeforeUnmount, onMounted } from 'vue'
import { useConfirm } from '@/composables/useConfirm'

const { state, resolve } = useConfirm()

// Esc 等同于取消，仅在确认框展示时生效。
function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape' && state.value?.visible) resolve(false)
}

onMounted(() => window.addEventListener('keydown', onKeydown))
onBeforeUnmount(() => window.removeEventListener('keydown', onKeydown))
</script>

<template>
  <Teleport to="body">
    <div v-if="state?.visible" class="confirm-overlay" @click.self="resolve(false)">
      <div class="confirm-modal" role="dialog" aria-modal="true">
        <h3 class="confirm-title">{{ state.options.title }}</h3>
        <p v-if="state.options.message" class="confirm-message">{{ state.options.message }}</p>
        <div class="confirm-actions">
          <button class="btn btn-ghost btn-sm" type="button" @click="resolve(false)">
            {{ state.options.cancelText || '取消' }}
          </button>
          <button
            class="btn btn-sm confirm-ok"
            :class="{ danger: state.options.danger }"
            type="button"
            @click="resolve(true)"
          >
            {{ state.options.confirmText || '确定' }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.confirm-overlay {
  position: fixed;
  inset: 0;
  z-index: 200;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(17, 17, 17, 0.45);
}
.confirm-modal {
  width: 100%;
  min-width: 320px;
  max-width: 440px;
  padding: 20px 22px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: 0;
  box-shadow: var(--shadow-md);
}
.confirm-title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  line-height: 1.4;
}
.confirm-message {
  margin: 10px 0 0;
  font-size: 14px;
  line-height: 1.6;
  color: var(--ink-secondary);
  word-break: break-word;
}
.confirm-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}
.confirm-ok {
  background: var(--button);
  color: #fff;
}
.confirm-ok.danger {
  background: var(--danger);
}
/* 极窄屏放开最小宽度，避免浮层撑破视口。 */
@media (max-width: 400px) {
  .confirm-overlay {
    padding: 16px;
  }
  .confirm-modal {
    min-width: 0;
  }
}
</style>
