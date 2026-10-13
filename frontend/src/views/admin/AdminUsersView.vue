<script setup lang="ts">
import { RouterLink } from 'vue-router'
</script>

<template>
  <div class="admin container">
    <RouterLink class="back" to="/admin">← 返回后台首页</RouterLink>
    <h1 class="page-title">用户管理</h1>
    <div class="card">
      <p class="empty">该页面将在后续阶段实现 —— 用户管理（P1）：列表、改角色、禁用/启用、重置密码</p>
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
