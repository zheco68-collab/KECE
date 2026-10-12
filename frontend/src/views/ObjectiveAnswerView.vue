<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { objectiveApi } from '@/api/objective'
import { renderMarkdown } from '@/utils/markdown'
import { useUserStore } from '@/stores/user'
import type { AnswerResult, ObjectiveQuestion, ObjectiveType } from '@/types'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const id = computed(() => Number(route.params.id))
const question = ref<ObjectiveQuestion | null>(null)
const loading = ref(true)
const error = ref('')

const selected = ref<string[]>([])
const fillText = ref('')
const result = ref<AnswerResult | null>(null)
const submitting = ref(false)
const submitError = ref('')

const typeLabels: Record<ObjectiveType, string> = {
  SINGLE: '单选',
  MULTIPLE: '多选',
  FILL: '填空',
}

const contentHtml = computed(() => renderMarkdown(question.value?.content ?? ''))

function optionLetter(index: number): string {
  return String.fromCharCode(65 + index)
}

function optionText(option: string): string {
  return option.replace(/^[A-Za-z][.、)）:：]\s*/, '')
}

function isSingle(q: ObjectiveQuestion): boolean {
  return q.type === 'SINGLE'
}

function isFill(q: ObjectiveQuestion): boolean {
  return q.type === 'FILL'
}

function toggleOption(letter: string) {
  if (selected.value.includes(letter)) {
    selected.value = selected.value.filter((l) => l !== letter)
  } else {
    selected.value = [...selected.value, letter].sort()
  }
}

async function submit() {
  if (submitting.value || !question.value) return
  if (!userStore.isLoggedIn) {
    submitError.value = '请先登录后再作答'
    return
  }
  const answer = isFill(question.value) ? fillText.value.trim() : selected.value.join(',')
  if (!answer) {
    submitError.value = '请先作答'
    return
  }
  submitting.value = true
  submitError.value = ''
  try {
    result.value = await objectiveApi.submit(id.value, answer)
  } catch (e) {
    submitError.value = e instanceof Error ? e.message : '提交失败'
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  try {
    question.value = await objectiveApi.detail(id.value)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="answer container">
    <button class="back" @click="router.back()">← 返回</button>

    <p v-if="loading" class="muted">加载中…</p>
    <div v-else-if="error" class="submit-error">{{ error }}</div>

    <template v-else-if="question">
      <div class="head">
        <span class="badge" :class="`type-${question.type.toLowerCase()}`">
          {{ typeLabels[question.type] }}
        </span>
        <span class="score">{{ question.score }} 分</span>
      </div>

      <!-- eslint-disable-next-line vue/no-v-html -- 内容已由 renderMarkdown 清洗 -->
      <div class="prose card" v-html="contentHtml"></div>

      <div v-if="!isFill(question)" class="options">
        <label
          v-for="(opt, i) in question.options"
          :key="i"
          class="option"
          :class="{ selected: selected.includes(optionLetter(i)) }"
        >
          <input
            v-if="isSingle(question)"
            type="radio"
            name="option"
            :checked="selected[0] === optionLetter(i)"
            @change="selected = [optionLetter(i)]"
          />
          <input
            v-else
            type="checkbox"
            :checked="selected.includes(optionLetter(i))"
            @change="toggleOption(optionLetter(i))"
          />
          <span class="option-letter">{{ optionLetter(i) }}</span>
          <span class="option-text">{{ optionText(opt) }}</span>
        </label>
      </div>

      <div v-else class="fill">
        <input
          v-model="fillText"
          class="input"
          type="text"
          placeholder="请输入答案"
          @keyup.enter="submit"
        />
      </div>

      <p v-if="submitError" class="submit-error">{{ submitError }}</p>

      <div class="actions">
        <button
          v-if="!userStore.isLoggedIn"
          class="btn btn-primary btn-lg"
          @click="router.push('/login')"
        >
          登录后作答
        </button>
        <button v-else class="btn btn-primary btn-lg" :disabled="submitting" @click="submit">
          {{ submitting ? '判分中…' : '提交答案' }}
        </button>
      </div>

      <div v-if="result" class="result card" :class="result.correct ? 'result-ok' : 'result-wrong'">
        <div class="result-head">{{ result.correct ? '回答正确' : '回答错误' }}</div>
        <div class="result-score">得分 {{ result.score }} / {{ result.fullScore }}</div>
        <div class="result-answer">正确答案：{{ result.correctAnswer }}</div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.answer {
  padding-top: 32px;
  padding-bottom: 56px;
}
.head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}
.score {
  color: var(--ink-tertiary);
  font-size: 13px;
}
.options {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 20px;
}
.option {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  background: var(--surface);
  cursor: pointer;
  transition:
    border-color 0.15s ease,
    background 0.15s ease;
}
.option:hover {
  border-color: var(--ink-tertiary);
}
.option.selected {
  border-color: var(--ink);
  background: var(--bg);
}
.option input {
  accent-color: var(--button);
}
.option-letter {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--bg);
  border: 1px solid var(--line);
  font-size: 13px;
  font-weight: 600;
  flex-shrink: 0;
}
.option.selected .option-letter {
  background: var(--button);
  border-color: var(--button);
  color: #fff;
}
.option-text {
  min-width: 0;
}
.fill {
  margin-top: 20px;
  max-width: 420px;
}
.actions {
  margin-top: 24px;
}
.submit-error {
  margin-top: 16px;
}
.result {
  margin-top: 24px;
  padding: 20px 24px;
}
.result-head {
  font-size: 17px;
  font-weight: 600;
  margin-bottom: 8px;
}
.result-score {
  color: var(--ink-secondary);
  margin-bottom: 4px;
}
.result-answer {
  color: var(--ink-tertiary);
  font-size: 14px;
}
.result-ok {
  border-color: #bfe3c6;
  background: var(--success-bg);
}
.result-ok .result-head {
  color: var(--success);
}
.result-wrong {
  border-color: #f0c4c6;
  background: var(--danger-bg);
}
.result-wrong .result-head {
  color: var(--danger);
}
</style>
