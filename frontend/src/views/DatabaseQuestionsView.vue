<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { databaseApi } from '@/api/database'
import { useUserStore } from '@/stores/user'
import type { DbQuestion, DbQuestionType } from '@/types'

const keyword = ref('')
const type = ref<DbQuestionType | ''>('')
const page = ref(1)
const size = 10

const list = ref<DbQuestion[]>([])
const total = ref(0)
const loading = ref(false)
const error = ref('')

const userStore = useUserStore()
const solved = ref<Set<number>>(new Set())

const typeLabels: Record<DbQuestionType, string> = {
  SELECT: 'SELECT 查询',
  DML: 'DML 操作',
}
const typeLevels: Array<DbQuestionType | ''> = ['', 'SELECT', 'DML']

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size)))

async function load() {
  loading.value = true
  error.value = ''
  try {
    const res = await databaseApi.list({
      keyword: keyword.value.trim() || undefined,
      type: type.value || undefined,
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

async function loadSolved() {
  if (!userStore.isLoggedIn) return
  try {
    const ids = await databaseApi.solved()
    solved.value = new Set(ids)
  } catch {
    /* 未登录或失败时忽略 */
  }
}

watch([keyword, type], () => {
  page.value = 1
  load()
})

onMounted(() => {
  load()
  loadSolved()
})
</script>

<template>
  <div class="questions container">
    <div class="page-head">
      <h1 class="page-title">数据库</h1>
      <p class="muted">SQL 语句题目 · 共 {{ total }} 题</p>
    </div>

    <div class="toolbar card">
      <input v-model="keyword" class="input search" type="search" placeholder="搜索题目标题…" />
      <div class="type-tabs">
        <button
          v-for="t in typeLevels"
          :key="t || 'all'"
          class="tab"
          :class="{ active: type === t }"
          @click="type = t"
        >
          {{ t ? typeLabels[t] : '全部' }}
        </button>
      </div>
    </div>

    <p v-if="error" class="submit-error">{{ error }}</p>
    <p v-if="loading" class="muted loading">加载中…</p>
    <p v-else-if="list.length === 0" class="empty">暂无题目</p>

    <div v-else class="question-list">
      <RouterLink
        v-for="q in list"
        :key="q.id"
        :to="`/database/${q.id}`"
        class="question-item card"
      >
        <div class="q-main">
          <h3 class="q-title">{{ q.title }}</h3>
          <p class="q-meta">
            {{ q.dialect || 'MySQL 8.0' }} · {{ q.orderSensitive ? '严格按行有序' : '无序比对' }}
          </p>
        </div>
        <div class="q-right">
          <span class="badge" :class="`type-${q.type.toLowerCase()}`">
            {{ typeLabels[q.type] || q.type }}
          </span>
          <span class="solved-slot">
            <span v-if="solved.has(q.id)" class="solved-mark" title="已通过">✓</span>
          </span>
        </div>
      </RouterLink>
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
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 16px;
  margin-bottom: 24px;
}
.search {
  max-width: 360px;
}
.type-tabs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.tab {
  padding: 7px 16px;
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
.loading {
  padding: 32px 0;
}
.empty {
  padding: 40px 0;
  text-align: center;
  color: var(--ink-tertiary);
}
.question-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.question-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
  transition: box-shadow 0.15s ease;
}
.question-item:hover {
  box-shadow: var(--shadow-md);
}
.q-main {
  min-width: 0;
}
.q-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}
.badge {
  min-width: 80px;
  justify-content: center;
  text-align: center;
}
.solved-slot {
  width: 20px;
  height: 20px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.solved-mark {
  color: var(--success);
  font-size: 18px;
  font-weight: 700;
  line-height: 1;
}
.q-title {
  margin: 0 0 6px;
  font-size: 17px;
  font-weight: 600;
}
.q-meta {
  margin: 0;
  color: var(--ink-tertiary);
  font-size: 13px;
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
</style>
