<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  page: number
  size: number
  total: number
}>()

const emit = defineEmits<{
  (e: 'update:page', p: number): void
}>()

const pageCount = computed(() => Math.max(1, Math.ceil(props.total / Math.max(1, props.size))))

/** 页码序列，null 表示省略号：超过 7 页时只保留首页、末页、当前页及其左右一页。 */
const pages = computed<Array<number | null>>(() => {
  const last = pageCount.value
  if (last <= 7) return Array.from({ length: last }, (_, i) => i + 1)
  const kept = [...new Set([1, last, props.page - 1, props.page, props.page + 1])]
    .filter((n) => n >= 1 && n <= last)
    .sort((a, b) => a - b)
  const items: Array<number | null> = []
  let prev = 0
  for (const n of kept) {
    if (n - prev > 1) items.push(null)
    items.push(n)
    prev = n
  }
  return items
})

function go(p: number | null) {
  if (p === null || p < 1 || p > pageCount.value || p === props.page) return
  emit('update:page', p)
}
</script>

<template>
  <div v-if="total > 0" class="pagination">
    <span class="page-total">共 {{ total }} 条</span>
    <div class="page-nav">
      <button class="btn btn-ghost btn-sm" :disabled="page <= 1" @click="go(page - 1)">
        上一页
      </button>
      <template v-for="(item, i) in pages" :key="i">
        <span v-if="item === null" class="page-gap" aria-hidden="true">…</span>
        <button
          v-else
          class="btn btn-ghost btn-sm page-num"
          :class="{ active: item === page }"
          @click="go(item)"
        >
          {{ item }}
        </button>
      </template>
      <button class="btn btn-ghost btn-sm" :disabled="page >= pageCount" @click="go(page + 1)">
        下一页
      </button>
    </div>
  </div>
</template>

<style scoped>
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  flex-wrap: wrap;
  padding: 20px 0;
}
.page-total {
  color: var(--ink-secondary);
  font-size: 14px;
}
.page-nav {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.page-num {
  min-width: 38px;
  padding: 8px 10px;
}
.page-num.active {
  background: var(--button);
  border-color: var(--button);
  color: #fff;
}
.page-gap {
  padding: 0 2px;
  color: var(--ink-tertiary);
  font-size: 14px;
  user-select: none;
}
</style>
