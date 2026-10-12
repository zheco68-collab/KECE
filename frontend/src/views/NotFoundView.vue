<script setup lang="ts">
import { RouterLink } from 'vue-router'
</script>

<template>
  <div class="not-found">
    <h1 class="code">404</h1>
    <p class="muted">页面不存在或已被移除</p>
    <RouterLink to="/" class="btn btn-primary">返回首页</RouterLink>
  </div>
</template>

<style scoped>
.not-found {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 24px;
}
.code {
  margin: 0;
  font-size: 72px;
  line-height: 1;
  letter-spacing: 2px;
}
</style>
