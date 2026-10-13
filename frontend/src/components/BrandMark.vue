<script setup lang="ts">
withDefaults(
  defineProps<{ size?: number; variant?: 'dark' | 'light' }>(),
  { size: 28, variant: 'dark' },
)
</script>

<template>
  <span
    class="brand-mark"
    :class="`brand-mark--${variant}`"
    :style="{
      width: `${size}px`,
      height: `${size}px`,
      fontSize: `${Math.round(size * 0.48)}px`,
    }"
  >
    K
  </span>
</template>

<style scoped>
.brand-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-weight: 700;
  border-radius: 0;
  line-height: 1;
  user-select: none;
}
.brand-mark--dark {
  background: var(--button);
  color: #fff;
}
.brand-mark--light {
  background: #fff;
  color: var(--button);
}
</style>
