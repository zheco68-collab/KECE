<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { questionApi } from '@/api/question'
import { renderMarkdown } from '@/utils/markdown'
import type { Difficulty, Question } from '@/types'

const route = useRoute()
const router = useRouter()
const id = computed(() => Number(route.params.id))

const question = ref<Question | null>(null)
const loading = ref(true)
const error = ref('')

const difficultyLabels: Record<Difficulty, string> = {
  EASY: '简单',
  MEDIUM: '中等',
  HARD: '困难',
}

const descriptionHtml = computed(() => renderMarkdown(question.value?.description ?? ''))

onMounted(async () => {
  try {
    question.value = await questionApi.detail(id.value)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="detail container">
    <button class="back" @click="router.back()">← 返回</button>

    <p v-if="loading" class="muted">加载中…</p>
    <div v-else-if="error" class="submit-error">{{ error }}</div>

    <template v-else-if="question">
      <div class="head">
        <h1 class="title">{{ question.title }}</h1>
        <span class="badge" :class="`difficulty-${question.difficulty.toLowerCase()}`">
          {{ difficultyLabels[question.difficulty] }}
        </span>
      </div>
      <div class="meta">
        <span>时间限制 {{ question.timeLimit ?? '-' }} ms</span>
        <span>内存限制 {{ question.memoryLimit ?? '-' }} MB</span>
        <span v-if="question.languages?.length">语言 {{ question.languages.join(' / ') }}</span>
      </div>

      <!-- eslint-disable-next-line vue/no-v-html -- 内容已由 renderMarkdown 清洗 -->
      <div class="prose card" v-html="descriptionHtml"></div>

      <h2 class="section-title">样例</h2>
      <div
        v-for="(tc, i) in question.sampleTestCases"
        :key="tc.id"
        class="case card"
      >
        <div class="case-head">样例 {{ i + 1 }}</div>
        <div class="case-block">
          <div class="case-label">输入</div>
          <pre>{{ tc.input || '（无）' }}</pre>
        </div>
        <div class="case-block">
          <div class="case-label">输出</div>
          <pre>{{ tc.expectedOutput }}</pre>
        </div>
      </div>

      <p class="muted notice">在线代码编辑器与评测将在后续版本开放。</p>
    </template>
  </div>
</template>

<style scoped>
.detail {
  padding-top: 32px;
  padding-bottom: 56px;
}
.head {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
}
.title {
  margin: 0;
  font-size: 26px;
}
.meta {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
  color: var(--ink-tertiary);
  font-size: 13px;
  margin-bottom: 20px;
}
.section-title {
  margin: 32px 0 16px;
  font-size: 18px;
}
.case {
  margin-bottom: 16px;
  overflow: hidden;
}
.case-head {
  padding: 12px 20px;
  font-weight: 600;
  border-bottom: 1px solid var(--line);
  background: var(--bg);
}
.case-block {
  padding: 14px 20px;
  border-bottom: 1px solid var(--line);
}
.case-block:last-child {
  border-bottom: none;
}
.case-label {
  color: var(--ink-tertiary);
  font-size: 12px;
  margin-bottom: 6px;
}
.case-block pre {
  margin: 0;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 14px;
  white-space: pre-wrap;
  word-break: break-all;
}
.notice {
  margin-top: 24px;
}
</style>
