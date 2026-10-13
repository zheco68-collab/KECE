<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { questionApi } from '@/api/question'
import { ApiError } from '@/api/request'
import FormField from '@/components/FormField.vue'
import MarkdownEditor from '@/components/MarkdownEditor.vue'
import { useToast } from '@/composables/useToast'
import type { Difficulty } from '@/types'

const route = useRoute()
const router = useRouter()
const toast = useToast()

const questionId = computed(() => {
  const raw = route.params.id
  return raw ? Number(raw) : null
})
const isEdit = computed(() => questionId.value !== null)

const submitting = ref(false)
const loading = ref(false)
const pageError = ref('')

const form = reactive({
  title: '',
  description: '',
  difficulty: 'EASY' as Difficulty,
  status: 1,
  timeLimit: 1000,
  memoryLimit: 256,
  languages: 'cpp,java,python',
})

const formErrors = reactive({
  title: '',
  description: '',
  timeLimit: '',
  memoryLimit: '',
})

/** 测试用例：clientId 区分本地条目，id 存在表示后端已有记录。 */
interface CaseRow {
  clientId: number
  id?: number
  input: string
  expectedOutput: string
  isSample: boolean
}

const cases = ref<CaseRow[]>([])
let caseSeq = 0
/** 编辑态下被删除的既有用例 id，保存时逐个调删除接口。 */
const removedCaseIds: number[] = []

function addCase() {
  cases.value.push({ clientId: ++caseSeq, input: '', expectedOutput: '', isSample: false })
}

function removeCase(clientId: number) {
  const target = cases.value.find((c) => c.clientId === clientId)
  if (target?.id !== undefined) removedCaseIds.push(target.id)
  cases.value = cases.value.filter((c) => c.clientId !== clientId)
}

async function loadQuestion() {
  if (!questionId.value) return
  loading.value = true
  pageError.value = ''
  try {
    const q = await questionApi.detail(questionId.value)
    form.title = q.title
    form.description = q.description
    form.difficulty = q.difficulty
    form.status = q.status
    form.timeLimit = q.timeLimit ?? 1000
    form.memoryLimit = q.memoryLimit ?? 256
    form.languages = q.languages.join(',')
    const testCases = await questionApi.listTestCases(questionId.value)
    cases.value = testCases.map((tc) => ({
      clientId: ++caseSeq,
      id: tc.id,
      input: tc.input ?? '',
      expectedOutput: tc.expectedOutput,
      isSample: tc.isSample === 1,
    }))
  } catch (e) {
    pageError.value = e instanceof ApiError ? e.message : '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function validate(): boolean {
  formErrors.title = form.title.trim() ? '' : '标题不能为空'
  formErrors.description = form.description.trim() ? '' : '题目描述不能为空'
  const t = form.timeLimit
  formErrors.timeLimit = t >= 1 && t <= 60000 ? '' : '时间限制需在 1-60000 毫秒之间'
  const m = form.memoryLimit
  formErrors.memoryLimit = m >= 1 && m <= 4096 ? '' : '内存限制需在 1-4096 MB 之间'
  const badCase = cases.value.find((c) => !c.expectedOutput.trim())
  if (badCase) {
    toast.error('测试用例的期望输出不能为空')
    return false
  }
  return !formErrors.title && !formErrors.description && !formErrors.timeLimit && !formErrors.memoryLimit
}

async function save() {
  if (submitting.value || !validate()) return
  submitting.value = true
  pageError.value = ''
  try {
    const body = {
      title: form.title.trim(),
      description: form.description,
      difficulty: form.difficulty,
      status: form.status,
      timeLimit: form.timeLimit,
      memoryLimit: form.memoryLimit,
      languages: form.languages.trim(),
    }
    let targetId: number
    if (questionId.value) {
      await questionApi.update(questionId.value, body)
      targetId = questionId.value
    } else {
      const created = await questionApi.create(body)
      targetId = created.id
    }
    // 用例逐条同步：老用例更新、新用例创建、删除掉的逐个移除。
    for (const c of cases.value) {
      const caseBody = {
        input: c.input || undefined,
        expectedOutput: c.expectedOutput,
        isSample: c.isSample ? 1 : 0,
      }
      if (c.id !== undefined) {
        await questionApi.updateTestCase(c.id, caseBody)
      } else {
        await questionApi.addTestCase(targetId, caseBody)
      }
    }
    for (const id of removedCaseIds) {
      await questionApi.removeTestCase(id)
    }
    toast.success(isEdit.value ? '题目已更新' : '题目已创建')
    router.push('/admin/questions')
  } catch (e) {
    pageError.value = e instanceof ApiError ? e.message : '保存失败，请稍后重试'
    toast.error(pageError.value)
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadQuestion()
})
</script>

<template>
  <div class="admin container">
    <RouterLink class="back" to="/admin/questions">← 返回编程题管理</RouterLink>
    <h1 class="page-title">{{ isEdit ? '编辑编程题' : '新建编程题' }}</h1>

    <p v-if="loading" class="muted">加载中…</p>
    <p v-if="pageError" class="submit-error">{{ pageError }}</p>

    <div v-if="!loading" class="form-cards">
      <div class="card form-card">
        <h2 class="card-title">基本信息</h2>
        <FormField label="标题" required :error="formErrors.title">
          <input v-model="form.title" class="input" placeholder="题目标题" />
        </FormField>
        <div class="field-row">
          <FormField label="难度" required>
            <select v-model="form.difficulty" class="input">
              <option value="EASY">简单</option>
              <option value="MEDIUM">中等</option>
              <option value="HARD">困难</option>
            </select>
          </FormField>
          <FormField label="状态">
            <select v-model="form.status" class="input">
              <option :value="1">公开</option>
              <option :value="0">私有</option>
            </select>
          </FormField>
        </div>
        <div class="field-row">
          <FormField label="时间限制" :error="formErrors.timeLimit" hint="毫秒，1-60000">
            <input v-model.number="form.timeLimit" class="input" type="number" min="1" max="60000" />
          </FormField>
          <FormField label="内存限制" :error="formErrors.memoryLimit" hint="MB，1-4096">
            <input v-model.number="form.memoryLimit" class="input" type="number" min="1" max="4096" />
          </FormField>
        </div>
        <FormField label="支持语言" hint="逗号分隔，如 cpp,java,python">
          <input v-model="form.languages" class="input" placeholder="cpp,java,python" />
        </FormField>
      </div>

      <div class="card form-card">
        <h2 class="card-title">题目描述</h2>
        <FormField label="描述（Markdown）" required :error="formErrors.description">
          <MarkdownEditor v-model="form.description" placeholder="支持 Markdown 语法" />
        </FormField>
      </div>

      <div class="card form-card">
        <div class="card-head">
          <h2 class="card-title">测试用例</h2>
          <button class="btn btn-ghost btn-sm" type="button" @click="addCase">添加测试点</button>
        </div>
        <p v-if="cases.length === 0" class="muted">暂无测试点，请添加。</p>
        <div v-for="c in cases" :key="c.clientId" class="case-card">
          <div class="case-head">
            <span class="muted">测试点 #{{ c.clientId }}</span>
            <label class="sample-check">
              <input v-model="c.isSample" type="checkbox" />
              样例用例
            </label>
            <button class="btn btn-ghost btn-sm case-remove" type="button" @click="removeCase(c.clientId)">
              删除
            </button>
          </div>
          <FormField label="输入" hint="可为空">
            <textarea v-model="c.input" class="input textarea" placeholder="标准输入" />
          </FormField>
          <FormField label="期望输出" required>
            <textarea v-model="c.expectedOutput" class="input textarea" placeholder="期望输出" />
          </FormField>
        </div>
      </div>

      <div class="form-actions">
        <button class="btn btn-primary" type="button" :disabled="submitting" @click="save">
          {{ submitting ? '保存中…' : '保存' }}
        </button>
        <button class="btn btn-ghost" type="button" :disabled="submitting" @click="router.back()">
          取消
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin {
  padding-top: 32px;
  padding-bottom: 48px;
}
.page-title {
  margin: 28px 0 16px;
  font-size: 22px;
  font-weight: 600;
}
.form-cards {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.form-card {
  padding: 20px 22px;
}
.card-title {
  margin: 0 0 16px;
  font-size: 16px;
  font-weight: 600;
}
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.card-head .card-title {
  margin: 0;
}
.field-row {
  display: flex;
  gap: 20px;
}
.field-row > * {
  flex: 1;
}
.case-card {
  border: 1px solid var(--line);
  padding: 14px 16px;
  margin-bottom: 14px;
  background: var(--bg);
}
.case-head {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
}
.sample-check {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--ink-secondary);
}
.case-remove {
  margin-left: auto;
}
.textarea {
  height: auto;
  min-height: 88px;
  padding: 10px 12px;
  resize: vertical;
  line-height: 1.5;
  font-family: inherit;
}
.form-actions {
  display: flex;
  gap: 12px;
}
</style>
