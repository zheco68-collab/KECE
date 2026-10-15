<script setup lang="ts">
import { computed, ref } from 'vue'
import { parseSchemaSql, type ParsedTableSchema } from '@/utils/sqlSchemaParser'
import { copyToClipboard } from '@/utils/clipboard'
import { useToast } from '@/composables/useToast'

const props = withDefaults(
  defineProps<{
    schemaSql: string
    compact?: boolean
  }>(),
  {
    compact: false,
  },
)

const { success, error: toastError } = useToast()
const viewMode = ref<'table' | 'raw'>('table')

const tables = computed<ParsedTableSchema[]>(() => {
  return parseSchemaSql(props.schemaSql)
})

const activeTableIndex = ref(0)
const currentTable = computed(() => {
  if (tables.value.length === 0) return null
  return tables.value[activeTableIndex.value] ?? tables.value[0]
})

async function handleCopy() {
  if (!props.schemaSql) return
  const ok = await copyToClipboard(props.schemaSql)
  if (ok) {
    success('建表 DDL 语句已复制到剪贴板')
  } else {
    toastError('复制失败，请手动选取复制')
  }
}
</script>

<template>
  <div class="db-schema-viewer card" :class="{ 'is-compact': compact }">
    <div class="schema-header">
      <div class="header-left">
        <span class="sec-title">数据库表结构字典 (Schema)</span>
        <span v-if="tables.length > 1" class="table-count-badge">
          共 {{ tables.length }} 张表
        </span>
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
            📋 字典视图
          </button>
          <button
            type="button"
            class="switch-btn"
            :class="{ active: viewMode === 'raw' }"
            @click="viewMode = 'raw'"
          >
            ⌨️ 原始 DDL
          </button>
        </div>

        <button type="button" class="btn-copy" @click="handleCopy">
          复制 DDL
        </button>
      </div>
    </div>

    <!-- 多表选择标签（如果有多张表） -->
    <div v-if="tables.length > 1 && viewMode === 'table'" class="table-tabs">
      <button
        v-for="(t, idx) in tables"
        :key="t.tableName"
        type="button"
        class="tab-btn"
        :class="{ active: activeTableIndex === idx }"
        @click="activeTableIndex = idx"
      >
        <span class="tab-table-icon">▤</span>
        <span class="tab-table-name">{{ t.tableName }}</span>
        <span class="tab-col-count">({{ t.columns.length }} 列)</span>
      </button>
    </div>

    <!-- 1. 字典表格视图 -->
    <div v-if="viewMode === 'table' && currentTable" class="table-view-container">
      <div v-if="tables.length <= 1" class="single-table-tag">
        <span class="table-badge">表名：<code>{{ currentTable.tableName }}</code></span>
      </div>

      <div class="table-scroll-wrap">
        <table class="schema-dict-table">
          <thead>
            <tr>
              <th class="th-name">字段名 (Field)</th>
              <th class="th-type">数据类型 (Type)</th>
              <th class="th-pk">主键</th>
              <th class="th-null">允许空</th>
              <th class="th-extra">约束与属性 (Attributes)</th>
              <th v-if="currentTable.columns.some((c) => c.comment)" class="th-comment">说明</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="col in currentTable.columns" :key="col.name">
              <td class="td-name mono">
                <span class="col-name-pill" :class="{ 'is-pk': col.isPrimaryKey }">
                  {{ col.name }}
                </span>
              </td>
              <td class="td-type mono">{{ col.type }}</td>
              <td class="td-pk">
                <span v-if="col.isPrimaryKey" class="pk-badge">PK</span>
                <span v-else class="dash-text">-</span>
              </td>
              <td class="td-null">
                <span v-if="col.isNullable" class="null-yes">YES</span>
                <span v-else class="null-no">NO</span>
              </td>
              <td class="td-extra mono">
                <span v-if="col.extra" class="extra-text">{{ col.extra }}</span>
                <span v-else class="dash-text">-</span>
              </td>
              <td v-if="currentTable.columns.some((c) => c.comment)" class="td-comment">
                {{ col.comment || '-' }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 2. 原始 DDL 视图（或解析失败回退） -->
    <div v-else class="raw-view-container">
      <pre class="sql-code">{{ schemaSql }}</pre>
    </div>
  </div>
</template>

<style scoped>
.db-schema-viewer {
  border: 1px solid var(--line);
  background: var(--surface);
  border-radius: 0;
  overflow: hidden;
}

.schema-header {
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

.table-count-badge {
  font-size: 11px;
  padding: 1px 6px;
  background: var(--bg);
  border: 1px solid var(--line);
  color: var(--ink-secondary);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
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

.btn-copy {
  padding: 3px 10px;
  font-size: 12px;
  font-weight: 500;
  border: 1px solid var(--line);
  background: var(--surface);
  color: var(--ink-secondary);
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-copy:hover {
  background: var(--bg);
  color: var(--ink);
  border-color: var(--ink-tertiary);
}

/* 多表标签栏 */
.table-tabs {
  display: flex;
  gap: 6px;
  padding: 8px 16px;
  background: var(--bg);
  border-bottom: 1px solid var(--line);
  overflow-x: auto;
}

.tab-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  font-size: 13px;
  border: 1px solid var(--line);
  background: var(--surface);
  color: var(--ink-secondary);
  cursor: pointer;
  border-radius: 0;
  transition: all 0.15s ease;
}

.tab-btn:hover {
  color: var(--ink);
  border-color: var(--ink-tertiary);
}

.tab-btn.active {
  background: #fff;
  color: var(--ink);
  border-color: var(--button);
  font-weight: 600;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.tab-table-icon {
  font-size: 14px;
}

.tab-col-count {
  font-size: 11px;
  color: var(--ink-tertiary);
}

/* 单表标示 */
.single-table-tag {
  padding: 10px 16px 0;
}

.table-badge {
  display: inline-block;
  font-size: 12px;
  color: var(--ink-secondary);
}

.table-badge code {
  font-weight: 700;
  font-size: 13px;
  color: var(--ink);
  background: var(--bg);
  padding: 1px 6px;
  border: 1px solid var(--line);
}

/* 字典表格 */
.table-scroll-wrap {
  overflow-x: auto;
  padding: 12px 16px 16px;
}

.schema-dict-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  border: 1px solid var(--line);
}

.schema-dict-table th {
  background: #f7f7f8;
  color: var(--ink-secondary);
  font-weight: 600;
  text-align: left;
  padding: 8px 12px;
  border: 1px solid var(--line);
  white-space: nowrap;
}

.schema-dict-table td {
  padding: 8px 12px;
  border: 1px solid var(--line);
  color: var(--ink);
}

.schema-dict-table tbody tr:hover {
  background: #fcfcfc;
}

.col-name-pill {
  font-weight: 600;
  color: var(--ink);
}

.col-name-pill.is-pk {
  color: #1a6f30;
  font-weight: 700;
}

.td-type {
  color: #3b5bdb;
  font-weight: 500;
}

.pk-badge {
  display: inline-block;
  padding: 1px 6px;
  font-size: 11px;
  font-weight: 700;
  background: #ebfbee;
  color: #2b8a3e;
  border: 1px solid #b2f2bb;
  font-family: 'SFMono-Regular', Consolas, monospace;
}

.null-yes {
  color: var(--ink-tertiary);
  font-size: 12px;
}

.null-no {
  color: #c92a2a;
  font-size: 12px;
  font-weight: 600;
}

.dash-text {
  color: var(--line);
}

.extra-text {
  font-size: 12px;
  color: var(--ink-secondary);
}

.td-comment {
  font-size: 12px;
  color: var(--ink-secondary);
}

/* 原始 DDL 视图 */
.raw-view-container {
  padding: 12px 16px;
}

.sql-code {
  margin: 0;
  padding: 12px 14px;
  background: #1e1e1e;
  color: #f8f9fa;
  font-family: 'JetBrains Mono', 'Fira Code', Consolas, monospace;
  font-size: 13px;
  line-height: 1.5;
  overflow-x: auto;
  border-radius: 0;
}

/* 紧凑模式（抽屉使用） */
.is-compact .schema-header {
  padding: 8px 12px;
}
.is-compact .table-scroll-wrap {
  padding: 8px 12px 12px;
}
.is-compact .schema-dict-table th,
.is-compact .schema-dict-table td {
  padding: 6px 8px;
  font-size: 12px;
}
</style>
