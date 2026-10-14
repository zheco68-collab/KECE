<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { practiceApi } from '@/api/practice'
import { renderMarkdown } from '@/utils/markdown'
import { useToast } from '@/composables/useToast'
import type { PracticeDetail, PracticeObjectiveResult } from '@/types/practice'

const route = useRoute()
const router = useRouter()
const { success, error: toastError } = useToast()

const code = computed(() => String(route.params.code).toUpperCase())
const detail = ref<PracticeDetail | null>(null)
const loading = ref(true)
const submitting = ref(false)
const gradeResult = ref<PracticeObjectiveResult | null>(null)

// 用户的作答映射: key = practiceQuestionId, value = 答案字符串
const userAnswers = ref<Record<number, string>>({})

let timer: number | null = null
const remainingSec = ref<number | null>(null)

const formattedRemaining = computed(() => {
  if (remainingSec.value === null) return '--:--'
  if (remainingSec.value <= 0) return '00:00 (已截止)'
  const m = Math.floor(remainingSec.value / 60)
  const s = remainingSec.value % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
})

const objectiveQuestions = computed(() => detail.value?.objectiveQuestions ?? [])

const totalFullScore = computed(() =>
  objectiveQuestions.value.reduce((sum, q) => sum + (q.score || 0), 0)
)

const hasSubmittedOnce = computed(() => {
  return objectiveQuestions.value.some((q) => q.isCorrect !== undefined && q.isCorrect !== null)
})

const answeredCount = computed(() => {
  return Object.values(userAnswers.value).filter((v) => v && v.trim().length > 0).length
})

function optionLetter(index: number): string {
  return String.fromCharCode(65 + index)
}

function optionText(option: string): string {
  return option.replace(/^[A-Za-z][.、)）:：]\s*/, '')
}

function isSelected(pqId: number, letter: string, type: string): boolean {
  const current = userAnswers.value[pqId] || ''
  if (type === 'SINGLE') {
    return current === letter
  }
  const set = current.split(',').filter(Boolean)
  return set.includes(letter)
}

function selectOption(pqId: number, letter: string, type: string) {
  if (detail.value?.status === 'ENDED') return

  if (type === 'SINGLE') {
    userAnswers.value[pqId] = letter
  } else {
    // MULTIPLE
    const current = (userAnswers.value[pqId] || '').split(',').filter(Boolean)
    if (current.includes(letter)) {
      userAnswers.value[pqId] = current.filter((l) => l !== letter).sort().join(',')
    } else {
      userAnswers.value[pqId] = [...current, letter].sort().join(',')
    }
  }

  // 随堂练习模式：修改选项后重置该题临时判分角标
  const targetQ = objectiveQuestions.value.find((q) => q.practiceQuestionId === pqId)
  if (targetQ && targetQ.isCorrect !== undefined) {
    targetQ.isCorrect = undefined
    targetQ.userScore = undefined
  }
}

function onFillInput(q: any) {
  if (q && q.isCorrect !== undefined) {
    q.isCorrect = undefined
    q.userScore = undefined
  }
}

async function loadData() {
  loading.value = true
  try {
    const res = await practiceApi.detail(code.value)
    detail.value = res
    if (res.status === 'ENDED') {
      remainingSec.value = 0
    } else if (res.remainingSeconds !== undefined && res.remainingSeconds !== null) {
      remainingSec.value = res.remainingSeconds
    }

    // 恢复用户历史填写的答案
    const initialAnswers: Record<number, string> = {}
    for (const q of res.objectiveQuestions) {
      if (q.userAnswer) {
        initialAnswers[q.practiceQuestionId] = q.userAnswer
      }
    }
    userAnswers.value = initialAnswers
  } catch (e) {
    toastError(e instanceof Error ? e.message : '加载客观题失败')
  } finally {
    loading.value = false
  }
}

async function handleSubmitBatch() {
  if (submitting.value) return
  if (detail.value?.status === 'ENDED') {
    toastError('当前课练已结束，无法继续提交')
    return
  }
  if (answeredCount.value === 0) {
    toastError('您尚未作答任何题目')
    return
  }

  submitting.value = true
  try {
    const res = await practiceApi.submitObjective(code.value, userAnswers.value)
    gradeResult.value = res
    success(
      hasSubmittedOnce.value
        ? `客观题已更新！重新判分得分：${res.totalScore} / ${res.fullScore} 分`
        : `客观题提交成功！本次得分：${res.totalScore} / ${res.fullScore} 分`,
    )
    await loadData()
  } catch (e) {
    toastError(e instanceof Error ? e.message : '提交客观题失败')
  } finally {
    submitting.value = false
  }
}

function startCountdown() {
  timer = window.setInterval(() => {
    if (remainingSec.value !== null && remainingSec.value > 0) {
      remainingSec.value--
      if (remainingSec.value === 0 && detail.value) {
        detail.value.status = 'ENDED'
      }
    }
  }, 1000)
}

onMounted(async () => {
  await loadData()
  startCountdown()
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<template>
  <div class="practice-objective-page container">
    <!-- 头部横幅 -->
    <header class="obj-header card">
      <div class="header-top">
        <button class="btn-back" @click="router.push(`/practice/${code}`)">
          ← 返回课练题目列表
        </button>
        <div class="header-right">
          <span class="code-pill">口令: {{ code }}</span>
          <div v-if="detail?.durationMinutes" class="cd-box" :class="{ urgent: (remainingSec ?? 100) < 300 }">
            <span class="cd-time">⏱ {{ formattedRemaining }}</span>
          </div>
        </div>
      </div>

      <div class="title-row">
        <div class="title-left">
          <span class="header-tag">OBJECTIVE SHEET</span>
          <h1 class="page-title">{{ detail?.title }} · 客观题集中答卷</h1>
        </div>
        <div class="progress-pill">
          已答: <strong>{{ answeredCount }}</strong> / {{ objectiveQuestions.length }} 题 ·
          总分 <strong>{{ totalFullScore }}</strong> 分
        </div>
      </div>
    </header>

    <!-- 加载中 -->
    <div v-if="loading" class="loading-state card">
      <div class="loading-spinner"></div>
      <p class="muted">正在加载客观题试卷…</p>
    </div>

    <!-- 题目列表集中排布 -->
    <div v-else class="questions-sheet">
      <div
        v-for="(q, idx) in objectiveQuestions"
        :key="q.practiceQuestionId"
        class="question-card card"
        :class="{
          answered: userAnswers[q.practiceQuestionId]?.trim().length > 0,
          graded: q.isCorrect !== undefined && q.isCorrect !== null,
          correct: q.isCorrect === true,
          wrong: q.isCorrect === false
        }"
      >
        <div class="q-header-row">
          <div class="q-left-tags">
            <span class="q-index-box">#{{ idx + 1 }}</span>
            <span class="q-type-tag" :class="q.type.toLowerCase()">
              {{ q.type === 'SINGLE' ? '单选题' : q.type === 'MULTIPLE' ? '多选题' : '填空题' }}
            </span>
            <span class="q-score-tag">{{ q.score }} 分</span>
          </div>

          <!-- 判分结果角标 -->
          <div v-if="q.isCorrect !== undefined && q.isCorrect !== null" class="grade-badge" :class="q.isCorrect ? 'pass' : 'fail'">
            <span v-if="q.isCorrect">✓ 答对 (+{{ q.userScore }}分)</span>
            <span v-else>✕ 错误 (得{{ q.userScore ?? 0 }}分)</span>
          </div>
        </div>

        <!-- 题干 Markdown 渲染 -->
        <div class="q-content markdown-body" v-html="renderMarkdown(q.content)"></div>

        <!-- 选项作答区域 (单选 / 多选) -->
        <div v-if="q.type === 'SINGLE' || q.type === 'MULTIPLE'" class="options-container">
          <div
            v-for="(opt, optIdx) in q.options"
            :key="optIdx"
            class="option-row"
            :class="{ active: isSelected(q.practiceQuestionId, optionLetter(optIdx), q.type) }"
            @click="selectOption(q.practiceQuestionId, optionLetter(optIdx), q.type)"
          >
            <div class="letter-box">
              {{ optionLetter(optIdx) }}
            </div>
            <div class="option-text">
              {{ optionText(opt) }}
            </div>
          </div>
        </div>

        <!-- 填空题作答区域 -->
        <div v-else-if="q.type === 'FILL'" class="fill-container">
          <input
            v-model="userAnswers[q.practiceQuestionId]"
            type="text"
            class="fill-input"
            placeholder="在此键入填空答案…"
            :disabled="detail?.status === 'ENDED'"
            @input="onFillInput(q)"
          />
        </div>
      </div>
    </div>

    <!-- 底部悬浮操作栏 -->
    <div class="bottom-action-bar">
      <div class="bar-left">
        <span>当前作答进度：<strong>{{ answeredCount }} / {{ objectiveQuestions.length }}</strong> 题</span>
        <span v-if="detail?.myScore" class="bar-score">已计入课练总分：<strong>{{ detail.myScore }}</strong> 分</span>
      </div>
      <div class="bar-right">
        <button class="btn btn-back-outline" @click="router.push(`/practice/${code}`)">
          返回题目清单
        </button>
        <button
          class="btn btn-submit-objective"
          :disabled="submitting || detail?.status === 'ENDED'"
          @click="handleSubmitBatch"
        >
          {{
            submitting
              ? '正在批量判分…'
              : detail?.status === 'ENDED'
                ? '课练已截止归档（只读）'
                : hasSubmittedOnce
                  ? '重新判分并更新成绩 ✓'
                  : '提交客观题答卷并判分 ✓'
          }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.practice-objective-page {
  padding-top: 24px;
  padding-bottom: 90px;
  max-width: 960px;
  margin: 0 auto;
}

.obj-header {
  background: #ffffff;
  border: 1px solid #1e1e1e;
  border-radius: 0;
  padding: 20px 24px;
  margin-bottom: 24px;
  box-shadow: 4px 4px 0 #1e1e1e;
}

.header-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.btn-back {
  background: transparent;
  border: none;
  font-size: 13px;
  font-weight: 700;
  color: #495057;
  cursor: pointer;
  padding: 0;
}

.btn-back:hover {
  color: #1e1e1e;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.code-pill {
  background: #f1f3f5;
  border: 1px solid #1e1e1e;
  padding: 2px 8px;
  font-family: monospace;
  font-size: 12px;
  font-weight: 700;
}

.cd-box {
  background: #1e1e1e;
  color: #fff;
  padding: 2px 8px;
  font-size: 12px;
  font-weight: 800;
  font-family: monospace;
}

.cd-box.urgent {
  background: #c92a2a;
}

.title-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
}

.header-tag {
  background: #7048e8;
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  padding: 1px 6px;
  font-family: monospace;
  margin-bottom: 4px;
  display: inline-block;
}

.page-title {
  font-size: 20px;
  font-weight: 800;
  color: #1e1e1e;
  margin: 0;
}

.progress-pill {
  font-size: 13px;
  color: #495057;
  background: #f8f9fa;
  border: 1px solid #dee2e6;
  padding: 6px 12px;
}

.questions-sheet {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.question-card {
  background: #ffffff;
  border: 1px solid #ced4da;
  border-radius: 0;
  padding: 24px;
  transition: all 0.15s ease;
}

.question-card:hover {
  border-color: #1e1e1e;
}

.question-card.correct {
  border-left: 4px solid #2b8a3e;
}

.question-card.wrong {
  border-left: 4px solid #c92a2a;
}

.q-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.q-left-tags {
  display: flex;
  align-items: center;
  gap: 8px;
}

.q-index-box {
  width: 26px;
  height: 26px;
  background: #1e1e1e;
  color: #fff;
  font-size: 12px;
  font-weight: 900;
  font-family: monospace;
  display: flex;
  align-items: center;
  justify-content: center;
}

.q-type-tag {
  font-size: 10px;
  font-weight: 700;
  padding: 2px 6px;
  color: #fff;
}

.q-type-tag.single { background: #1971c2; }
.q-type-tag.multiple { background: #7048e8; }
.q-type-tag.fill { background: #e67700; }

.q-score-tag {
  background: #f1f3f5;
  border: 1px solid #ced4da;
  font-size: 11px;
  font-weight: 700;
  padding: 2px 6px;
}

.grade-badge {
  font-size: 12px;
  font-weight: 700;
  padding: 2px 8px;
}

.grade-badge.pass {
  background: #ebfbee;
  color: #2b8a3e;
  border: 1px solid #b2f2bb;
}

.grade-badge.fail {
  background: #fff5f5;
  color: #c92a2a;
  border: 1px solid #ffc9c9;
}

.q-content {
  font-size: 15px;
  line-height: 1.6;
  color: #212529;
  margin-bottom: 20px;
}

.options-container {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.option-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 10px 14px;
  border: 1px solid #dee2e6;
  background: #fafafa;
  cursor: pointer;
  transition: all 0.15s ease;
}

.option-row:hover {
  background: #f1f3f5;
  border-color: #1e1e1e;
}

.option-row.active {
  background: #ffffff;
  border-color: #1e1e1e;
  box-shadow: 2px 2px 0 #1e1e1e;
}

.letter-box {
  width: 32px;
  height: 32px;
  border: 1px solid #1e1e1e;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 900;
  font-family: monospace;
  font-size: 14px;
  background: #ffffff;
  color: #1e1e1e;
  transition: all 0.15s ease;
}

.option-row.active .letter-box {
  background: #1e1e1e;
  color: #ffffff;
}

.option-text {
  font-size: 14px;
  color: #212529;
  flex: 1;
}

.fill-container {
  margin-top: 10px;
}

.fill-input {
  width: 100%;
  max-width: 480px;
  padding: 10px 14px;
  border: 1px solid #1e1e1e;
  border-radius: 0;
  font-size: 14px;
  font-family: monospace;
  outline: none;
  box-sizing: border-box;
}

.fill-input:focus {
  border-color: #1971c2;
  box-shadow: 2px 2px 0 #1971c2;
}

.bottom-action-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #ffffff;
  border-top: 2px solid #1e1e1e;
  padding: 14px 40px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 -4px 12px rgba(0, 0, 0, 0.08);
  z-index: 100;
}

.bar-left {
  font-size: 14px;
  color: #212529;
  display: flex;
  gap: 20px;
  align-items: center;
}

.bar-score {
  color: #2b8a3e;
}

.bar-right {
  display: flex;
  gap: 12px;
}

.btn {
  padding: 10px 22px;
  font-size: 14px;
  font-weight: 700;
  border-radius: 0;
  cursor: pointer;
  border: 1px solid #1e1e1e;
}

.btn-back-outline {
  background: #fff;
  color: #1e1e1e;
}

.btn-submit-objective {
  background: #7048e8;
  color: #fff;
  border-color: #5f3dc4;
}

.btn-submit-objective:hover:not(:disabled) {
  background: #5f3dc4;
}

.btn-submit-objective:disabled {
  background: #ced4da;
  border-color: #adb5bd;
  cursor: not-allowed;
}

.loading-state {
  padding: 60px;
  text-align: center;
}
</style>

