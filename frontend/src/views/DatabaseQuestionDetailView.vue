<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { databaseApi } from '@/api/database'
import { renderMarkdown } from '@/utils/markdown'
import { useUserStore } from '@/stores/user'
import { useToast } from '@/composables/useToast'
import type { DbQuestion, DbQuestionType } from '@/types'
import { copyToClipboard } from '@/utils/clipboard'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { success, error: toastError } = useToast()

const id = computed(() => Number(route.params.id))

const question = ref<DbQuestion | null>(null)
const loading = ref(true)
const error = ref('')
const isSolved = ref(false)

const typeLabels: Record<DbQuestionType, string> = {
  SELECT: 'SELECT 查询',
  DML: 'DML 数据操作',
}

const descriptionHtml = computed(() => renderMarkdown(question.value?.description ?? ''))
const sampleCases = computed(() => (question.value?.testCases ?? []).filter((tc) => tc.isSample === 1))

async function copyText(text: string | null | undefined, label: string) {
  if (!text) return
  const ok = await copyToClipboard(text)
  if (ok) {
    success(`${label}已复制到剪贴板`)
  } else {
    toastError('复制失败，请手动选取复制')
  }
}

function goToSubmit() {
  router.push(`/database/${id.value}/submit`)
}

async function loadSolvedStatus() {
  if (!userStore.isLoggedIn) return
  try {
    const solvedIds = await databaseApi.solved()
    if (solvedIds.includes(id.value)) {
      isSolved.value = true
    }
  } catch {
    /* 忽略已通过状态拉取失败 */
  }
}

onMounted(async () => {
  try {
    question.value = await databaseApi.detail(id.value)
    await loadSolvedStatus()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载题目失败'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="db-question-page container">
    <!-- 加载与异常状态 -->
    <div v-if="loading" class="state-card card">
      <div class="loading-spinner"></div>
      <p class="muted">正在加载数据库题目描述与表结构…</p>
    </div>

    <div v-else-if="error" class="state-card card">
      <div class="error-title">加载失败</div>
      <p class="error-msg">{{ error }}</p>
      <button class="btn btn-outline" @click="router.push('/database')">返回数据库题库</button>
    </div>

    <!-- 题目主内容区 -->
    <template v-else-if="question">
      <!-- 顶部信息与导航卡片 -->
      <header class="q-header card">
        <div class="header-top">
          <button class="btn-back" @click="router.push('/database')">
            <svg class="back-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="19" y1="12" x2="5" y2="12" />
              <polyline points="12 19 5 12 12 5" />
            </svg>
            <span>返回数据库题库</span>
          </button>

          <div class="header-status-tags">
            <span v-if="isSolved" class="solved-pill">✓ 已通过此题</span>
            <span class="badge type-pill" :class="`type-${question.type.toLowerCase()}`">
              {{ typeLabels[question.type] }}
            </span>
          </div>
        </div>

        <div class="header-main">
          <h1 class="q-title">
            <span class="q-id">#{{ question.id }}</span>
            <span class="q-text">{{ question.title }}</span>
          </h1>

          <button class="btn btn-primary btn-submit-cta" @click="goToSubmit">
            <span>提交 SQL</span>
            <svg class="cta-arrow" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="5" y1="12" x2="19" y2="12" />
              <polyline points="12 5 19 12 12 19" />
            </svg>
          </button>
        </div>

        <!-- 数据库规格参数栏 -->
        <div class="header-meta-row">
          <div class="meta-item">
            <span class="meta-label">SQL 方言</span>
            <span class="meta-val">{{ question.dialect || 'MYSQL 8.0' }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">测试点数量</span>
            <span class="meta-val">{{ question.testCases?.length ?? 0 }} 个独立测试点</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">行序校验规则</span>
            <span class="meta-val" :class="{ 'highlight-order': question.orderSensitive }">
              {{ question.orderSensitive ? '严格按行顺序比对 (ORDER BY)' : '无序集合比对' }}
            </span>
          </div>
          <div class="meta-item">
            <span class="meta-label">执行沙箱</span>
            <span class="meta-val">专用隔离 MySQL 实例</span>
          </div>
        </div>
      </header>

      <!-- 题面正文卡片 -->
      <article class="q-body-card card">
        <div class="card-section-head">
          <span class="sec-title">题目要求与说明</span>
        </div>
        <!-- eslint-disable-next-line vue/no-v-html -- 内容已由 renderMarkdown 清洗 -->
        <div class="prose" v-html="descriptionHtml"></div>
      </article>

      <!-- 表结构定义 (Schema DDL) 卡片 -->
      <section class="schema-card card">
        <div class="card-section-head">
          <span class="sec-title">数据库表结构定义 (Schema DDL)</span>
          <button class="btn-copy" @click="copyText(question.schemaSql, '建表语句')">
            复制 DDL
          </button>
        </div>
        <div class="schema-content">
          <pre class="sql-code">{{ question.schemaSql }}</pre>
        </div>
      </section>

      <!-- 样例数据卡片 -->
      <section v-if="sampleCases.length" class="samples-card card">
        <div class="card-section-head">
          <span class="sec-title">样例初始数据 (Seed Data)</span>
          <span class="sec-count">共 {{ sampleCases.length }} 组公开样例</span>
        </div>

        <div class="samples-grid">
          <div
            v-for="(tc, i) in sampleCases"
            :key="tc.id ?? i"
            class="sample-item card"
          >
            <div class="sample-item-head">
              <span class="sample-idx">样例 #{{ i + 1 }}</span>
              <button class="btn-copy" @click="copyText(tc.seedSql, `样例 #${i + 1} 初始数据`)">
                复制初始 SQL
              </button>
            </div>
            <div class="sample-content">
              <pre class="sql-code">{{ tc.seedSql }}</pre>
            </div>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.db-question-page {
  padding-top: 24px;
  padding-bottom: 64px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 状态加载卡片 */
.state-card {
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
  to {
    transform: rotate(360deg);
  }
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

/* 顶部信息卡片 */
.q-header {
  padding: 24px 28px;
  border-radius: 0;
  display: flex;
  flex-direction: column;
  gap: 18px;
  background: var(--surface);
}
.header-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.btn-back {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border: 1px solid var(--line);
  background: var(--bg);
  color: var(--ink-secondary);
  font-size: 13px;
  font-weight: 500;
  border-radius: 0;
  cursor: pointer;
  transition: all 0.15s ease;
}
.btn-back:hover {
  background: var(--surface);
  color: var(--ink);
  border-color: var(--ink);
}
.back-icon {
  width: 14px;
  height: 14px;
}
.header-status-tags {
  display: flex;
  align-items: center;
  gap: 10px;
}
.solved-pill {
  font-size: 12px;
  font-weight: 700;
  color: var(--success);
  background: #e8f5e9;
  border: 1px solid var(--success);
  padding: 3px 8px;
  border-radius: 0;
}
.type-pill {
  border-radius: 0 !important;
  font-size: 12px;
  padding: 3px 10px;
  font-weight: 700;
}

.header-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;
}
.q-title {
  margin: 0;
  font-size: 26px;
  font-weight: 800;
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--ink);
}
.q-id {
  color: var(--ink-tertiary);
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
}
.btn-submit-cta {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 24px;
  font-size: 14px;
  font-weight: 700;
  border-radius: 0;
  cursor: pointer;
}
.cta-arrow {
  width: 16px;
  height: 16px;
}

.header-meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 28px;
  padding-top: 16px;
  border-top: 1px solid var(--line);
}
.meta-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.meta-label {
  font-size: 11px;
  color: var(--ink-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.meta-val {
  font-size: 13px;
  font-weight: 600;
  color: var(--ink);
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
}
.highlight-order {
  color: #e65100;
}

/* 章节头部 */
.card-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 24px;
  background: var(--bg);
  border-bottom: 1px solid var(--line);
}
.sec-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--ink);
}
.sec-count {
  font-size: 12px;
  color: var(--ink-tertiary);
}
.btn-copy {
  padding: 3px 10px;
  font-size: 11px;
  font-weight: 600;
  border: 1px solid var(--line);
  background: var(--surface);
  color: var(--ink-secondary);
  cursor: pointer;
  border-radius: 0;
  transition: all 0.15s ease;
}
.btn-copy:hover {
  background: var(--ink);
  color: #fff;
  border-color: var(--ink);
}

/* 正文与代码块 */
.q-body-card,
.schema-card,
.samples-card {
  border-radius: 0;
  background: var(--surface);
  overflow: hidden;
}
.prose {
  padding: 28px;
  line-height: 1.8;
  font-size: 15px;
  color: var(--ink);
}
.prose :deep(p) {
  margin: 14px 0;
}
.prose :deep(code) {
  padding: 2px 6px;
  background: var(--bg);
  color: var(--ink);
  border: 1px solid var(--line);
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 13px;
  border-radius: 0;
}
.prose :deep(pre) {
  background: var(--bg);
  color: var(--ink);
  border: 1px solid var(--line);
  padding: 16px;
  overflow-x: auto;
  border-radius: 0;
}
.prose :deep(pre code) {
  background: none;
  border: none;
  padding: 0;
  color: inherit;
}

.schema-content {
  padding: 20px 24px;
}
.sql-code {
  margin: 0;
  padding: 16px;
  background: var(--bg);
  border: 1px solid var(--line);
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 13px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-all;
  color: var(--ink);
  border-radius: 0;
}

/* 样例卡片 */
.samples-grid {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.sample-item {
  border-radius: 0;
  border: 1px solid var(--line);
  background: var(--surface);
  overflow: hidden;
}
.sample-item-head {
  padding: 10px 16px;
  background: var(--bg);
  border-bottom: 1px solid var(--line);
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.sample-idx {
  font-size: 13px;
  font-weight: 700;
  color: var(--ink);
}
.sample-content {
  padding: 14px 16px;
}
</style>
