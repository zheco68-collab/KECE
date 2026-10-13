<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { examApi, type ExamSubmissionRow, type RankRow } from '@/api/exam';
import { ApiError } from '@/api/request';
import DataTable from '@/components/DataTable.vue';
import { STATUS_ABBR, statusTone } from '@/utils/submission';
import type { SubmissionStatus } from '@/types';

const route = useRoute();
const examId = Number(route.params.id);

const results = ref<ExamSubmissionRow[]>([]);
const ranking = ref<RankRow[]>([]);
const loading = ref(true);
const error = ref('');

const resultColumns = [
  { key: 'title', label: '题目' },
  { key: 'questionType', label: '题型', width: '90px' },
  { key: 'status', label: '评测', width: '90px' },
  { key: 'score', label: '得分', width: '120px' },
];

const rankColumns = [
  { key: 'rank', label: '名次', width: '60px' },
  { key: 'username', label: '考生', width: '120px' },
  { key: 'solved', label: '通过', width: '70px' },
  { key: 'penaltyMinutes', label: '罚时(分)', width: '90px' },
  { key: 'totalScore', label: '总分', width: '80px' },
];

const rankRows = ref<Array<RankRow & { rank: number }>>([]);

const typeLabels: Record<string, string> = {
  ALGO: '编程',
  DB: '数据库',
  OBJECTIVE: '客观题',
};

onMounted(async () => {
  try {
    const [r, rk] = await Promise.all([examApi.results(examId), examApi.ranking(examId)]);
    results.value = r;
    ranking.value = rk;
    rankRows.value = rk.map((row, index) => ({ ...row, rank: index + 1 }));
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : '加载失败';
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <div class="exam-result container">
    <h1 class="page-title">考试结果</h1>

    <p v-if="loading" class="muted">加载中…</p>
    <p v-else-if="error" class="submit-error">{{ error }}</p>

    <template v-else>
      <section class="card section">
        <h2 class="section-title">我的作答</h2>
        <DataTable :columns="resultColumns" :rows="results" row-key="examQuestionId">
          <template #cell-questionType="{ row }">
            <span>{{ typeLabels[row.questionType] ?? row.questionType }}</span>
          </template>
          <template #cell-status="{ row }">
            <span
              v-if="row.status"
              class="status-badge"
              :class="statusTone(row.status as SubmissionStatus)"
            >
              {{ STATUS_ABBR[row.status as SubmissionStatus] ?? row.status }}
            </span>
            <span v-else class="muted">待公布</span>
          </template>
          <template #cell-score="{ row }">
            <span class="mono">{{ row.score ?? '-' }} / {{ row.fullScore }}</span>
          </template>
        </DataTable>
      </section>

      <section class="card section">
        <h2 class="section-title">排行榜</h2>
        <DataTable :columns="rankColumns" :rows="rankRows" row-key="userId">
          <template #cell-rank="{ row }">
            <span class="mono rank-no">{{ row.rank }}</span>
          </template>
          <template #cell-totalScore="{ row }">
            <span class="mono">{{ row.totalScore }} / {{ row.fullScore }}</span>
          </template>
        </DataTable>
      </section>
    </template>
  </div>
</template>

<style scoped>
.exam-result {
  padding-top: 32px;
}
.page-title {
  margin: 28px 0 16px;
  font-size: 22px;
  font-weight: 600;
}
.section {
  padding: 16px;
  margin-bottom: 16px;
}
.section-title {
  margin: 0 0 12px;
  font-size: 16px;
  font-weight: 600;
}
.status-badge {
  display: inline-block;
  min-width: 44px;
  padding: 2px 10px;
  border-radius: var(--pill);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  text-align: center;
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
.rank-no {
  font-weight: 700;
}
</style>
