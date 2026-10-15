<script setup lang="ts">
import { computed, ref } from 'vue'
import {
  parseSchemaSql,
  parseSeedSql,
  parseExpectedOutput,
  type ParsedSeedTable,
  type ParsedResultSet,
} from '@/utils/sqlSchemaParser'
import { copyToClipboard } from '@/utils/clipboard'
import { useToast } from '@/composables/useToast'

interface SampleCaseItem {
  id?: number
  caseOrder?: number
  seedSql: string
  expectedOutput?: string | null
  isSample?: number
}

const props = defineProps<{
  sampleCases: SampleCaseItem[]
  schemaSql: string
}>()

const { success, error: toastError } = useToast()

const schemas = computed(() => parseSchemaSql(props.schemaSql))

interface ParsedCaseGroup {
  originalIndex: number
  seedSql: string
  tables: ParsedSeedTable[]
  expectedOutputRaw: string | null
  expectedResult: ParsedResultSet | null
  showRawSeed: boolean
  showRawOutput: boolean
}

const rawToggles = ref<Record<number, { seed: boolean; output: boolean }>>({})

const parsedCases = computed<ParsedCaseGroup[]>(() => {
  return props.sampleCases
    .filter((c) => c.isSample === undefined || c.isSample === 1)
    .map((c, idx) => {
      const caseIdx = idx + 1
      const toggles = rawToggles.value[caseIdx] || { seed: false, output: false }
      return {
        originalIndex: caseIdx,
        seedSql: c.seedSql,
        tables: parseSeedSql(c.seedSql, schemas.value),
        expectedOutputRaw: c.expectedOutput ?? null,
        expectedResult: parseExpectedOutput(c.expectedOutput),
        showRawSeed: toggles.seed,
        showRawOutput: toggles.output,
      }
    })
})

function toggleRawSeed(caseIdx: number) {
  if (!rawToggles.value[caseIdx]) {
    rawToggles.value[caseIdx] = { seed: false, output: false }
  }
  rawToggles.value[caseIdx].seed = !rawToggles.value[caseIdx].seed
}

function toggleRawOutput(caseIdx: number) {
  if (!rawToggles.value[caseIdx]) {
    rawToggles.value[caseIdx] = { seed: false, output: false }
  }
  rawToggles.value[caseIdx].output = !rawToggles.value[caseIdx].output
}

async function copyText(text: string | null | undefined, label: string) {
  if (!text) return
  const ok = await copyToClipboard(text)
  if (ok) {
    success(`${label}已复制到剪贴板`)
  } else {
    toastError('复制失败，请手动选取复制')
  }
}
</script>

<template>
  <div class="db-sample-viewer card">
    <div class="sample-header">
      <div class="header-left">
        <span class="sec-title">样例测试数据</span>
        <span class="sample-count-badge">共 {{ parsedCases.length }} 组公开样例</span>
      </div>
    </div>

    <!-- 样例列表 -->
    <div class="samples-body">
      <div
        v-for="grp in parsedCases"
        :key="grp.originalIndex"
        class="sample-case-box"
      >
        <div class="case-box-head">
          <span class="case-badge">样例 #{{ grp.originalIndex }}</span>
        </div>

        <div class="case-split-grid">
          <!-- 左栏：输入数据 -->
          <div class="case-part input-part">
            <div class="part-header">
              <span class="part-title">输入数据</span>
              <div class="part-actions">
                <button
                  type="button"
                  class="btn-tool-sm"
                  @click="toggleRawSeed(grp.originalIndex)"
                >
                  {{ grp.showRawSeed ? '查看表格' : '查看 SQL' }}
                </button>
                <button
                  type="button"
                  class="btn-tool-sm"
                  @click="copyText(grp.seedSql, `样例 #${grp.originalIndex} 初始 SQL`)"
                >
                  复制 SQL
                </button>
              </div>
            </div>

            <!-- 1. 表格视图 -->
            <div v-if="!grp.showRawSeed && grp.tables.length > 0" class="case-tables-list">
              <div
                v-for="(tbl, tIdx) in grp.tables"
                :key="tIdx"
                class="data-grid-card"
              >
                <div class="grid-card-head">
                  <span class="table-name-tag">
                    <code>{{ tbl.tableName }}</code> 表
                  </span>
                  <span class="row-count-tag mono">共 {{ tbl.rows.length }} 条记录</span>
                </div>

                <div class="grid-table-scroll">
                  <table class="db-data-table">
                    <thead>
                      <tr>
                        <th class="th-index">#</th>
                        <th
                          v-for="col in tbl.columns"
                          :key="col"
                          class="th-col mono"
                        >
                          {{ col }}
                        </th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="(row, rIdx) in tbl.rows" :key="rIdx">
                        <td class="td-index mono">{{ rIdx + 1 }}</td>
                        <td
                          v-for="(val, cIdx) in row"
                          :key="cIdx"
                          class="td-val mono"
                          :class="{ 'is-null': val === 'NULL' }"
                        >
                          <span v-if="val === 'NULL'" class="null-pill">NULL</span>
                          <span v-else>{{ val }}</span>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </div>

            <!-- 2. 原始 SQL 视图 -->
            <div v-else class="raw-sql-box">
              <pre class="sql-code">{{ grp.seedSql }}</pre>
            </div>
          </div>

          <!-- 右栏：期望结果集 -->
          <div class="case-part output-part">
            <div class="part-header output-header">
              <span class="part-title">期望输出结果</span>
              <div v-if="grp.expectedOutputRaw" class="part-actions">
                <button
                  type="button"
                  class="btn-tool-sm"
                  @click="toggleRawOutput(grp.originalIndex)"
                >
                  {{ grp.showRawOutput ? '查看表格' : '查看原始数据' }}
                </button>
                <button
                  type="button"
                  class="btn-tool-sm"
                  @click="copyText(grp.expectedOutputRaw, `样例 #${grp.originalIndex} 期望结果`)"
                >
                  复制结果
                </button>
              </div>
            </div>

            <!-- 表格视图 -->
            <div v-if="!grp.showRawOutput && grp.expectedResult" class="case-tables-list">
              <div class="data-grid-card output-grid-card">
                <div class="grid-card-head output-card-head">
                  <span class="table-name-tag">
                    <span>查询结果集</span>
                  </span>
                  <span class="row-count-tag mono">共 {{ grp.expectedResult.rows.length }} 行</span>
                </div>

                <div class="grid-table-scroll">
                  <table class="db-data-table output-data-table">
                    <thead>
                      <tr>
                        <th class="th-index">#</th>
                        <th
                          v-for="col in grp.expectedResult.columns"
                          :key="col.name"
                          class="th-col mono"
                        >
                          {{ col.name }}
                          <span v-if="col.type" class="col-type-tag">{{ col.type }}</span>
                        </th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-if="grp.expectedResult.rows.length === 0">
                        <td :colspan="grp.expectedResult.columns.length + 1" class="empty-rows-cell mono">
                          （结果集为空 - 0 行）
                        </td>
                      </tr>
                      <tr v-for="(row, rIdx) in grp.expectedResult.rows" :key="rIdx">
                        <td class="td-index mono">{{ rIdx + 1 }}</td>
                        <td
                          v-for="(val, cIdx) in row"
                          :key="cIdx"
                          class="td-val mono"
                          :class="{ 'is-null': val === 'NULL' }"
                        >
                          <span v-if="val === 'NULL'" class="null-pill">NULL</span>
                          <span v-else>{{ val }}</span>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </div>

            <!-- 原始文本视图 -->
            <div v-else-if="grp.expectedOutputRaw" class="raw-sql-box">
              <pre class="sql-code">{{ grp.expectedOutputRaw }}</pre>
            </div>

            <!-- 未配置期望输出提示 -->
            <div v-else class="empty-expected-box">
              <span class="empty-note">暂无预存期望结果集表格（判题将以出题人标准 SQL 动态运行结果为准）</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.db-sample-viewer {
  border: 1px solid var(--line);
  background: var(--surface);
  border-radius: 0;
  overflow: hidden;
}

.sample-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  background: #fbfbfb;
  border-bottom: 1px solid var(--line);
  gap: 12px;
  flex-wrap: wrap;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.sec-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--ink);
  letter-spacing: 0.2px;
}

.sample-count-badge {
  font-size: 11px;
  padding: 1px 6px;
  background: var(--bg);
  border: 1px solid var(--line);
  color: var(--ink-secondary);
}

.samples-body {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.sample-case-box {
  border: 1px solid var(--line);
  background: var(--surface);
  overflow: hidden;
}

.case-box-head {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  background: var(--bg);
  border-bottom: 1px solid var(--line);
}

.case-badge {
  font-size: 12px;
  font-weight: 700;
  color: var(--ink);
  padding: 2px 8px;
  background: var(--surface);
  border: 1px solid var(--line);
  font-family: 'SFMono-Regular', Consolas, monospace;
}

.case-split-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
}

@media (max-width: 900px) {
  .case-split-grid {
    grid-template-columns: 1fr;
  }
  .input-part {
    border-right: none !important;
    border-bottom: 1px solid var(--line);
  }
}

.case-part {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.input-part {
  border-right: 1px solid var(--line);
}

.part-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: #fdfdfd;
  border-bottom: 1px solid var(--line);
}

.part-title {
  font-size: 12px;
  font-weight: 700;
  color: var(--ink);
}

.part-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.btn-tool-sm {
  padding: 2px 8px;
  font-size: 11px;
  border: 1px solid var(--line);
  background: var(--surface);
  color: var(--ink-secondary);
  cursor: pointer;
  border-radius: 0;
  transition: all 0.15s ease;
}

.btn-tool-sm:hover {
  background: var(--bg);
  color: var(--ink);
  border-color: var(--ink-tertiary);
}

/* 数据网格卡片 */
.case-tables-list {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.data-grid-card {
  border: 1px solid var(--line);
  background: var(--surface);
  overflow: hidden;
}

.output-grid-card {
  border-color: #d0d7de;
}

.grid-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 12px;
  background: #f7f7f8;
  border-bottom: 1px solid var(--line);
}

.output-card-head {
  background: #f6f8fa;
}

.table-name-tag {
  font-size: 13px;
  font-weight: 600;
  color: var(--ink);
}

.table-name-tag code {
  font-size: 13px;
  font-weight: 700;
  color: #1a6f30;
  font-family: 'SFMono-Regular', Consolas, monospace;
}

.row-count-tag {
  font-size: 11px;
  color: var(--ink-tertiary);
}

.grid-table-scroll {
  overflow-x: auto;
  max-height: 360px;
}

.db-data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.output-data-table th {
  background: #fafbfc !important;
  color: #1f2328 !important;
}

.db-data-table th {
  background: #fdfdfd;
  color: var(--ink);
  font-weight: 700;
  padding: 6px 10px;
  text-align: left;
  border-bottom: 1.5px solid var(--line);
  border-right: 1px solid #f0f0f0;
  white-space: nowrap;
}

.col-type-tag {
  margin-left: 4px;
  font-size: 9px;
  padding: 0 3px;
  background: #eef2f6;
  color: #495057;
  font-weight: 500;
  border: 1px solid #ced4da;
  font-family: 'SFMono-Regular', Consolas, monospace;
}

.db-data-table td {
  padding: 6px 10px;
  border-bottom: 1px solid var(--line);
  border-right: 1px solid #f0f0f0;
  color: var(--ink);
  white-space: nowrap;
}

.db-data-table tbody tr:hover {
  background: #f9f9fa;
}

.th-index,
.td-index {
  width: 38px;
  text-align: center !important;
  color: var(--ink-tertiary) !important;
  font-size: 11px;
  background: #fafafa;
}

.null-pill {
  display: inline-block;
  padding: 0 4px;
  font-size: 10px;
  font-weight: 700;
  background: #f1f3f5;
  color: #868e96;
  border: 1px solid #dee2e6;
  border-radius: 2px;
  font-family: 'SFMono-Regular', Consolas, monospace;
}

.empty-rows-cell {
  text-align: center;
  padding: 18px !important;
  color: var(--ink-secondary);
  font-size: 12px;
}

.raw-sql-box {
  padding: 12px;
}

.sql-code {
  margin: 0;
  padding: 10px 12px;
  background: #1e1e1e;
  color: #f8f9fa;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 12px;
  line-height: 1.5;
  overflow-x: auto;
  border-radius: 0;
}

.empty-expected-box {
  padding: 24px 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg);
  min-height: 120px;
}

.empty-note {
  color: var(--ink-tertiary);
  font-size: 12px;
  line-height: 1.6;
}
</style>
