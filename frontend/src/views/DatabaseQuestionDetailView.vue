<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { databaseApi } from '@/api/database'
import { renderMarkdown } from '@/utils/markdown'
import { useUserStore } from '@/stores/user'
import CodeEditor from '@/components/CodeEditor.vue'
import type { DbQuestion, DbQuestionType, Submission, SubmissionStatus } from '@/types'
import { STATUS_ABBR, statusTone } from '@/utils/submission'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const id = computed(() => Number(route.params.id))

const question = ref<DbQuestion | null>(null)
const loading = ref(true)
const error = ref('')

const typeLabels: Record<DbQuestionType, string> = {
  SELECT: 'SELECT 查询',
  DML: 'DML 数据操作',
}

const descriptionHtml = computed(() => renderMarkdown(question.value?.description ?? ''))
const sampleCases = computed(() => (question.value?.testCases ?? []).filter((tc) => tc.isSample === 1))
/** 结果面板要用它区分「没有比对结果是因为隐藏」与「本来就没有可比对内容」。 */
const sampleCaseIds = computed(
  () => new Set(sampleCases.value.map((tc) => tc.id).filter((id): id is number => id != null)),
)

// ---- SQL 提交与评测 ----
const SQL_TEMPLATE = '-- 在此编写你的 SQL（单条语句）\n'
const TERMINAL_STATUSES: SubmissionStatus[] = [
  'ACCEPTED',
  'WRONG_ANSWER',
  'TIME_LIMIT_EXCEEDED',
  'MEMORY_LIMIT_EXCEEDED',
  'RUNTIME_ERROR',
  'COMPILE_ERROR',
  'SYSTEM_ERROR',
]

const sql = ref(SQL_TEMPLATE)
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

/** 判题机对 WA 用例写入结果集 JSON；语法错误等写入纯文本。 */
interface ParsedResultSet {
  columns: Array<{ name: string; type: string }>
  rows: string[][]
}

function parseResultSet(raw: string | null): ParsedResultSet | null {
  if (!raw) return null
  try {
    const obj = JSON.parse(raw) as ParsedResultSet
    if (obj && Array.isArray(obj.columns) && Array.isArray(obj.rows)) return obj
  } catch {
    /* 非结果集 JSON，按文本展示 */
  }
  return null
}

function cellText(value: string): string {
  return value === 'NULL' ? 'NULL' : value
}

/** 逐用例整理展示数据：结果集解析为表格，其余（错误信息等）按文本展示。 */
interface ReportItem {
  caseId: number
  /** 测试点序号（从 1 开始，与状态方块上的 #N 一致）。 */
  index: number
  status: string
  timeMs: number | null
  isSample: boolean
  expected: ParsedResultSet | null
  actual: ParsedResultSet | null
  expectedRaw: string | null
  actualRaw: string | null
}

const reports = computed<ReportItem[]>(() =>
  (submission.value?.result?.testCasesReport ?? []).map((c, i) => ({
    caseId: c.caseId,
    index: i + 1,
    status: c.status,
    timeMs: c.timeMs,
    isSample: sampleCaseIds.value.has(c.caseId),
    expected: parseResultSet(c.expectedOutput),
    actual: parseResultSet(c.actualOutput),
    expectedRaw: c.expectedOutput,
    actualRaw: c.actualOutput,
  })),
)

/** 判题机只在用例未通过时写回期望/实际结果，有内容的才展开比对详情。 */
const diffReports = computed(() =>
  reports.value.filter((r) => r.expectedRaw != null || r.actualRaw != null),
)

/** 未通过却没有比对结果的非样例测试点（被后端脱敏，或判题机本就不产出比对）。 */
const maskedCount = computed(
  () =>
    reports.value.filter(
      (r) => !r.isSample && r.status !== 'ACCEPTED' && r.expectedRaw == null && r.actualRaw == null,
    ).length,
)

async function submit() {
  if (submitting.value) return
  if (!userStore.isLoggedIn) {
    submitError.value = '请先登录后再提交'
    return
  }
  if (!sql.value.trim()) {
    submitError.value = '请先编写 SQL'
    return
  }
  submitting.value = true
  submitError.value = ''
  submission.value = null
  try {
    const s = await databaseApi.submit(id.value, sql.value)
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
      const s = await databaseApi.getSubmission(submissionId)
      submission.value = s
      if (isTerminal(s.status)) stopPolling()
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
    question.value = await databaseApi.detail(id.value)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
})

onBeforeUnmount(stopPolling)
</script>

<template>
  <div class="detail container">
    <button class="back" @click="router.back()">← 返回</button>

    <p v-if="loading" class="muted">加载中…</p>
    <div v-else-if="error" class="submit-error">{{ error }}</div>

    <template v-else-if="question">
      <div class="head">
        <h1 class="title">{{ question.title }}</h1>
        <span class="badge" :class="`type-${question.type.toLowerCase()}`">
          {{ typeLabels[question.type] }}
        </span>
      </div>
      <div class="meta">
        <span>方言 {{ question.dialect }}</span>
        <span>共 {{ question.testCases?.length ?? 0 }} 个测试点</span>
        <span v-if="question.orderSensitive">严格行序</span>
        <span v-if="question.status !== 1">未公开</span>
      </div>

      <!-- eslint-disable-next-line vue/no-v-html -- 内容已由 renderMarkdown 清洗 -->
      <div class="prose card" v-html="descriptionHtml"></div>

      <h2 class="section-title">表结构</h2>
      <div class="schema card">
        <pre class="sql">{{ question.schemaSql }}</pre>
      </div>

      <template v-if="sampleCases.length">
        <h2 class="section-title">样例数据</h2>
        <div v-for="(tc, i) in sampleCases" :key="tc.id ?? i" class="case card">
          <div class="case-head">样例 {{ i + 1 }}</div>
          <div class="case-block">
            <div class="case-label">初始数据</div>
            <pre class="sql">{{ tc.seedSql }}</pre>
          </div>
        </div>
      </template>

      <h2 class="section-title">提交 SQL</h2>
      <div class="submit-card card">
        <CodeEditor v-model="sql" language="mysql" />

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
            <span class="status-text">{{ STATUS_ABBR[submission.status] ?? submission.status }}</span>
            <span v-if="submission.result?.timeUsedMs != null" class="result-stat">
              {{ submission.result.timeUsedMs }} ms
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

          <!-- 测试点状态：沿用算法题的结果方块 -->
          <div v-if="reports.length" class="node-statuses">
            <span
              v-for="r in reports"
              :key="r.caseId"
              class="node-status"
              :class="statusTone(r.status as SubmissionStatus)"
            >
              <span class="node-no">#{{ r.index }}</span>
              <span class="node-abbr">
                {{ STATUS_ABBR[r.status as SubmissionStatus] ?? r.status }}
              </span>
              <span class="node-meta">{{ r.timeMs != null ? `${r.timeMs}ms` : '-' }}</span>
            </span>
          </div>

          <template v-if="diffReports.length">
            <h3 class="diff-title">比对详情</h3>
            <div v-for="r in diffReports" :key="r.caseId" class="report card">
              <div class="report-head">
                <span class="node-status" :class="statusTone(r.status as SubmissionStatus)">
                  {{ STATUS_ABBR[r.status as SubmissionStatus] ?? r.status }}
                </span>
                <span class="report-no">测试点 {{ r.index }}</span>
                <span v-if="r.timeMs != null" class="report-meta">{{ r.timeMs }} ms</span>
              </div>

              <div class="report-body">
                <div class="report-col">
                  <div class="case-label">期望结果</div>
                  <div v-if="r.expected" class="table-wrap">
                    <table class="rs-table">
                      <thead>
                        <tr>
                          <th v-for="col in r.expected.columns" :key="col.name">{{ col.name }}</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr v-for="(row, ri) in r.expected.rows" :key="ri">
                          <td v-for="(cell, ci) in row" :key="ci">{{ cellText(cell) }}</td>
                        </tr>
                        <tr v-if="r.expected.rows.length === 0">
                          <td :colspan="r.expected.columns.length || 1" class="empty-cell">
                            （空结果集）
                          </td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                  <pre v-else class="sql plain">{{ r.expectedRaw }}</pre>
                </div>

                <div class="report-col">
                  <div class="case-label">实际结果</div>
                  <div v-if="r.actual" class="table-wrap">
                    <table class="rs-table">
                      <thead>
                        <tr>
                          <th v-for="col in r.actual.columns" :key="col.name">{{ col.name }}</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr v-for="(row, ri) in r.actual.rows" :key="ri">
                          <td v-for="(cell, ci) in row" :key="ci">{{ cellText(cell) }}</td>
                        </tr>
                        <tr v-if="r.actual.rows.length === 0">
                          <td :colspan="r.actual.columns.length || 1" class="empty-cell">
                            （空结果集）
                          </td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                  <pre v-else class="sql plain error-text">{{ r.actualRaw }}</pre>
                </div>
              </div>
            </div>
          </template>

          <!-- 非样例测试点的比对结果由后端脱敏，说明一句，免得误以为判题没跑 -->
          <p v-if="maskedCount" class="report-note">
            另有 {{ maskedCount }} 个非样例测试点仅回报状态，不展示比对结果
          </p>
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
.schema {
  padding: 16px 20px;
  overflow-x: auto;
}
.case {
  margin-bottom: 16px;
  overflow: hidden;
}
.case-head {
  display: flex;
  align-items: center;
  gap: 12px;
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
.sql {
  margin: 0;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}
.sql.plain {
  padding: 12px 14px;
  background: var(--bg);
  border-radius: 0;
  font-size: 12px;
}
.error-text {
  color: var(--danger);
  background: var(--danger-bg);
}

/* 提交区 */
.submit-card {
  padding: 20px;
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
  border-radius: 0;
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

/* 测试点状态方块：与算法题结果面板保持一致（尺寸/配色/字体均照搬） */
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

/* 比对详情 */
.diff-title {
  margin: 20px 0 0;
  font-size: 14px;
  color: var(--ink-secondary);
}
.report {
  margin-top: 14px;
  padding: 14px 16px;
}
.report-head {
  display: flex;
  align-items: center;
  gap: 12px;
}
/* 详情卡头部复用 .node-status 的配色，但收成一行内的小徽章 */
.report-head .node-status {
  position: static;
  width: auto;
  height: auto;
  min-width: 44px;
  padding: 2px 10px;
  border-radius: var(--pill);
  font-size: 12px;
  font-weight: 600;
  text-align: center;
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
.node-status.run {
  background: #9e9e9e;
}
.report-no {
  font-weight: 600;
  font-size: 14px;
}
.report-meta {
  color: var(--ink-tertiary);
  font-size: 13px;
}
.report-note {
  margin: 10px 0 0;
  color: var(--ink-tertiary);
  font-size: 13px;
}
.report-body {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 16px;
  margin-top: 14px;
}
.report-col {
  min-width: 0;
}
.table-wrap {
  overflow-x: auto;
  border: 1px solid var(--line);
  border-radius: 0;
}
.rs-table {
  width: 100%;
  border-collapse: collapse;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 12px;
}
.rs-table th,
.rs-table td {
  padding: 6px 12px;
  text-align: left;
  border-bottom: 1px solid var(--line);
  white-space: nowrap;
}
.rs-table thead th {
  background: var(--bg);
  color: var(--ink-secondary);
  font-weight: 600;
}
.rs-table tbody tr:last-child td {
  border-bottom: none;
}
.empty-cell {
  color: var(--ink-tertiary);
  text-align: center;
}
</style>
