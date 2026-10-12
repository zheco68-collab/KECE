<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { databaseApi } from '@/api/database'
import type { DbQuestion, DbQuestionType } from '@/types'

const keyword = ref('')
const type = ref<DbQuestionType | ''>('')
const page = ref(1)
const size = 10

const list = ref<DbQuestion[]>([])
const total = ref(0)
const loading = ref(false)
const error = ref('')

const typeLabels: Record<DbQuestionType, string> = {
  SELECT: '查询',
  DML: '数据操作',
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

watch([keyword, type], () => {
  page.value = 1
  load()
})

onMounted(load)
</script>

<template>
  <div class="db container">
    <div class="page-head">
      <div>
        <h1 class="page-title">数据库 OJ</h1>
        <p class="muted">SQL 编写与结果集校验 · MySQL 8.0 · 共 {{ total }} 题</p>
      </div>
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

    <div v-else class="db-list">
      <RouterLink v-for="q in list" :key="q.id" :to="`/database/${q.id}`" class="db-item card">
        <span class="badge" :class="`type-${q.type.toLowerCase()}`">{{ typeLabels[q.type] }}</span>
        <span class="db-title">{{ q.title }}</span>
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
.db-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.db-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 18px 20px;
  color: var(--ink);
  transition: box-shadow 0.15s ease;
}
.db-item:hover {
  box-shadow: var(--shadow-md);
}
.db-title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 500;
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
