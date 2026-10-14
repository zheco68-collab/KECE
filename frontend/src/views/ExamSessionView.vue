<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  examApi,
  type ExamSessionQuestion,
  type RankRow,
  type SessionEnter,
} from '@/api/exam'
import { ApiError } from '@/api/request'
import { databaseApi } from '@/api/database'
import { submissionApi } from '@/api/submission'
import CodeEditor from '@/components/CodeEditor.vue'
import { useConfirm } from '@/composables/useConfirm'
import { useToast } from '@/composables/useToast'
import { renderMarkdown } from '@/utils/markdown'
import { STATUS_ABBR, statusTone } from '@/utils/submission'
import type { Submission, SubmissionStatus } from '@/types'

const route = useRoute()
const router = useRouter()
const { confirm } = useConfirm()
const { success, error, info } = useToast()

const examId = computed(() => Number(route.params.id))
const enter = ref<SessionEnter | null>(null)
const loading = ref(true)
const loadError = ref('')

const currentIndex = ref(0)
const remaining = ref(0)
const submitting = ref(false)
const switchCount = ref(0)
let timer: ReturnType<typeof setInterval> | null = null

/**
 * 每题作答状态
 */
interface AnswerState {
  saved: boolean
  code: string
  language: string
  selected: string[]
  fillText: string
}
const answers = reactive<Record<number, AnswerState>>({})
const savedSet = ref(new Set<number>())

/** ACM 单题实时评测状态（仅 ALGO/DB 使用） */
interface JudgeState {
  submitting: boolean
  status: string | null
  score: number | null
}
const judgeState = reactive<Record<number, JudgeState>>({})
let pollTimer: ReturnType<typeof setInterval> | null = null

const TERMINAL_STATUSES: SubmissionStatus[] = [
  'ACCEPTED',
  'WRONG_ANSWER',
  'TIME_LIMIT_EXCEEDED',
  'MEMORY_LIMIT_EXCEEDED',
  'RUNTIME_ERROR',
  'COMPILE_ERROR',
  'SYSTEM_ERROR',
]

const currentQuestion = computed<ExamSessionQuestion | null>(() => {
  const qs = enter.value?.questions
  if (!qs || qs.length === 0) return null
  return qs[currentIndex.value]
})

const answerState = computed(() => {
  const q = currentQuestion.value
  if (!q) return null
  return answers[q.id] ?? null
})

const currentJudge = computed<JudgeState | null>(() => {
  const q = currentQuestion.value
  if (!q) return null
  return judgeState[q.id] ?? null
})

const countdownText = computed(() => {
  const s = Math.max(remaining.value, 0)
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  const sec = s % 60
  return [h, m, sec].map((v) => String(v).padStart(2, '0')).join(':')
})

function startTimer() {
  if (timer) clearInterval(timer)
  timer = setInterval(() => {
    remaining.value -= 1
    if (remaining.value <= 0) {
      clearInterval(timer!)
      timer = null
      autoSubmit()
    }
  }, 1000)
}

function optionLetter(index: number): string {
  return String.fromCharCode(65 + index)
}

function optionText(option: string): string {
  return option.replace(/^[A-Za-z][.、)）:：]\s*/, '')
}

const CODE_FILES: Record<string, string> = {
  cpp: 'main.cpp',
  java: 'Main.java',
  python: 'main.py',
  arkts: 'main.ets',
}

function algoAnswer(state: { code: string; language: string }): string {
  const file = CODE_FILES[state.language] ?? 'main.cpp'
  return JSON.stringify({ [file]: state.code })
}

function buildAnswer(q: ExamSessionQuestion, state: AnswerState): string {
  if (q.questionType === 'ALGO') {
    return algoAnswer(state)
  }
  if (q.questionType === 'DB') {
    return state.code
  }
  if (q.objectiveType === 'FILL') {
    return state.fillText
  }
  return state.selected.join(',')
}

function buildLanguage(q: ExamSessionQuestion, state: AnswerState): string | undefined {
  return q.questionType === 'ALGO' ? state.language : undefined
}

async function copySchema() {
  if (!currentQuestion.value?.schemaSql) return
  try {
    await navigator.clipboard.writeText(currentQuestion.value.schemaSql)
    success('建表 DDL 已复制到剪贴板')
  } catch {
    error('复制失败，请手动选取')
  }
}

async function saveCurrent(silent = false) {
  const q = currentQuestion.value
  const state = answerState.value
  if (!q || !state) return
  try {
    await examApi.saveAnswer(examId.value, {
      examQuestionId: q.id,
      answer: buildAnswer(q, state),
      language: buildLanguage(q, state),
    })
    state.saved = true
    savedSet.value.add(q.id)
    if (!silent) {
      success(`第 ${currentIndex.value + 1} 题作答已保存`)
    }
  } catch (e) {
    state.saved = false
    if (!silent) {
      error(e instanceof ApiError ? e.message : '保存失败')
    }
  }
}

function stopPoll() {
  if (pollTimer !== null) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

async function submitCurrent() {
  const q = currentQuestion.value
  const state = answerState.value
  if (!q || !state) return

  const isEmpty =
    q.questionType === 'ALGO' || q.questionType === 'DB'
      ? state.code.trim() === ''
      : q.objectiveType === 'FILL'
        ? state.fillText.trim() === ''
        : state.selected.length === 0

  if (isEmpty) {
    error('请先编写作答内容再提交评测')
    return
  }

  if (judgeState[q.id]?.submitting) return
  if (!judgeState[q.id]) judgeState[q.id] = { submitting: false, status: null, score: null }
  const st = judgeState[q.id]
  st.submitting = true
  st.status = null
  st.score = null

  try {
    await examApi.saveAnswer(examId.value, {
      examQuestionId: q.id,
      answer: buildAnswer(q, state),
      language: buildLanguage(q, state),
    })
    state.saved = true
    savedSet.value.add(q.id)

    const res = await examApi.submitQuestion(examId.value, q.id)
    info('已提交沙箱，评测中…')
    startPoll(q.id, res.submissionId, res.questionType)
  } catch (e) {
    st.submitting = false
    error(e instanceof ApiError ? e.message : '提交失败')
  }
}

function startPoll(questionId: number, submissionId: number, type: 'ALGO' | 'DB') {
  stopPoll()
  pollTimer = setInterval(async () => {
    try {
      const s: Submission =
        type === 'ALGO'
          ? await submissionApi.get(submissionId)
          : await databaseApi.getSubmission(submissionId)
      const st = judgeState[questionId]
      if (st) {
        st.status = s.status
        st.score = s.result?.score ?? null
      }
      if (TERMINAL_STATUSES.includes(s.status)) {
        stopPoll()
        if (st) st.submitting = false
        if (s.status === 'ACCEPTED') {
          success(`第 ${currentIndex.value + 1} 题评测通过 (AC)`)
        } else {
          info(`第 ${currentIndex.value + 1} 题评测未通过 (${STATUS_ABBR[s.status] || s.status})，可修改重新提交`)
        }
      }
    } catch {
      stopPoll()
      if (judgeState[questionId]) judgeState[questionId].submitting = false
    }
  }, 1000)
}

watch(currentIndex, (_newIdx, oldIdx) => {
  stopPoll()
  // 切换题目时静默暂存上一题
  const prevQ = enter.value?.questions?.[oldIdx]
  const prevState = prevQ ? answers[prevQ.id] : null
  if (prevQ && prevState) {
    examApi
      .saveAnswer(examId.value, {
        examQuestionId: prevQ.id,
        answer: buildAnswer(prevQ, prevState),
        language: buildLanguage(prevQ, prevState),
      })
      .then(() => {
        prevState.saved = true
        savedSet.value.add(prevQ.id)
      })
      .catch(() => {
        /* 静默保存失败 */
      })
  }
})

async function doSubmit() {
  if (submitting.value) return
  submitting.value = true
  try {
    const q = currentQuestion.value
    const state = answerState.value
    if (q && state) {
      try {
        await examApi.saveAnswer(examId.value, {
          examQuestionId: q.id,
          answer: buildAnswer(q, state),
          language: buildLanguage(q, state),
        })
      } catch {
        /* 保存失败不阻塞交卷 */
      }
    }
    const res = await examApi.submit(examId.value)
    success(`交卷成功：${res.graded} 题已判分，${res.queued} 题后台评测中`)
    router.push(`/exams/${examId.value}/result`)
  } catch (e) {
    error(e instanceof ApiError ? e.message : '交卷失败')
  } finally {
    submitting.value = false
  }
}

async function submitWithConfirm() {
  const unansweredCount = (enter.value?.questions?.length ?? 0) - savedSet.value.size
  const extraTip =
    unansweredCount > 0
      ? `目前还有 ${unansweredCount} 道题未保存作答。`
      : '所有题目均已完成作答保存。'

  const ok = await confirm({
    title: '确认提交全卷',
    message: `${extraTip}交卷后将锁定作答并生成成绩战报，确定现在交卷吗？`,
    confirmText: '确认交卷',
    cancelText: '继续答题',
  })
  if (ok) await doSubmit()
}

async function autoSubmit() {
  if (submitting.value) return
  info('考试倒计时已截止，系统自动锁定并交卷')
  await doSubmit()
}

// ==================== 切屏反作弊监听 ====================
async function handleVisibilityChange() {
  if (document.hidden && !submitting.value) {
    try {
      const res = await examApi.reportSwitch(examId.value, 1)
      switchCount.value = res.count
      info(`[监控提醒] 检测到离开考试界面，当前切屏已累计记录 ${switchCount.value} 次`)
    } catch {
      /* 忽略上报异常 */
    }
  }
}

// ==================== 实时排名 ====================
const rankingVisible = ref(false)
const ranking = ref<RankRow[]>([])
const rankingLoading = ref(false)
const rankRows = computed(() => ranking.value.map((row, index) => ({ ...row, rank: index + 1 })))
let rankTimer: ReturnType<typeof setInterval> | null = null

async function loadRanking() {
  rankingLoading.value = true
  try {
    ranking.value = await examApi.ranking(examId.value)
  } catch (e) {
    error(e instanceof ApiError ? e.message : '排名加载失败')
  } finally {
    rankingLoading.value = false
  }
}

function openRanking() {
  rankingVisible.value = true
  loadRanking()
  if (rankTimer) clearInterval(rankTimer)
  rankTimer = setInterval(loadRanking, 10000)
}

function closeRanking() {
  rankingVisible.value = false
  if (rankTimer) {
    clearInterval(rankTimer)
    rankTimer = null
  }
}

onMounted(async () => {
  document.addEventListener('visibilitychange', handleVisibilityChange)
  try {
    enter.value = await examApi.enter(examId.value)
    for (const q of enter.value.questions) {
      answers[q.id] = {
        saved: false,
        code: q.questionType === 'DB' ? (q.schemaSql ? '-- 编写 SQL 语句\n' : '') : '',
        language: 'cpp',
        selected: [],
        fillText: '',
      }
    }
    remaining.value = enter.value.remainingSeconds
    startTimer()

    // 拉取本人已有切屏次数
    try {
      const sw = await examApi.mySwitchCount(examId.value)
      switchCount.value = sw.count
    } catch {
      /* 忽略 */
    }
  } catch (e) {
    loadError.value = e instanceof ApiError ? e.message : '进入考场失败'
  } finally {
    loading.value = false
  }
})

onBeforeUnmount(() => {
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  if (timer) clearInterval(timer)
  stopPoll()
  if (rankTimer) clearInterval(rankTimer)
})
</script>

<template>
  <div class="exam-station">
    <!-- 加载与异常状态 -->
    <div v-if="loading" class="state-full card">
      <div class="loading-spinner"></div>
      <p class="muted">正在初始化全真隔离考场环境并下载题目快照…</p>
    </div>

    <div v-else-if="loadError" class="state-full card">
      <div class="error-title">进入考场受限</div>
      <p class="error-msg">{{ loadError }}</p>
      <div style="display: flex; gap: 12px; justify-content: center;">
        <button class="btn btn-outline" @click="router.push('/exams')">返回考试大厅</button>
        <button class="btn btn-primary" @click="router.push(`/exams/${examId}/result`)">查看考试战报 →</button>
      </div>
    </div>

    <template v-else-if="enter">
      <!-- 顶部控制仪表盘 (Header Dashboard) -->
      <header class="dash-bar card">
        <div class="dash-left">
          <div class="exam-title-group">
            <span class="badge mode-tag" :class="enter.mode === 'ACM' ? 'mode-acm' : 'mode-oi'">
              {{ enter.mode === 'ACM' ? 'ACM 赛制' : 'OI 赛制' }}
            </span>
            <h1 class="exam-station-title">{{ enter.examTitle }}</h1>
          </div>

          <div class="progress-pill">
            <span class="pill-k">答题进度</span>
            <span class="pill-v mono">{{ savedSet.size }} / {{ enter.questions.length }}</span>
          </div>

          <div v-if="switchCount > 0" class="switch-pill" title="切屏监控已启用">
            <span class="switch-warn-dot"></span>
            <span>切屏 {{ switchCount }} 次</span>
          </div>
        </div>

        <div class="dash-right">
          <!-- 等宽数码倒计时时钟 -->
          <div class="clock-box" :class="{ alert: remaining < 300 }">
            <span class="clock-label">剩余作答时间</span>
            <span class="clock-digits mono">{{ countdownText }}</span>
          </div>

          <button class="btn btn-outline btn-rank" type="button" @click="openRanking">
            <svg class="rank-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M18 20V10" />
              <path d="M12 20V4" />
              <path d="M6 20v-6" />
            </svg>
            <span>实时排名</span>
          </button>

          <button
            class="btn btn-primary btn-submit-exam"
            :disabled="submitting"
            type="button"
            @click="submitWithConfirm"
          >
            {{ submitting ? '交卷归档中…' : '交卷' }}
          </button>
        </div>
      </header>

      <!-- 考场核心交互工作区：左侧题号方块导轨 + 右侧双栏开阔分屏 -->
      <div class="station-body">
        <!-- -------------------------------------------------------------
             用户的核心标志性设计：直角方块题号导轨（完全保留并强化设计感）
             ------------------------------------------------------------- -->
        <aside class="nav-rail card">
          <div class="nav-rail-head">
            <span class="rail-title">考卷试题矩阵</span>
            <span class="rail-count mono">{{ enter.questions.length }} 题</span>
          </div>

          <div class="q-squares-list">
            <button
              v-for="(q, i) in enter.questions"
              :key="q.id"
              class="q-nav-item"
              :class="{
                active: i === currentIndex,
                saved: savedSet.has(q.id),
                ac: judgeState[q.id]?.status === 'ACCEPTED',
                wa: judgeState[q.id]?.status && judgeState[q.id]?.status !== 'ACCEPTED',
              }"
              @click="currentIndex = i"
            >
              <!-- 核心直角方块题号标识 (严格 0-radius) -->
              <div class="q-square-block">
                <span class="sq-no">{{ i + 1 < 10 ? `0${i + 1}` : i + 1 }}</span>
                <span v-if="judgeState[q.id]?.status === 'ACCEPTED'" class="sq-status-icon">✓</span>
              </div>

              <div class="q-item-info">
                <div class="q-item-top">
                  <span class="q-type-label">
                    {{
                      q.questionType === 'ALGO'
                        ? '编程'
                        : q.questionType === 'DB'
                          ? 'SQL'
                          : q.objectiveType === 'FILL'
                            ? '填空'
                            : '选择'
                    }}
                  </span>
                  <span class="q-score-label mono">{{ q.score }}分</span>
                </div>
                <span class="q-item-title">{{ q.title }}</span>
              </div>
            </button>
          </div>
        </aside>

        <!-- 答题工作台：双栏开阔联动分屏 -->
        <main v-if="currentQuestion" class="workspace card">
          <div class="workspace-grid">
            <!-- 左栏：题面要求与规格 -->
            <section class="left-pane">
              <div class="pane-head">
                <div class="pane-q-title-row">
                  <span class="q-cur-no">试题 #{{ currentIndex + 1 }}</span>
                  <h2 class="pane-title">{{ currentQuestion.title }}</h2>
                </div>

                <div class="pane-tags">
                  <span class="badge" :class="`type-${currentQuestion.questionType.toLowerCase()}`">
                    {{
                      currentQuestion.questionType === 'ALGO'
                        ? '编程算法题'
                        : currentQuestion.questionType === 'DB'
                          ? '数据库 SQL'
                          : currentQuestion.objectiveType === 'FILL'
                            ? '客观填空题'
                            : '客观选择题'
                    }}
                  </span>
                  <span class="badge score-badge mono">满分 {{ currentQuestion.score }} 分</span>
                  <span v-if="currentQuestion.timeLimitMs" class="spec-text mono">
                    {{ currentQuestion.timeLimitMs }} ms · {{ currentQuestion.memoryLimitMb }} MB
                  </span>
                </div>
              </div>

              <!-- 题面 Markdown 渲染正文 -->
              <div class="pane-content prose" v-html="renderMarkdown(currentQuestion.content ?? '')"></div>

              <!-- 数据库表结构 DDL 参考抽屉 -->
              <div v-if="currentQuestion.questionType === 'DB' && currentQuestion.schemaSql" class="schema-box">
                <div class="schema-box-head">
                  <span class="schema-title">数据库 Schema 表结构 (参考)</span>
                  <button class="btn-copy-schema" @click="copySchema">复制 DDL</button>
                </div>
                <pre class="schema-code">{{ currentQuestion.schemaSql }}</pre>
              </div>
            </section>

            <!-- 右栏：作答与评测执行区 -->
            <section class="right-pane">
              <!-- 编程题作答区 -->
              <template v-if="currentQuestion.questionType === 'ALGO' && answerState">
                <div class="editor-ctrl-bar">
                  <div class="editor-ctrl-left">
                    <span class="ctrl-label">作答语言</span>
                    <select v-model="answerState.language" class="select-lang">
                      <option value="cpp">C++ 17</option>
                      <option value="java">Java 17</option>
                      <option value="python">Python 3.10</option>
                      <option value="arkts">ArkTS</option>
                    </select>
                  </div>
                  <div class="editor-ctrl-right">
                    <span class="shortcut-tip">Ctrl+Enter 提交评测</span>
                  </div>
                </div>

                <div class="editor-wrapper">
                  <CodeEditor v-model="answerState.code" :language="answerState.language" />
                </div>
              </template>

              <!-- 数据库 SQL 作答区 -->
              <template v-else-if="currentQuestion.questionType === 'DB' && answerState">
                <div class="editor-ctrl-bar">
                  <div class="editor-ctrl-left">
                    <span class="ctrl-label">SQL 方言</span>
                    <span class="dialect-pill">MySQL 8.0</span>
                  </div>
                  <div class="editor-ctrl-right">
                    <span class="shortcut-tip">单条 SQL 语句</span>
                  </div>
                </div>

                <div class="editor-wrapper">
                  <CodeEditor v-model="answerState.code" language="mysql" />
                </div>
              </template>

              <!-- 客观题作答区 -->
              <template v-else-if="currentQuestion.questionType === 'OBJECTIVE' && answerState">
                <div class="objective-pane">
                  <div class="obj-hint-bar">
                    <span v-if="currentQuestion.objectiveType === 'SINGLE'">单选题 · 请选取唯一正确选项</span>
                    <span v-else-if="currentQuestion.objectiveType === 'MULTIPLE'">多选题 · 请选取所有符合条件的选项</span>
                    <span v-else>填空题 · 请在输入框中填入精准答案文本</span>
                  </div>

                  <!-- 填空题输入 -->
                  <div v-if="currentQuestion.objectiveType === 'FILL'" class="fill-box">
                    <label class="fill-label">考生作答内容：</label>
                    <input
                      v-model="answerState.fillText"
                      class="input fill-input"
                      type="text"
                      placeholder="在此输入你的填空答案…"
                      @keyup.enter="saveCurrent(false)"
                    />
                  </div>

                  <!-- 选择题选项列表（直角方块字母 A/B/C/D） -->
                  <div v-else class="options-container">
                    <label
                      v-for="(opt, i) in currentQuestion.options ?? []"
                      :key="i"
                      class="opt-card"
                      :class="{ selected: answerState.selected.includes(optionLetter(i)) }"
                    >
                      <!-- 单选 radio -->
                      <input
                        v-if="currentQuestion.objectiveType === 'SINGLE'"
                        type="radio"
                        :name="`exam-q-${currentQuestion.id}`"
                        class="opt-native-input"
                        :checked="answerState.selected[0] === optionLetter(i)"
                        @change="answerState.selected = [optionLetter(i)]; saveCurrent(true)"
                      />
                      <!-- 多选 checkbox -->
                      <input
                        v-else
                        v-model="answerState.selected"
                        type="checkbox"
                        class="opt-native-input"
                        :value="optionLetter(i)"
                        @change="saveCurrent(true)"
                      />

                      <!-- 直角选项字母大方块 -->
                      <span class="opt-square-letter mono">{{ optionLetter(i) }}</span>
                      <span class="opt-desc">{{ optionText(opt) }}</span>
                    </label>
                  </div>
                </div>
              </template>

              <!-- 右栏底部统一操作栏 -->
              <footer class="pane-footer">
                <div class="footer-left">
                  <!-- ACM 实时提交按钮与状态 -->
                  <template
                    v-if="
                      enter.mode === 'ACM' &&
                      (currentQuestion.questionType === 'ALGO' || currentQuestion.questionType === 'DB')
                    "
                  >
                    <button
                      class="btn btn-primary btn-judge"
                      :disabled="currentJudge?.submitting"
                      type="button"
                      @click="submitCurrent"
                    >
                      <span>{{ currentJudge?.submitting ? '沙箱评测中…' : '提交本题评测' }}</span>
                    </button>

                    <div
                      v-if="currentJudge?.status"
                      class="judge-status-block"
                      :class="statusTone(currentJudge.status as SubmissionStatus)"
                    >
                      <span class="status-name mono">
                        {{ STATUS_ABBR[currentJudge.status as SubmissionStatus] ?? currentJudge.status }}
                      </span>
                      <span v-if="currentJudge.score != null" class="status-score mono">
                        {{ currentJudge.score }}分
                      </span>
                    </div>
                  </template>

                  <!-- 保存作答按钮 -->
                  <button
                    class="btn btn-outline btn-save"
                    type="button"
                    @click="saveCurrent(false)"
                  >
                    <span>{{ answerState?.saved ? '✓ 已保存作答' : '保存本题作答' }}</span>
                  </button>
                </div>

                <div class="footer-right">
                  <button
                    class="btn btn-ghost btn-sm"
                    :disabled="currentIndex <= 0"
                    type="button"
                    @click="currentIndex--"
                  >
                    ← 上一题
                  </button>
                  <button
                    class="btn btn-ghost btn-sm"
                    :disabled="currentIndex >= enter.questions.length - 1"
                    type="button"
                    @click="currentIndex++"
                  >
                    下一题 →
                  </button>
                </div>
              </footer>
            </section>
          </div>
        </main>
      </div>

      <!-- 实时排名弹窗 -->
      <Teleport to="body">
        <div v-if="rankingVisible" class="rank-overlay" @click.self="closeRanking">
          <div class="rank-modal card" role="dialog" aria-modal="true">
            <div class="rank-head">
              <div class="rank-head-title">
                <h3 class="rank-modal-h">ACM 全场实时排行榜</h3>
                <span class="rank-refresh-tag">每 10 秒自动轮询</span>
              </div>
              <button class="btn btn-ghost btn-sm" type="button" @click="closeRanking">关闭 [ESC]</button>
            </div>

            <div class="rank-body">
              <p v-if="rankingLoading && ranking.length === 0" class="muted loading-text">拉取天梯排名中…</p>
              <table v-else class="rank-table">
                <thead>
                  <tr>
                    <th class="th-rank">名次</th>
                    <th>考生</th>
                    <th class="th-num">通过 (AC)</th>
                    <th class="th-num">罚时 (分)</th>
                    <th class="th-num">综合得分</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="row in rankRows" :key="row.userId" :class="{ 'top-three': row.rank <= 3 }">
                    <td class="td-rank">
                      <span class="rank-badge-square mono" :class="`rank-${row.rank}`">
                        {{ row.rank }}
                      </span>
                    </td>
                    <td class="td-user font-bold">{{ row.username }}</td>
                    <td class="td-num mono font-bold text-success">{{ row.solved }} 题</td>
                    <td class="td-num mono">{{ row.penaltyMinutes }}</td>
                    <td class="td-num mono score-bold">{{ row.totalScore }} / {{ row.fullScore }}</td>
                  </tr>
                  <tr v-if="rankRows.length === 0">
                    <td colspan="5" class="empty-rank muted">暂无考生作答数据</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </Teleport>
    </template>
  </div>
</template>

<style scoped>
.exam-station {
  min-height: calc(100vh - 64px);
  padding: 16px 24px 40px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  background: var(--bg);
}

/* 全屏加载与异常 */
.state-full {
  margin: 64px auto;
  max-width: 600px;
  padding: 64px 32px;
  text-align: center;
  border-radius: 0;
}
.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--line);
  border-top-color: var(--ink);
  animation: spin 0.8s linear infinite;
  margin: 0 auto 16px;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}
.error-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--danger);
  margin-bottom: 8px;
}
.error-msg {
  color: var(--ink-secondary);
  margin-bottom: 24px;
}

/* 顶部控制仪表盘 */
.dash-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  border-radius: 0;
  gap: 16px;
  background: var(--surface);
  border: 1px solid var(--line);
}
.dash-left {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}
.exam-title-group {
  display: flex;
  align-items: center;
  gap: 10px;
}
.exam-station-title {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: var(--ink);
}
.mode-tag {
  border-radius: 0;
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
}
.mode-acm { background: var(--ink); color: #fff; }
.mode-oi { background: #333; color: #fff; }

.progress-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  background: var(--bg);
  border: 1px solid var(--line);
  font-size: 12px;
}
.pill-k { color: var(--ink-tertiary); }
.pill-v { font-weight: 700; color: var(--ink); }

.switch-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  background: #fff3e0;
  color: #e65100;
  border: 1px solid #ffe0b2;
  font-size: 12px;
  font-weight: 600;
}
.switch-warn-dot {
  width: 6px;
  height: 6px;
  background: #e65100;
}

.dash-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.clock-box {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  padding: 4px 12px;
  border: 1px solid var(--line);
  background: var(--bg);
}
.clock-label {
  font-size: 10px;
  color: var(--ink-tertiary);
  text-transform: uppercase;
  line-height: 1;
  margin-bottom: 2px;
}
.clock-digits {
  font-size: 18px;
  font-weight: 700;
  color: var(--ink);
  line-height: 1;
}
.clock-box.alert {
  border-color: var(--danger);
  background: #ffebee;
}
.clock-box.alert .clock-digits {
  color: var(--danger);
  animation: pulse 1s infinite alternate;
}

.btn-rank {
  border-radius: 0;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  padding: 6px 12px;
}
.rank-icon { width: 14px; height: 14px; }

.btn-submit-exam {
  border-radius: 0;
  padding: 8px 18px;
  font-weight: 700;
  font-size: 14px;
}

/* 考场核心交互工作区 */
.station-body {
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 16px;
  align-items: stretch;
}

/* -------------------------------------------------------------
   用户的核心标志性设计：直角方块题号导轨 (0-radius)
   ------------------------------------------------------------- */
.nav-rail {
  border-radius: 0;
  padding: 16px;
  background: var(--surface);
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: calc(100vh - 170px);
  overflow-y: auto;
}
.nav-rail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--line);
}
.rail-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--ink);
}
.rail-count {
  font-size: 12px;
  color: var(--ink-tertiary);
}

.q-squares-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.q-nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px;
  border: 1px solid transparent;
  background: transparent;
  cursor: pointer;
  text-align: left;
  transition: all 0.15s ease;
  border-radius: 0;
}
.q-nav-item:hover {
  background: var(--bg);
  border-color: var(--line);
}
.q-nav-item.active {
  background: var(--bg);
  border-color: var(--ink);
}

/* 核心直角方块 */
.q-square-block {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  border: 1px solid var(--line);
  background: var(--bg);
  color: var(--ink-secondary);
  border-radius: 0;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-weight: 700;
  font-size: 13px;
  transition: all 0.15s ease;
}
.q-nav-item.saved .q-square-block {
  background: var(--surface);
  color: var(--ink);
  border: 2px solid var(--ink);
}
.q-nav-item.active .q-square-block {
  background: var(--ink);
  color: #fff;
  border-color: var(--ink);
}
.q-nav-item.ac .q-square-block {
  background: var(--success);
  color: #fff;
  border-color: var(--success);
}
.q-nav-item.wa .q-square-block {
  background: var(--danger);
  color: #fff;
  border-color: var(--danger);
}
.sq-status-icon {
  position: absolute;
  bottom: 2px;
  right: 2px;
  font-size: 9px;
  line-height: 1;
}

.q-item-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.q-item-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.q-type-label {
  font-size: 11px;
  color: var(--ink-tertiary);
  font-weight: 600;
}
.q-score-label {
  font-size: 11px;
  color: var(--ink-secondary);
  font-weight: 600;
}
.q-item-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 主工作台双栏分屏 */
.workspace {
  border-radius: 0;
  background: var(--surface);
  border: 1px solid var(--line);
  height: calc(100vh - 170px);
  overflow: hidden;
}
.workspace-grid {
  display: grid;
  grid-template-columns: minmax(360px, 45%) minmax(460px, 55%);
  height: 100%;
}

/* 左栏：题面 */
.left-pane {
  border-right: 1px solid var(--line);
  padding: 24px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.pane-head {
  display: flex;
  flex-direction: column;
  gap: 10px;
  border-bottom: 1px solid var(--line);
  padding-bottom: 16px;
}
.pane-q-title-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
}
.q-cur-no {
  font-size: 13px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-weight: 700;
  color: var(--ink-tertiary);
}
.pane-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--ink);
}
.pane-tags {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.score-badge {
  background: #f0f0f0;
  color: var(--ink);
  border-radius: 0;
}
.spec-text {
  font-size: 12px;
  color: var(--ink-tertiary);
}

.pane-content {
  font-size: 14px;
  line-height: 1.6;
}

.schema-box {
  border: 1px solid var(--line);
  background: var(--bg);
  border-radius: 0;
  margin-top: 12px;
}
.schema-box-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border-bottom: 1px solid var(--line);
}
.schema-title {
  font-size: 12px;
  font-weight: 700;
  color: var(--ink-secondary);
}
.btn-copy-schema {
  background: none;
  border: 1px solid var(--line);
  padding: 2px 8px;
  font-size: 11px;
  cursor: pointer;
  border-radius: 0;
}
.schema-code {
  margin: 0;
  padding: 12px;
  background: var(--bg);
  color: var(--ink);
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 12px;
  overflow-x: auto;
  line-height: 1.4;
}

/* 右栏：作答与执行 */
.right-pane {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--surface);
}

.editor-ctrl-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  border-bottom: 1px solid var(--line);
  background: var(--bg);
}
.editor-ctrl-left {
  display: flex;
  align-items: center;
  gap: 8px;
}
.ctrl-label {
  font-size: 12px;
  color: var(--ink-tertiary);
}
.select-lang {
  padding: 4px 8px;
  border: 1px solid var(--line);
  background: var(--surface);
  font-size: 13px;
  border-radius: 0;
}
.dialect-pill {
  font-size: 12px;
  font-weight: 700;
  font-family: monospace;
}
.shortcut-tip {
  font-size: 11px;
  color: var(--ink-tertiary);
}

.editor-wrapper {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

/* 客观题交互 */
.objective-pane {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.obj-hint-bar {
  font-size: 13px;
  color: var(--ink-secondary);
  padding: 8px 12px;
  background: var(--bg);
  border-left: 3px solid var(--ink);
}

.fill-box {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 12px;
}
.fill-label {
  font-size: 13px;
  font-weight: 600;
}
.fill-input {
  width: 100%;
  border-radius: 0;
  font-size: 15px;
}

.options-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.opt-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 18px;
  border: 1px solid var(--line);
  background: var(--surface);
  cursor: pointer;
  border-radius: 0;
  transition: all 0.15s ease;
}
.opt-card:hover {
  border-color: var(--ink-secondary);
  background: var(--bg);
}
.opt-card.selected {
  border-color: var(--ink);
  background: var(--surface);
  box-shadow: inset 0 0 0 1px var(--ink);
}
.opt-native-input {
  display: none;
}
.opt-square-letter {
  width: 32px;
  height: 32px;
  border: 1px solid var(--line);
  background: var(--bg);
  color: var(--ink);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  border-radius: 0;
  flex-shrink: 0;
  transition: all 0.15s ease;
}
.opt-card.selected .opt-square-letter {
  background: var(--ink);
  color: #fff;
  border-color: var(--ink);
}
.opt-desc {
  font-size: 14px;
  color: var(--ink);
  line-height: 1.4;
}

/* 右栏底部操作条 */
.pane-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 18px;
  border-top: 1px solid var(--line);
  background: var(--bg);
  gap: 16px;
}
.footer-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.btn-judge {
  border-radius: 0;
  padding: 8px 16px;
  font-size: 13px;
  font-weight: 700;
}
.btn-save {
  border-radius: 0;
  padding: 8px 14px;
  font-size: 13px;
}
.judge-status-block {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 0;
  font-size: 12px;
  font-weight: 700;
  color: #fff;
}
.judge-status-block.ac { background: var(--success); }
.judge-status-block.wa { background: var(--danger); }
.judge-status-block.tle { background: #000; }
.judge-status-block.re { background: #9c27b0; }

.footer-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 实时排名弹窗 */
.rank-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 24px;
}
.rank-modal {
  width: min(800px, 100%);
  max-height: 85vh;
  display: flex;
  flex-direction: column;
  background: var(--surface);
  border-radius: 0;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.2);
}
.rank-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  border-bottom: 1px solid var(--line);
}
.rank-head-title {
  display: flex;
  align-items: baseline;
  gap: 12px;
}
.rank-modal-h {
  margin: 0;
  font-size: 17px;
  font-weight: 800;
}
.rank-refresh-tag {
  font-size: 11px;
  color: var(--ink-tertiary);
}
.rank-body {
  padding: 16px 24px 24px;
  overflow-y: auto;
}
.rank-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.rank-table th {
  text-align: left;
  padding: 10px 12px;
  color: var(--ink-tertiary);
  font-weight: 600;
  border-bottom: 1px solid var(--line);
}
.rank-table td {
  padding: 12px;
  border-bottom: 1px solid var(--line);
}
.th-rank, .td-rank { width: 60px; }
.th-num, .td-num { text-align: right; }
.rank-badge-square {
  width: 24px;
  height: 24px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 800;
  border-radius: 0;
  background: var(--bg);
  border: 1px solid var(--line);
}
.rank-badge-square.rank-1 { background: #000; color: #fff; border-color: #000; }
.rank-badge-square.rank-2 { background: #333; color: #fff; border-color: #333; }
.rank-badge-square.rank-3 { background: #666; color: #fff; border-color: #666; }
.score-bold { font-weight: 700; color: var(--ink); }

@media (max-width: 900px) {
  .station-body {
    grid-template-columns: 1fr;
  }
  .nav-rail {
    height: auto;
    max-height: 160px;
  }
  .q-squares-list {
    flex-direction: row;
    overflow-x: auto;
  }
  .workspace-grid {
    grid-template-columns: 1fr;
  }
}
</style>
