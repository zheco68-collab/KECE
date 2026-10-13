# KECE — 综合编程与技能评测平台

一站式技术能力评估平台，整合算法 OJ、数据库 OJ、前端 AI 智能评测与标准客观题，服务于企业招聘笔试、高校教学与技术社区刷题场景。

## 产品特性

- 算法 OJ：支持 C/C++、Java、Python、Go、Rust 等多语言，完整判题状态机（AC/WA/TLE/MLE/RE/CE）
- 数据库 OJ：MySQL 8.0 方言，SELECT 与 DML 两类题型。建表语句、标准答案、DML 校验查询与行序要求属于**题目级**，测试点只描述「一套数据」，不照搬算法题的 input/expected 结构
- 数据库判题：在隔离判题库的一次性临时库 `judge_{提交号}_{测试点号}` 中执行，跑完即 DROP；结果集按列（名严格/型宽容差）+ 单元格（浮点宽容差）+ 可选严格行序比对。逐用例比对结果只对**样例**测试点回显（样例数据本就随题面公开），隐藏测试点仅回报状态，避免由比对结果反推数据与答案
- 前端 AI 评测：Playwright 无头浏览器 DOM 断言 + 多模态 LLM 视觉/代码审计，输出 UI 还原度、代码质量结构化评分
- 客观题：单选、多选（漏选半分）、填空（精确/忽略大小写/正则），支持 Markdown + 代码高亮 + LaTeX
- 考试模式：ACM（即时反馈+罚时排名）与 OI（交卷后统一评测+部分分）
- 防作弊：切屏计数、禁右键/复制、（后续）AST 代码查重

## 技术栈

| 层 | 选型 |
|---|---|
| 前端 | Vue 3 + Vite + TypeScript · Monaco 编辑器 · Pinia · Vue Router |
| 后端 | Java 17+ · Spring Boot 3 · MyBatis · Flyway |
| 数据库 | MySQL 8 · Redis（状态缓存 + 消息队列）· 独立判题 MySQL（与业务库隔离） |
| 判题机 | Go · Docker 强隔离沙箱（Cgroups v2 + Namespace + Seccomp）；SQL 判题直连判题 MySQL |
| 对象存储 | MVP 本地磁盘 / MinIO |
| 部署 | 单机/云服务器直接部署（MVP） |

## 核心架构

```
┌─────────────┐   HTTPS/JWT   ┌──────────────────┐
│  Vue 3 SPA  │ ────────────▶ │  Spring Boot API │──▶ MySQL
└─────────────┘               │  (统一响应包装)     │──▶ Redis（状态缓存）
                              └────────┬─────────┘
                                       │ 提交任务（消息队列）
                                       ▼
                              ┌──────────────────┐
                              │  Go 判题机 Worker │──▶ Docker 沙箱（编译/运行）
                              └──────────────────┘      └─▶ 写回评测结果 → Redis
```

异步评测链路：前端提交 → 后端入库+压队列 → Go 判题机拉取 → Docker 沙箱编译运行 → 状态写 Redis → 前端轮询（1s）获取结果。

数据库 OJ 复用同一条链路，但使用**独立 Redis 键**（`judge:sql:*` / `dbsub:*`）与**独立判题 MySQL 实例**：判题机为每个测试点创建一次性临时库 `judge_{提交号}_{测试点号}`，跑完即 DROP。用户 SQL 只能读到该实例的临时库，无法触及 `kece` 业务库。

## 状态机

| 类型 | 状态 |
|---|---|
| 非终态（继续轮询） | `PENDING` → `COMPILING` → `JUDGING` → `AI_REVIEWING`（前端特有） |
| 终态（停止轮询） | `ACCEPTED` · `WRONG_ANSWER` · `TIME_LIMIT_EXCEEDED` · `MEMORY_LIMIT_EXCEEDED` · `RUNTIME_ERROR` · `COMPILE_ERROR` · `SYSTEM_ERROR` |

## 开发路线

| 阶段 | 目标 | 状态 |
|---|---|---|
| Phase 1（MVP） | 注册/题库/算法沙箱评测/客观题 | 已完成（含数据库 OJ、后台管理 P0–P6） |
| Phase 2 | 数据库 OJ + 组卷倒计时考试 + 消息队列 | 进行中（数据库 OJ/消息队列已完成，组卷考试开发中） |
| Phase 3 | 前端多文件编辑器 + Playwright + LLM AI 评测 | 待开始 |
| Phase 4 | 防作弊 + 数据大屏 + 判题集群扩容 | 待开始 |

## 项目结构

```
KECE/
├── frontend/      # Vue 3 + Vite + TypeScript SPA
├── backend/       # Spring Boot 3 + Java 17 API 服务
├── judger/        # Go 判题机 Worker + Docker 沙箱
├── deploy/        # Docker Compose、启动脚本
├── docs/          # 架构决策 (ADR)、文档
└── .github/       # CI/CD 工作流
```

## 本地开发

### 前置依赖
- Node.js 20+
- Java 17+
- Go 1.22+
- Docker Desktop（判题机拉沙箱容器 + judge-mysql/redis 容器）
- 本机已运行的 MySQL 8.0（业务库 `kece`，账号 `kece/kece123`）

### 一键启动（推荐）
启动脚本会确保 `judge-mysql`（3307）与 `redis`（6379）两个容器在跑、编译判题机，
然后在后台拉起后端、判题机、前端三个本地进程（已运行的自动跳过，日志写入 `.logs/`）：
```bash
# Windows PowerShell
deploy\start.ps1
# Linux/macOS
./deploy/start.sh
```
停止（容器与原生 MySQL 保留）：
```bash
deploy\stop.ps1    # 或 ./deploy/stop.sh
```

> 业务 MySQL 不在脚本内启动，请保持本机 MySQL 服务运行在 3306。

### 手动分步启动（便于排查）
依赖容器可单独拉起：
```bash
docker run -d --name kece-judge-mysql --restart unless-stopped \
  -p 3307:3306 -e MYSQL_ROOT_PASSWORD=judge123 mysql:8.0 \
  --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci
docker run -d --name kece-redis --restart unless-stopped -p 6379:6379 redis:7-alpine
```
后端：
```bash
cd backend
# dev profile 开启演示数据：种子账号 admin/admin123、teacher/teacher123 与示例题目/作答
$env:SPRING_PROFILES_ACTIVE = "dev"   # Windows PowerShell；bash 用 export SPRING_PROFILES_ACTIVE=dev
./mvnw spring-boot:run    # Windows 用 mvnw.cmd spring-boot:run
```
- API: http://localhost:8080/api/v1
- Swagger: http://localhost:8080/api/v1/swagger-ui.html
- 演示数据开关 `kece.demo-data.enabled` 默认关（生产不播种默认口令账号），仅 dev profile 开启

前端：
```bash
cd frontend
npm install
npm run dev
```
- 页面: http://localhost:5173

判题机（Windows 需先把 go 加入 PATH 或设 GOROOT，默认值见 start.ps1）：
```bash
cd judger
go build -o judger.exe ./cmd/judger    # Linux/macOS 去掉 .exe
REDIS_ADDR=127.0.0.1:6379 JUDGE_MYSQL_HOST=127.0.0.1 JUDGE_MYSQL_PORT=3307 \
JUDGE_MYSQL_USER=root JUDGE_MYSQL_PASSWORD=judge123 ./judger.exe
```

> **判题机沙箱镜像**：判题机通过宿主机 Docker daemon 拉起沙箱容器，首次判题会按需拉取
> `gcc:13`、`eclipse-temurin:17-jdk`、`python:3.12-slim` 三个镜像。建议预先拉取以降低首次判题延迟：
> ```bash
> docker pull gcc:13 eclipse-temurin:17-jdk python:3.12-slim
> ```
> 判题工作目录通过 `docker-compose.yml` 中的 `/workspace` 卷在宿主机与判题机之间共享，
> 供沙箱容器挂载编译/运行产物；请勿改动该挂载路径，否则沙箱将无法读取到源码。

> **判题 MySQL 连接**：判题机通过 `JUDGE_MYSQL_HOST` / `JUDGE_MYSQL_PORT` / `JUDGE_MYSQL_USER` /
> `JUDGE_MYSQL_PASSWORD` 连接判题 MySQL（容器内默认 `judge-mysql:3306`，本机直跑默认
> `127.0.0.1:3307`）。该实例必须与业务库隔离——判题机以 root 身份在其上建库建表，
> 用户 SQL 也在其中执行，绝不能指向 `kece` 业务库。

### 数据库 OJ 出题
数据库题目暂未提供前端出题入口（不与算法题的用例表单共用一套交互），由 `TEACHER` / `ADMIN`
账号调接口创建；测试点数据可随题面公开的标记为样例：

```jsonc
// POST /api/v1/db-questions    Authorization: Bearer <TEACHER/ADMIN token>
{
  "title": "查询研发部员工",
  "description": "返回 `emp` 表中 `dept = 'RD'` 员工的 id 与 name，按 id 升序",
  "type": "SELECT",              // SELECT 查询 / DML 数据操作
  "schemaSql": "CREATE TABLE emp (id INT PRIMARY KEY, name VARCHAR(32), dept VARCHAR(16));",
  "referenceSql": "SELECT id, name FROM emp WHERE dept = 'RD' ORDER BY id",
  "verifySql": null,             // DML 题填判分用的校验 SELECT，SELECT 题留空
  "orderSensitive": true,        // 是否强制行序一致
  "testCases": [
    { "seedSql": "INSERT INTO emp VALUES (1,'Alice','RD'),(2,'Bob','HR');", "isSample": 1 },
    { "seedSql": "INSERT INTO emp VALUES (3,'Cara','RD');",                "isSample": 0 }
  ]
}
```

> 判分时每个测试点都用题目的 `schemaSql` 建表、灌入该测试点的 `seedSql`：SELECT 题直接跑标准答案与
> 用户 SQL 取结果集比对；DML 题先跑标准答案再跑 `verifySql` 得期望结果，重置数据后跑用户 SQL 再跑
> `verifySql` 得实际结果。`referenceSql` 与 `verifySql` 只对 `TEACHER` / `ADMIN` 下发。

## 项目文档

| 文档 | 说明 |
|---|---|
| [需求文档.md](./需求文档.md) | PRD：角色权限、核心模块、非功能需求、分期规划 |
| [PRODUCT.md](./PRODUCT.md) | 产品规格：定位、技术栈、用户角色、产品原则 |
| [全局接口规范与公共约定.md](./全局接口规范与公共约定.md) | 接口契约：统一响应、鉴权、异步状态机、核心 API |
| [开发计划.md](./开发计划.md) | 实施计划：架构决策、WBS 任务分解、工程规范、风险管理 |

## License

私有项目，未经授权不得商用。
