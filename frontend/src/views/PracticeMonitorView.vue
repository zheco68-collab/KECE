<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { practiceApi } from '@/api/practice'
import { useToast } from '@/composables/useToast'
import { useConfirm } from '@/composables/useConfirm'
import type { PracticeMonitorData } from '@/types/practice'

const route = useRoute()
const router = useRouter()
const { success, error: toastError, info } = useToast()
const { confirm } = useConfirm()

const code = computed(() => String(route.params.code).toUpperCase())
const monitor = ref<PracticeMonitorData | null>(null)
const loading = ref(true)
const anonymize = ref(false)

let pollTimer: number | null = null

const formattedRemaining = computed(() => {
  if (!monitor.value?.remainingSeconds) return '--:--'
  if (monitor.value.remainingSeconds <= 0) return '00:00 (已结束)'
  const m = Math.floor(monitor.value.remainingSeconds / 60)
  const s = monitor.value.remainingSeconds % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
})

function maskName(name: string): string {
  if (!anonymize.value) return name
  if (!name || name.length <= 1) return '学生*'
  if (name.length === 2) return `${name[0]}*`
  return `${name[0]}***${name[name.length - 1]}`
}

async function fetchMonitorData() {
  try {
    const data = await practiceApi.monitor(code.value)
    monitor.value = data
  } catch (e) {
    // 轮询静默失败
  } finally {
    loading.value = false
  }
}

async function handleExtend(minutes: number) {
  try {
    await practiceApi.extend(code.value, minutes)
    success(`已成功延长作答时间 ${minutes} 分钟`)
    await fetchMonitorData()
  } catch (e) {
    toastError(e instanceof Error ? e.message : '延长失败')
  }
}

async function handleEnd() {
  const ok = await confirm({
    title: '确认提前截止课练',
    message: '截止后所有学生将无法再提交新的作答，确定要提前收卷吗？',
    confirmText: '确认截止',
    cancelText: '取消',
    danger: true,
  })
  if (!ok) return

  try {
    await practiceApi.end(code.value)
    info('课练已截止收卷')
    await fetchMonitorData()
  } catch (e) {
    toastError(e instanceof Error ? e.message : '截止失败')
  }
}

onMounted(async () => {
  await fetchMonitorData()
  pollTimer = window.setInterval(fetchMonitorData, 3000)
})

onBeforeUnmount(() => {
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<template>
  <div class="projector-dashboard-page">
    <!-- 加载中 -->
    <div v-if="loading && !monitor" class="state-card card">
      <div class="loading-spinner"></div>
      <p class="muted">正在初始化课练数据看板…</p>
    </div>

    <template v-else-if="monitor">
      <!-- 顶部控制与口令大屏区 -->
      <header class="projector-header card">
        <div class="header-left">
          <button class="btn-back" @click="router.push('/practice')">
            <svg class="back-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="19" y1="12" x2="5" y2="12" />
              <polyline points="12 19 5 12 12 5" />
            </svg>
            <span>返回课练大厅</span>
          </button>
          <div class="title-group">
            <span class="mode-tag">DATA DASHBOARD · 课练数据</span>
            <h1 class="screen-title">{{ monitor.title }}</h1>
          </div>
        </div>

        <!-- 居中超大口令展示方块 -->
        <div class="center-access-code">
          <span class="code-label">上机口令 (CLASS CODE)</span>
          <div class="code-letters">
            <span v-for="(char, i) in monitor.accessCode.split('')" :key="i" class="code-box">
              {{ char }}
            </span>
          </div>
        </div>

        <!-- 右侧倒计时与教师控制 -->
        <div class="header-right">
          <div class="cd-container" :class="{ urgent: (monitor.remainingSeconds ?? 100) < 300 }">
            <span class="cd-title">剩余时间</span>
            <span class="cd-digits">{{ formattedRemaining }}</span>
          </div>

          <div class="ctrl-btn-group">
            <button
              class="btn-ctrl btn-anon"
              :class="{ active: anonymize }"
              @click="anonymize = !anonymize"
            >
              {{ anonymize ? '脱敏模式: 开' : '脱敏模式: 关' }}
            </button>
            <button
              v-if="monitor.status === 'ACTIVE'"
              class="btn-ctrl btn-extend"
              @click="handleExtend(5)"
            >
              +5 分钟
            </button>
            <button
              v-if="monitor.status === 'ACTIVE'"
              class="btn-ctrl btn-stop"
              @click="handleEnd"
            >
              提前收卷
            </button>
          </div>
        </div>
      </header>

      <!-- Bento 四大宏观指标方块 (统一 KECE 直角卡片风格) -->
      <section class="stat-squares-grid">
        <div class="stat-square card">
          <div class="stat-square-head">实到上机人数</div>
          <div class="stat-square-body">
            <span class="stat-num mono">{{ monitor.joinedCount }}</span>
            <span class="stat-unit">人</span>
          </div>
          <div class="stat-square-foot">
            <span class="foot-tag">已主动交卷: {{ monitor.submittedCount }} 人</span>
          </div>
        </div>

        <div class="stat-square card">
          <div class="stat-square-head">全班累计提交</div>
          <div class="stat-square-body">
            <span class="stat-num mono">{{ monitor.totalSubmissions }}</span>
            <span class="stat-unit">次</span>
          </div>
          <div class="stat-square-foot">
            <span class="foot-tag">代码 / SQL / 客观题</span>
          </div>
        </div>

        <div class="stat-square card">
          <div class="stat-square-head">全班当前均分</div>
          <div class="stat-square-body">
            <span class="stat-num mono">{{ monitor.avgScore }}</span>
            <span class="stat-unit">分</span>
          </div>
          <div class="stat-square-foot">
            <span class="foot-tag">满分全通关: {{ monitor.perfectScoreCount }} 人</span>
          </div>
        </div>

        <div class="stat-square card">
          <div class="stat-square-head">课堂活跃状态</div>
          <div class="stat-square-body">
            <span class="stat-num live-num mono" :class="{ 'is-live': monitor.status === 'ACTIVE' }">
              {{ monitor.status === 'ACTIVE' ? 'LIVE' : 'ENDED' }}
            </span>
          </div>
          <div class="stat-square-foot">
            <span class="foot-tag">3 秒静默轮询同步中</span>
          </div>
        </div>
      </section>

      <!-- 主体分栏：左题目统计与天梯榜 + 右实时提交流水 -->
      <div class="main-body-grid">
        <div class="left-col">
          <!-- 逐题通过率面板 -->
          <div class="panel-card card">
            <div class="card-section-head">
              <h2 class="panel-title">逐题通过率监控矩阵</h2>
              <span class="panel-subtitle">按题目顺序监控全班通过情况</span>
            </div>

            <div class="question-stat-list">
              <div
                v-for="q in monitor.questionStats"
                :key="q.practiceQuestionId"
                class="q-stat-row"
              >
                <div class="q-stat-info">
                  <span class="q-stat-type" :class="q.questionType.toLowerCase()">
                    {{ q.questionType === 'OBJECTIVE' ? '客观题卷' : q.questionType === 'ALGO' ? '算法题' : 'SQL题' }}
                  </span>
                  <span class="q-stat-name">{{ q.title }}</span>
                  <span class="q-stat-score">({{ q.score }}分)</span>
                </div>
                <div class="q-stat-bar-wrap">
                  <div class="bar-bg">
                    <div class="bar-fill" :style="{ width: `${q.passRate}%` }"></div>
                  </div>
                  <span class="bar-pct mono">{{ q.passRate }}%</span>
                  <span class="bar-counts mono">{{ q.passCount }} / {{ q.submitCount }} 人通过</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 全班作答天梯榜 -->
          <div class="panel-card card">
            <div class="card-section-head">
              <h2 class="panel-title">全班作答实时天梯榜</h2>
              <span class="panel-subtitle">按总得分 ➔ 通过题数 ➔ 加入时间排序</span>
            </div>

            <div class="rank-table-wrap">
              <table class="rank-table">
                <thead>
                  <tr>
                    <th class="th-rank">名次</th>
                    <th>学生姓名</th>
                    <th width="120">AC 题数</th>
                    <th width="120">当前得分</th>
                    <th width="110">作答状态</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-if="monitor.leaderboard.length === 0">
                    <td colspan="5" class="empty-td">暂无学生加入房间</td>
                  </tr>
                  <tr
                    v-for="item in monitor.leaderboard"
                    :key="item.userId"
                    class="rank-row"
                  >
                    <td class="td-rank">
                      <span class="rank-square-badge" :class="`rank-${item.rank}`">
                        {{ item.rank }}
                      </span>
                    </td>
                    <td class="username-td">
                      <span class="username-text">{{ maskName(item.nickname || item.username) }}</span>
                    </td>
                    <td class="bold-td mono">{{ item.solvedCount }} 题</td>
                    <td class="score-td mono">{{ item.totalScore }} 分</td>
                    <td>
                      <span v-if="item.submitted" class="sub-pill pass">已交卷</span>
                      <span v-else class="sub-pill ongoing">作答中</span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <!-- 右侧：实时提交流水栏 (Live Activity Ticker) -->
        <div class="right-col">
          <div class="panel-card card ticker-panel">
            <div class="card-section-head">
              <div class="ticker-title-group">
                <span class="live-dot"></span>
                <h2 class="panel-title">实时提交流水</h2>
              </div>
              <span class="panel-subtitle">实时推送</span>
            </div>

            <div class="ticker-stream">
              <div v-if="monitor.recentActivities.length === 0" class="empty-ticker">
                等待首个提交产生…
              </div>
              <div
                v-for="act in monitor.recentActivities"
                :key="act.id"
                class="ticker-card"
                :class="act.status.toLowerCase()"
              >
                <div class="ticker-top">
                  <span class="act-user">{{ maskName(act.nickname || act.username) }}</span>
                  <span class="act-status" :class="act.status.toLowerCase()">
                    {{ act.status === 'ACCEPTED' ? 'AC 通过' : 'WA 未通过' }}
                  </span>
                </div>
                <div class="ticker-question">
                  <span class="act-type">[{{ act.questionType }}]</span>
                  <span class="act-title">{{ act.questionTitle }}</span>
                </div>
                <div class="ticker-bottom">
                  <span v-if="act.score > 0" class="act-score mono">+{{ act.score }}分</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.projector-dashboard-page {
  min-height: 100vh;
  background: var(--bg);
  color: var(--ink);
  padding: 24px 32px 64px;
  max-width: 1440px;
  margin: 0 auto;
  box-sizing: border-box;
}

.mono {
  font-family: monospace;
}

/* 状态加载 */
.state-card {
  padding: 64px 32px;
  text-align: center;
  border-radius: 0;
  background: var(--surface);
  border: 1px solid var(--line);
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

/* 顶部控制与口令大屏区 */
.projector-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: 0;
  padding: 20px 28px;
  margin-bottom: 24px;
  gap: 20px;
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 12px;
  flex: 1;
}

.btn-back {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border: 1px solid var(--line);
  background: var(--surface);
  color: var(--ink-secondary);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  border-radius: 0;
  align-self: flex-start;
  transition: all 0.15s ease;
}

.btn-back:hover {
  background: var(--bg);
  color: var(--ink);
  border-color: var(--ink);
}

.back-icon {
  width: 14px;
  height: 14px;
}

.title-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.mode-tag {
  background: var(--ink);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  font-family: monospace;
  letter-spacing: 0.5px;
  align-self: flex-start;
  border-radius: 0;
}

.screen-title {
  font-size: 24px;
  font-weight: 800;
  color: var(--ink);
  margin: 0;
  line-height: 1.3;
}

/* 居中口令方块 */
.center-access-code {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 0 16px;
}

.code-label {
  font-size: 11px;
  color: var(--ink-tertiary);
  font-weight: 700;
  letter-spacing: 1.5px;
  text-transform: uppercase;
}

.code-letters {
  display: flex;
  gap: 8px;
}

.code-box {
  width: 52px;
  height: 62px;
  background: var(--surface);
  border: 2px solid var(--ink);
  color: var(--ink);
  font-size: 32px;
  font-weight: 900;
  font-family: monospace;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 0;
  box-shadow: none;
}

/* 右侧控制栏 */
.header-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 12px;
  flex: 1;
}

.cd-container {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  background: var(--bg);
  border: 1px solid var(--line);
  padding: 6px 14px;
  border-radius: 0;
}

.cd-container.urgent {
  border-color: var(--danger);
  background: var(--danger-bg);
}

.cd-title {
  font-size: 11px;
  color: var(--ink-tertiary);
  font-weight: 600;
}

.cd-digits {
  font-size: 22px;
  font-weight: 900;
  font-family: monospace;
  color: var(--ink);
}

.cd-container.urgent .cd-digits {
  color: var(--danger);
}

.ctrl-btn-group {
  display: flex;
  gap: 8px;
}

.btn-ctrl {
  background: var(--surface);
  color: var(--ink);
  border: 1px solid var(--line);
  padding: 6px 14px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  border-radius: 0;
  transition: all 0.15s ease;
}

.btn-ctrl:hover {
  background: var(--bg);
  border-color: var(--ink);
}

.btn-anon.active {
  background: var(--ink);
  color: #fff;
  border-color: var(--ink);
}

.btn-extend:hover {
  border-color: var(--ink);
}

.btn-stop {
  background: var(--danger-bg);
  border-color: var(--danger);
  color: var(--danger);
}

.btn-stop:hover {
  background: var(--danger);
  color: #fff;
}

/* Bento 四大指标方块 */
.stat-squares-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-square {
  padding: 20px 24px;
  border-radius: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
  background: var(--surface);
  border: 1px solid var(--line);
}

.stat-square-head {
  font-size: 13px;
  font-weight: 600;
  color: var(--ink-secondary);
}

.stat-square-body {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.stat-num {
  font-size: 34px;
  font-weight: 800;
  color: var(--ink);
  line-height: 1;
}

.stat-num.live-num {
  font-size: 28px;
}

.stat-num.live-num.is-live {
  color: var(--success);
}

.stat-unit {
  font-size: 14px;
  color: var(--ink-tertiary);
  font-weight: 600;
}

.stat-square-foot {
  font-size: 12px;
  color: var(--ink-tertiary);
}

/* 主体分栏 */
.main-body-grid {
  display: grid;
  grid-template-columns: 2.2fr 1fr;
  gap: 20px;
}

@media (max-width: 1200px) {
  .main-body-grid {
    grid-template-columns: 1fr;
  }
}

.left-col {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.panel-card {
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: 0;
  padding: 24px;
}

.card-section-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--line);
}

.panel-title {
  font-size: 16px;
  font-weight: 800;
  color: var(--ink);
  margin: 0;
}

.panel-subtitle {
  font-size: 12px;
  color: var(--ink-tertiary);
}

/* 逐题通过率 */
.question-stat-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.q-stat-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--bg);
  border: 1px solid var(--line);
  padding: 12px 16px;
  border-radius: 0;
}

.q-stat-info {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
}

.q-stat-type {
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  color: #fff;
  border-radius: 0;
  flex-shrink: 0;
}

.q-stat-type.objective { background: #7048e8; }
.q-stat-type.algo { background: #2f9e44; }
.q-stat-type.db { background: #1e1e1e; }

.q-stat-name {
  font-size: 14px;
  font-weight: 700;
  color: var(--ink);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.q-stat-score {
  font-size: 12px;
  color: var(--ink-tertiary);
  font-family: monospace;
  flex-shrink: 0;
}

.q-stat-bar-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 320px;
  flex-shrink: 0;
}

.bar-bg {
  flex: 1;
  height: 8px;
  background: #e6e6e6;
  position: relative;
  overflow: hidden;
  border-radius: 0;
}

.bar-fill {
  height: 100%;
  background: var(--button);
  transition: width 0.3s ease;
}

.bar-pct {
  font-size: 13px;
  font-weight: 800;
  color: var(--ink);
  width: 48px;
  text-align: right;
}

.bar-counts {
  font-size: 12px;
  color: var(--ink-secondary);
  width: 95px;
  text-align: right;
}

/* 天梯榜表格 */
.rank-table-wrap {
  overflow-x: auto;
}

.rank-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.rank-table th {
  text-align: left;
  font-size: 12px;
  color: var(--ink-tertiary);
  padding: 10px 14px;
  background: var(--bg);
  border-bottom: 1px solid var(--line);
  font-weight: 600;
}

.rank-table td {
  padding: 12px 14px;
  border-bottom: 1px solid var(--line);
  color: var(--ink);
}

.th-rank, .td-rank {
  width: 70px;
}

.rank-square-badge {
  width: 26px;
  height: 26px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 800;
  border-radius: 0;
  background: var(--bg);
  border: 1px solid var(--line);
  color: var(--ink-secondary);
}

.rank-square-badge.rank-1 { background: #000; color: #fff; border-color: #000; }
.rank-square-badge.rank-2 { background: #333; color: #fff; border-color: #333; }
.rank-square-badge.rank-3 { background: #666; color: #fff; border-color: #666; }

.username-td {
  font-weight: 600;
}

.username-text {
  color: var(--ink);
}

.score-td {
  font-weight: 800;
  color: var(--ink);
}

.bold-td {
  font-weight: 600;
}

.sub-pill {
  display: inline-block;
  font-size: 11px;
  padding: 2px 8px;
  font-weight: 700;
  border-radius: 0;
}

.sub-pill.pass {
  background: var(--success-bg);
  color: var(--success);
  border: 1px solid var(--success);
}

.sub-pill.ongoing {
  background: var(--bg);
  color: var(--ink-secondary);
  border: 1px solid var(--line);
}

.empty-td {
  text-align: center;
  padding: 36px;
  color: var(--ink-tertiary);
}

/* 右侧提交流水 */
.ticker-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  box-sizing: border-box;
}

.ticker-title-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.live-dot {
  width: 8px;
  height: 8px;
  border-radius: 0;
  background: var(--success);
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0% { opacity: 1; }
  50% { opacity: 0.3; }
  100% { opacity: 1; }
}

.ticker-stream {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 640px;
  overflow-y: auto;
}

.ticker-card {
  background: var(--bg);
  border: 1px solid var(--line);
  padding: 12px 14px;
  border-left: 4px solid var(--line);
  border-radius: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.ticker-card.accepted {
  border-left-color: var(--success);
  background: #fbfdfb;
}

.ticker-card.wrong_answer {
  border-left-color: var(--danger);
  background: #fdfbfb;
}

.ticker-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.act-user {
  font-size: 13px;
  font-weight: 700;
  color: var(--ink);
}

.act-status {
  font-size: 11px;
  font-weight: 800;
  padding: 2px 8px;
  border-radius: 0;
}

.act-status.accepted {
  background: var(--success-bg);
  color: var(--success);
  border: 1px solid var(--success);
}

.act-status.wrong_answer {
  background: var(--danger-bg);
  color: var(--danger);
  border: 1px solid var(--danger);
}

.ticker-question {
  display: flex;
  gap: 6px;
  font-size: 12px;
  color: var(--ink-secondary);
}

.act-type {
  font-weight: 700;
  color: var(--ink);
}

.act-title {
  color: var(--ink-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ticker-bottom {
  display: flex;
  justify-content: flex-end;
}

.act-score {
  font-size: 12px;
  color: var(--success);
  font-weight: 800;
}

.empty-ticker {
  padding: 40px 16px;
  text-align: center;
  color: var(--ink-tertiary);
  font-size: 13px;
}
</style>
