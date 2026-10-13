<script setup lang="ts">
import { RouterLink, useRoute } from 'vue-router';
import { useUserStore } from '@/stores/user';

const route = useRoute();
const userStore = useUserStore();

/** 后台各分区入口：编辑页归入对应分区的激活态。 */
const sections = [
  { path: '/admin/questions', label: '编程题' },
  { path: '/admin/db-questions', label: '数据库题' },
  { path: '/admin/objective-questions', label: '客观题' },
  { path: '/admin/exams', label: '考试管理' },
  { path: '/admin/users', label: '用户管理', adminOnly: true },
  { path: '/admin/submissions', label: '提交监控' },
];

function isActive(path: string): boolean {
  return route.path === path || route.path.startsWith(path + '/');
}
</script>

<template>
  <nav class="admin-nav">
    <RouterLink
      v-for="s in sections"
      v-show="!s.adminOnly || userStore.hasRole('ADMIN')"
      :key="s.path"
      :to="s.path"
      class="admin-nav-link"
      :class="{ active: isActive(s.path) }"
      >{{ s.label }}</RouterLink
    >
  </nav>
</template>

<style scoped>
.admin-nav {
  display: flex;
  align-items: center;
  gap: 4px;
  margin: 16px 0 20px;
  border-bottom: 1px solid var(--line);
  overflow-x: auto;
}
.admin-nav-link {
  padding: 10px 16px;
  font-size: 14px;
  color: var(--ink-secondary);
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  white-space: nowrap;
  transition: color 0.15s ease;
}
.admin-nav-link:hover {
  color: var(--ink);
}
.admin-nav-link.active {
  color: var(--ink);
  font-weight: 600;
  border-bottom-color: var(--ink);
}
</style>
