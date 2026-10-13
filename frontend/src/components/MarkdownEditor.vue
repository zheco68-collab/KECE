<script setup lang="ts">
import { computed } from 'vue'
import { renderMarkdown } from '@/utils/markdown'

const props = withDefaults(
  defineProps<{
    modelValue: string
    placeholder?: string
  }>(),
  {
    placeholder: '',
  },
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

/** 预览区 HTML：由 renderMarkdown 渲染并清洗。 */
const previewHtml = computed(() => renderMarkdown(props.modelValue))

function onInput(event: Event) {
  emit('update:modelValue', (event.target as HTMLTextAreaElement).value)
}
</script>

<template>
  <div class="markdown-editor">
    <div class="pane">
      <div class="pane-label">编辑</div>
      <textarea
        class="editor-input"
        :value="modelValue"
        :placeholder="placeholder"
        @input="onInput"
      ></textarea>
    </div>
    <div class="pane">
      <div class="pane-label">预览</div>
      <!-- eslint-disable-next-line vue/no-v-html -- 预览内容已由 renderMarkdown 清洗 -->
      <div class="prose pane-preview" v-html="previewHtml"></div>
    </div>
  </div>
</template>

<style scoped>
.markdown-editor {
  display: flex;
  align-items: stretch;
  gap: 16px;
}
.pane {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.pane-label {
  margin-bottom: 6px;
  font-size: 12px;
  color: var(--ink-tertiary);
}
.editor-input {
  flex: 1;
  width: 100%;
  min-height: 320px;
  padding: 12px;
  border: 1px solid var(--line);
  border-radius: 0;
  background: var(--bg);
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 14px;
  line-height: 1.6;
  resize: vertical;
  outline: none;
}
.editor-input:focus {
  border-color: var(--ink);
}
.editor-input::placeholder {
  color: var(--ink-tertiary);
}
.pane-preview {
  flex: 1;
  min-height: 320px;
  padding: 12px 16px;
  border: 1px solid var(--line);
  overflow: auto;
}
@media (max-width: 760px) {
  .markdown-editor {
    flex-direction: column;
  }
}
</style>
