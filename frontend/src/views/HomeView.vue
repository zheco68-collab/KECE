<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'

const router = useRouter()

// 交互终端演示 Tab 切换
type DemoMode = 'algo' | 'sql' | 'exam'
const activeTab = ref<DemoMode>('algo')

interface DemoContent {
  filename: string
  langBadge: string
  code: string
  verdict: {
    status: string
    statusClass: string
    metrics: { label: string; value: string }[]
    details: string
  }
}

const demos: Record<DemoMode, DemoContent> = {
  algo: {
    filename: 'solution.cpp',
    langBadge: 'C++ 17',
    code: `#include <iostream>
#include <vector>
using namespace std;

// 二分查找目标值
int search(const vector<int>& nums, int target) {
    int left = 0, right = nums.size() - 1;
    while (left <= right) {
        int mid = left + (right - left) / 2;
        if (nums[mid] == target) return mid;
        if (nums[mid] < target) left = mid + 1;
        else right = mid - 1;
    }
    return -1;
}`,
    verdict: {
      status: 'ACCEPTED (AC)',
      statusClass: 'status-ac',
      metrics: [
        { label: '运行耗时', value: '12 ms' },
        { label: '内存消耗', value: '4.2 MB' },
        { label: '测试点通过', value: '10 / 10' },
      ],
      details: 'Docker Cgroups v2 强隔离沙箱运行 · 资源严格受控',
    },
  },
  sql: {
    filename: 'query.sql',
    langBadge: 'MySQL 8.0',
    code: `-- 查询研发部薪资最高的前三名员工
SELECT 
    e.id, 
    e.name, 
    e.salary
FROM emp e
WHERE e.dept = 'RD'
ORDER BY e.salary DESC, e.id ASC
LIMIT 3;`,
    verdict: {
      status: 'ACCEPTED (AC)',
      statusClass: 'status-ac',
      metrics: [
        { label: '执行耗时', value: '18 ms' },
        { label: '行序校验', value: '严格匹配' },
        { label: '数据比对', value: '3 / 3 完全一致' },
      ],
      details: '独立判题临时库 judge_sub_01 执行 · 跑完即自动销毁',
    },
  },
  exam: {
    filename: 'session_report.json',
    langBadge: 'ACM 赛制',
    code: `{
  "exam_id": 1024,
  "title": "2026 技术工程能力机试",
  "mode": "ACM",
  "rank": 1,
  "solved_count": 4,
  "penalty_minutes": 118,
  "screen_switch_count": 0,
  "status": "COMPLETED"
}`,
    verdict: {
      status: 'RANK #1 (100 分)',
      statusClass: 'status-exam',
      metrics: [
        { label: '通过题数', value: '4 题' },
        { label: '罚时', value: '118 分钟' },
        { label: '切屏记录', value: '0 次（正常）' },
      ],
      details: 'ACM 实时榜单结算 · 客观题 + 编程题自动折算分值',
    },
  },
}

function start() {
  router.push('/questions')
}
</script>

<template>
  <div class="home">
    <!-- 官网 Hero 区域（严格对齐品牌 VI 规范） -->
    <section class="hero container">
      <div class="hero-text">
        <h1 class="hero-title">
          多维度编程与<br />
          技能评测平台
        </h1>
        <p class="hero-sub">
          无缝集成算法 OJ、数据库 OJ、前端 AI 智能评测<br />
          及标准客观题系统
        </p>

        <div class="hero-actions">
          <button class="btn btn-primary btn-lg hero-cta" @click="start">开始使用 →</button>
          <RouterLink to="/exams" class="btn btn-ghost btn-lg">在线考试系统</RouterLink>
        </div>
      </div>

      <!-- 右侧 3D 纸雕折叠几何装置（还原 VI 规范概念图） -->
      <div class="hero-visual">
        <div class="sculpture-wrapper">
          <svg
            class="origami-svg"
            viewBox="0 0 520 400"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
          >
            <defs>
              <!-- 投影渐变 -->
              <radialGradient
                id="groundShadow"
                cx="50%"
                cy="50%"
                r="50%"
                fx="50%"
                fy="50%"
              >
                <stop offset="0%" stop-color="#141824" stop-opacity="0.14" />
                <stop offset="60%" stop-color="#141824" stop-opacity="0.05" />
                <stop offset="100%" stop-color="#141824" stop-opacity="0" />
              </radialGradient>

              <!-- 各折面微妙明暗渐变 -->
              <linearGradient id="facet1" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stop-color="#ffffff" />
                <stop offset="100%" stop-color="#eaedf2" />
              </linearGradient>

              <linearGradient id="facet2" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stop-color="#d6dae2" />
                <stop offset="100%" stop-color="#c1c7d2" />
              </linearGradient>

              <linearGradient id="facet3" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stop-color="#ffffff" />
                <stop offset="70%" stop-color="#f6f8fb" />
                <stop offset="100%" stop-color="#ebedf3" />
              </linearGradient>

              <linearGradient id="facet4" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stop-color="#d4d8e0" />
                <stop offset="100%" stop-color="#bec4ce" />
              </linearGradient>

              <linearGradient id="facet5" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stop-color="#ffffff" />
                <stop offset="100%" stop-color="#f1f3f7" />
              </linearGradient>

              <linearGradient id="facet6" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stop-color="#ccd2dc" />
                <stop offset="100%" stop-color="#b8bfcb" />
              </linearGradient>

              <!-- 边缘高光 -->
              <linearGradient id="edgeGleam" x1="0%" y1="0%" x2="0%" y2="100%">
                <stop offset="0%" stop-color="#ffffff" stop-opacity="0.9" />
                <stop offset="100%" stop-color="#ffffff" stop-opacity="0.2" />
              </linearGradient>
            </defs>

            <!-- 地面柔和接触阴影 -->
            <ellipse cx="265" cy="340" rx="225" ry="34" fill="url(#groundShadow)" />

            <!-- 折纸屏风 面 1（左外侧向前） -->
            <polygon
              points="45,95 130,55 130,295 45,325"
              fill="url(#facet1)"
            />

            <!-- 折纸屏风 面 2（向内折入暗面） -->
            <polygon
              points="130,55 205,115 205,345 130,295"
              fill="url(#facet2)"
            />

            <!-- 折纸屏风 面 3（中心最高主受光面） -->
            <polygon
              points="205,115 315,65 315,295 205,345"
              fill="url(#facet3)"
            />

            <!-- 折纸屏风 面 4（向内折入侧暗面） -->
            <polygon
              points="315,65 385,120 385,345 315,295"
              fill="url(#facet4)"
            />

            <!-- 折纸屏风 面 5（右侧向前延伸受光面） -->
            <polygon
              points="385,120 475,80 475,305 385,345"
              fill="url(#facet5)"
            />

            <!-- 折纸屏风 面 6（右端收尾暗面） -->
            <polygon
              points="475,80 500,102 500,325 475,305"
              fill="url(#facet6)"
            />

            <!-- 折棱高光线，强化几何立体感与纯净质感 -->
            <line x1="130" y1="55" x2="130" y2="295" stroke="url(#edgeGleam)" stroke-width="1.5" />
            <line x1="205" y1="115" x2="205" y2="345" stroke="#b0b7c4" stroke-width="0.8" />
            <line x1="315" y1="65" x2="315" y2="295" stroke="url(#edgeGleam)" stroke-width="1.5" />
            <line x1="385" y1="120" x2="385" y2="345" stroke="#adb4c2" stroke-width="0.8" />
            <line x1="475" y1="80" x2="475" y2="305" stroke="url(#edgeGleam)" stroke-width="1.2" />
          </svg>
        </div>
      </div>
    </section>

    <!-- 功能模块图标卡片区（精确对应 VI 规范设计） -->
    <section class="modules-section container">
      <div class="modules-grid">
        <!-- 算法 OJ -->
        <RouterLink to="/questions" class="module-card">
          <div class="module-icon-box">
            <!-- 线性代码图标 </> -->
            <svg class="module-svg" viewBox="0 0 48 48" fill="none">
              <rect x="6" y="6" width="36" height="36" rx="0" stroke="currentColor" stroke-width="2.5" />
              <path d="M18 20L14 24L18 28" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" />
              <path d="M30 20L34 24L30 28" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" />
              <path d="M26 17L22 31" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" />
            </svg>
          </div>
          <div class="module-title">算法 OJ</div>
          <div class="module-desc">在线编程 · 智能评测</div>
          <div class="module-arrow">进入题库 →</div>
        </RouterLink>

        <!-- 数据库 OJ -->
        <RouterLink to="/database" class="module-card">
          <div class="module-icon-box">
            <!-- 线性数据库图标 -->
            <svg class="module-svg" viewBox="0 0 48 48" fill="none">
              <ellipse cx="24" cy="14" rx="14" ry="5.5" stroke="currentColor" stroke-width="2.5" />
              <path d="M10 14V24C10 27 16.3 29.5 24 29.5C31.7 29.5 38 27 38 24V14" stroke="currentColor" stroke-width="2.5" />
              <path d="M10 24V34C10 37 16.3 39.5 24 39.5C31.7 39.5 38 37 38 34V24" stroke="currentColor" stroke-width="2.5" />
            </svg>
          </div>
          <div class="module-title">数据库 OJ</div>
          <div class="module-desc">SQL 实战 · 结果评测</div>
          <div class="module-arrow">进入题库 →</div>
        </RouterLink>

        <!-- 前端 AI 评测 -->
        <RouterLink to="/frontend" class="module-card">
          <div class="module-icon-box">
            <!-- 线性浏览器窗口与代码检查图标 -->
            <svg class="module-svg" viewBox="0 0 48 48" fill="none">
              <rect x="7" y="9" width="34" height="30" rx="0" stroke="currentColor" stroke-width="2.5" />
              <line x1="7" y1="18" x2="41" y2="18" stroke="currentColor" stroke-width="2.5" />
              <circle cx="13" cy="13.5" r="1.5" fill="currentColor" />
              <circle cx="18" cy="13.5" r="1.5" fill="currentColor" />
              <circle cx="23" cy="13.5" r="1.5" fill="currentColor" />
              <path d="M16 29L20 33L32 23" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
          </div>
          <div class="module-title">前端 AI 评测</div>
          <div class="module-desc">代码检查 · 智能反馈</div>
          <div class="module-arrow">体验评测 →</div>
        </RouterLink>

        <!-- 标准客观题 -->
        <RouterLink to="/objective" class="module-card">
          <div class="module-icon-box">
            <!-- 线性问卷/答题卡图标 -->
            <svg class="module-svg" viewBox="0 0 48 48" fill="none">
              <rect x="9" y="7" width="30" height="34" rx="0" stroke="currentColor" stroke-width="2.5" />
              <line x1="16" y1="16" x2="32" y2="16" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" />
              <line x1="16" y1="23" x2="32" y2="23" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" />
              <line x1="16" y1="30" x2="26" y2="30" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" />
              <circle cx="13" cy="16" r="1" fill="currentColor" />
              <circle cx="13" cy="23" r="1" fill="currentColor" />
              <circle cx="13" cy="30" r="1" fill="currentColor" />
            </svg>
          </div>
          <div class="module-title">标准客观题</div>
          <div class="module-desc">自动判分 · 成绩统计</div>
          <div class="module-arrow">进入题库 →</div>
        </RouterLink>

        <!-- 机考与竞赛（Phase 2 核心延伸） -->
        <RouterLink to="/exams" class="module-card exam-highlight">
          <div class="module-icon-box">
            <!-- 线性秒表/竞赛勋章图标 -->
            <svg class="module-svg" viewBox="0 0 48 48" fill="none">
              <circle cx="24" cy="26" r="14" stroke="currentColor" stroke-width="2.5" />
              <path d="M24 18V26L29 29" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" />
              <line x1="21" y1="7" x2="27" y2="7" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" />
              <line x1="24" y1="7" x2="24" y2="12" stroke="currentColor" stroke-width="2.5" />
              <line x1="34" y1="14" x2="37" y2="11" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" />
            </svg>
          </div>
          <div class="module-title">在线考试系统</div>
          <div class="module-desc">ACM / OI 双赛制 · 实时排名</div>
          <div class="module-arrow">进入机考 →</div>
        </RouterLink>
      </div>
    </section>

    <!-- 交互式技术体验区：实时沙箱评测终端 -->
    <section class="demo-section container">
      <div class="demo-header">
        <div class="demo-eyebrow">// SANDBOX RUNNER · LIVE VERDICT</div>
        <h2 class="demo-title">真实评测闭环体验</h2>
        <p class="demo-desc">
          基于 Linux 强隔离容器沙箱与独立 MySQL 临时库，提供毫秒级、防作弊、确定性的技术评测。
        </p>
      </div>

      <div class="terminal-card">
        <div class="terminal-top">
          <div class="terminal-badge">
            <span class="live-dot"></span>
            <span>JUDGE RUNNER #01</span>
          </div>

          <div class="terminal-tabs">
            <button
              class="terminal-tab"
              :class="{ active: activeTab === 'algo' }"
              @click="activeTab = 'algo'"
            >
              C++ 算法 OJ
            </button>
            <button
              class="terminal-tab"
              :class="{ active: activeTab === 'sql' }"
              @click="activeTab = 'sql'"
            >
              SQL 数据库 OJ
            </button>
            <button
              class="terminal-tab"
              :class="{ active: activeTab === 'exam' }"
              @click="activeTab = 'exam'"
            >
              ACM 考试结算
            </button>
          </div>
        </div>

        <div class="terminal-meta-bar">
          <span class="file-name">{{ demos[activeTab].filename }}</span>
          <span class="lang-tag">{{ demos[activeTab].langBadge }}</span>
        </div>

        <div class="terminal-body">
          <pre class="code-view"><code>{{ demos[activeTab].code }}</code></pre>
        </div>

        <div class="terminal-hud">
          <div class="hud-status-row">
            <span class="verdict-tag" :class="demos[activeTab].verdict.statusClass">
              ● {{ demos[activeTab].verdict.status }}
            </span>
            <span class="verdict-detail">{{ demos[activeTab].verdict.details }}</span>
          </div>
          <div class="hud-metric-row">
            <div
              v-for="m in demos[activeTab].verdict.metrics"
              :key="m.label"
              class="metric-item"
            >
              <div class="metric-title">{{ m.label }}</div>
              <div class="metric-data">{{ m.value }}</div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 底部行动区 CTA -->
    <section class="cta-section container">
      <div class="cta-card">
        <div class="cta-left">
          <h2 class="cta-title">准备好开启专业级技术测评了吗？</h2>
          <p class="cta-desc">
            无论是日常算法与 SQL 训练，还是举办期末机考与企业笔试，KECE 现已全面就绪。
          </p>
        </div>
        <div class="cta-right">
          <button class="btn btn-primary btn-lg" @click="start">开始使用 →</button>
          <RouterLink to="/exams" class="btn btn-ghost btn-lg">参加在线考试</RouterLink>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.home {
  padding-bottom: 80px;
}

/* =========================================
   1. Hero 区域（对齐 VI 规范）
   ========================================= */
.hero {
  display: grid;
  grid-template-columns: 1fr 1.05fr;
  gap: 40px;
  align-items: center;
  padding-top: 60px;
  padding-bottom: 70px;
}

.hero-text {
  max-width: 500px;
}

.hero-title {
  font-size: 46px;
  line-height: 1.22;
  font-weight: 700;
  letter-spacing: -0.6px;
  color: var(--ink);
  margin: 0 0 20px;
}

.hero-sub {
  font-size: 16px;
  line-height: 1.65;
  color: var(--ink-secondary);
  margin: 0 0 36px;
  font-weight: 400;
}

.hero-actions {
  display: flex;
  gap: 16px;
  align-items: center;
  flex-wrap: wrap;
}

.hero-cta {
  padding: 13px 36px;
  font-size: 16px;
  font-weight: 600;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

.hero-cta:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.18);
}

/* 3D 纸雕折叠装置容器 */
.hero-visual {
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  min-height: 380px;
}

.sculpture-wrapper {
  width: 100%;
  max-width: 520px;
  animation: gentleFloat 5s ease-in-out infinite alternate;
  filter: drop-shadow(0 20px 30px rgba(0, 0, 0, 0.04));
}

@keyframes gentleFloat {
  0% {
    transform: translateY(0px);
  }
  100% {
    transform: translateY(-8px);
  }
}

.origami-svg {
  width: 100%;
  height: auto;
  display: block;
  overflow: visible;
}

/* =========================================
   2. 功能模块卡片区（精确还原 VI 规范图标与排版）
   ========================================= */
.modules-section {
  padding-top: 10px;
  padding-bottom: 60px;
}

.modules-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 18px;
}

.module-card {
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  padding: 28px 20px 22px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  box-shadow: var(--shadow-sm);
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
  position: relative;
  text-decoration: none;
  color: inherit;
}

.module-card:hover {
  transform: translateY(-4px);
  border-color: #cfd4dc;
  box-shadow: 0 14px 30px rgba(17, 24, 39, 0.08);
}

.module-card.exam-highlight {
  background: #ffffff;
  border-color: #d8dde6;
}

.module-icon-box {
  width: 56px;
  height: 56px;
  border-radius: 0;
  background: #f4f5f7;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--ink);
  margin-bottom: 18px;
  transition: all 0.25s ease;
}

.module-card:hover .module-icon-box {
  background: var(--ink);
  color: #ffffff;
  transform: scale(1.06);
}

.module-svg {
  width: 28px;
  height: 28px;
}

.module-title {
  font-size: 17px;
  font-weight: 700;
  color: var(--ink);
  margin-bottom: 6px;
}

.module-desc {
  font-size: 13px;
  color: var(--ink-tertiary);
  margin-bottom: 16px;
  white-space: nowrap;
}

.module-arrow {
  margin-top: auto;
  font-size: 13px;
  font-weight: 600;
  color: var(--ink-secondary);
  transition: color 0.15s ease;
}

.module-card:hover .module-arrow {
  color: var(--ink);
}

/* =========================================
   3. 交互式沙箱评测终端
   ========================================= */
.demo-section {
  padding-top: 20px;
  padding-bottom: 60px;
}

.demo-header {
  text-align: center;
  margin-bottom: 32px;
}

.demo-eyebrow {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.8px;
  color: var(--ink-tertiary);
  margin-bottom: 8px;
}

.demo-title {
  font-size: 26px;
  font-weight: 700;
  letter-spacing: -0.4px;
  color: var(--ink);
  margin: 0 0 8px;
}

.demo-desc {
  font-size: 15px;
  color: var(--ink-secondary);
  margin: 0 auto;
  max-width: 580px;
}

.terminal-card {
  background: #141415;
  border-radius: var(--radius);
  border: 1px solid #28282a;
  color: #f1f1f1;
  overflow: hidden;
  box-shadow:
    0 20px 48px rgba(0, 0, 0, 0.14),
    0 2px 6px rgba(0, 0, 0, 0.08);
}

.terminal-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  background: #1b1b1d;
  border-bottom: 1px solid #28282a;
  gap: 12px;
}

.terminal-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
  font-weight: 600;
  color: #8c8c90;
  letter-spacing: 0.5px;
}

.live-dot {
  width: 7px;
  height: 7px;
  border-radius: 0;
  background: #40c057;
  box-shadow: 0 0 8px rgba(64, 192, 87, 0.7);
}

.terminal-tabs {
  display: flex;
  gap: 6px;
}

.terminal-tab {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  padding: 5px 14px;
  border-radius: var(--pill);
  color: #88888e;
  background: transparent;
  border: 1px solid transparent;
  cursor: pointer;
  transition: all 0.15s ease;
}

.terminal-tab:hover {
  color: #ffffff;
  background: #252528;
}

.terminal-tab.active {
  color: #ffffff;
  background: #2c2c30;
  border-color: #404046;
}

.terminal-meta-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 20px;
  background: #171719;
  border-bottom: 1px solid #232326;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  color: #66666e;
}

.file-name {
  color: #9da0a8;
}

.lang-tag {
  color: #7b7e87;
}

.terminal-body {
  padding: 20px;
  background: #111112;
  font-family: 'JetBrains Mono', ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 13.5px;
  line-height: 1.6;
  overflow-x: auto;
  min-height: 200px;
}

.code-view {
  margin: 0;
  color: #e2e4e9;
  white-space: pre;
}

.terminal-hud {
  padding: 16px 20px;
  background: #171719;
  border-top: 1px solid #262629;
}

.hud-status-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.verdict-tag {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  font-weight: 700;
  padding: 3px 12px;
  border-radius: var(--pill);
}

.verdict-tag.status-ac {
  background: rgba(47, 158, 68, 0.16);
  color: #40c057;
  border: 1px solid rgba(47, 158, 68, 0.35);
}

.verdict-tag.status-exam {
  background: rgba(34, 139, 230, 0.16);
  color: #4dabf7;
  border: 1px solid rgba(34, 139, 230, 0.35);
}

.verdict-detail {
  font-size: 12px;
  color: #8c8f98;
}

.hud-metric-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.metric-item {
  background: #1f1f22;
  border: 1px solid #2a2a2e;
  border-radius: 0;
  padding: 9px 12px;
}

.metric-title {
  font-size: 11px;
  color: #72747d;
  margin-bottom: 2px;
}

.metric-data {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
  font-weight: 600;
  color: #ffffff;
}

/* =========================================
   4. 底部 CTA 行动区
   ========================================= */
.cta-section {
  padding-top: 20px;
}

.cta-card {
  background: #141416;
  border-radius: var(--radius);
  border: 1px solid #26262a;
  padding: 44px 40px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 32px;
  color: #ffffff;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.08);
}

.cta-title {
  font-size: 26px;
  font-weight: 700;
  color: #ffffff;
  margin: 0 0 8px;
  letter-spacing: -0.4px;
}

.cta-desc {
  font-size: 15px;
  color: #9fa2ab;
  margin: 0;
  max-width: 520px;
  line-height: 1.6;
}

.cta-right {
  display: flex;
  gap: 14px;
  flex-shrink: 0;
}

.cta-card .btn-primary {
  background: #ffffff;
  color: #111111;
}

.cta-card .btn-primary:hover {
  background: #f0f0f0;
}

.cta-card .btn-ghost {
  border-color: #3e3e46;
  color: #e1e3e8;
}

.cta-card .btn-ghost:hover {
  background: #25252a;
  color: #ffffff;
}

/* =========================================
   5. 响应式适配
   ========================================= */
@media (max-width: 1080px) {
  .modules-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 860px) {
  .hero {
    grid-template-columns: 1fr;
    padding-top: 40px;
    padding-bottom: 50px;
    gap: 36px;
  }
  .hero-text {
    max-width: 100%;
    text-align: center;
  }
  .hero-sub br {
    display: none;
  }
  .hero-actions {
    justify-content: center;
  }
  .modules-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .cta-card {
    flex-direction: column;
    text-align: center;
    padding: 32px 24px;
  }
  .cta-right {
    width: 100%;
    flex-direction: column;
  }
  .cta-right .btn {
    width: 100%;
  }
}

@media (max-width: 540px) {
  .hero-title {
    font-size: 34px;
  }
  .modules-grid {
    grid-template-columns: 1fr;
  }
  .hud-metric-row {
    grid-template-columns: 1fr;
  }
  .terminal-tabs {
    display: none;
  }
}
</style>
