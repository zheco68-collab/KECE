<script setup lang="ts">
import { RouterLink } from 'vue-router'
</script>

<template>
  <div class="admin container">
    <RouterLink class="back" to="/admin">← 返回后台首页</RouterLink>
    <h1 class="page-title">数据库题编辑</h1>
    <div class="card">
      <p class="empty">该页面将在后续阶段实现 —— 数据库题编辑（P2）：建库脚本、参考答案与判分用例</p>
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
