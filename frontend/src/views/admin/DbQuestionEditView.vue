<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { databaseApi } from '@/api/database'
import { ApiError } from '@/api/request'
import AdminNav from '@/components/AdminNav.vue'
import FormField from '@/components/FormField.vue'
import MarkdownEditor from '@/components/MarkdownEditor.vue'
import DbSchemaViewer from '@/components/DbSchemaViewer.vue'
import DbSampleViewer from '@/components/DbSampleViewer.vue'
import { generateMarkdownTemplate } from '@/utils/sqlSchemaParser'
import { useToast } from '@/composables/useToast'
import { useConfirm } from '@/composables/useConfirm'
import type { DbQuestionType } from '@/types'

const route = useRoute()
const router = useRouter()
const toast = useToast()
const { confirm } = useConfirm()

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
  dialect: 'MYSQL',
  type: 'SELECT' as DbQuestionType,
  status: 1,
  schemaSql: '',
  referenceSql: '',
  verifySql: '',
  orderSensitive: false,
})

const formErrors = reactive({
  title: '',
  description: '',
  schemaSql: '',
  referenceSql: '',
  verifySql: '',
})

/** 测试点：clientId 区分本地条目；保存时整包提交，后端整表替换。 */
interface SeedRow {
  clientId: number
  seedSql: string
  expectedOutput: string
  isSample: boolean
}

const seeds = ref<SeedRow[]>([])
let seedSeq = 0

function addSeed() {
  seeds.value.push({
    clientId: ++seedSeq,
    seedSql: '',
    expectedOutput: '',
    isSample: false,
  })
}

function removeSeed(clientId: number) {
  seeds.value = seeds.value.filter((s) => s.clientId !== clientId)
}

const sampleCasesList = computed(() => {
  return seeds.value
    .filter((s) => s.seedSql.trim())
    .map((s, idx) => ({
      id: s.clientId,
      caseOrder: idx + 1,
      seedSql: s.seedSql,
      expectedOutput: s.expectedOutput,
      isSample: s.isSample ? 1 : 0,
    }))
})

async function handleGenerateTemplate() {
  if (!form.schemaSql.trim()) {
    toast.error('请先在下方填写建表 DDL，以便解析表结构并生成样例模板')
    return
  }
  const sampleSeed =
    seeds.value.find((s) => s.isSample && s.seedSql.trim()) ||
    seeds.value.find((s) => s.seedSql.trim())
  const template = generateMarkdownTemplate(
    form.schemaSql,
    sampleSeed?.seedSql,
    form.title,
    sampleSeed?.expectedOutput,
  )

  if (form.description.trim()) {
    const ok = await confirm({
      title: '生成标准题面模板',
      message: '检测到当前题目描述已有内容。是否覆盖现有内容？（点击「确定覆盖」替换，点击「取消」则追加到末尾）',
      confirmText: '确定覆盖',
      cancelText: '追加至末尾',
      danger: true,
    })
    if (ok) {
      form.description = template
      toast.success('已覆盖生成标准题面模板')
    } else {
      form.description += '\n\n' + template
      toast.success('已将标准模板追加至描述末尾')
    }
  } else {
    form.description = template
    toast.success('已自动生成标准题面与示例表格')
  }
}

async function loadQuestion() {
  if (!questionId.value) return
  loading.value = true
  pageError.value = ''
  try {
    const q = await databaseApi.detail(questionId.value)
    form.title = q.title
    form.description = q.description
    form.dialect = q.dialect
    form.type = q.type
    form.status = q.status
    form.schemaSql = q.schemaSql
    form.referenceSql = q.referenceSql ?? ''
    form.verifySql = q.verifySql ?? ''
    form.orderSensitive = q.orderSensitive
    seeds.value = q.testCases.map((tc) => ({
      clientId: ++seedSeq,
      seedSql: tc.seedSql,
      expectedOutput: tc.expectedOutput ?? '',
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
  formErrors.schemaSql = form.schemaSql.trim() ? '' : '建表语句不能为空'
  formErrors.referenceSql = form.referenceSql.trim() ? '' : '标准答案 SQL 不能为空'
  formErrors.verifySql = form.type === 'DML' && !form.verifySql.trim() ? 'DML 题需要校验查询' : ''
  const badSeed = seeds.value.find((s) => !s.seedSql.trim())
  if (badSeed) {
    toast.error('测试点的灌数据 SQL 不能为空')
    return false
  }
  return (
    !formErrors.title &&
    !formErrors.description &&
    !formErrors.schemaSql &&
    !formErrors.referenceSql &&
    !formErrors.verifySql
  )
}

async function save() {
  if (submitting.value || !validate()) return
  submitting.value = true
  pageError.value = ''
  try {
    const body = {
      title: form.title.trim(),
      description: form.description,
      dialect: form.dialect,
      type: form.type,
      status: form.status,
      schemaSql: form.schemaSql,
      referenceSql: form.referenceSql,
      verifySql: form.type === 'DML' ? form.verifySql : undefined,
      orderSensitive: form.orderSensitive,
      testCases: seeds.value.map((s) => ({
        seedSql: s.seedSql,
        expectedOutput: s.expectedOutput.trim() || undefined,
        isSample: s.isSample ? 1 : 0,
      })),
    }
    if (questionId.value) {
      await databaseApi.update(questionId.value, body)
    } else {
      await databaseApi.create(body)
    }
    toast.success(isEdit.value ? '题目已更新' : '题目已创建')
    router.push('/admin/db-questions')
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
    <RouterLink class="back" to="/admin/db-questions">← 返回数据库题管理</RouterLink>
    <h1 class="page-title">{{ isEdit ? '编辑数据库题' : '新建数据库题' }}</h1>
    <AdminNav />

    <p v-if="loading" class="muted">加载中…</p>
    <p v-if="pageError" class="submit-error">{{ pageError }}</p>

    <div v-if="!loading" class="form-cards">
      <div class="card form-card">
        <h2 class="card-title">基本信息</h2>
        <FormField label="标题" required :error="formErrors.title">
          <input v-model="form.title" class="input" placeholder="题目标题" />
        </FormField>
        <div class="field-row">
          <FormField label="题型" required>
            <select v-model="form.type" class="input">
              <option value="SELECT">查询</option>
              <option value="DML">变更</option>
            </select>
          </FormField>
          <FormField label="方言">
            <select v-model="form.dialect" class="input">
              <option value="MYSQL">MySQL</option>
            </select>
          </FormField>
          <FormField label="状态">
            <select v-model="form.status" class="input">
              <option :value="1">公开</option>
              <option :value="0">私有</option>
            </select>
          </FormField>
        </div>
        <FormField label="题目描述" required :error="formErrors.description">
          <div class="editor-assistant-bar">
            <span class="assistant-tip">
              规范题面建议包含题目要求、表结构说明及样例输入与期望输出表格
            </span>
            <button
              type="button"
              class="btn-assistant"
              title="根据下方建表 DDL 与测试点数据，自动生成标准 Markdown 模板"
              @click="handleGenerateTemplate"
            >
              生成标准题面模板
            </button>
          </div>
          <MarkdownEditor v-model="form.description" placeholder="支持 Markdown 语法" />
        </FormField>
      </div>

      <div class="card form-card">
        <h2 class="card-title">SQL 定义</h2>
        <FormField label="建表 DDL" required :error="formErrors.schemaSql">
          <textarea v-model="form.schemaSql" class="input textarea sql-area" placeholder="CREATE TABLE ..." />
        </FormField>
        <div v-if="form.schemaSql.trim()" class="preview-block">
          <div class="preview-title">DDL 字典视图预览：</div>
          <DbSchemaViewer compact :schema-sql="form.schemaSql" />
        </div>
        <FormField label="标准答案 SQL" required :error="formErrors.referenceSql">
          <textarea
            v-model="form.referenceSql"
            class="input textarea sql-area"
            placeholder="SELECT ... / UPDATE ..."
          />
        </FormField>
        <FormField
          v-if="form.type === 'DML'"
          label="校验查询"
          required
          :error="formErrors.verifySql"
          hint="DML 题判分用的校验 SELECT，查询题留空"
        >
          <textarea v-model="form.verifySql" class="input textarea sql-area" placeholder="SELECT ..." />
        </FormField>
        <label class="order-check">
          <input v-model="form.orderSensitive" type="checkbox" />
          行序敏感（结果集按行顺序比对）
        </label>
      </div>

      <div class="card form-card">
        <div class="card-head">
          <h2 class="card-title">数据测试点</h2>
          <button class="btn btn-ghost btn-sm" type="button" @click="addSeed">添加测试点</button>
        </div>
        <p v-if="seeds.length === 0" class="muted">暂无测试点，请添加。</p>
        <div v-for="s in seeds" :key="s.clientId" class="case-card">
          <div class="case-head">
            <span class="muted">测试点 #{{ s.clientId }}</span>
            <label class="sample-check">
              <input v-model="s.isSample" type="checkbox" />
              样例用例
            </label>
            <button class="btn btn-ghost btn-sm case-remove" type="button" @click="removeSeed(s.clientId)">
              删除
            </button>
          </div>
          <FormField label="灌数据 SQL" required hint="INSERT / UPDATE 等初始化数据语句">
            <textarea v-model="s.seedSql" class="input textarea sql-area" placeholder="INSERT INTO ..." />
          </FormField>
          <FormField
            label="期望查询结果 (可选)"
            hint="支持标准结果集 JSON（如 {&quot;columns&quot;:[...],&quot;rows&quot;:[...]}）或 Markdown 管道表格；公开样例填写后将直接在学生题面展现结果网格"
          >
            <textarea
              v-model="s.expectedOutput"
              class="input textarea sql-area"
              placeholder='{"columns":[{"name":"id","type":"INT"},{"name":"name","type":"VARCHAR"}],"rows":[["1","Alice"],["2","Bob"]]}'
            />
          </FormField>
        </div>

        <!-- 测试点数据表格全景预览 -->
        <div v-if="sampleCasesList.length > 0 && form.schemaSql.trim()" class="preview-block">
          <div class="preview-title">测试点数据网格全景预览：</div>
          <DbSampleViewer :sample-cases="sampleCasesList" :schema-sql="form.schemaSql" />
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
.order-check {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: var(--ink-secondary);
}
.textarea {
  height: auto;
  min-height: 88px;
  padding: 10px 12px;
  resize: vertical;
  line-height: 1.5;
  font-family: inherit;
}
.sql-area {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 13px;
}
.form-actions {
  display: flex;
  gap: 12px;
}
.editor-assistant-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
  padding: 8px 12px;
  background: var(--bg);
  border: 1px dashed var(--line);
  font-size: 12px;
}
.assistant-tip {
  color: var(--ink-secondary);
}
.btn-assistant {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 12px;
  background: var(--surface);
  border: 1px solid var(--line);
  font-size: 12px;
  font-weight: 600;
  color: var(--ink);
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.15s ease;
}
.btn-assistant:hover {
  background: var(--ink);
  color: #fff;
  border-color: var(--ink);
}
.preview-block {
  margin-top: 14px;
  padding: 14px 16px;
  background: var(--bg);
  border: 1px solid var(--line);
}
.preview-title {
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: var(--ink-secondary);
  margin-bottom: 10px;
}
</style>
