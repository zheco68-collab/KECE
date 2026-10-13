<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue';
import {
  adminApi,
  type CodeSubmissionRow,
  type DbSubmissionRow,
  type ObjectiveSubmissionRow,
} from '@/api/admin';
import { ApiError } from '@/api/request';
import AdminNav from '@/components/AdminNav.vue';
import DataTable from '@/components/DataTable.vue';
import Pagination from '@/components/Pagination.vue';
import { useConfirm } from '@/composables/useConfirm';
import { useToast } from '@/composables/useToast';
import type { SubmissionStatus } from '@/types';
import { LANGUAGE_LABELS, STATUS_ABBR, statusTone } from '@/utils/submission';

type TabKey = 'code' | 'database' | 'objective';
type CorrectFilter = '' | '1' | '0';

const activeTab = ref<TabKey>('code');
const size = 20;

// ---- 编程题提交 ----
const codeUserId = ref('');
const codeQuestionId = ref('');
const codeStatus = ref<SubmissionStatus | ''>('');
const codePage = ref(1);
const codeRows = ref<CodeSubmissionRow[]>([]);
const codeTotal = ref(0);
const codeLoading = ref(false);
const codeError = ref('');

// ---- 数据库题提交 ----
const dbUserId = ref('');
const dbQuestionId = ref('');
const dbStatus = ref<SubmissionStatus | ''>('');
const dbPage = ref(1);
const dbRows = ref<DbSubmissionRow[]>([]);
const dbTotal = ref(0);
const dbLoading = ref(false);
const dbError = ref('');

// ---- 客观题作答 ----
const objUserId = ref('');
const objQuestionId = ref('');
const objCorrect = ref<CorrectFilter>('');
const objPage = ref(1);
const objRows = ref<ObjectiveSubmissionRow[]>([]);
const objTotal = ref(0);
const objLoading = ref(false);
const objError = ref('');

const statusOptions: Array<{ value: SubmissionStatus | ''; label: string }> = [
  { value: '', label: '全部状态' },
  { value: 'ACCEPTED', label: 'AC' },
  { value: 'WRONG_ANSWER', label: 'WA' },
  { value: 'TIME_LIMIT_EXCEEDED', label: 'TLE' },
  { value: 'MEMORY_LIMIT_EXCEEDED', label: 'MLE' },
  { value: 'RUNTIME_ERROR', label: 'RE' },
  { value: 'COMPILE_ERROR', label: 'CE' },
  { value: 'SYSTEM_ERROR', label: 'SE' },
];

const correctOptions: Array<{ value: CorrectFilter; label: string }> = [
  { value: '', label: '全部对错' },
  { value: '1', label: '答对' },
  { value: '0', label: '答错' },
];

const codeColumns = [
  { key: 'submissionId', label: 'ID', width: '70px' },
  { key: 'username', label: '用户', width: '110px' },
  { key: 'questionId', label: '题号', width: '70px' },
  { key: 'questionTitle', label: '题目' },
  { key: 'language', label: '语言', width: '80px' },
  { key: 'status', label: '状态', width: '80px' },
  { key: 'score', label: '得分', width: '70px' },
  { key: 'timeUsedMs', label: '耗时', width: '90px' },
  { key: 'memoryUsedKb', label: '内存', width: '100px' },
  { key: 'createdAt', label: '提交时间', width: '170px' },
];

const dbColumns = [
  { key: 'submissionId', label: 'ID', width: '70px' },
  { key: 'username', label: '用户', width: '110px' },
  { key: 'questionId', label: '题号', width: '70px' },
  { key: 'questionTitle', label: '题目' },
  { key: 'status', label: '状态', width: '80px' },
  { key: 'score', label: '得分', width: '70px' },
  { key: 'timeUsedMs', label: '耗时', width: '90px' },
  { key: 'createdAt', label: '提交时间', width: '170px' },
];

const objColumns = [
  { key: 'submissionId', label: 'ID', width: '70px' },
  { key: 'username', label: '用户', width: '110px' },
  { key: 'questionId', label: '题号', width: '70px' },
  { key: 'questionTitle', label: '题干' },
  { key: 'type', label: '题型', width: '80px' },
  { key: 'correct', label: '对错', width: '80px' },
  { key: 'score', label: '得分', width: '90px' },
  { key: 'createdAt', label: '提交时间', width: '170px' },
];

const currentColumns = () =>
  activeTab.value === 'code'
    ? codeColumns
    : activeTab.value === 'database'
      ? dbColumns
      : objColumns;
const currentRows = () =>
  activeTab.value === 'code'
    ? codeRows.value
    : activeTab.value === 'database'
      ? dbRows.value
      : objRows.value;
const currentTotal = () =>
  activeTab.value === 'code'
    ? codeTotal.value
    : activeTab.value === 'database'
      ? dbTotal.value
      : objTotal.value;
const currentLoading = () =>
  activeTab.value === 'code'
    ? codeLoading.value
    : activeTab.value === 'database'
      ? dbLoading.value
      : objLoading.value;
const currentError = () =>
  activeTab.value === 'code'
    ? codeError.value
    : activeTab.value === 'database'
      ? dbError.value
      : objError.value;

/** 当前标签页的用户 ID 输入（v-model 需要可赋值目标，用 get/set 分发到各 tab 状态）。 */
const activeUserId = ref('');
const activeQuestionId = ref('');
const activeStatus = ref<SubmissionStatus | ''>('');
const activePage = ref(1);

// 切换标签时，把当前 tab 的筛选值同步到「活动」ref，让模板用统一 v-model。
watch(activeTab, () => {
  if (activeTab.value === 'code') {
    activeUserId.value = codeUserId.value;
    activeQuestionId.value = codeQuestionId.value;
    activeStatus.value = codeStatus.value;
    activePage.value = codePage.value;
  } else if (activeTab.value === 'database') {
    activeUserId.value = dbUserId.value;
    activeQuestionId.value = dbQuestionId.value;
    activeStatus.value = dbStatus.value;
    activePage.value = dbPage.value;
  } else {
    activeUserId.value = objUserId.value;
    activeQuestionId.value = objQuestionId.value;
    activePage.value = objPage.value;
  }
});

watch(activeUserId, (v) => {
  if (activeTab.value === 'code') codeUserId.value = v;
  else if (activeTab.value === 'database') dbUserId.value = v;
  else objUserId.value = v;
});
watch(activeQuestionId, (v) => {
  if (activeTab.value === 'code') codeQuestionId.value = v;
  else if (activeTab.value === 'database') dbQuestionId.value = v;
  else objQuestionId.value = v;
});
watch(activeStatus, (v) => {
  if (activeTab.value === 'code') codeStatus.value = v;
  else if (activeTab.value === 'database') dbStatus.value = v;
});
watch(activePage, (v) => {
  if (activeTab.value === 'code') codePage.value = v;
  else if (activeTab.value === 'database') dbPage.value = v;
  else objPage.value = v;
});

/** 数字筛选输入转可选 number：空串/非数字一律 undefined。 */
function toOptionalNumber(raw: string): number | undefined {
  const trimmed = raw.trim();
  if (trimmed === '') return undefined;
  const n = Number(trimmed);
  return Number.isFinite(n) ? n : undefined;
}

async function load() {
  const key = activeTab.value;
  const setLoading = (v: boolean) => {
    if (key === 'code') codeLoading.value = v;
    else if (key === 'database') dbLoading.value = v;
    else objLoading.value = v;
  };
  const setError = (msg: string) => {
    if (key === 'code') codeError.value = msg;
    else if (key === 'database') dbError.value = msg;
    else objError.value = msg;
  };
  setLoading(true);
  setError('');
  try {
    const page = activePage.value;
    if (key === 'code') {
      const res = await adminApi.listCodeSubmissions({
        userId: toOptionalNumber(codeUserId.value),
        questionId: toOptionalNumber(codeQuestionId.value),
        status: codeStatus.value || undefined,
        page,
        size,
      });
      codeRows.value = res.list;
      codeTotal.value = res.total;
    } else if (key === 'database') {
      const res = await adminApi.listDbSubmissions({
        userId: toOptionalNumber(dbUserId.value),
        questionId: toOptionalNumber(dbQuestionId.value),
        status: dbStatus.value || undefined,
        page,
        size,
      });
      dbRows.value = res.list;
      dbTotal.value = res.total;
    } else {
      const res = await adminApi.listObjectiveSubmissions({
        userId: toOptionalNumber(objUserId.value),
        questionId: toOptionalNumber(objQuestionId.value),
        correct: objCorrect.value === '' ? undefined : Number(objCorrect.value),
        page,
        size,
      });
      objRows.value = res.list;
      objTotal.value = res.total;
    }
  } catch (e) {
    setError(e instanceof ApiError ? e.message : '请求失败，请稍后重试');
  } finally {
    setLoading(false);
  }
}

/** 筛选变化时回到第一页再加载；已在第一页则手动触发。 */
function resetAndLoad() {
  if (activePage.value === 1) {
    load();
  } else {
    activePage.value = 1;
  }
}

function switchTab(key: TabKey) {
  if (activeTab.value === key) return;
  activeTab.value = key;
  load();
}

// 用户/题号输入防抖 300ms；下拉筛选变化直接触发。
let filterTimer: ReturnType<typeof setTimeout> | null = null;

watch([activeUserId, activeQuestionId], () => {
  if (filterTimer !== null) clearTimeout(filterTimer);
  filterTimer = setTimeout(resetAndLoad, 300);
});

watch([activeStatus, objCorrect], resetAndLoad);

watch(activePage, () => {
  load();
});

/** 创建时间：ISO 串截取前 19 位并把 T 换成空格，得到 "YYYY-MM-DD HH:mm:ss"。 */
function formatTime(iso: string): string {
  return iso ? iso.slice(0, 19).replace('T', ' ') : '—';
}

const OBJECTIVE_TYPE_LABELS: Record<string, string> = {
  SINGLE: '单选',
  MULTIPLE: '多选',
  FILL: '填空',
};

function formatMemory(kb: number | null): string {
  if (kb == null) return '-';
  if (kb < 1024) return `${kb}KB`;
  const mb = kb / 1024;
  if (mb >= 1000) return `${Math.round(mb)}MB`;
  return `${mb.toFixed(1).replace(/\.0$/, '')}MB`;
}

const { confirm } = useConfirm();
const { success, error } = useToast();

const REJUDGE_HINT =
  '重判将使用题目当前的数据（用例/时限/答案），结果可能与原结果不同；已通过的标记会暂时消失。确认重判？';

/** 单条重判：按当前 tab 调用对应接口，成功刷新列表。 */
async function rejudgeRow(row: CodeSubmissionRow | DbSubmissionRow | ObjectiveSubmissionRow) {
  const ok = await confirm({ title: '重判该提交', message: REJUDGE_HINT, confirmText: '重判' });
  if (!ok) return;
  try {
    if (activeTab.value === 'code') {
      await adminApi.rejudgeSubmission(row.submissionId);
    } else if (activeTab.value === 'database') {
      await adminApi.rejudgeDbSubmission(row.submissionId);
    } else {
      await adminApi.rejudgeObjectiveSubmission(row.submissionId);
    }
    success('重判完成，列表已刷新');
    load();
  } catch (e) {
    error(e instanceof ApiError ? e.message : '重判失败，请稍后重试');
  }
}

/** 客观题按题批量重判：使用当前题目 ID 筛选框的值。 */
async function rejudgeQuestionBatch() {
  const qid = toOptionalNumber(objQuestionId.value);
  if (qid == null) return;
  const ok = await confirm({
    title: '按题批量重判',
    message: `将用该题当前答案与分值重判全部作答记录（题目 ${qid}），确认？`,
    confirmText: '重判',
  });
  if (!ok) return;
  try {
    const res = await adminApi.rejudgeObjectiveQuestion(qid);
    success(`已重判 ${res.rejudged ?? 0} 条作答记录`);
    load();
  } catch (e) {
    error(e instanceof ApiError ? e.message : '批量重判失败，请稍后重试');
  }
}

/** 客观题 tab：题目 ID 已填时允许按题批量重判。 */
const canBatchRejudge = () =>
  activeTab.value === 'objective' && toOptionalNumber(objQuestionId.value) != null;

onMounted(() => {
  load();
});

onBeforeUnmount(() => {
  if (filterTimer !== null) clearTimeout(filterTimer);
});
</script>

<template>
  <div class="admin container">
    <h1 class="page-title">提交监控</h1>
    <AdminNav />

    <div class="tabs">
      <button class="tab" :class="{ active: activeTab === 'code' }" @click="switchTab('code')">
        编程题
      </button>
      <button
        class="tab"
        :class="{ active: activeTab === 'database' }"
        @click="switchTab('database')"
      >
        数据库题
      </button>
      <button
        class="tab"
        :class="{ active: activeTab === 'objective' }"
        @click="switchTab('objective')"
      >
        客观题
      </button>
    </div>

    <div class="filter-bar card">
      <label class="filter">
        <span class="filter-label">用户 ID</span>
        <input
          v-model="activeUserId"
          class="input id-input"
          type="search"
          placeholder="提交者用户 ID"
        />
      </label>
      <label class="filter">
        <span class="filter-label">题目 ID</span>
        <input
          v-model="activeQuestionId"
          class="input id-input"
          type="search"
          placeholder="题目 ID"
        />
      </label>
      <label v-if="activeTab !== 'objective'" class="filter">
        <span class="filter-label">状态</span>
        <select v-model="activeStatus" class="input">
          <option v-for="s in statusOptions" :key="s.value || 'all'" :value="s.value">
            {{ s.label }}
          </option>
        </select>
      </label>
      <label v-else class="filter">
        <span class="filter-label">对错</span>
        <select v-model="objCorrect" class="input">
          <option v-for="c in correctOptions" :key="c.value || 'all'" :value="c.value">
            {{ c.label }}
          </option>
        </select>
      </label>
      <button
        v-if="canBatchRejudge()"
        class="btn btn-ghost btn-sm batch-btn"
        type="button"
        @click="rejudgeQuestionBatch"
      >
        按题批量重判
      </button>
    </div>

    <p v-if="currentError()" class="submit-error">{{ currentError() }}</p>

    <DataTable
      :key="activeTab"
      :columns="currentColumns()"
      :rows="currentRows()"
      :loading="currentLoading()"
      row-key="submissionId"
    >
      <template #cell-status="{ row }">
        <span class="status-badge" :class="statusTone(row.status as SubmissionStatus)">
          {{ STATUS_ABBR[row.status as SubmissionStatus] ?? row.status }}
        </span>
      </template>
      <template #cell-score="{ row }">
        <span class="mono">{{ row.score ?? '-' }}</span>
      </template>
      <template #cell-timeUsedMs="{ row }">
        <span class="mono">{{ row.timeUsedMs != null ? row.timeUsedMs + 'ms' : '-' }}</span>
      </template>
      <template #cell-memoryUsedKb="{ row }">
        <span class="mono">{{ formatMemory(row.memoryUsedKb) }}</span>
      </template>
      <template #cell-language="{ row }">
        <span>{{ LANGUAGE_LABELS[row.language] ?? row.language }}</span>
      </template>
      <template #cell-correct="{ row }">
        <span class="status-badge" :class="row.correct === 1 ? 'ac' : 'wa'">
          {{ row.correct === 1 ? 'AC' : 'WA' }}
        </span>
      </template>
      <template #cell-type="{ row }">
        <span>{{ OBJECTIVE_TYPE_LABELS[row.type] ?? row.type }}</span>
      </template>
      <template #cell-questionTitle="{ row }">
        <span class="q-title">{{ row.questionTitle || '—' }}</span>
      </template>
      <template #cell-createdAt="{ row }">
        <span class="time">{{ formatTime(row.createdAt) }}</span>
      </template>
      <template #cell-username="{ row }">
        <span class="muted">{{ row.username }}</span>
      </template>
      <template #actions="{ row }">
        <button class="btn btn-ghost btn-sm" type="button" @click="rejudgeRow(row)">重判</button>
      </template>
    </DataTable>

    <Pagination
      :key="'p-' + activeTab"
      v-model:page="activePage"
      :size="size"
      :total="currentTotal()"
    />
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
.tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}
.tab {
  padding: 7px 20px;
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
.filter-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px 20px;
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
  white-space: nowrap;
}
.filter select {
  width: 130px;
}
.id-input {
  width: 150px;
}
.batch-btn {
  margin-left: auto;
}
.submit-error {
  margin: 0 0 16px;
}
.status-badge {
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
.status-badge.ac {
  background: var(--success);
}
.status-badge.wa {
  background: var(--danger);
}
.status-badge.tle {
  background: #000;
}
.status-badge.mle {
  background: #f57c00;
}
.status-badge.re {
  background: #9c27b0;
}
.status-badge.ce,
.status-badge.se {
  background: #607d8b;
}
.status-badge.run {
  background: #9e9e9e;
}
.q-title {
  max-width: 260px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: inline-block;
  vertical-align: bottom;
}
.time {
  color: var(--ink-tertiary);
  font-size: 13px;
}
</style>
