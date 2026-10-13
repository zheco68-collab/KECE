<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { examApi } from '@/api/exam';
import { ApiError } from '@/api/request';
import { databaseApi } from '@/api/database';
import { objectiveApi } from '@/api/objective';
import { questionApi } from '@/api/question';
import AdminNav from '@/components/AdminNav.vue';
import FormField from '@/components/FormField.vue';
import { useToast } from '@/composables/useToast';
import type { Question } from '@/types';
import type { DbQuestion } from '@/types';
import type { ObjectiveQuestion } from '@/types';

const route = useRoute();
const router = useRouter();
const toast = useToast();

const examId = computed(() => {
  const raw = route.params.id;
  return raw ? Number(raw) : null;
});
const isEdit = computed(() => examId.value !== null);

const submitting = ref(false);
const loading = ref(false);
const pageError = ref('');

const form = reactive({
  title: '',
  description: '',
  durationMinutes: 120,
  mode: 'OI' as 'ACM' | 'OI',
  status: 1,
  startTime: '',
  endTime: '',
});

/** 已选组卷条目：引用 + 分值快照 + 展示标题。 */
interface SelectedQuestion {
  questionId: number;
  questionType: 'ALGO' | 'DB' | 'OBJECTIVE';
  score: number;
  title: string;
}

const selected = ref<SelectedQuestion[]>([]);
const formErrors = reactive({ title: '', questions: '' });

// ---- 题库选取 ----
type PickTab = 'ALGO' | 'DB' | 'OBJECTIVE';
const tab = ref<PickTab>('ALGO');
const tabKeyword = ref('');
const algoList = ref<Question[]>([]);
const dbList = ref<DbQuestion[]>([]);
const objList = ref<ObjectiveQuestion[]>([]);
const listLoading = ref(false);

const typeTabs: Array<{ value: PickTab; label: string }> = [
  { value: 'ALGO', label: '编程题' },
  { value: 'DB', label: '数据库题' },
  { value: 'OBJECTIVE', label: '客观题' },
];

async function loadSource() {
  listLoading.value = true;
  try {
    const keyword = tabKeyword.value.trim() || undefined;
    if (tab.value === 'ALGO') {
      algoList.value = (await questionApi.list({ keyword, page: 1, size: 50 })).list;
    } else if (tab.value === 'DB') {
      dbList.value = (await databaseApi.list({ keyword, page: 1, size: 50 })).list;
    } else {
      objList.value = (await objectiveApi.list({ keyword, page: 1, size: 50 })).list;
    }
  } finally {
    listLoading.value = false;
  }
}

let tabKeywordTimer: ReturnType<typeof setTimeout> | null = null;
watch(tabKeyword, () => {
  if (tabKeywordTimer !== null) clearTimeout(tabKeywordTimer);
  tabKeywordTimer = setTimeout(loadSource, 300);
});
watch(tab, loadSource);

function sourceRows(): Array<{ id: number; label: string }> {
  if (tab.value === 'ALGO') return algoList.value.map((q) => ({ id: q.id, label: q.title }));
  if (tab.value === 'DB') return dbList.value.map((q) => ({ id: q.id, label: q.title }));
  return objList.value.map((q) => ({
    id: q.id,
    label: q.content.length > 40 ? `${q.content.slice(0, 40)}…` : q.content,
  }));
}

function isSelected(questionId: number): boolean {
  return selected.value.some((s) => s.questionId === questionId && s.questionType === tab.value);
}

function addQuestion(row: { id: number; label: string }) {
  if (isSelected(row.id)) return;
  selected.value.push({ questionId: row.id, questionType: tab.value, score: 5, title: row.label });
}

function removeQuestion(index: number) {
  selected.value.splice(index, 1);
}

function moveQuestion(index: number, dir: -1 | 1) {
  const target = index + dir;
  if (target < 0 || target >= selected.value.length) return;
  const [item] = selected.value.splice(index, 1);
  selected.value.splice(target, 0, item);
}

/** 在 datetime-local 字符串上加 N 分钟，返回 yyyy-MM-ddTHH:mm 格式。 */
function addMinutes(dtLocal: string, minutes: number): string {
  const d = new Date(dtLocal);
  d.setMinutes(d.getMinutes() + minutes);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

// ---- 加载与保存 ----
async function loadExam() {
  if (!examId.value) return;
  loading.value = true;
  pageError.value = '';
  try {
    const exam = await examApi.detail(examId.value);
    form.title = exam.title;
    form.description = exam.description ?? '';
    form.durationMinutes = exam.durationMinutes;
    form.mode = exam.mode;
    form.status = exam.status;
    form.startTime = exam.startTime ? exam.startTime.slice(0, 16) : '';
    form.endTime = exam.endTime ? exam.endTime.slice(0, 16) : '';
    selected.value = exam.questions.map((q) => ({
      questionId: q.questionId,
      questionType: q.questionType,
      score: q.score,
      title: q.title,
    }));
  } catch (e) {
    pageError.value = e instanceof ApiError ? e.message : '加载失败';
  } finally {
    loading.value = false;
  }
}

async function save() {
  formErrors.title = form.title.trim() ? '' : '请填写试卷标题';
  formErrors.questions = selected.value.length > 0 ? '' : '请至少选择一道题目';
  if (formErrors.title || formErrors.questions) return;
  if (selected.value.some((s) => !s.score || s.score <= 0)) {
    formErrors.questions = '每题分值需大于 0';
    return;
  }
  submitting.value = true;
  pageError.value = '';
  try {
    const body = {
      title: form.title.trim(),
      description: form.description.trim() || null,
      durationMinutes: form.durationMinutes,
      mode: form.mode,
      status: form.status,
      startTime: form.startTime || null,
      questions: selected.value.map((s) => ({
        questionId: s.questionId,
        questionType: s.questionType,
        score: s.score,
      })),
    };
    if (isEdit.value && examId.value != null) {
      await examApi.update(examId.value, body);
      toast.success('试卷已更新');
    } else {
      await examApi.create(body);
      toast.success('试卷已创建');
    }
    router.push('/admin/exams');
  } catch (e) {
    pageError.value = e instanceof ApiError ? e.message : '保存失败';
  } finally {
    submitting.value = false;
  }
}

onMounted(async () => {
  if (isEdit.value) await loadExam();
  await loadSource();
});
</script>

<template>
  <div class="exam-edit container">
    <h1 class="page-title">{{ isEdit ? '编辑试卷' : '新建试卷' }}</h1>
    <AdminNav />

    <p v-if="loading" class="muted">加载中…</p>
    <p v-else-if="pageError" class="submit-error">{{ pageError }}</p>

    <template v-else>
      <div class="form card">
        <FormField label="试卷标题" :error="formErrors.title">
          <input
            v-model="form.title"
            class="input"
            type="text"
            placeholder="如：Java 基础综合测试"
          />
        </FormField>
        <FormField label="试卷说明（Markdown）">
          <textarea
            v-model="form.description"
            class="input"
            rows="3"
            placeholder="可选，展示在考试信息处"
          ></textarea>
        </FormField>

        <div class="field-row">
          <FormField label="时长（分钟）">
            <input v-model.number="form.durationMinutes" class="input num" type="number" min="1" />
          </FormField>
          <FormField label="赛制">
            <select v-model="form.mode" class="input">
              <option value="OI">OI（交卷后统一评测）</option>
              <option value="ACM">ACM（考试中可单题提交看结果）</option>
            </select>
          </FormField>
          <FormField label="可见性">
            <select v-model.number="form.status" class="input">
              <option :value="1">公开</option>
              <option :value="0">私有</option>
            </select>
          </FormField>
        </div>

        <div class="field-row">
          <FormField label="考试开始时间（留空则立即开考）">
            <input v-model="form.startTime" class="input" type="datetime-local" />
          </FormField>
          <FormField label="结束时间">
            <input
              class="input"
              type="text"
              :value="form.startTime ? addMinutes(form.startTime, form.durationMinutes) : '开始时间 + 时长'"
              disabled
            />
          </FormField>
        </div>
      </div>

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
          <input v-model="tabKeyword" class="input search" type="text" placeholder="搜索该题库…" />
        </div>

        <p v-if="listLoading" class="muted">加载题库中…</p>
        <ul v-else class="source-list">
          <li v-for="row in sourceRows()" :key="`${tab}-${row.id}`" class="source-row">
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
          <li v-if="sourceRows().length === 0" class="muted empty">该题库暂无公开题目</li>
        </ul>
      </div>

      <div class="chosen card">
        <h2 class="section-title">
          已选题目（{{ selected.length }}）
          <span v-if="formErrors.questions" class="error-text">{{ formErrors.questions }}</span>
        </h2>
        <ul class="chosen-list">
          <li v-for="(s, i) in selected" :key="i" class="chosen-row">
            <span class="chosen-order">{{ i + 1 }}</span>
            <span class="chosen-label">{{ s.title }}</span>
            <span class="chosen-score">
              <input v-model.number="s.score" class="input score-input" type="number" min="1" /> 分
            </span>
            <button
              class="btn btn-ghost btn-sm"
              type="button"
              :disabled="i === 0"
              @click="moveQuestion(i, -1)"
            >
              ↑
            </button>
            <button
              class="btn btn-ghost btn-sm"
              type="button"
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
          <li v-if="selected.length === 0" class="muted empty">尚未选择题目</li>
        </ul>
      </div>

      <div class="actions">
        <button class="btn btn-ghost" type="button" @click="router.push('/admin/exams')">
          取消
        </button>
        <button class="btn btn-primary" type="button" :disabled="submitting" @click="save">
          {{ submitting ? '保存中…' : isEdit ? '保存修改' : '创建试卷' }}
        </button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.exam-edit {
  padding-top: 32px;
}
.page-title {
  margin: 28px 0 8px;
  font-size: 22px;
  font-weight: 600;
}
.form,
.picker,
.chosen {
  padding: 16px;
  margin-bottom: 16px;
}
.field-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
}
.num {
  width: 100%;
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
}
.tab.active {
  border-color: var(--ink);
  background: var(--ink);
  color: #fff;
}
.search {
  width: 220px;
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
}
.source-label,
.chosen-label {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
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
  padding: 10px 0;
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
}
.btn-danger {
  color: var(--danger);
}
</style>
