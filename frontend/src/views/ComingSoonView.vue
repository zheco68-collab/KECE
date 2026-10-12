<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const title = computed(() => String(route.meta.title ?? '功能'))
</script>

<template>
  <div class="coming container">
    <div class="page-head">
      <h1 class="page-title">{{ title }}</h1>
    </div>
    <div class="card note">
      <p class="note-title">模块开发中</p>
      <p class="muted">该模块将在后续版本中开放，敬请期待。</p>
    </div>
  </div>
</template>

<style scoped>
.page-head {
  padding-top: 40px;
  margin-bottom: 24px;
}
.page-title {
  margin: 0 0 4px;
  font-size: 28px;
}
.note {
  padding: 28px 24px;
}
.note-title {
  margin: 0 0 6px;
  font-size: 17px;
  font-weight: 600;
}
</style>
