<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { examApi, type ExamSessionInfo } from '@/api/exam';
import { ApiError } from '@/api/request';
import DataTable from '@/components/DataTable.vue';
import Pagination from '@/components/Pagination.vue';

const router = useRouter();
const rows = ref<ExamSessionInfo[]>([]);
const total = ref(0);
const page = ref(1);
const size = 20;
const loading = ref(false);
const error = ref('');

const columns = [
  { key: 'id', label: 'ID', width: '70px' },
  { key: 'examTitle', label: '考试' },
  { key: 'status', label: '状态', width: '110px' },
  { key: 'startTime', label: '开考时间', width: '170px' },
  { key: 'endTime', label: '结束时间', width: '170px' },
  { key: 'durationMinutes', label: '时长(分)', width: '90px' },
];

async function load() {
  loading.value = true;
  error.value = '';
  try {
    const res = await examApi.listOpen({ page: page.value, size });
    rows.value = res.list;
    total.value = res.total;
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : '请求失败，请稍后重试';
  } finally {
    loading.value = false;
  }
}

function formatTime(iso: string): string {
  return iso ? iso.slice(0, 16).replace('T', ' ') : '—';
}

function goExam(row: ExamSessionInfo) {
  router.push(`/exams/${row.id}`);
}

onMounted(() => {
  load();
});
</script>

<template>
  <div class="exam-list container">
    <h1 class="page-title">考试</h1>
    <p class="muted">选择一场进行中的考试进入（时间到自动交卷）</p>

    <p v-if="error" class="submit-error">{{ error }}</p>

    <DataTable
      :columns="columns"
      :rows="rows"
      :loading="loading"
      row-key="id"
      empty-text="暂无进行中的考试"
    >
      <template #cell-status="{ row }">
        <span class="badge" :class="row.status === 'ONGOING' ? 'status-enabled' : 'status-pending'">
          {{ row.status === 'ONGOING' ? '进行中' : row.status === 'UPCOMING' ? '未开始' : '已结束' }}
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
          v-if="row.status === 'ONGOING' || row.status === 'UPCOMING'"
          class="btn btn-primary btn-sm"
          type="button"
          @click="goExam(row)"
        >
          进入考试
        </button>
      </template>
    </DataTable>

    <Pagination v-model:page="page" :size="size" :total="total" />
  </div>
</template>

<style scoped>
.exam-list {
  padding-top: 32px;
}
.page-title {
  margin: 28px 0 8px;
  font-size: 22px;
  font-weight: 600;
}
.status-enabled {
  background: var(--success-bg);
  color: var(--success);
}
.status-pending {
  background: var(--accent-bg, #eef2ff);
  color: #6366f1;
}
.time {
  color: var(--ink-tertiary);
  font-size: 13px;
}
</style>
