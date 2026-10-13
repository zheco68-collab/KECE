<script setup lang="ts">
import { onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { examApi, type Exam } from '@/api/exam';
import { ApiError } from '@/api/request';
import AdminNav from '@/components/AdminNav.vue';
import DataTable from '@/components/DataTable.vue';
import Pagination from '@/components/Pagination.vue';
import { useConfirm } from '@/composables/useConfirm';
import { useToast } from '@/composables/useToast';

const router = useRouter();
const { confirm } = useConfirm();
const toast = useToast();

const keyword = ref('');
const statusFilter = ref('');
const modeFilter = ref('');
const page = ref(1);
const size = 20;

const rows = ref<Exam[]>([]);
const total = ref(0);
const loading = ref(false);
const loadError = ref('');

const columns = [
  { key: 'id', label: 'ID', width: '64px' },
  { key: 'title', label: '试卷' },
  { key: 'mode', label: '赛制', width: '80px' },
  { key: 'status', label: '状态', width: '80px' },
  { key: 'durationMinutes', label: '时长(分)', width: '90px' },
  { key: 'startTime', label: '开考窗口', width: '160px' },
  { key: 'endTime', label: '最晚开考', width: '160px' },
];

async function load() {
  loading.value = true;
  loadError.value = '';
  try {
    const res = await examApi.listAdmin({
      keyword: keyword.value.trim() || undefined,
      status: statusFilter.value === '' ? undefined : Number(statusFilter.value),
      page: page.value,
      size,
    });
    rows.value = res.list;
    total.value = res.total;
  } catch (e) {
    loadError.value = e instanceof ApiError ? e.message : '请求失败，请稍后重试';
  } finally {
    loading.value = false;
  }
}

function resetAndLoad() {
  if (page.value === 1) {
    load();
  } else {
    page.value = 1;
  }
}

let keywordTimer: ReturnType<typeof setTimeout> | null = null;
watch(keyword, () => {
  if (keywordTimer !== null) clearTimeout(keywordTimer);
  keywordTimer = setTimeout(resetAndLoad, 300);
});
watch([statusFilter, modeFilter], resetAndLoad);
watch(page, () => {
  load();
});

function formatTime(iso: string | null): string {
  return iso ? iso.slice(0, 16).replace('T', ' ') : '—';
}

async function removeExam(row: Exam) {
  const ok = await confirm({
    title: '删除试卷',
    message: `确定删除考试「${row.title}」吗？有作答记录的考试仅管理员可强制删除，其作答与评测数据将被级联清理，出题人无法删除。`,
    confirmText: '删除',
  });
  if (!ok) return;
  try {
    await examApi.remove(row.id);
    toast.success(`已删除「${row.title}」`);
    load();
  } catch (e) {
    toast.error(e instanceof ApiError ? e.message : '删除失败');
  }
}

onMounted(load);
</script>

<template>
  <div class="admin-exams container">
    <h1 class="page-title">考试管理</h1>
    <AdminNav />

    <div class="toolbar">
      <input v-model="keyword" class="input search" type="text" placeholder="搜索试卷标题…" />
      <select v-model="statusFilter" class="input">
        <option value="">全部状态</option>
        <option value="1">公开</option>
        <option value="0">私有</option>
      </select>
      <button class="btn btn-primary" type="button" @click="router.push('/admin/exams/new')">
        新建试卷
      </button>
    </div>

    <p v-if="loadError" class="submit-error">{{ loadError }}</p>

    <DataTable
      :columns="columns"
      :rows="rows"
      :loading="loading"
      row-key="id"
      empty-text="暂无试卷"
    >
      <template #cell-mode="{ row }">
        <span class="badge" :class="row.mode === 'ACM' ? 'mode-acm' : 'mode-oi'">
          {{ row.mode === 'ACM' ? 'ACM' : 'OI' }}
        </span>
      </template>
      <template #cell-status="{ row }">
        <span class="badge" :class="row.status === 1 ? 'status-public' : 'status-private'">
          {{ row.status === 1 ? '公开' : '私有' }}
        </span>
      </template>
      <template #cell-startTime="{ row }">
        <span class="time">{{ formatTime(row.startTime) }}</span>
      </template>
      <template #cell-endTime="{ row }">
        <span class="time">{{ formatTime(row.endTime) }}</span>
      </template>
      <template #actions="{ row }">
        <button
          class="btn btn-ghost btn-sm"
          type="button"
          @click="router.push(`/admin/exams/${row.id}/edit`)"
        >
          编辑
        </button>
        <button class="btn btn-ghost btn-sm btn-danger" type="button" @click="removeExam(row)">
          删除
        </button>
      </template>
    </DataTable>

    <Pagination v-model:page="page" :size="size" :total="total" />
  </div>
</template>

<style scoped>
.admin-exams {
  padding-top: 32px;
}
.page-title {
  margin: 28px 0 8px;
  font-size: 22px;
  font-weight: 600;
}
.toolbar {
  display: flex;
  gap: 10px;
  margin: 16px 0;
  flex-wrap: wrap;
}
.search {
  width: 240px;
}
.time {
  color: var(--ink-tertiary);
  font-size: 13px;
}
.mode-acm {
  background: var(--accent-bg, #eef2ff);
  color: #6366f1;
}
.mode-oi {
  background: var(--success-bg, #ecfdf5);
  color: var(--success);
}
.status-public {
  background: var(--success-bg, #ecfdf5);
  color: var(--success);
}
.status-private {
  background: var(--bg);
  color: var(--ink-tertiary);
}
.btn-danger {
  color: var(--danger);
}
</style>
