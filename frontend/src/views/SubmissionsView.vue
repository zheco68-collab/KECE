<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { submissionApi } from '@/api/submission'
import { databaseApi } from '@/api/database'
import { useUserStore } from '@/stores/user'
import type { DbSubmissionRecord, SubmissionRecord, SubmissionStatus } from '@/types'
import { LANGUAGE_LABELS, STATUS_ABBR, statusTone } from '@/utils/submission'

const router = useRouter()
const userStore = useUserStore()

type TabKey = 'code' | 'database'
const activeTab = ref<TabKey>('code')
const size = 10

// ---- 编程题提交 ----
const codeStatus = ref<SubmissionStatus | ''>('')
const codePage = ref(1)
const codeList = ref<SubmissionRecord[]>([])
const codeTotal = ref(0)
const codeLoading = ref(false)
const codeError = ref('')
const codeSelected = ref<SubmissionRecord | null>(null)

// ---- 数据库提交 ----
const dbStatus = ref<SubmissionStatus | ''>('')
const dbPage = ref(1)
const dbList = ref<DbSubmissionRecord[]>([])
const dbTotal = ref(0)
const dbLoading = ref(false)
const dbError = ref('')
const dbSelected = ref<DbSubmissionRecord | null>(null)

const codeStatusFilters: Array<{ value: SubmissionStatus | ''; label: string }> = [
  { value: '', label: '全部' },
  { value: 'ACCEPTED', label: 'AC' },
  { value: 'WRONG_ANSWER', label: 'WA' },
  { value: 'TIME_LIMIT_EXCEEDED', label: 'TLE' },
  { value: 'MEMORY_LIMIT_EXCEEDED', label: 'MLE' },
  { value: 'RUNTIME_ERROR', label: 'RE' },
  { value: 'COMPILE_ERROR', label: 'CE' },
  { value: 'SYSTEM_ERROR', label: 'SE' },
]

const dbStatusFilters: Array<{ value: SubmissionStatus | ''; label: string }> = [
  { value: '', label: '全部' },
  { value: 'ACCEPTED', label: 'AC' },
  { value: 'WRONG_ANSWER', label: 'WA' },
  { value: 'TIME_LIMIT_EXCEEDED', label: 'TLE' },
  { value: 'SYSTEM_ERROR', label: 'SE' },
]

const statusFilters = computed(() =>
  activeTab.value === 'code' ? codeStatusFilters : dbStatusFilters,
)

/** 状态下拉框绑定到当前标签页对应的 ref，切换标签即切换数据源。 */
const activeStatus = computed<SubmissionStatus | ''>({
  get: () => (activeTab.value === 'code' ? codeStatus.value : dbStatus.value),
  set: (v) => {
    if (activeTab.value === 'code') codeStatus.value = v
    else dbStatus.value = v
  },
})

const currentTotal = computed(() => (activeTab.value === 'code' ? codeTotal.value : dbTotal.value))
const currentPage = computed(() => (activeTab.value === 'code' ? codePage.value : dbPage.value))
const totalPages = computed(() => Math.max(1, Math.ceil(currentTotal.value / size)))

const subtitle = computed(() => {
  const prefix = activeTab.value === 'database' ? '仅显示你自己的 SQL 提交 · ' : ''
  return `${prefix}共 ${currentTotal.value} 条`
})

function formatTime(iso: string): string {
  const d = new Date(iso)
  return Number.isNaN(d.getTime()) ? iso : d.toLocaleString()
}

function parseCode(codeContent: string): string {
  if (!codeContent) return ''
  try {
    const obj = JSON.parse(codeContent)
    if (obj && typeof obj === 'object') {
      const val = Object.values(obj).find((v) => typeof v === 'string' && v.length > 0)
      if (typeof val === 'string') return val
    }
  } catch {
    /* 非 JSON，原样展示 */
  }
  return codeContent
}

async function loadCode() {
  codeLoading.value = true
  codeError.value = ''
  try {
    const res = await submissionApi.list({
      status: codeStatus.value || undefined,
      page: codePage.value,
      size,
    })
    codeList.value = res.list
    codeTotal.value = res.total
  } catch (e) {
    codeError.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    codeLoading.value = false
  }
}

async function loadDb() {
  dbLoading.value = true
  dbError.value = ''
  try {
    const res = await databaseApi.listSubmissions({
      status: dbStatus.value || undefined,
      page: dbPage.value,
      size,
    })
    dbList.value = res.list
    dbTotal.value = res.total
  } catch (e) {
    dbError.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    dbLoading.value = false
  }
}

function switchTab(t: TabKey) {
  if (activeTab.value === t) return
  activeTab.value = t
  // 惰性加载：首次切到数据库标签才拉取 SQL 提交。
  if (t === 'database' && dbList.value.length === 0 && !dbLoading.value) loadDb()
}

function changePage(p: number) {
  if (activeTab.value === 'code') {
    codePage.value = p
    loadCode()
  } else {
    dbPage.value = p
    loadDb()
  }
}

watch(codeStatus, () => {
  codePage.value = 1
  loadCode()
})

watch(dbStatus, () => {
  dbPage.value = 1
  loadDb()
})

onMounted(() => {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  loadCode()
})
</script>

<template>
  <div class="submissions container">
    <div class="page-head">
      <h1 class="page-title">我的提交</h1>
      <p class="muted">{{ subtitle }}</p>
    </div>

    <div class="tabs">
      <button class="tab" :class="{ active: activeTab === 'code' }" @click="switchTab('code')">
        编程题
      </button>
      <button
        class="tab"
        :class="{ active: activeTab === 'database' }"
        @click="switchTab('database')"
      >
        数据库
      </button>
    </div>

    <div class="toolbar card">
      <label class="filter">
        <span class="filter-label">状态</span>
        <select v-model="activeStatus" class="input">
          <option v-for="f in statusFilters" :key="f.value || 'all'" :value="f.value">
            {{ f.label }}
          </option>
        </select>
      </label>
    </div>

    <!-- 编程题提交 -->
    <template v-if="activeTab === 'code'">
      <p v-if="codeError" class="submit-error">{{ codeError }}</p>
      <p v-if="codeLoading" class="muted loading">加载中…</p>
      <p v-else-if="codeList.length === 0" class="empty">暂无提交记录</p>

      <div v-else class="table-wrap card">
        <table class="submission-table">
          <thead>
            <tr>
              <th>提交</th>
              <th>题目</th>
              <th>语言</th>
              <th>状态</th>
              <th>Score</th>
              <th>用时</th>
              <th>内存</th>
              <th>提交时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in codeList" :key="r.submissionId">
              <td class="mono">#{{ r.submissionId }}</td>
              <td class="q-title">
                <RouterLink :to="`/questions/${r.questionId}`">
                  {{ r.questionTitle || `题目 #${r.questionId}` }}
                </RouterLink>
              </td>
              <td>{{ LANGUAGE_LABELS[r.language] ?? r.language }}</td>
              <td>
                <button
                  type="button"
                  class="status-badge"
                  :class="statusTone(r.status)"
                  :title="'查看代码'"
                  @click="codeSelected = r"
                >
                  {{ STATUS_ABBR[r.status] ?? r.status }}
                </button>
              </td>
              <td class="mono">{{ r.score ?? '-' }}</td>
              <td class="mono">{{ r.timeUsedMs != null ? r.timeUsedMs + 'ms' : '-' }}</td>
              <td class="mono">{{ r.memoryUsedKb != null ? r.memoryUsedKb + 'KB' : '-' }}</td>
              <td class="time">{{ formatTime(r.createdAt) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </template>

    <!-- 数据库提交 -->
    <template v-else>
      <p v-if="dbError" class="submit-error">{{ dbError }}</p>
      <p v-if="dbLoading" class="muted loading">加载中…</p>
      <p v-else-if="dbList.length === 0" class="empty">暂无提交记录</p>

      <div v-else class="table-wrap card">
        <table class="submission-table">
          <thead>
            <tr>
              <th>提交</th>
              <th>题目</th>
              <th>状态</th>
              <th>Score</th>
              <th>用时</th>
              <th>提交时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in dbList" :key="r.submissionId">
              <td class="mono">#{{ r.submissionId }}</td>
              <td class="q-title">
                <RouterLink :to="`/database/${r.questionId}`">
                  {{ r.questionTitle || `题目 #${r.questionId}` }}
                </RouterLink>
              </td>
              <td>
                <button
                  type="button"
                  class="status-badge"
                  :class="statusTone(r.status)"
                  title="查看提交的 SQL"
                  @click="dbSelected = r"
                >
                  {{ STATUS_ABBR[r.status] ?? r.status }}
                </button>
              </td>
              <td class="mono">{{ r.score ?? '-' }}</td>
              <td class="mono">{{ r.timeUsedMs != null ? r.timeUsedMs + 'ms' : '-' }}</td>
              <td class="time">{{ formatTime(r.createdAt) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </template>

    <div v-if="currentTotal > 0" class="pagination">
      <button class="btn btn-ghost btn-sm" :disabled="currentPage <= 1" @click="changePage(currentPage - 1)">
        上一页
      </button>
      <span class="page-info">第 {{ currentPage }} / {{ totalPages }} 页</span>
      <button
        class="btn btn-ghost btn-sm"
        :disabled="currentPage >= totalPages"
        @click="changePage(currentPage + 1)"
      >
        下一页
      </button>
    </div>

    <!-- 编程题代码弹窗 -->
    <Teleport to="body">
      <div v-if="codeSelected" class="modal-overlay" @click.self="codeSelected = null">
        <div class="modal card">
          <div class="modal-head">
            <h3 class="modal-title">
              提交 #{{ codeSelected.submissionId }} · {{ codeSelected.questionTitle }}
            </h3>
            <span class="modal-lang">{{ LANGUAGE_LABELS[codeSelected.language] ?? codeSelected.language }}</span>
            <button class="btn btn-ghost btn-sm" @click="codeSelected = null">关闭</button>
          </div>
          <pre class="modal-code">{{ parseCode(codeSelected.codeContent) }}</pre>
        </div>
      </div>
    </Teleport>

    <!-- 数据库 SQL 弹窗 -->
    <Teleport to="body">
      <div v-if="dbSelected" class="modal-overlay" @click.self="dbSelected = null">
        <div class="modal card">
          <div class="modal-head">
            <h3 class="modal-title">
              提交 #{{ dbSelected.submissionId }} · {{ dbSelected.questionTitle }}
            </h3>
            <span class="modal-status" :class="statusTone(dbSelected.status)">
              {{ STATUS_ABBR[dbSelected.status] ?? dbSelected.status }}
            </span>
            <button class="btn btn-ghost btn-sm" @click="dbSelected = null">关闭</button>
          </div>
          <pre class="modal-code">{{ dbSelected.sqlContent }}</pre>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.page-head {
  padding-top: 40px;
  margin-bottom: 24px;
}
.page-title {
  margin: 0 0 4px;
  font-size: 28px;
}
.tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}
.tab {
  padding: 7px 20px;
  border-radius: var(--pill);
  font-size: 14px;
  color: var(--ink-secondary);
  border: 1px solid var(--line);
  transition:
    color 0.15s ease,
    background 0.15s ease;
}
.tab.active {
  background: var(--button);
  border-color: var(--button);
  color: #fff;
}
.toolbar {
  padding: 16px;
  margin-bottom: 24px;
}
.filter {
  display: flex;
  align-items: center;
  gap: 10px;
}
.filter-label {
  color: var(--ink-secondary);
  font-size: 14px;
}
.filter select {
  max-width: 160px;
}
.loading {
  padding: 32px 0;
}
.table-wrap {
  overflow-x: auto;
  padding: 0;
}
.submission-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}
.submission-table th,
.submission-table td {
  padding: 12px 16px;
  text-align: left;
  border-bottom: 1px solid var(--line);
  white-space: nowrap;
}
.submission-table thead th {
  color: var(--ink-tertiary);
  font-size: 13px;
  font-weight: 600;
  background: var(--bg);
}
.submission-table tbody tr:last-child td {
  border-bottom: none;
}
.mono {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 13px;
}
.q-title a {
  color: var(--ink);
  font-weight: 500;
}
.q-title a:hover {
  text-decoration: underline;
}
.time {
  color: var(--ink-tertiary);
  font-size: 13px;
}
.status-badge,
.modal-status {
  display: inline-block;
  min-width: 44px;
  padding: 2px 10px;
  border: none;
  border-radius: var(--pill);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  text-align: center;
  cursor: pointer;
  font-family: inherit;
}
.status-badge.ac,
.modal-status.ac {
  background: var(--success);
}
.status-badge.wa,
.modal-status.wa {
  background: var(--danger);
}
.status-badge.tle,
.modal-status.tle {
  background: #000;
}
.status-badge.mle,
.modal-status.mle {
  background: #f57c00;
}
.status-badge.re,
.modal-status.re {
  background: #9c27b0;
}
.status-badge.ce,
.status-badge.se,
.modal-status.ce,
.modal-status.se {
  background: #607d8b;
}
.status-badge.run,
.modal-status.run {
  background: #9e9e9e;
}
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
  padding: 28px 0 56px;
}
.page-info {
  color: var(--ink-secondary);
  font-size: 14px;
}
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 200;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(0, 0, 0, 0.45);
}
.modal {
  width: min(760px, 100%);
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.modal-head {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  border-bottom: 1px solid var(--line);
}
.modal-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.modal-lang {
  color: var(--ink-tertiary);
  font-size: 13px;
  flex-shrink: 0;
}
.modal-status {
  flex-shrink: 0;
}
.modal-head .btn {
  margin-left: auto;
  flex-shrink: 0;
}
.modal-code {
  margin: 0;
  padding: 16px 20px;
  overflow: auto;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 13px;
  line-height: 1.5;
  white-space: pre;
  color: var(--ink);
}
</style>
