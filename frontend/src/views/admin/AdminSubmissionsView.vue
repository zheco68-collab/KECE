<script setup lang="ts">
import { RouterLink } from 'vue-router'
</script>

<template>
  <div class="admin container">
    <RouterLink class="back" to="/admin">← 返回后台首页</RouterLink>
    <h1 class="page-title">提交监控</h1>
    <div class="card">
      <p class="empty">
        该页面将在后续阶段实现 —— 提交监控（P4）：编程题 / 数据库题 / 客观题三 tab，按用户、题目、状态筛选
      </p>
    </div>
  </div>
</template>

<style scoped>
.admin {
  padding-top: 32px;
}
.page-title {
  margin: 28px 0 16px;
  font-size: 22px;
  font-weight: 600;
}
</style>
