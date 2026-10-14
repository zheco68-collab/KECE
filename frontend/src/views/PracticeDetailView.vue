<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { practiceApi } from '@/api/practice'
import { useToast } from '@/composables/useToast'
import { useConfirm } from '@/composables/useConfirm'
import type { PracticeDetail } from '@/types/practice'

const route = useRoute()
const router = useRouter()
const { success, error: toastError } = useToast()
const { confirm } = useConfirm()

const code = computed(() => String(route.params.code).toUpperCase())
const detail = ref<PracticeDetail | null>(null)
const loading = ref(true)
const submittingPractice = ref(false)

let timer: number | null = null
const remainingSec = ref<number | null>(null)

const objectiveFullScore = computed(() => {
  return detail.value?.objectiveQuestions.reduce((sum, q) => sum + (q.score || 0), 0) ?? 0
})

const objectiveUserScore = computed(() => {
  return detail.value?.objectiveQuestions.reduce((sum, q) => sum + (q.userScore || 0), 0) ?? 0
})

const objectiveAnsweredCount = computed(() => {
  return detail.value?.objectiveQuestions.filter((q) => q.answered).length ?? 0
})

const formattedRemaining = computed(() => {
  if (detail.value?.status === 'ENDED' || (remainingSec.value !== null && remainingSec.value <= 0)) {
    return '00:00 (已截止)'
  }
  if (remainingSec.value === null) return '--:--'
  const m = Math.floor(remainingSec.value / 60)
  const s = remainingSec.value % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
})

async function fetchDetail() {
  loading.value = true
  try {
    const res = await practiceApi.detail(code.value)
    detail.value = res
    if (res.status === 'ENDED') {
      remainingSec.value = 0
    } else if (res.remainingSeconds !== undefined && res.remainingSeconds !== null) {
      remainingSec.value = res.remainingSeconds
    }
  } catch (e) {
    toastError(e instanceof Error ? e.message : '加载课练详情失败')
  } finally {
    loading.value = false
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

async function handleSubmitPractice() {
  const ok = await confirm({
    title: '确认提交整场课练',
    message: '提交后本次课练作答将锁定归档，确定要现在交卷吗？',
    confirmText: '确认交卷',
    cancelText: '继续作答',
  })
  if (!ok) return

  submittingPractice.value = true
  try {
    await practiceApi.submit(code.value)
    success('已成功交卷！')
    await fetchDetail()
  } catch (e) {
    toastError(e instanceof Error ? e.message : '交卷失败')
  } finally {
    submittingPractice.value = false
  }
}

onMounted(async () => {
  await fetchDetail()
  startCountdown()
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<template>
  <div class="practice-detail-page container">
    <!-- 加载中 -->
    <div v-if="loading" class="state-card card">
      <div class="loading-spinner"></div>
      <p class="muted">正在加载课练题目与作答档案…</p>
    </div>

    <template v-else-if="detail">
      <!-- 顶部信息栏 -->
      <header class="detail-header card">
        <div class="header-top">
          <button class="btn-back" @click="router.push('/practice')">
            ← 返回课练大厅
          </button>
          <div class="header-actions">
            <span class="status-pill" :class="detail.status.toLowerCase()">
              {{ detail.status === 'ACTIVE' ? '● 进行中' : '已结束' }}
            </span>
            <span class="code-pill">房间口令: <strong>{{ detail.accessCode }}</strong></span>
          </div>
        </div>

        <div class="title-row">
          <h1 class="practice-title">{{ detail.title }}</h1>
          <div
            v-if="detail.durationMinutes > 0"
            class="countdown-badge"
            :class="{
              urgent: detail.status === 'ACTIVE' && (remainingSec ?? 100) < 300,
              ended: detail.status === 'ENDED' || detail.mySubmitted
            }"
          >
            <span class="cd-label">
              {{ detail.status === 'ENDED' ? '作答状态' : detail.mySubmitted ? '交卷状态' : '剩余作答时间' }}
            </span>
            <span class="cd-time">
              {{ detail.status === 'ENDED' ? '已截止收卷' : detail.mySubmitted ? '已交卷锁定' : formattedRemaining }}
            </span>
          </div>
        </div>

        <p v-if="detail.description" class="practice-desc">
          {{ detail.description }}
        </p>

        <!-- Bento 指标方块 -->
        <div class="bento-strip">
          <div class="bento-item">
            <span class="b-label">授课教师</span>
            <span class="b-value">{{ detail.teacherName }}</span>
          </div>
          <div class="bento-item">
            <span class="b-label">参练人数</span>
            <span class="b-value">{{ detail.memberCount }} 人</span>
          </div>
          <div class="bento-item">
            <span class="b-label">我的当前得分</span>
            <span class="b-value highlight">{{ detail.myScore }} 分</span>
          </div>
          <div class="bento-item">
            <span class="b-label">实战 AC 通过</span>
            <span class="b-value success-text">{{ detail.mySolvedCount }} 题</span>
          </div>
          <div class="bento-item action-item">
            <button
              v-if="!detail.mySubmitted"
              class="btn btn-submit-practice"
              :disabled="submittingPractice || detail.status === 'ENDED'"
              @click="handleSubmitPractice"
            >
              {{ submittingPractice ? '交卷中…' : '提交课练卷 ✓' }}
            </button>
            <span v-else class="submitted-label">✓ 已完成交卷</span>
          </div>
        </div>
      </header>

      <!-- 板块 1: 客观题集中答卷入口 -->
      <section v-if="detail.objectiveQuestions.length > 0" class="objective-section card">
        <div class="obj-card-left">
          <div class="obj-icon-square">OBJ</div>
          <div class="obj-text-wrap">
            <div class="obj-badge-row">
              <span class="obj-tag">客观题综合卷</span>
              <span class="obj-score-tag">总分值: {{ objectiveFullScore }} 分</span>
            </div>
            <h2 class="obj-title">客观题集中答题区（共 {{ detail.objectiveQuestions.length }} 道）</h2>
            <p class="obj-desc">
              本场所有单选、多选（漏选半分）与填空题已汇聚在同一答卷中，支持即刻批量判分与答案比对。
            </p>
          </div>
        </div>
        <div class="obj-card-right">
          <div class="obj-progress-info">
            <span class="progress-txt">完成进度: {{ objectiveAnsweredCount }} / {{ detail.objectiveQuestions.length }} 题</span>
            <span v-if="objectiveAnsweredCount > 0" class="score-txt">
              已得分: <strong>{{ objectiveUserScore }}</strong> / {{ objectiveFullScore }} 分
            </span>
          </div>
          <button
            class="btn btn-enter-obj"
            :class="{ 'btn-view-mode': detail.mySubmitted || detail.status === 'ENDED' }"
            @click="router.push(`/practice/${detail.accessCode}/objective`)"
          >
            {{
              detail.mySubmitted || detail.status === 'ENDED'
                ? '查看客观题答卷 →'
                : objectiveAnsweredCount > 0
                  ? '查看或修改客观题 →'
                  : '进入集中答卷 →'
            }}
          </button>
        </div>
      </section>

      <!-- 板块 2: 编程与 SQL 实战题目列表 -->
      <section v-if="detail.practicalQuestions.length > 0" class="practical-section">
        <div class="section-header">
          <div class="sec-left">
            <h2 class="section-title">上机实操试题清单</h2>
            <span class="sec-count">共 {{ detail.practicalQuestions.length }} 题</span>
          </div>
          <span class="sec-hint">点击题目即可进入标准做题与评测工作台</span>
        </div>

        <div class="practical-list">
          <div
            v-for="(q, idx) in detail.practicalQuestions"
            :key="q.practiceQuestionId"
            class="practical-item card"
            :class="{ solved: q.userStatus === 'ACCEPTED' }"
          >
            <!-- 状态标记 -->
            <div class="status-col">
              <div v-if="q.userStatus === 'ACCEPTED'" class="status-circle solved" title="已通过">✓</div>
              <div v-else-if="q.userStatus === 'WRONG_ANSWER'" class="status-circle failed" title="未完全通过">✕</div>
              <div v-else class="status-circle unsolved" title="未解答">○</div>
            </div>

            <!-- 题号与标题 -->
            <div class="info-col">
              <div class="title-top">
                <span class="q-number">#{{ idx + 1 }}</span>
                <span class="q-type-badge" :class="q.questionType.toLowerCase()">
                  {{ q.questionType === 'ALGO' ? '算法编程' : '数据库 SQL' }}
                </span>
                <span class="q-score-badge">{{ q.score }} 分</span>
                <span v-if="q.difficulty" class="diff-tag" :class="q.difficulty.toLowerCase()">
                  {{ q.difficulty }}
                </span>
              </div>
              <h3
                class="q-title"
                @click="
                  q.questionType === 'ALGO'
                    ? router.push(`/practice/${detail.accessCode}/question/${q.practiceQuestionId}`)
                    : router.push(`/practice/${detail.accessCode}/db-question/${q.practiceQuestionId}`)
                "
              >
                {{ q.title }}
              </h3>
              <div class="q-meta">
                <span class="meta-spec">规格: {{ q.timeLimitMs }}ms / {{ Math.round(q.memoryLimitKb / 1024) }}MB</span>
                <span class="meta-langs">语言: {{ q.languages.join(' · ') }}</span>
                <span v-if="q.userScore > 0" class="meta-user-score">本题得分: {{ q.userScore }} 分</span>
              </div>
            </div>

            <!-- CTA 按钮 -->
            <div class="action-col">
              <button
                class="btn btn-start"
                :class="{ 'btn-view-mode': detail.mySubmitted || detail.status === 'ENDED' }"
                @click="
                  q.questionType === 'ALGO'
                    ? router.push(`/practice/${detail.accessCode}/question/${q.practiceQuestionId}`)
                    : router.push(`/practice/${detail.accessCode}/db-question/${q.practiceQuestionId}`)
                "
              >
                {{
                  detail.mySubmitted || detail.status === 'ENDED'
                    ? (q.userStatus === 'ACCEPTED' ? '查看测评结果 →' : '查看题目 →')
                    : (q.userStatus === 'ACCEPTED' ? '重新作答 →' : '开始做题 →')
                }}
              </button>
            </div>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.practice-detail-page {
  padding-top: 24px;
  padding-bottom: 60px;
  max-width: 1100px;
  margin: 0 auto;
}

.detail-header {
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: 0;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: var(--shadow-sm);
}

.header-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
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

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.status-pill {
  font-size: 11px;
  font-weight: 700;
  padding: 3px 8px;
  color: #fff;
}

.status-pill.active {
  background: #2b8a3e;
}

.status-pill.ended {
  background: #495057;
}

.code-pill {
  background: #f1f3f5;
  border: 1px solid var(--line);
  padding: 3px 8px;
  font-size: 12px;
  font-family: monospace;
}

.title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.practice-title {
  font-size: 24px;
  font-weight: 800;
  color: var(--ink);
  margin: 0;
}

.countdown-badge {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  background: var(--surface);
  border: 1px solid var(--line);
  color: var(--ink);
  padding: 6px 14px;
  border-radius: 0;
}

.countdown-badge.urgent {
  background: var(--danger-bg);
  border-color: var(--danger);
  color: var(--danger);
}

.countdown-badge.ended {
  background: var(--bg);
  border-color: var(--line);
  color: var(--ink-secondary);
}

.cd-label {
  font-size: 10px;
  color: var(--ink-tertiary);
  letter-spacing: 0.5px;
}

.cd-time {
  font-size: 18px;
  font-weight: 800;
  font-family: monospace;
}

.countdown-badge.ended .cd-time {
  font-size: 14px;
  color: var(--ink-secondary);
}

.btn-view-mode {
  background: var(--surface) !important;
  color: var(--ink) !important;
  border: 1px solid var(--line) !important;
}

.btn-view-mode:hover {
  background: var(--bg) !important;
  border-color: var(--ink) !important;
}

.practice-desc {
  font-size: 14px;
  color: #495057;
  margin: 0 0 18px 0;
  line-height: 1.6;
}

.bento-strip {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  padding: 12px 16px;
}

.bento-item {
  display: flex;
  flex-direction: column;
}

.b-label {
  font-size: 11px;
  color: #868e96;
  margin-bottom: 4px;
}

.b-value {
  font-size: 15px;
  font-weight: 800;
  color: #212529;
}

.b-value.highlight {
  color: #1971c2;
}

.b-value.success-text {
  color: #2b8a3e;
}

.action-item {
  justify-content: center;
}

.btn-submit-practice {
  background: #1e1e1e;
  color: #fff;
  border: 1px solid #1e1e1e;
  padding: 6px 14px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.submitted-label {
  font-size: 12px;
  font-weight: 700;
  color: #2b8a3e;
}

.objective-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #ffffff;
  border: 1px solid #1e1e1e;
  border-left: 5px solid #7048e8;
  padding: 20px 24px;
  margin-bottom: 24px;
}

.obj-card-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.obj-icon-square {
  width: 50px;
  height: 50px;
  background: #7048e8;
  color: #fff;
  font-weight: 900;
  font-size: 16px;
  font-family: monospace;
  display: flex;
  align-items: center;
  justify-content: center;
}

.obj-badge-row {
  display: flex;
  gap: 8px;
  margin-bottom: 4px;
}

.obj-tag {
  background: #7048e8;
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  padding: 1px 6px;
}

.obj-score-tag {
  background: #f1f3f5;
  border: 1px solid #ced4da;
  font-size: 11px;
  font-weight: 700;
  padding: 1px 6px;
}

.obj-title {
  font-size: 17px;
  font-weight: 800;
  color: #1e1e1e;
  margin: 0 0 4px 0;
}

.obj-desc {
  font-size: 13px;
  color: #868e96;
  margin: 0;
}

.obj-card-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

.obj-progress-info {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  font-size: 12px;
  color: #495057;
}

.btn-enter-obj {
  background: #7048e8;
  color: #fff;
  border: 1px solid #5f3dc4;
  padding: 8px 20px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  border-radius: 0;
}

.practical-section {
  margin-top: 28px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 14px;
}

.sec-left {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.section-title {
  font-size: 20px;
  font-weight: 800;
  color: #1e1e1e;
  margin: 0;
}

.sec-count {
  font-size: 13px;
  font-weight: 700;
  color: #868e96;
}

.sec-hint {
  font-size: 12px;
  color: #868e96;
}

.practical-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.practical-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  background: #fff;
  border: 1px solid #ced4da;
  border-radius: 0;
  transition: all 0.15s ease;
}

.practical-item:hover {
  border-color: #1e1e1e;
  box-shadow: 2px 2px 0 rgba(0, 0, 0, 0.1);
}

.practical-item.solved {
  border-left: 4px solid #2b8a3e;
}

.status-col {
  width: 28px;
}

.status-circle {
  width: 24px;
  height: 24px;
  border-radius: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 900;
}

.status-circle.solved {
  background: #2b8a3e;
  color: #fff;
}

.status-circle.failed {
  background: #c92a2a;
  color: #fff;
}

.status-circle.unsolved {
  border: 1px solid #ced4da;
  color: #adb5bd;
}

.info-col {
  flex: 1;
  min-width: 0;
}

.title-top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.q-number {
  font-size: 12px;
  font-weight: 800;
  font-family: monospace;
  color: #495057;
}

.q-type-badge {
  font-size: 10px;
  font-weight: 700;
  padding: 1px 6px;
  color: #fff;
}

.q-type-badge.algo {
  background: #2b8a3e;
}

.q-type-badge.db {
  background: #1e1e1e;
}

.q-score-badge {
  background: #f8f9fa;
  border: 1px solid #ced4da;
  font-size: 10px;
  font-weight: 700;
  padding: 1px 5px;
}

.diff-tag {
  font-size: 10px;
  font-weight: 700;
  padding: 1px 6px;
  color: #fff;
}

.diff-tag.easy { background: #2b8a3e; }
.diff-tag.medium { background: #e67700; }
.diff-tag.hard { background: #c92a2a; }

.q-title {
  font-size: 15px;
  font-weight: 700;
  color: #1e1e1e;
  margin: 0 0 6px 0;
  cursor: pointer;
}

.q-title:hover {
  color: #1971c2;
}

.q-meta {
  display: flex;
  gap: 12px;
  font-size: 11px;
  color: #868e96;
}

.meta-spec, .meta-langs {
  font-family: monospace;
}

.meta-user-score {
  color: #2b8a3e;
  font-weight: 700;
}

.btn-start {
  background: #1e1e1e;
  color: #fff;
  border: 1px solid #1e1e1e;
  padding: 8px 18px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  border-radius: 0;
}

.btn-start:hover {
  background: #343a40;
}

.state-card {
  padding: 60px;
  text-align: center;
}
</style>
