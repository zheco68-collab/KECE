<script setup lang="ts">
import { RouterLink, useRouter } from 'vue-router'

const router = useRouter()

const features = [
  { title: '算法 OJ', desc: '在线编程评测，实时判定' },
  { title: '数据库 OJ', desc: 'SQL 语句评测与结果比对' },
  { title: '前端 AI 评测', desc: '智能分析代码质量与规范' },
  { title: '标准客观题', desc: '单选 / 多选 / 填空即时判分' },
]

function start() {
  router.push('/questions')
}
</script>

<template>
  <div class="home">
    <section class="hero container">
      <div class="hero-text">
        <h1>多维度编程与<br />技能评测平台</h1>
        <p class="hero-sub">无缝集成算法 OJ、数据库 OJ、前端 AI 智能评测及标准客观题系统</p>
        <div class="hero-actions">
          <button class="btn btn-primary btn-lg" @click="start">开始使用 →</button>
          <RouterLink to="/objective" class="btn btn-ghost btn-lg">浏览客观题</RouterLink>
        </div>
      </div>
      <div class="hero-visual">
        <div class="cube-scene">
          <div class="cube c1"></div>
          <div class="cube c2"></div>
          <div class="cube c3"></div>
          <div class="cube c4"></div>
        </div>
      </div>
    </section>

    <section class="features container">
      <div v-for="f in features" :key="f.title" class="feature">
        <div class="feature-title">{{ f.title }}</div>
        <div class="feature-desc">{{ f.desc }}</div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.hero {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 48px;
  align-items: center;
  padding-top: 72px;
  padding-bottom: 72px;
}
.hero-text h1 {
  font-size: 48px;
  line-height: 1.2;
  margin: 0 0 20px;
  letter-spacing: -0.5px;
  font-weight: 700;
}
.hero-sub {
  color: var(--ink-secondary);
  font-size: 17px;
  margin: 0 0 32px;
  max-width: 420px;
}
.hero-actions {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}
.hero-visual {
  position: relative;
  min-height: 360px;
  border-radius: var(--radius);
  background: linear-gradient(135deg, #ececec 0%, #f4f4f4 50%, #e8e8e8 100%);
  overflow: hidden;
}
.cube-scene {
  position: absolute;
  inset: 0;
  perspective: 1200px;
}
.cube {
  position: absolute;
  width: 130px;
  height: 130px;
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.7);
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.55), rgba(255, 255, 255, 0.18));
  box-shadow:
    0 20px 50px rgba(17, 17, 17, 0.08),
    inset 0 0 0 1px rgba(255, 255, 255, 0.4);
}
.c1 {
  top: 16%;
  left: 18%;
  transform: rotateX(45deg) rotateY(-28deg) translateZ(10px);
}
.c2 {
  top: 24%;
  left: 44%;
  transform: rotateX(50deg) rotateY(18deg) translateZ(40px);
}
.c3 {
  top: 46%;
  left: 26%;
  transform: rotateX(40deg) rotateY(-40deg) translateZ(70px);
}
.c4 {
  top: 52%;
  left: 52%;
  transform: rotateX(42deg) rotateY(8deg) translateZ(110px);
}
.features {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  padding-bottom: 72px;
}
.feature {
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  padding: 24px;
}
.feature-title {
  font-weight: 600;
  margin-bottom: 8px;
}
.feature-desc {
  color: var(--ink-tertiary);
  font-size: 14px;
}
@media (max-width: 860px) {
  .hero {
    grid-template-columns: 1fr;
    gap: 32px;
    padding-top: 40px;
    padding-bottom: 40px;
  }
  .hero-text h1 {
    font-size: 34px;
  }
  .hero-visual {
    min-height: 240px;
  }
  .features {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
