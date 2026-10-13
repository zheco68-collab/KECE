<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { adminApi } from '@/api/admin'
import { ApiError } from '@/api/request'
import AdminNav from '@/components/AdminNav.vue'
import DataTable from '@/components/DataTable.vue'
import Pagination from '@/components/Pagination.vue'
import { useUserStore } from '@/stores/user'
import type { ObjectiveType } from '@/types'

type TypeFilter = '' | ObjectiveType
type StatusFilter = '' | '1' | '0'

/** 管理列表行：字段与后端 ObjectiveQuestionVO 对齐，只声明页面用到的列。 */
interface ObjectiveRow {
  id: number
  type: ObjectiveType
  content: string
  score: number
  status: number
  createdBy: number
  createdAt: string
}

const router = useRouter()
const userStore = useUserStore()

const keyword = ref('')
const typeFilter = ref<TypeFilter>('')
const statusFilter = ref<StatusFilter>('')
const createdByInput = ref('')
const page = ref(1)
const size = 20

const rows = ref<ObjectiveRow[]>([])
const total = ref(0)
const loading = ref(false)
const loadError = ref('')

const TYPE_LABELS: Record<ObjectiveType, string> = {
  SINGLE: '单选',
  MULTIPLE: '多选',
  FILL: '填空',
}

const typeOptions: Array<{ value: TypeFilter; label: string }> = [
  { value: '', label: '全部题型' },
  { value: 'SINGLE', label: '单选' },
  { value: 'MULTIPLE', label: '多选' },
  { value: 'FILL', label: '填空' },
]

const statusOptions: Array<{ value: StatusFilter; label: string }> = [
  { value: '', label: '全部状态' },
  { value: '1', label: '公开' },
  { value: '0', label: '私有' },
]

const columns = [
  { key: 'id', label: 'ID', width: '64px' },
  { key: 'content', label: '题干' },
  { key: 'type', label: '题型', width: '90px' },
  { key: 'score', label: '分值', width: '70px' },
  { key: 'status', label: '状态', width: '90px' },
  { key: 'createdBy', label: '出题人', width: '100px' },
  { key: 'createdAt', label: '创建时间', width: '170px' },
]

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    const createdByRaw = createdByInput.value.trim()
    const createdByNum = createdByRaw === '' ? NaN : Number(createdByRaw)
    const res = await adminApi.listObjectiveQuestions({
      keyword: keyword.value.trim() || undefined,
      type: typeFilter.value || undefined,
      status: statusFilter.value === '' ? undefined : Number(statusFilter.value),
      createdBy: Number.isFinite(createdByNum) ? createdByNum : undefined,
      page: page.value,
      size,
    })
    rows.value = res.list
    total.value = res.total
  } catch (e) {
    loadError.value = e instanceof ApiError ? e.message : '请求失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

/** 筛选变化时回到第一页再加载；已在第一页则手动触发。 */
function resetAndLoad() {
  if (page.value === 1) {
    load()
  } else {
    page.value = 1
  }
}

// 关键词与创建人输入防抖 300ms；下拉筛选变化直接触发。
let filterTimer: ReturnType<typeof setTimeout> | null = null

watch([keyword, createdByInput], () => {
  if (filterTimer !== null) clearTimeout(filterTimer)
  filterTimer = setTimeout(resetAndLoad, 300)
})

watch([typeFilter, statusFilter], resetAndLoad)

watch(page, () => {
  load()
})

/** 创建时间：ISO 串截取前 19 位并把 T 换成空格。 */
function formatTime(iso: string): string {
  return iso ? iso.slice(0, 19).replace('T', ' ') : '—'
}

function goEdit(row: ObjectiveRow) {
  router.push(`/admin/objective-questions/${row.id}/edit`)
}

onMounted(() => {
  load()
})

onBeforeUnmount(() => {
  if (filterTimer !== null) clearTimeout(filterTimer)
})
</script>

<template>
  <div class="admin container">
    <h1 class="page-title">客观题管理</h1>
    <AdminNav />

    <div class="filter-bar card">
      <input v-model="keyword" class="input keyword-input" type="search" placeholder="题干关键词" />
      <label class="filter">
        <span class="filter-label">题型</span>
        <select v-model="typeFilter" class="input">
          <option v-for="t in typeOptions" :key="t.value || 'all'" :value="t.value">
            {{ t.label }}
          </option>
        </select>
      </label>
      <label class="filter">
        <span class="filter-label">状态</span>
        <select v-model="statusFilter" class="input">
          <option v-for="s in statusOptions" :key="s.value || 'all'" :value="s.value">
            {{ s.label }}
          </option>
        </select>
      </label>
      <label v-if="userStore.hasRole('ADMIN')" class="filter">
        <span class="filter-label">出题人</span>
        <input
          v-model="createdByInput"
          class="input creator-input"
          type="search"
          placeholder="创建人 ID"
        />
      </label>
      <RouterLink class="btn btn-primary btn-sm new-btn" to="/admin/objective-questions/new">
        新建
      </RouterLink>
    </div>

    <p v-if="loadError" class="submit-error">{{ loadError }}</p>

    <DataTable :columns="columns" :rows="rows" :loading="loading">
      <template #cell-content="{ row }">
        <span class="content-cell" :title="row.content">{{ row.content }}</span>
      </template>
      <template #cell-type="{ row }">
        <span class="badge" :class="`type-${row.type.toLowerCase()}`">
          {{ TYPE_LABELS[row.type as ObjectiveType] ?? row.type }}
        </span>
      </template>
      <template #cell-status="{ row }">
        <span class="badge" :class="row.status === 1 ? 'status-enabled' : 'status-disabled'">
          {{ row.status === 1 ? '公开' : '私有' }}
        </span>
      </template>
      <template #cell-createdBy="{ row }">
        <span class="muted">#{{ row.createdBy }}</span>
      </template>
      <template #cell-createdAt="{ row }">
        <span class="time">{{ formatTime(row.createdAt) }}</span>
      </template>
      <template #actions="{ row }">
        <div class="row-actions">
          <button class="btn btn-ghost btn-sm" type="button" @click="goEdit(row)">编辑</button>
        </div>
      </template>
    </DataTable>

    <Pagination v-model:page="page" :size="size" :total="total" />
  </div>
</template>

<style scoped>
.admin {
  padding-top: 32px;
}
.page-title {
  margin: 28px 0 16px;
  font-size: 22px;
  font-weight: 600;
}
.filter-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px 20px;
  padding: 16px;
  margin-bottom: 24px;
}
.keyword-input {
  max-width: 280px;
}
.filter {
  display: flex;
  align-items: center;
  gap: 10px;
}
.filter-label {
  color: var(--ink-secondary);
  font-size: 14px;
  white-space: nowrap;
}
.filter select {
  width: 120px;
}
.creator-input {
  width: 140px;
}
.new-btn {
  margin-left: auto;
}
.submit-error {
  margin: 0 0 16px;
}
.content-cell {
  display: block;
  max-width: 380px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
/* 题型徽章沿用 style.css 既有灰度分级。 */
.type-single {
  background: #f0f0f0;
  color: var(--ink-secondary);
}
.type-multiple {
  background: #e2e2e2;
  color: var(--ink);
}
.type-fill {
  background: var(--button);
  color: #fff;
}
.status-enabled {
  background: var(--success-bg);
  color: var(--success);
}
.status-disabled {
  background: var(--danger-bg);
  color: var(--danger);
}
.time {
  color: var(--ink-tertiary);
  font-size: 13px;
}
.row-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
