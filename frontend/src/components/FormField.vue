<script setup lang="ts">
const props = defineProps<{
  label: string
  required?: boolean
  error?: string
  hint?: string
  for?: string
}>()
</script>

<template>
  <div class="form-field">
    <!-- for 是关键字，模板里经 props 访问，用来把 label 关联到输入控件的 id。 -->
    <label class="form-label" :for="props.for">
      {{ label }}
      <span v-if="required" class="form-required">*</span>
    </label>
    <slot />
    <p v-if="error" class="form-error">{{ error }}</p>
    <p v-else-if="hint" class="form-hint">{{ hint }}</p>
  </div>
</template>

<style scoped>
.form-field {
  margin-bottom: 18px;
}
.form-label {
  display: block;
  margin-bottom: 6px;
  font-size: 14px;
  font-weight: 500;
}
.form-required {
  margin-left: 2px;
  color: var(--danger);
}
.form-error {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--danger);
}
.form-hint {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--ink-tertiary);
}
</style>
