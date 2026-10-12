<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { databaseApi } from '@/api/database'
import { useUserStore } from '@/stores/user'
import type { DbSubmissionRecord, SubmissionStatus } from '@/types'
import { STATUS_ABBR, statusTone } from '@/utils/submission'

const router = useRouter()
const userStore = useUserStore()

const status = ref<SubmissionStatus | ''>('')
const page = ref(1)
const size = 10

const list = ref<DbSubmissionRecord[]>([])
const total = ref(0)
const loading = ref(false)
const error = ref('')
const selected = ref<DbSubmissionRecord | null>(null)

const statusFilters: Array<{ value: SubmissionStatus | ''; label: string }> = [
  { value: '', label: '全部' },
  { value: 'ACCEPTED', label: 'AC' },
  { value: 'WRONG_ANSWER', label: 'WA' },
  { value: 'TIME_LIMIT_EXCEEDED', label: 'TLE' },
  { value: 'SYSTEM_ERROR', label: 'SE' },
]

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size)))

function formatTime(iso: string): string {
  const d = new Date(iso)
  return Number.isNaN(d.getTime()) ? iso : d.toLocaleString()
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const res = await databaseApi.listSubmissions({
      status: status.value || undefined,
      page: page.value,
      size,
    })
    list.value = res.list
    total.value = res.total
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

function changePage(p: number) {
  page.value = p
  load()
}

watch(status, () => {
  page.value = 1
  load()
})

onMounted(() => {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  load()
})
</script>

<template>
  <div class="db-subs container">
    <div class="page-head">
      <h1 class="page-title">数据库提交记录</h1>
      <p class="muted">仅显示你自己的 SQL 提交 · 共 {{ total }} 条</p>
    </div>

    <div class="toolbar card">
      <label class="filter">
        <span class="filter-label">状态</span>
        <select v-model="status" class="input">
          <option v-for="f in statusFilters" :key="f.value || 'all'" :value="f.value">
            {{ f.label }}
          </option>
        </select>
      </label>
    </div>

    <p v-if="error" class="submit-error">{{ error }}</p>
    <p v-if="loading" class="muted loading">加载中…</p>
    <p v-else-if="list.length === 0" class="empty">暂无提交记录</p>

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
          <tr v-for="r in list" :key="r.submissionId">
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
                @click="selected = r"
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

    <div v-if="total > 0" class="pagination">
      <button class="btn btn-ghost btn-sm" :disabled="page <= 1" @click="changePage(page - 1)">
        上一页
      </button>
      <span class="page-info">第 {{ page }} / {{ totalPages }} 页</span>
      <button
        class="btn btn-ghost btn-sm"
        :disabled="page >= totalPages"
        @click="changePage(page + 1)"
      >
        下一页
      </button>
    </div>

    <Teleport to="body">
      <div v-if="selected" class="modal-overlay" @click.self="selected = null">
        <div class="modal card">
          <div class="modal-head">
            <h3 class="modal-title">
              提交 #{{ selected.submissionId }} · {{ selected.questionTitle }}
            </h3>
            <span class="modal-status" :class="statusTone(selected.status)">
              {{ STATUS_ABBR[selected.status] ?? selected.status }}
            </span>
            <button class="btn btn-ghost btn-sm" @click="selected = null">关闭</button>
          </div>
          <pre class="modal-code">{{ selected.sqlContent }}</pre>
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
  font-family: inherit;
}
.status-badge {
  cursor: pointer;
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
