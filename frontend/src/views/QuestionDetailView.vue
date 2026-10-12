<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { questionApi } from '@/api/question'
import { submissionApi } from '@/api/submission'
import { renderMarkdown } from '@/utils/markdown'
import { useUserStore } from '@/stores/user'
import CodeEditor from '@/components/CodeEditor.vue'
import type { Difficulty, Question, Submission, SubmissionStatus } from '@/types'
import { LANGUAGE_LABELS, STATUS_ABBR, statusTone } from '@/utils/submission'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const id = computed(() => Number(route.params.id))

const question = ref<Question | null>(null)
const loading = ref(true)
const error = ref('')

const difficultyLabels: Record<Difficulty, string> = {
  EASY: '简单',
  MEDIUM: '中等',
  HARD: '困难',
}

const descriptionHtml = computed(() => renderMarkdown(question.value?.description ?? ''))

// ---- 代码提交与评测 ----
const SUPPORTED_LANGUAGES: string[] = ['cpp', 'java', 'python']
const TEMPLATES: Record<string, string> = {
  cpp: '#include <iostream>\nusing namespace std;\n\nint main() {\n    // 在此编写代码\n    return 0;\n}\n',
  java: 'import java.util.*;\n\npublic class Main {\n    public static void main(String[] args) {\n        // 在此编写代码\n    }\n}\n',
  python: '# 在此编写代码\n',
}
const FILENAMES: Record<string, string> = { cpp: 'main.cpp', java: 'Main.java', python: 'main.py' }

const TERMINAL_STATUSES: SubmissionStatus[] = [
  'ACCEPTED',
  'WRONG_ANSWER',
  'TIME_LIMIT_EXCEEDED',
  'MEMORY_LIMIT_EXCEEDED',
  'RUNTIME_ERROR',
  'COMPILE_ERROR',
  'SYSTEM_ERROR',
]

const languages = computed(() =>
  (question.value?.languages ?? []).filter((l) => SUPPORTED_LANGUAGES.includes(l)),
)

const language = ref('cpp')
const codes = ref<Record<string, string>>({ ...TEMPLATES })
const submitting = ref(false)
const submitError = ref('')
const submission = ref<Submission | null>(null)
let pollTimer: number | null = null

const progressPercent = computed(() => {
  const p = submission.value?.progress
  if (!p || p.totalCases === 0) return 0
  return Math.round((p.finishedCases / p.totalCases) * 100)
})

function isTerminal(status: SubmissionStatus): boolean {
  return TERMINAL_STATUSES.includes(status)
}

function statusClass(status: SubmissionStatus): string {
  if (status === 'ACCEPTED') return 'ok'
  if (isTerminal(status)) return 'wrong'
  return 'running'
}

function setLanguage(lang: string) {
  language.value = lang
}

async function submit() {
  if (submitting.value) return
  if (!userStore.isLoggedIn) {
    submitError.value = '请先登录后再提交'
    return
  }
  const code = codes.value[language.value] ?? ''
  if (!code.trim()) {
    submitError.value = '请先编写代码'
    return
  }
  submitting.value = true
  submitError.value = ''
  submission.value = null
  try {
    const s = await submissionApi.submit({
      questionId: id.value,
      language: language.value,
      codeContent: { [FILENAMES[language.value]]: code },
    })
    submission.value = s
    startPolling(s.submissionId)
  } catch (e) {
    submitError.value = e instanceof Error ? e.message : '提交失败'
  } finally {
    submitting.value = false
  }
}

function startPolling(submissionId: number) {
  stopPolling()
  pollTimer = window.setInterval(async () => {
    try {
      const s = await submissionApi.get(submissionId)
      submission.value = s
      if (isTerminal(s.status)) {
        stopPolling()
      }
    } catch {
      stopPolling()
    }
  }, 1000)
}

function stopPolling() {
  if (pollTimer != null) {
    window.clearInterval(pollTimer)
    pollTimer = null
  }
}

onMounted(async () => {
  try {
    question.value = await questionApi.detail(id.value)
    if (question.value.languages?.length) {
      language.value = question.value.languages[0]
    }
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
})

onBeforeUnmount(() => {
  stopPolling()
})
</script>

<template>
  <div class="detail container">
    <button class="back" @click="router.back()">← 返回</button>

    <p v-if="loading" class="muted">加载中…</p>
    <div v-else-if="error" class="submit-error">{{ error }}</div>

    <template v-else-if="question">
      <div class="head">
        <h1 class="title">{{ question.title }}</h1>
        <span class="badge" :class="`difficulty-${question.difficulty.toLowerCase()}`">
          {{ difficultyLabels[question.difficulty] }}
        </span>
      </div>
      <div class="meta">
        <span>时间限制 {{ question.timeLimit ?? '-' }} ms</span>
        <span>内存限制 {{ question.memoryLimit ?? '-' }} MB</span>
        <span v-if="question.languages?.length">语言 {{ question.languages.join(' / ') }}</span>
      </div>

      <!-- eslint-disable-next-line vue/no-v-html -- 内容已由 renderMarkdown 清洗 -->
      <div class="prose card" v-html="descriptionHtml"></div>

      <h2 class="section-title">样例</h2>
      <div v-for="(tc, i) in question.sampleTestCases" :key="tc.id" class="case card">
        <div class="case-head">样例 {{ i + 1 }}</div>
        <div class="case-block">
          <div class="case-label">输入</div>
          <pre>{{ tc.input || '（无）' }}</pre>
        </div>
        <div class="case-block">
          <div class="case-label">输出</div>
          <pre>{{ tc.expectedOutput }}</pre>
        </div>
      </div>

      <h2 class="section-title">提交代码</h2>
      <div class="submit-card card">
        <div class="lang-tabs">
          <button
            v-for="lang in languages"
            :key="lang"
            class="lang-tab"
            :class="{ active: lang === language }"
            @click="setLanguage(lang)"
          >
            {{ LANGUAGE_LABELS[lang] ?? lang }}
          </button>
        </div>

        <CodeEditor v-model="codes[language]" :language="language" />

        <div class="submit-row">
          <button
            v-if="!userStore.isLoggedIn"
            class="btn btn-primary btn-lg"
            @click="router.push('/login')"
          >
            登录后提交
          </button>
          <button v-else class="btn btn-primary btn-lg" :disabled="submitting" @click="submit">
            {{ submitting ? '提交中…' : '提交评测' }}
          </button>
          <span v-if="submission" class="submission-id">提交 #{{ submission.submissionId }}</span>
        </div>

        <p v-if="submitError" class="submit-error">{{ submitError }}</p>

        <div v-if="submission" class="result">
          <div class="result-head" :class="statusClass(submission.status)">
            <span class="status-dot"></span>
            <span class="status-text">{{ STATUS_ABBR[submission.status] }}</span>
            <span v-if="submission.result?.timeUsedMs != null" class="result-stat">
              {{ submission.result.timeUsedMs }} ms
            </span>
            <span v-if="submission.result?.memoryUsedKb != null" class="result-stat">
              {{ submission.result.memoryUsedKb }} KB
            </span>
            <span v-if="submission.result?.score != null" class="result-score">
              Score {{ submission.result.score }}
            </span>
          </div>

          <div v-if="submission.progress && !isTerminal(submission.status)" class="progress">
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
            </div>
            <span class="progress-text">
              {{ submission.progress.finishedCases }} / {{ submission.progress.totalCases }}
            </span>
          </div>

          <pre
            v-if="submission.result?.compileErrorLog"
            class="compile-error"
          >{{ submission.result.compileErrorLog }}</pre>

          <div v-if="submission.result?.testCasesReport?.length" class="node-statuses">
            <span
              v-for="(c, i) in submission.result.testCasesReport"
              :key="c.caseId"
              class="node-status"
              :class="statusTone(c.status as SubmissionStatus)"
            >
              <span class="node-no">#{{ i + 1 }}</span>
              <span class="node-abbr">{{ STATUS_ABBR[c.status as SubmissionStatus] ?? c.status }}</span>
              <span class="node-meta"
                >{{ c.timeMs != null ? c.timeMs + 'ms' : '-' }} ·
                {{ c.memoryKb != null ? c.memoryKb + 'KB' : '-' }}</span
              >
            </span>
          </div>

        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.detail {
  padding-top: 32px;
  padding-bottom: 56px;
}
.head {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
}
.title {
  margin: 0;
  font-size: 26px;
}
.meta {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
  color: var(--ink-tertiary);
  font-size: 13px;
  margin-bottom: 20px;
}
.section-title {
  margin: 32px 0 16px;
  font-size: 18px;
}
.case {
  margin-bottom: 16px;
  overflow: hidden;
}
.case-head {
  padding: 12px 20px;
  font-weight: 600;
  border-bottom: 1px solid var(--line);
  background: var(--bg);
}
.case-block {
  padding: 14px 20px;
  border-bottom: 1px solid var(--line);
}
.case-block:last-child {
  border-bottom: none;
}
.case-label {
  color: var(--ink-tertiary);
  font-size: 12px;
  margin-bottom: 6px;
}
.case-block pre {
  margin: 0;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 14px;
  white-space: pre-wrap;
  word-break: break-all;
}

/* 提交区 */
.submit-card {
  padding: 20px;
}
.lang-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}
.lang-tab {
  padding: 6px 16px;
  border: 1px solid var(--line);
  border-radius: var(--pill);
  font-size: 13px;
  color: var(--ink-secondary);
  transition:
    border-color 0.15s ease,
    color 0.15s ease;
}
.lang-tab:hover {
  color: var(--ink);
}
.lang-tab.active {
  border-color: var(--ink);
  color: var(--ink);
  font-weight: 600;
}
.submit-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 16px;
}
.submission-id {
  color: var(--ink-tertiary);
  font-size: 13px;
}

/* 结果面板 */
.result {
  margin-top: 20px;
  border-top: 1px solid var(--line);
  padding-top: 16px;
}
.result-head {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  font-weight: 600;
}
.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--ink-tertiary);
}
.result-head.ok .status-dot {
  background: var(--success);
}
.result-head.ok .status-text {
  color: var(--success);
}
.result-head.wrong .status-dot {
  background: var(--danger);
}
.result-head.wrong .status-text {
  color: var(--danger);
}
.result-stat {
  color: var(--ink-secondary);
  font-size: 13px;
  font-weight: 400;
}
.result-score {
  margin-left: auto;
  color: var(--ink);
}

.progress {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 14px;
}
.progress-bar {
  flex: 1;
  height: 6px;
  border-radius: var(--pill);
  background: var(--bg);
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  background: var(--ink);
  border-radius: var(--pill);
  transition: width 0.3s ease;
}
.progress-text {
  color: var(--ink-tertiary);
  font-size: 12px;
  white-space: nowrap;
}

.compile-error {
  margin: 14px 0 0;
  padding: 14px;
  background: var(--danger-bg);
  color: var(--danger);
  border-radius: 10px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 13px;
  white-space: pre-wrap;
  word-break: break-all;
}

.node-statuses {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}
.node-status {
  position: relative;
  width: 150px;
  height: 150px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  color: #fff;
}
.node-status.ac {
  background: var(--success);
}
.node-status.wa {
  background: var(--danger);
}
.node-status.tle {
  background: #000;
}
.node-status.mle {
  background: #f57c00;
}
.node-status.re {
  background: #9c27b0;
}
.node-status.ce,
.node-status.se {
  background: #607d8b;
}
.node-no {
  position: absolute;
  top: 8px;
  left: 10px;
  font-size: 12px;
  font-weight: 400;
  opacity: 0.85;
  line-height: 1;
}
.node-abbr {
  font-size: 22px;
  font-weight: 700;
  line-height: 1;
}
.node-meta {
  font-size: 11px;
  font-weight: 400;
  opacity: 0.9;
  line-height: 1.3;
}

</style>
