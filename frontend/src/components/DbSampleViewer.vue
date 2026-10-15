<script setup lang="ts">
import { computed, ref } from 'vue'
import {
  parseSchemaSql,
  parseSeedSql,
  type ParsedSeedTable,
} from '@/utils/sqlSchemaParser'
import { copyToClipboard } from '@/utils/clipboard'
import { useToast } from '@/composables/useToast'

interface SampleCaseItem {
  id?: number
  caseOrder?: number
  seedSql: string
  isSample?: number
}

const props = defineProps<{
  sampleCases: SampleCaseItem[]
  schemaSql: string
}>()

const { success, error: toastError } = useToast()
const viewMode = ref<'table' | 'raw'>('table')

const schemas = computed(() => parseSchemaSql(props.schemaSql))

interface ParsedCaseGroup {
  originalIndex: number
  seedSql: string
  tables: ParsedSeedTable[]
}

const parsedCases = computed<ParsedCaseGroup[]>(() => {
  return props.sampleCases.map((c, idx) => ({
    originalIndex: idx + 1,
    seedSql: c.seedSql,
    tables: parseSeedSql(c.seedSql, schemas.value),
  }))
})

async function copySeedSql(text: string, label: string) {
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
        <span class="sec-title">样例初始数据 (Seed Data)</span>
        <span class="sample-count-badge">共 {{ sampleCases.length }} 组公开样例</span>
      </div>

      <div class="header-right">
        <!-- 视图切换 -->
        <div class="view-switch-tabs">
          <button
            type="button"
            class="switch-btn"
            :class="{ active: viewMode === 'table' }"
            @click="viewMode = 'table'"
          >
            📊 表格视图
          </button>
          <button
            type="button"
            class="switch-btn"
            :class="{ active: viewMode === 'raw' }"
            @click="viewMode = 'raw'"
          >
            ⌨️ 插入 SQL
          </button>
        </div>
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
          <div class="case-head-left">
            <span class="case-badge">样例 #{{ grp.originalIndex }}</span>
            <span class="case-hint muted">输入数据表结构如下：</span>
          </div>
          <button
            type="button"
            class="btn-copy-sm"
            @click="copySeedSql(grp.seedSql, `样例 #${grp.originalIndex} 初始数据`)"
          >
            复制初始 SQL
          </button>
        </div>

        <!-- 1. 表格视图 -->
        <div v-if="viewMode === 'table' && grp.tables.length > 0" class="case-tables-list">
          <div
            v-for="(tbl, tIdx) in grp.tables"
            :key="tIdx"
            class="data-grid-card"
          >
            <div class="grid-card-head">
              <span class="table-name-tag">
                <span class="table-icon">▦</span>
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

        <!-- 2. 原始 SQL 视图（或无数据回退） -->
        <div v-else class="raw-sql-box">
          <pre class="sql-code">{{ grp.seedSql }}</pre>
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

.view-switch-tabs {
  display: inline-flex;
  border: 1px solid var(--line);
  background: var(--surface);
  border-radius: 0;
}

.switch-btn {
  padding: 3px 10px;
  font-size: 12px;
  font-weight: 500;
  border: none;
  background: transparent;
  color: var(--ink-secondary);
  cursor: pointer;
  transition: all 0.15s ease;
}

.switch-btn:hover {
  color: var(--ink);
}

.switch-btn.active {
  background: var(--button);
  color: #fff;
  font-weight: 600;
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
  justify-content: space-between;
  padding: 8px 12px;
  background: var(--bg);
  border-bottom: 1px solid var(--line);
}

.case-head-left {
  display: flex;
  align-items: center;
  gap: 10px;
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

.case-hint {
  font-size: 12px;
}

.btn-copy-sm {
  padding: 2px 8px;
  font-size: 11px;
  border: 1px solid var(--line);
  background: var(--surface);
  color: var(--ink-secondary);
  cursor: pointer;
  border-radius: 0;
  transition: all 0.15s ease;
}

.btn-copy-sm:hover {
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

.grid-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 12px;
  background: #f7f7f8;
  border-bottom: 1px solid var(--line);
}

.table-name-tag {
  font-size: 13px;
  font-weight: 600;
  color: var(--ink);
  display: flex;
  align-items: center;
  gap: 6px;
}

.table-icon {
  font-size: 13px;
  color: var(--ink-tertiary);
}

.table-name-tag code {
  font-size: 13px;
  font-weight: 700;
  color: #1a6f30;
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

.db-data-table th {
  background: #fdfdfd;
  color: var(--ink);
  font-weight: 700;
  padding: 8px 12px;
  text-align: left;
  border-bottom: 1.5px solid var(--line);
  border-right: 1px solid #f0f0f0;
  white-space: nowrap;
}

.db-data-table td {
  padding: 7px 12px;
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
}

.raw-sql-box {
  padding: 12px;
}

.sql-code {
  margin: 0;
  padding: 10px 12px;
  background: #1e1e1e;
  color: #f8f9fa;
  font-family: 'JetBrains Mono', 'Fira Code', Consolas, monospace;
  font-size: 13px;
  line-height: 1.5;
  overflow-x: auto;
  border-radius: 0;
}
</style>
