<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { practiceApi } from '@/api/practice'
import { questionApi } from '@/api/question'
import { databaseApi } from '@/api/database'
import { objectiveApi } from '@/api/objective'
import { ApiError } from '@/api/request'
import AdminNav from '@/components/AdminNav.vue'
import FormField from '@/components/FormField.vue'
import { useToast } from '@/composables/useToast'
import type { Question, DbQuestion, ObjectiveQuestion } from '@/types'
import type { PracticeQuestionItem } from '@/types/practice'

const router = useRouter()
const toast = useToast()

const submitting = ref(false)
const listLoading = ref(false)
const pageError = ref('')

const form = reactive({
  title: '',
  description: '',
  durationMinutes: 45,
})

/** 已选题目项 */
interface SelectedItem {
  questionId: number
  questionType: 'ALGO' | 'DB' | 'OBJECTIVE'
  score: number
  title: string
}

const selected = ref<SelectedItem[]>([])
const formErrors = reactive({ title: '', questions: '' })

// ---- 题库选取 ----
type PickTab = 'ALGO' | 'DB' | 'OBJECTIVE'
const tab = ref<PickTab>('ALGO')
const tabKeyword = ref('')
const algoList = ref<Question[]>([])
const dbList = ref<DbQuestion[]>([])
const objList = ref<ObjectiveQuestion[]>([])

const typeTabs: Array<{ value: PickTab; label: string }> = [
  { value: 'ALGO', label: '编程题' },
  { value: 'DB', label: '数据库题' },
  { value: 'OBJECTIVE', label: '客观题' },
]

const typeLabels: Record<PickTab, string> = {
  ALGO: '算法',
  DB: 'SQL',
  OBJECTIVE: '客观',
}

async function loadSource() {
  listLoading.value = true
  try {
    const keyword = tabKeyword.value.trim() || undefined
    if (tab.value === 'ALGO') {
      algoList.value = (await questionApi.list({ keyword, page: 1, size: 50 })).list
    } else if (tab.value === 'DB') {
      dbList.value = (await databaseApi.list({ keyword, page: 1, size: 50 })).list
    } else {
      objList.value = (await objectiveApi.list({ keyword, page: 1, size: 50 })).list
    }
  } catch (e) {
    pageError.value = '加载题库失败，请稍后重试'
  } finally {
    listLoading.value = false
  }
}

let tabKeywordTimer: ReturnType<typeof setTimeout> | null = null
watch(tabKeyword, () => {
  if (tabKeywordTimer !== null) clearTimeout(tabKeywordTimer)
  tabKeywordTimer = setTimeout(loadSource, 300)
})
watch(tab, loadSource)

function sourceRows(): Array<{ id: number; label: string }> {
  if (tab.value === 'ALGO') return algoList.value.map((q) => ({ id: q.id, label: q.title }))
  if (tab.value === 'DB') return dbList.value.map((q) => ({ id: q.id, label: q.title }))
  return objList.value.map((q) => ({
    id: q.id,
    label: q.content.length > 40 ? `${q.content.slice(0, 40)}…` : q.content,
  }))
}

function isSelected(questionId: number): boolean {
  return selected.value.some((s) => s.questionId === questionId && s.questionType === tab.value)
}

function addQuestion(row: { id: number; label: string }) {
  if (isSelected(row.id)) return
  const defaultScore = tab.value === 'OBJECTIVE' ? 5 : 20
  selected.value.push({
    questionId: row.id,
    questionType: tab.value,
    score: defaultScore,
    title: row.label,
  })
}

function removeQuestion(index: number) {
  selected.value.splice(index, 1)
}

function moveQuestion(index: number, dir: -1 | 1) {
  const target = index + dir
  if (target < 0 || target >= selected.value.length) return
  const [item] = selected.value.splice(index, 1)
  selected.value.splice(target, 0, item)
}

const totalScore = computed(() =>
  selected.value.reduce((acc, curr) => acc + (Number(curr.score) || 0), 0)
)

async function save() {
  formErrors.title = form.title.trim() ? '' : '请填写课练标题'
  formErrors.questions = selected.value.length > 0 ? '' : '请至少选择一道题目'
  if (formErrors.title || formErrors.questions) return

  if (selected.value.some((s) => !s.score || s.score <= 0)) {
    formErrors.questions = '每题分值需大于 0'
    return
  }

  submitting.value = true
  pageError.value = ''
  try {
    const questionsPayload: PracticeQuestionItem[] = selected.value.map((s, idx) => ({
      questionId: s.questionId,
      questionType: s.questionType,
      title: s.title,
      score: s.score,
      sortOrder: idx + 1,
    }))

    const res = await practiceApi.create({
      title: form.title.trim(),
      description: form.description.trim() || undefined,
      durationMinutes: form.durationMinutes,
      questions: questionsPayload,
    })

    toast.success(`课堂练习发起成功！6 位上机口令：${res.accessCode}`)
    router.push('/admin/practices')
  } catch (e) {
    pageError.value = e instanceof ApiError ? e.message : '发起课练失败，请重试'
  } finally {
    submitting.value = false
  }
}

onMounted(loadSource)
</script>

<template>
  <div class="practice-edit container">
    <h1 class="page-title">发起课堂练习</h1>
    <AdminNav />

    <p v-if="pageError" class="submit-error">{{ pageError }}</p>

    <!-- 基本信息卡片 -->
    <div class="form card">
      <FormField label="练习标题" :error="formErrors.title">
        <input
          v-model="form.title"
          class="input"
          type="text"
          placeholder="例如：第4周上机实验：动态规划与SQL分析实战"
        />
      </FormField>

      <FormField label="练习说明（Markdown）">
        <textarea
          v-model="form.description"
          class="input"
          rows="3"
          placeholder="可选，提示学生本次上机考核范围、重点算法或课堂注意事项..."
        ></textarea>
      </FormField>

      <FormField label="课上时长（分钟）">
        <input
          v-model.number="form.durationMinutes"
          class="input num"
          type="number"
          min="5"
          max="300"
          placeholder="45"
        />
      </FormField>
    </div>

    <!-- 组卷：选择题库 -->
    <div class="picker card">
      <h2 class="section-title">组卷</h2>
      <div class="tabs">
        <button
          v-for="t in typeTabs"
          :key="t.value"
          class="tab"
          :class="{ active: tab === t.value }"
          type="button"
          @click="tab = t.value"
        >
          {{ t.label }}
        </button>
        <input
          v-model="tabKeyword"
          class="input search"
          type="text"
          placeholder="搜索该题库…"
        />
      </div>

      <p v-if="listLoading" class="muted">加载题库中…</p>
      <ul v-else class="source-list">
        <li
          v-for="row in sourceRows()"
          :key="`${tab}-${row.id}`"
          class="source-row"
        >
          <span class="source-label">{{ row.label }}</span>
          <button
            class="btn btn-ghost btn-sm"
            type="button"
            :disabled="isSelected(row.id)"
            @click="addQuestion(row)"
          >
            {{ isSelected(row.id) ? '已添加' : '添加' }}
          </button>
        </li>
        <li v-if="sourceRows().length === 0" class="muted empty">该题库暂无可选题库</li>
      </ul>
    </div>

    <!-- 已选题目清单 -->
    <div class="chosen card">
      <h2 class="section-title">
        已选题目（{{ selected.length }} 题 · 满分 {{ totalScore }} 分）
        <span v-if="formErrors.questions" class="error-text">{{ formErrors.questions }}</span>
      </h2>
      <ul class="chosen-list">
        <li v-for="(s, i) in selected" :key="i" class="chosen-row">
          <span class="chosen-order mono">{{ i + 1 }}</span>
          <span class="type-pill" :class="`pill-${s.questionType.toLowerCase()}`">
            {{ typeLabels[s.questionType] }}
          </span>
          <span class="chosen-label">{{ s.title }}</span>
          <span class="chosen-score">
            <input
              v-model.number="s.score"
              class="input score-input"
              type="number"
              min="1"
            />
            分
          </span>
          <button
            class="btn btn-ghost btn-sm btn-icon"
            type="button"
            title="上移"
            :disabled="i === 0"
            @click="moveQuestion(i, -1)"
          >
            ↑
          </button>
          <button
            class="btn btn-ghost btn-sm btn-icon"
            type="button"
            title="下移"
            :disabled="i === selected.length - 1"
            @click="moveQuestion(i, 1)"
          >
            ↓
          </button>
          <button
            class="btn btn-ghost btn-sm btn-danger"
            type="button"
            @click="removeQuestion(i)"
          >
            移除
          </button>
        </li>
        <li v-if="selected.length === 0" class="muted empty">尚未选择题目，请在上方题库中点击「添加」</li>
      </ul>
    </div>

    <!-- 底部操作按钮 -->
    <div class="actions">
      <button class="btn btn-ghost" type="button" @click="router.push('/admin/practices')">
        取消
      </button>
      <button class="btn btn-primary" type="button" :disabled="submitting" @click="save">
        {{ submitting ? '生成口令中…' : '发起课堂练习并生成口令 →' }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.practice-edit {
  padding-top: 32px;
}

.page-title {
  margin: 28px 0 8px;
  font-size: 22px;
  font-weight: 600;
  letter-spacing: -0.5px;
}

.form,
.picker,
.chosen {
  padding: 16px;
  margin-bottom: 16px;
}

.field-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
}

.num {
  max-width: 240px;
}

.section-title {
  margin: 0 0 12px;
  font-size: 16px;
  font-weight: 600;
}

.tabs {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.tab {
  padding: 6px 14px;
  border: 1px solid var(--line);
  border-radius: var(--pill);
  background: var(--surface);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.tab.active {
  border-color: var(--ink);
  background: var(--ink);
  color: #fff;
}

.search {
  width: 240px;
  margin-left: auto;
}

.source-list,
.chosen-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.source-row,
.chosen-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  background: var(--surface);
}

.source-label,
.chosen-label {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13.5px;
}

.chosen-order {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: var(--ink);
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  flex-shrink: 0;
}

.type-pill {
  padding: 1px 7px;
  font-size: 11px;
  font-weight: 700;
  border: 1px solid var(--line);
  border-radius: 0;
  white-space: nowrap;
}

.pill-algo {
  color: #2b8a3e;
  background: #ebfbee;
  border-color: #b2f2bb;
}

.pill-db {
  color: #111111;
  background: #f1f3f5;
  border-color: #ced4da;
}

.pill-objective {
  color: #7048e8;
  background: #f8f0fc;
  border-color: #d0bfff;
}

.chosen-score {
  color: var(--ink-tertiary);
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.score-input {
  width: 64px;
}

.empty {
  padding: 12px 0;
  font-size: 13px;
}

.error-text {
  color: var(--danger);
  font-size: 13px;
  font-weight: 400;
  margin-left: 8px;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-bottom: 40px;
}

.btn-icon {
  width: 28px;
  padding: 0;
}

.btn-danger {
  color: var(--danger);
}

.btn-danger:hover {
  background: var(--danger-bg);
  border-color: var(--danger);
}

.submit-error {
  color: var(--danger);
  font-size: 13px;
  margin-bottom: 12px;
}
</style>

