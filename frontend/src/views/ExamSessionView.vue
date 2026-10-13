<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { examApi, type ExamSessionQuestion, type RankRow, type SessionEnter } from '@/api/exam';
import { ApiError } from '@/api/request';
import { databaseApi } from '@/api/database';
import { submissionApi } from '@/api/submission';
import CodeEditor from '@/components/CodeEditor.vue';
import { useConfirm } from '@/composables/useConfirm';
import { useToast } from '@/composables/useToast';
import { renderMarkdown } from '@/utils/markdown';
import { STATUS_ABBR, statusTone } from '@/utils/submission';
import type { Submission, SubmissionStatus } from '@/types';

const route = useRoute();
const router = useRouter();
const { confirm } = useConfirm();
const { success, error, info } = useToast();

const examId = computed(() => Number(route.params.id));
const enter = ref<SessionEnter | null>(null);
const loading = ref(true);
const loadError = ref('');

const currentIndex = ref(0);
const remaining = ref(0);
const submitting = ref(false);
let timer: ReturnType<typeof setInterval> | null = null;

/**
 * 每题作答状态：按题型分字段存储，避免用单一 string 承载不同语义。
 * - ALGO: code(纯文本) + language
 * - DB:   code(SQL 文本)
 * - OBJECTIVE 选择: selected(字母数组)
 * - OBJECTIVE 填空: fillText
 */
interface AnswerState {
  saved: boolean;
  code: string;
  language: string;
  selected: string[];
  fillText: string;
}
const answers = reactive<Record<number, AnswerState>>({});
/** 已保存但未交卷的题目快照 ID。 */
const savedSet = ref(new Set<number>());

/** ACM 单题实时评测状态（仅 ALGO/DB 使用）。 */
interface JudgeState {
  submitting: boolean;
  status: string | null;
  score: number | null;
}
const judgeState = reactive<Record<number, JudgeState>>({});
let pollTimer: ReturnType<typeof setInterval> | null = null;

const TERMINAL_STATUSES: SubmissionStatus[] = [
  'ACCEPTED',
  'WRONG_ANSWER',
  'TIME_LIMIT_EXCEEDED',
  'MEMORY_LIMIT_EXCEEDED',
  'RUNTIME_ERROR',
  'COMPILE_ERROR',
  'SYSTEM_ERROR',
];

const currentQuestion = computed<ExamSessionQuestion | null>(() => {
  const qs = enter.value?.questions;
  if (!qs || qs.length === 0) return null;
  return qs[currentIndex.value];
});

const answerState = computed(() => {
  const q = currentQuestion.value;
  if (!q) return null;
  return answers[q.id] ?? null;
});

/** 当前题的 ACM 单题评测状态（无则 null）。 */
const currentJudge = computed<JudgeState | null>(() => {
  const q = currentQuestion.value;
  if (!q) return null;
  return judgeState[q.id] ?? null;
});

const countdownText = computed(() => {
  const s = Math.max(remaining.value, 0);
  const h = Math.floor(s / 3600);
  const m = Math.floor((s % 3600) / 60);
  const sec = s % 60;
  return [h, m, sec].map((v) => String(v).padStart(2, '0')).join(':');
});

function startTimer() {
  if (timer) clearInterval(timer);
  timer = setInterval(() => {
    remaining.value -= 1;
    if (remaining.value <= 0) {
      clearInterval(timer!);
      timer = null;
      autoSubmit();
    }
  }, 1000);
}

function optionLetter(index: number): string {
  return String.fromCharCode(65 + index);
}

/** 去掉选项字符串可能自带的字母前缀（如 "A. 选项一" → "选项一"），避免与左侧字母重复。 */
function optionText(option: string): string {
  return option.replace(/^[A-Za-z][.、)）:：]\s*/, '');
}

/** 代码题语言 → 默认源文件名（后端对 ALGO 作答要求多文件 JSON，与普通提交链路一致）。 */
const CODE_FILES: Record<string, string> = {
  cpp: 'main.cpp',
  java: 'Main.java',
  python: 'main.py',
};

/** 将代码题纯文本包装为多文件 JSON 字符串（不改动编辑器里的原始内容）。 */
function algoAnswer(state: { code: string; language: string }): string {
  const file = CODE_FILES[state.language] ?? 'main.cpp';
  return JSON.stringify({ [file]: state.code });
}

/** 按题型把当前题的作答状态组装成后端要求的 answer 字符串。 */
function buildAnswer(q: ExamSessionQuestion, state: AnswerState): string {
  if (q.questionType === 'ALGO') {
    return algoAnswer(state);
  }
  if (q.questionType === 'DB') {
    return state.code;
  }
  // 客观题
  if (q.objectiveType === 'FILL') {
    return state.fillText;
  }
  return state.selected.join(',');
}

function buildLanguage(q: ExamSessionQuestion, state: AnswerState): string | undefined {
  return q.questionType === 'ALGO' ? state.language : undefined;
}

async function saveCurrent() {
  const q = currentQuestion.value;
  const state = answerState.value;
  if (!q || !state) return;
  try {
    await examApi.saveAnswer(examId.value, {
      examQuestionId: q.id,
      answer: buildAnswer(q, state),
      language: buildLanguage(q, state),
    });
    state.saved = true;
    savedSet.value.add(q.id);
  } catch (e) {
    state.saved = false;
    error(e instanceof ApiError ? e.message : '保存失败');
  }
}

function stopPoll() {
  if (pollTimer !== null) {
    clearInterval(pollTimer);
    pollTimer = null;
  }
}

/** ACM 赛制：单题实时提交（先保存作答，再入队并轮询判题结果）。 */
async function submitCurrent() {
  const q = currentQuestion.value;
  const state = answerState.value;
  if (!q || !state) return;
  // 按题型判断作答是否为空
  const isEmpty =
    q.questionType === 'ALGO' || q.questionType === 'DB'
      ? state.code.trim() === ''
      : q.objectiveType === 'FILL'
        ? state.fillText.trim() === ''
        : state.selected.length === 0;
  if (isEmpty) {
    error('请先作答再提交');
    return;
  }
  if (judgeState[q.id]?.submitting) return;
  if (!judgeState[q.id]) judgeState[q.id] = { submitting: false, status: null, score: null };
  const st = judgeState[q.id];
  st.submitting = true;
  st.status = null;
  st.score = null;
  try {
    await examApi.saveAnswer(examId.value, {
      examQuestionId: q.id,
      answer: buildAnswer(q, state),
      language: buildLanguage(q, state),
    });
    state.saved = true;
    savedSet.value.add(q.id);
    const res = await examApi.submitQuestion(examId.value, q.id);
    info('已提交，评测中…');
    startPoll(q.id, res.submissionId, res.questionType);
  } catch (e) {
    st.submitting = false;
    error(e instanceof ApiError ? e.message : '提交失败');
  }
}

/** 轮询判题状态：代码题走 submissionApi、数据库题走 databaseApi，终态后停止。 */
function startPoll(questionId: number, submissionId: number, type: 'ALGO' | 'DB') {
  stopPoll();
  pollTimer = setInterval(async () => {
    try {
      const s: Submission =
        type === 'ALGO'
          ? await submissionApi.get(submissionId)
          : await databaseApi.getSubmission(submissionId);
      const st = judgeState[questionId];
      if (st) {
        st.status = s.status;
        st.score = s.result?.score ?? null;
      }
      if (TERMINAL_STATUSES.includes(s.status)) {
        stopPoll();
        if (st) st.submitting = false;
        if (s.status === 'ACCEPTED') {
          success('本题评测通过');
        } else {
          info('本题未通过，可修改后重新提交');
        }
      }
    } catch {
      stopPoll();
      if (judgeState[questionId]) judgeState[questionId].submitting = false;
    }
  }, 1000);
}

/** 切题时停止上一题的轮询，避免结果串题。 */
watch(currentIndex, stopPoll);

async function doSubmit() {
  if (submitting.value) return;
  submitting.value = true;
  try {
    // 交卷前先保存当前题，避免未保存的修改丢失
    const q = currentQuestion.value;
    const state = answerState.value;
    if (q && state) {
      try {
        await examApi.saveAnswer(examId.value, {
          examQuestionId: q.id,
          answer: buildAnswer(q, state),
          language: buildLanguage(q, state),
        });
      } catch {
        /* 保存失败不阻塞交卷 */
      }
    }
    const res = await examApi.submit(examId.value);
    success(`交卷成功：${res.graded} 题已判分，${res.queued} 题评测中`);
    router.push(`/exams/${examId.value}/result`);
  } catch (e) {
    error(e instanceof ApiError ? e.message : '交卷失败');
  } finally {
    submitting.value = false;
  }
}

async function submitWithConfirm() {
  const ok = await confirm({
    title: '确认交卷',
    message: '交卷后将按当前作答统一评测（OI），已保存的作答将不再可修改。确定交卷吗？',
    confirmText: '交卷',
  });
  if (ok) await doSubmit();
}

/** 到点自动交卷：先保存当前题再交卷。 */
async function autoSubmit() {
  if (submitting.value) return;
  info('考试时间到，自动交卷');
  await doSubmit();
}

// ==================== 实时排名 ====================
const rankingVisible = ref(false);
const ranking = ref<RankRow[]>([]);
const rankingLoading = ref(false);
const rankRows = computed(() => ranking.value.map((row, index) => ({ ...row, rank: index + 1 })));
let rankTimer: ReturnType<typeof setInterval> | null = null;

async function loadRanking() {
  rankingLoading.value = true;
  try {
    ranking.value = await examApi.ranking(examId.value);
  } catch (e) {
    error(e instanceof ApiError ? e.message : '排名加载失败');
  } finally {
    rankingLoading.value = false;
  }
}

function openRanking() {
  rankingVisible.value = true;
  loadRanking();
  // 自动刷新：每 10 秒拉取一次
  if (rankTimer) clearInterval(rankTimer);
  rankTimer = setInterval(loadRanking, 10000);
}

function closeRanking() {
  rankingVisible.value = false;
  if (rankTimer) {
    clearInterval(rankTimer);
    rankTimer = null;
  }
}

// ==================== 生命周期 ====================
onMounted(async () => {
  try {
    enter.value = await examApi.enter(examId.value);
    for (const q of enter.value.questions) {
      answers[q.id] = {
        saved: false,
        code: '',
        language: 'cpp',
        selected: [],
        fillText: '',
      };
    }
    remaining.value = enter.value.remainingSeconds;
    startTimer();
  } catch (e) {
    loadError.value = e instanceof ApiError ? e.message : '加载失败';
  } finally {
    loading.value = false;
  }
});

onBeforeUnmount(() => {
  if (timer) clearInterval(timer);
  stopPoll();
  if (rankTimer) clearInterval(rankTimer);
});
</script>

<template>
  <div class="exam-session container">
    <p v-if="loading" class="muted">加载中…</p>
    <div v-else-if="loadError" class="submit-error">{{ loadError }}</div>

    <template v-else-if="enter">
      <div class="bar card">
        <div class="title-block">
          <h1 class="exam-title">{{ enter.examTitle }}</h1>
          <span class="badge">{{ enter.mode === 'ACM' ? 'ACM' : 'OI' }}</span>
        </div>
        <div class="bar-right">
          <button class="btn btn-ghost btn-sm" type="button" @click="openRanking">
            实时排名
          </button>
          <div class="countdown" :class="{ danger: remaining < 300 }">
            剩余 <span class="mono">{{ countdownText }}</span>
          </div>
        </div>
      </div>

      <div class="layout">
        <aside class="nav card">
          <h3 class="nav-title">题目</h3>
          <button
            v-for="(q, i) in enter.questions"
            :key="q.id"
            class="q-nav"
            :class="{
              active: i === currentIndex,
              done: savedSet.has(q.id),
            }"
            @click="currentIndex = i"
          >
            <span class="q-no">{{ i + 1 }}</span>
            <span class="q-label">{{ q.title }}</span>
            <span class="q-score">{{ q.score }}分</span>
          </button>
        </aside>

        <main class="question card">
          <template v-if="currentQuestion">
            <div class="q-head">
              <span class="badge" :class="`type-${currentQuestion.questionType.toLowerCase()}`">
                {{
                  currentQuestion.questionType === 'ALGO'
                    ? '编程'
                    : currentQuestion.questionType === 'DB'
                      ? '数据库'
                      : '客观题'
                }}
              </span>
              <span class="q-score">{{ currentQuestion.score }} 分</span>
            </div>
            <!-- eslint-disable-next-line vue/no-v-html -- 内容已由 renderMarkdown 清洗 -->
            <div class="prose" v-html="renderMarkdown(currentQuestion.content ?? '')"></div>

            <!-- 数据库题：表结构参考 -->
            <div v-if="currentQuestion.questionType === 'DB' && currentQuestion.schemaSql" class="schema-block">
              <div class="schema-label">表结构（参考）</div>
              <pre class="schema">{{ currentQuestion.schemaSql }}</pre>
            </div>

            <!-- 编程题 -->
            <template v-if="currentQuestion.questionType === 'ALGO' && answerState">
              <label class="lang-row">
                <span class="muted">语言</span>
                <select v-model="answerState.language" class="input">
                  <option value="cpp">C++</option>
                  <option value="java">Java</option>
                  <option value="python">Python</option>
                </select>
              </label>
              <CodeEditor v-model="answerState.code" :language="answerState.language" />
            </template>

            <!-- 数据库题：Monaco SQL 编辑器 -->
            <template v-else-if="currentQuestion.questionType === 'DB' && answerState">
              <CodeEditor v-model="answerState.code" language="mysql" />
            </template>

            <!-- ACM 赛制：编程/数据库题可单题实时提交，看是否通过 -->
            <div
              v-if="
                enter.mode === 'ACM' &&
                (currentQuestion.questionType === 'ALGO' ||
                  currentQuestion.questionType === 'DB') &&
                answerState
              "
              class="judge-row"
            >
              <button
                class="btn btn-primary btn-sm"
                type="button"
                :disabled="currentJudge?.submitting"
                @click="submitCurrent"
              >
                {{ currentJudge?.submitting ? '评测中…' : '提交本题' }}
              </button>
              <span
                v-if="currentJudge?.status"
                class="badge judge-badge"
                :class="statusTone(currentJudge.status as SubmissionStatus)"
              >
                {{ STATUS_ABBR[currentJudge.status as SubmissionStatus] ?? currentJudge.status }}
              </span>
              <span v-if="currentJudge?.score != null" class="judge-score">
                {{ currentJudge?.score }} 分
              </span>
              <span class="muted judge-hint">ACM 赛制可实时查看通过与否，可重复提交</span>
            </div>

            <!-- 客观题：单选 / 多选 / 填空（ACM 与 OI 均可作答，ACM 加提示行） -->
            <template v-if="currentQuestion.questionType === 'OBJECTIVE' && answerState">
              <p v-if="enter.mode === 'ACM'" class="muted objective-hint">
                客观题在交卷后统一判分
              </p>
              <!-- 填空题 -->
              <div v-if="currentQuestion.objectiveType === 'FILL'" class="fill-row">
                <input
                  v-model="answerState.fillText"
                  class="input fill-input"
                  type="text"
                  placeholder="填写答案…"
                />
              </div>

              <!-- 选择题 -->
              <div v-else class="options">
                <label
                  v-for="(opt, i) in currentQuestion.options ?? []"
                  :key="i"
                  class="option"
                  :class="{ selected: answerState.selected.includes(optionLetter(i)) }"
                >
                  <!-- 单选：radio 原生互斥，选中时把 selected 替换为单元素数组 -->
                  <input
                    v-if="currentQuestion.objectiveType === 'SINGLE'"
                    type="radio"
                    :name="`q-${currentQuestion.id}`"
                    :checked="answerState.selected[0] === optionLetter(i)"
                    @change="answerState.selected = [optionLetter(i)]"
                  />
                  <!-- 多选：checkbox 的 v-model 绑定数组，Vue 自动 push/remove -->
                  <input
                    v-else
                    v-model="answerState.selected"
                    type="checkbox"
                    :value="optionLetter(i)"
                  />
                  <span class="opt-letter">{{ optionLetter(i) }}</span>
                  <span>{{ optionText(opt) }}</span>
                </label>
              </div>
            </template>

            <div class="q-actions">
              <button class="btn btn-ghost btn-sm" type="button" @click="saveCurrent()">
                {{ answerState?.saved ? '已保存' : '保存本题' }}
              </button>
              <button
                class="btn btn-ghost btn-sm"
                type="button"
                :disabled="currentIndex <= 0"
                @click="currentIndex--"
              >
                上一题
              </button>
              <button
                class="btn btn-ghost btn-sm"
                type="button"
                :disabled="currentIndex >= (enter.questions?.length ?? 1) - 1"
                @click="currentIndex++"
              >
                下一题
              </button>
            </div>
          </template>
        </main>
      </div>

      <div class="submit-bar card">
        <span class="muted"
          >已保存 {{ savedSet.size }} / {{ enter.questions?.length ?? 0 }} 题</span
        >
        <button
          class="btn btn-primary"
          :disabled="submitting"
          type="button"
          @click="submitWithConfirm"
        >
          {{ submitting ? '交卷中…' : '交卷' }}
        </button>
      </div>
    </template>

    <!-- 实时排名弹窗 -->
    <Teleport to="body">
      <div v-if="rankingVisible" class="rank-overlay" @click.self="closeRanking">
        <div class="rank-modal" role="dialog" aria-modal="true">
          <div class="rank-head">
            <h3 class="rank-title">实时排名</h3>
            <button class="btn btn-ghost btn-sm" type="button" @click="closeRanking">关闭</button>
          </div>
          <p v-if="rankingLoading && ranking.length === 0" class="muted">加载中…</p>
          <table v-else class="rank-table">
            <thead>
              <tr>
                <th>名次</th>
                <th>考生</th>
                <th>通过</th>
                <th>罚时(分)</th>
                <th>总分</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in rankRows" :key="row.userId">
                <td class="mono">{{ row.rank }}</td>
                <td>{{ row.username }}</td>
                <td class="mono">{{ row.solved }}</td>
                <td class="mono">{{ row.penaltyMinutes }}</td>
                <td class="mono">{{ row.totalScore }} / {{ row.fullScore }}</td>
              </tr>
              <tr v-if="rankRows.length === 0">
                <td colspan="5" class="muted">暂无排名数据</td>
              </tr>
            </tbody>
          </table>
          <p class="rank-foot muted">每 10 秒自动刷新</p>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.exam-session {
  padding-top: 24px;
}
.bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  margin-bottom: 16px;
}
.title-block {
  display: flex;
  align-items: center;
  gap: 12px;
}
.bar-right {
  display: flex;
  align-items: center;
  gap: 16px;
}
.exam-title {
  font-size: 20px;
  font-weight: 600;
  margin: 0;
}
.countdown {
  font-size: 15px;
  color: var(--ink-secondary);
}
.countdown .mono {
  font-weight: 700;
  font-size: 18px;
  color: var(--success);
}
.countdown.danger .mono {
  color: var(--danger);
}
.layout {
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 16px;
  align-items: start;
}
.nav {
  padding: 12px;
  max-height: 70vh;
  overflow-y: auto;
}
.nav-title {
  margin: 4px 8px 8px;
  font-size: 14px;
  color: var(--ink-secondary);
}
.q-nav {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 8px 10px;
  border: none;
  border-radius: var(--radius);
  background: transparent;
  text-align: left;
  font-size: 13px;
}
.q-nav.active {
  background: var(--accent-bg, #eef2ff);
}
.q-nav.done .q-no {
  background: var(--success);
  color: #fff;
}
.q-no {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: var(--line);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  flex-shrink: 0;
}
.q-label {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.q-score {
  color: var(--ink-tertiary);
  font-size: 12px;
}
.question {
  padding: 20px;
}
.q-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}
.lang-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 12px 0;
}
.lang-row select {
  width: 120px;
}
.fill-row {
  margin-top: 12px;
}
.fill-input {
  width: 100%;
}
.options {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 12px;
}
.option {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  cursor: pointer;
}
.option.selected {
  border-color: var(--success);
  background: var(--success-bg);
}
.opt-letter {
  font-weight: 700;
}
.schema-block {
  margin-top: 12px;
}
.schema-label {
  font-size: 12px;
  color: var(--ink-tertiary);
  margin-bottom: 6px;
}
.schema {
  background: var(--bg);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  padding: 12px;
  font-size: 12px;
  overflow-x: auto;
  white-space: pre-wrap;
  margin: 0;
}
.q-actions {
  display: flex;
  gap: 8px;
  margin-top: 16px;
}
.judge-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 14px;
  flex-wrap: wrap;
}
.judge-badge {
  min-width: 44px;
  padding: 2px 10px;
  border-radius: var(--pill);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  text-align: center;
}
.judge-badge.ac {
  background: var(--success);
}
.judge-badge.wa {
  background: var(--danger);
}
.judge-badge.tle {
  background: #000;
}
.judge-badge.mle {
  background: #f57c00;
}
.judge-badge.re {
  background: #9c27b0;
}
.judge-badge.ce,
.judge-badge.se {
  background: #607d8b;
}
.judge-badge.run {
  background: #9e9e9e;
}
.judge-score {
  font-size: 13px;
  color: var(--ink-secondary);
}
.judge-hint {
  font-size: 12px;
}
.objective-hint {
  margin-top: 14px;
  font-size: 13px;
}
.submit-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  margin-top: 16px;
}

/* 实时排名弹窗 */
.rank-overlay {
  position: fixed;
  inset: 0;
  z-index: 200;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(17, 17, 17, 0.45);
}
.rank-modal {
  width: 100%;
  max-width: 640px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  padding: 20px 22px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: 0;
  box-shadow: var(--shadow-md);
}
.rank-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.rank-title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
}
.rank-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
  overflow-y: auto;
}
.rank-table th,
.rank-table td {
  padding: 8px 12px;
  text-align: left;
  border-bottom: 1px solid var(--line);
}
.rank-table th {
  background: var(--bg);
  font-weight: 600;
  color: var(--ink-secondary);
}
.rank-table .mono {
  font-family: var(--mono, monospace);
}
.rank-foot {
  margin-top: 12px;
  font-size: 12px;
}
</style>
