<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as monaco from 'monaco-editor/editor/editor.api'
import './monaco-setup'

const props = defineProps<{
  modelValue: string
  language: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

const container = ref<HTMLElement | null>(null)
let editor: monaco.editor.IStandaloneCodeEditor | null = null

onMounted(() => {
  if (!container.value) return
  editor = monaco.editor.create(container.value, {
    value: props.modelValue,
    language: props.language,
    theme: 'vs',
    automaticLayout: true,
    minimap: { enabled: false },
    fontSize: 14,
    tabSize: 4,
    scrollBeyondLastLine: false,
    wordWrap: 'on',
  })
  editor.onDidChangeModelContent(() => {
    emit('update:modelValue', editor?.getValue() ?? '')
  })
})

watch(
  () => props.language,
  (lang) => {
    const model = editor?.getModel()
    if (model) {
      monaco.editor.setModelLanguage(model, lang)
    }
  },
)

watch(
  () => props.modelValue,
  (value) => {
    if (editor && value !== editor.getValue()) {
      editor.setValue(value)
    }
  },
)

onBeforeUnmount(() => {
  editor?.dispose()
})
</script>

<template>
  <div ref="container" class="code-editor"></div>
</template>

<style scoped>
.code-editor {
  height: 420px;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  overflow: hidden;
  background: var(--surface);
}
</style>
