<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
</script>

<template>
  <div class="layout">
    <header class="header">
      <div class="header-inner">
        <RouterLink to="/" class="brand">KECE</RouterLink>
        <nav class="nav">
          <RouterLink to="/">首页</RouterLink>
          <RouterLink to="/questions">题库</RouterLink>
        </nav>
      </div>
    </header>
    <main class="main">
      <RouterView />
    </main>
    <footer class="footer">
      <span>KECE 综合编程与技能评测平台</span>
    </footer>
  </div>
</template>

<style scoped>
.layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}
.header {
  border-bottom: 1px solid #e5e7eb;
  background: #fff;
}
.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  height: 56px;
  display: flex;
  align-items: center;
  gap: 32px;
}
.brand {
  font-weight: 700;
  font-size: 20px;
  text-decoration: none;
  color: #111827;
}
.nav {
  display: flex;
  gap: 24px;
}
.nav a {
  text-decoration: none;
  color: #4b5563;
  font-size: 14px;
}
.nav a.router-link-active {
  color: #2563eb;
}
.main {
  flex: 1;
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  padding: 24px;
}
.footer {
  border-top: 1px solid #e5e7eb;
  padding: 16px 24px;
  text-align: center;
  font-size: 13px;
  color: #6b7280;
}
</style>
