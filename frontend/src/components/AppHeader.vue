<script setup lang="ts">
import { RouterLink, useRoute, useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

// 按路径段匹配，避免 /database 与其详情页 /database/:id 同时高亮。
function isActive(path: string): boolean {
  if (path === '/') return route.path === '/';
  return route.path === path || route.path.startsWith(path + '/');
}

function logout() {
  userStore.clearAuth();
  router.push('/login');
}
</script>

<template>
  <header class="app-header">
    <div class="header-inner">
      <RouterLink to="/" class="brand">
        <img class="brand-logo" src="/KECElogo浅色版-trim.png" alt="KECE 课测" />
      </RouterLink>
      <nav class="nav">
        <RouterLink to="/" :class="{ active: isActive('/') }">首页</RouterLink>
        <RouterLink to="/questions" :class="{ active: isActive('/questions') }">编程</RouterLink>
        <RouterLink to="/database" :class="{ active: isActive('/database') }">数据库</RouterLink>
        <RouterLink to="/frontend" :class="{ active: isActive('/frontend') }">前端</RouterLink>
        <RouterLink to="/objective" :class="{ active: isActive('/objective') }">客观题</RouterLink>
        <RouterLink to="/exams" :class="{ active: isActive('/exams') }">考试</RouterLink>
        <RouterLink
          v-if="userStore.isLoggedIn"
          to="/submissions"
          :class="{ active: isActive('/submissions') }"
          >我的提交</RouterLink
        >
      </nav>
      <div class="header-right">
        <template v-if="userStore.isLoggedIn">
          <span class="username">{{ userStore.userInfo?.username }}</span>
          <span v-if="userStore.hasRole('ADMIN') || userStore.hasRole('TEACHER')" class="role-tag">
            {{ userStore.hasRole('ADMIN') ? '管理员' : '出题人' }}
          </span>
          <button class="btn btn-ghost btn-sm" @click="logout">退出</button>
          <RouterLink
            v-if="userStore.hasRole('ADMIN') || userStore.hasRole('TEACHER')"
            to="/admin"
            class="btn btn-primary btn-sm"
            >后台管理</RouterLink
          >
        </template>
        <RouterLink v-else to="/login" class="btn btn-primary btn-sm">登录</RouterLink>
      </div>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-bottom: 1px solid var(--line);
}
.header-inner {
  max-width: 1120px;
  margin: 0 auto;
  padding: 0 24px;
  height: 62px;
  display: flex;
  align-items: center;
  gap: 24px;
}
.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
  font-weight: 700;
  font-size: 18px;
  color: var(--ink);
}
.brand-logo {
  height: 40px;
  width: auto;
  display: block;
}
.nav {
  flex: 1;
  display: flex;
  justify-content: center;
  gap: 28px;
}
.nav a {
  color: var(--ink-secondary);
  font-size: 15px;
  transition: color 0.15s ease;
}
.nav a:hover {
  color: var(--ink);
}
.nav a.active {
  color: var(--ink);
  font-weight: 600;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
  min-width: 0;
}
.username {
  color: var(--ink-secondary);
  font-size: 14px;
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.role-tag {
  font-size: 12px;
  color: var(--ink-tertiary);
  border: 1px solid var(--line);
  border-radius: var(--pill);
  padding: 1px 10px;
  white-space: nowrap;
}
@media (max-width: 860px) {
  .nav {
    display: none;
  }
}
</style>
