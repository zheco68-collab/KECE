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
const showRawDdl = ref(false)

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
        <span class="sec-title">数据库表结构</span>
        <span v-if="tables.length > 1" class="table-count-badge">
          共 {{ tables.length }} 张表
        </span>
      </div>

      <div class="header-right">
        <button
          v-if="tables.length > 0"
          type="button"
          class="btn-text-tool"
          @click="showRawDdl = !showRawDdl"
        >
          {{ showRawDdl ? '隐藏 DDL' : '查看 DDL' }}
        </button>
        <button type="button" class="btn-copy" @click="handleCopy">
          复制 DDL
        </button>
      </div>
    </div>

    <!-- 多表选择标签（如果有多张表） -->
    <div v-if="tables.length > 1" class="table-tabs">
      <button
        v-for="(t, idx) in tables"
        :key="t.tableName"
        type="button"
        class="tab-btn"
        :class="{ active: activeTableIndex === idx }"
        @click="activeTableIndex = idx"
      >
        <span class="tab-table-name">{{ t.tableName }}</span>
        <span class="tab-col-count">({{ t.columns.length }} 列)</span>
      </button>
    </div>

    <!-- 1. 字典表格视图 -->
    <div v-if="currentTable" class="table-view-container">
      <div v-if="tables.length <= 1" class="single-table-tag">
        <span class="table-badge">表名：<code>{{ currentTable.tableName }}</code></span>
      </div>

      <div class="table-scroll-wrap">
        <table class="schema-dict-table">
          <thead>
            <tr>
              <th class="th-name">字段名</th>
              <th class="th-type">类型</th>
              <th class="th-pk">主键</th>
              <th class="th-null">允许空</th>
              <th class="th-extra">属性与约束</th>
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

    <!-- 可选折叠的原始 DDL 面板（或解析失败回退） -->
    <div v-if="showRawDdl || tables.length === 0" class="raw-view-container">
      <div v-if="tables.length === 0" class="parse-fallback-note">
        （非标准 SQL 或建表语句为空，展示原始建表内容）：
      </div>
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

.btn-text-tool {
  padding: 3px 8px;
  font-size: 12px;
  background: transparent;
  border: 1px solid var(--line);
  color: var(--ink-secondary);
  cursor: pointer;
  border-radius: 0;
  transition: all 0.15s ease;
}

.btn-text-tool:hover {
  background: var(--bg);
  color: var(--ink);
  border-color: var(--ink-tertiary);
}

.btn-copy {
  padding: 3px 10px;
  font-size: 12px;
  font-weight: 500;
  border: 1px solid var(--line);
  background: var(--surface);
  color: var(--ink-secondary);
  cursor: pointer;
  border-radius: 0;
  transition: all 0.15s ease;
}

.btn-copy:hover {
  background: var(--bg);
  color: var(--ink);
  border-color: var(--ink-tertiary);
}

/* 多表标签页 */
.table-tabs {
  display: flex;
  background: var(--bg);
  border-bottom: 1px solid var(--line);
  overflow-x: auto;
  padding: 0 8px;
}

.tab-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  font-size: 13px;
  font-weight: 500;
  border: none;
  border-bottom: 2px solid transparent;
  background: transparent;
  color: var(--ink-secondary);
  cursor: pointer;
  transition: all 0.15s ease;
  white-space: nowrap;
}

.tab-btn:hover {
  color: var(--ink);
}

.tab-btn.active {
  color: var(--ink);
  font-weight: 700;
  border-bottom-color: var(--ink);
  background: var(--surface);
}

.tab-table-name {
  font-family: 'SFMono-Regular', Consolas, monospace;
}

.tab-col-count {
  font-size: 11px;
  color: var(--ink-tertiary);
}

/* 单表表名标识 */
.single-table-tag {
  padding: 8px 16px;
  background: #fdfdfd;
  border-bottom: 1px solid var(--line);
  font-size: 12px;
  color: var(--ink-secondary);
}

.table-badge code {
  font-size: 13px;
  font-weight: 700;
  color: #1a6f30;
  padding: 1px 6px;
  background: #eef7ee;
  border: 1px solid #d0e7d0;
  font-family: 'SFMono-Regular', Consolas, monospace;
}

/* 字典表格 */
.table-scroll-wrap {
  overflow-x: auto;
}

.schema-dict-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.schema-dict-table th {
  background: #fdfdfd;
  color: var(--ink);
  font-weight: 700;
  padding: 10px 14px;
  text-align: left;
  border-bottom: 1.5px solid var(--line);
  border-right: 1px solid #f0f0f0;
  white-space: nowrap;
}

.schema-dict-table td {
  padding: 8px 14px;
  border-bottom: 1px solid var(--line);
  border-right: 1px solid #f0f0f0;
  color: var(--ink);
}

.schema-dict-table tbody tr:hover {
  background: #f9f9fa;
}

.col-name-pill {
  font-weight: 700;
  color: var(--ink);
}

.col-name-pill.is-pk {
  color: #1a6f30;
}

.pk-badge {
  display: inline-block;
  padding: 1px 6px;
  font-size: 11px;
  font-weight: 800;
  background: #e6f4ea;
  color: #137333;
  border: 1px solid #ceead6;
  font-family: 'SFMono-Regular', Consolas, monospace;
  letter-spacing: 0.5px;
}

.null-yes {
  font-size: 11px;
  font-family: 'SFMono-Regular', Consolas, monospace;
  color: var(--ink-tertiary);
}

.null-no {
  font-size: 11px;
  font-weight: 700;
  font-family: 'SFMono-Regular', Consolas, monospace;
  color: var(--danger);
}

.dash-text {
  color: var(--ink-tertiary);
}

.extra-text {
  font-size: 12px;
  color: var(--ink-secondary);
}

.raw-view-container {
  padding: 12px 16px;
  background: var(--bg);
  border-top: 1px solid var(--line);
}

.parse-fallback-note {
  font-size: 12px;
  color: var(--ink-secondary);
  margin-bottom: 6px;
}

.sql-code {
  margin: 0;
  padding: 12px 16px;
  background: #1e1e1e;
  color: #f8f9fa;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 12px;
  line-height: 1.5;
  overflow-x: auto;
  border-radius: 0;
}

/* 紧凑模式（用于提交页抽屉） */
.is-compact .schema-header {
  padding: 6px 12px;
}
.is-compact .sec-title {
  font-size: 13px;
}
.is-compact .schema-dict-table th {
  padding: 6px 10px;
  font-size: 12px;
}
.is-compact .schema-dict-table td {
  padding: 5px 10px;
  font-size: 12px;
}
</style>
