# 电子衣橱 MVP 项目状态总结（Handoff）

> 用途：在新 Codex 会话中，把本文档作为背景信息粘贴进去，即可无缝接续当前进度。
> 状态：T1-T36 全部完成 + 端到端验证通过（后端 / 前端 / 数据库 / 对象存储均可达）。

## 1. 项目概述

- 名称：电子衣橱（Digital Closet）MVP
- 路径：`C:\Users\huchang\Documents\Codex\2026-07-01\new-chat`
- 定位：个人使用，单用户，自托管，跑在用户自己的 PC 上
- 范围：衣物录入 + 搭配组合 + 日历规划 + 穿着统计 + 搭配导出分享
- 不做：多用户 / 社交、AI 打标、电商、推送

## 2. 技术栈

| 层 | 选型 |
|---|---|
| 前端 | uni-app（Vue 3 + TypeScript），uView Plus，Pinia，Vite |
| 前端测试 | Vitest + @vue/test-utils（仅核心组件 + 页面） |
| 后端 | Java 21 + Spring Boot 3.3.4 + MyBatis-Plus 3.5.5（钉死，见踩坑 #7） |
| 后端测试 | JUnit 5 + Mockito + Spring MockMvc（unit + IT） |
| 数据库 | PostgreSQL 16 |
| 对象存储 | MinIO（latest） |
| 构建 | Maven（不用 Flyway，schema 走 `schema.sql` + `data.sql` 启动加载） |
| 部署 | Docker Compose（dev 与 prod 两套） |

## 3. 当前进度

### T1-T36 全部完成（最终 commit `f59032d`）

完整 commit 链：

```
f59032d docs: final handoff for T36 - project MVP complete
746548b docs: initial README
0ee428a chore(deploy): production docker compose
c2ee169 chore(backend): multi-stage Dockerfile
0dfaac2 T30 + T31 + T33 page 工作 (衣橱 + 搭配 + 统计 + 设置 + Vitest)
23bcb10 feat(frontend): home page with stats overview
33e209d feat(frontend): OutfitCanvas with sort-by-order
6839fdf feat(frontend): ClothingForm component
a861a4c feat(frontend): ClothingList component
fd03739 feat(frontend): FilterBar / CategoryPicker / TagPicker components
789a285 feat(frontend): ImageUploader component
2fe8116 feat(frontend): ClothingCard with image fallback
…（T1-T24 后端 + 初始化的 commit 链）
```

每个 Task 的实现产物：
- 后端 8 个 controller / 5 个 service + impl / 10 张表 / 5+5 默认分类
- 前端 6 个组件 + 10 个 page + 3 个 store
- 部署：`backend/Dockerfile`（多阶段 maven → jre）+ `deploy/docker-compose.yml`（prod）+ `deploy/.env.example` + `README.md`

### 本次会话的"完成度核查 + 修复"工作（未 commit）

| 改动 | 文件 | 说明 |
|---|---|---|
| plan.md 全勾 | `docs/superpowers/plans/2026-07-01-digital-closet-mvp.md` | 158 个 Step checkbox + 9 个验收项全部勾上 |
| IT 测试修复 | `backend/src/test/java/com/closet/integration/StatsControllerIT.java` | 加 `TRUNCATE ... RESTART IDENTITY CASCADE` 让测试自包含，避开 `mode=always` 不重置 clothing 的历史数据累积 |
| CategoryPicker 数据流修复 | `frontend/src/components/CategoryPicker.vue` | 双向 ref + watch → 单向 computed，组件内不持有状态，消除 vitest 偶发的 "Maximum recursive updates" unhandled rejection |
| TagPicker 数据流修复 | `frontend/src/components/TagPicker.vue` | 同上 |
| handoff 更新 | 本文件 | 重写为最新状态 |

## 4. 端到端验证（实测）

| 项 | 状态 | 证据 |
|---|---|---|
| 后端 HTTP | ✅ | `GET http://localhost:8080/api/v1/categories` → 200 OK，categories JSON |
| 前端 H5 | ✅ | `GET http://localhost:5174/` → 200 OK，Vite dev HTML |
| PostgreSQL | ✅ | docker `closet-pg-dev` Up + healthy |
| MinIO | ✅ | docker `closet-minio-dev` Up + healthy |
| 后端单测 | ✅ | `mvn test` → 34/34 passed（ResultTest + ClothingServiceTest + OutfitServiceTest + StatsServiceTest + WearLogSyncServiceTest） |
| 后端 IT | ✅ | `mvn test -Dtest='*IT'` → 24/24 passed（Calendar/Category/Clothing/ClothingImage/Minio/Outfit/Stats/Tag/WearLog ControllerIT） |
| 前端 Vitest | ✅ | `npx vitest run` → 76/76 passed（16 文件），无 unhandled rejection |
| 前端 sass 依赖 | ✅ | `frontend/package.json` 有 `"sass": "^1.101.0"` |

## 5. 计划文档完成度（plan.md）

- 36 个 Task 共 158 个 Step checkbox ✅ 全部已勾
- 验收清单 9 项 ✅ 全部已勾
- 已 commit 的 hash 待下一次 commit 时一起补

## 6. 工作目录与关键路径

```
C:\Users\huchang\Documents\Codex\2026-07-01\new-chat\
├── backend/                                # Spring Boot 工程
│   ├── pom.xml                             # Spring Boot 3.3.4 + Java 21 + MyBatis-Plus 3.5.5
│   ├── Dockerfile                          # 多阶段 maven → jre
│   └── src/main/java/com/closet/...        # common / config / controller / dto / entity / mapper / service / storage
├── deploy/
│   ├── docker-compose.dev.yml              # PG + MinIO + minio-init
│   ├── docker-compose.yml                  # prod: pg + minio + minio-init + backend
│   └── .env.example                        # prod 密码模板
├── frontend/
│   ├── src/
│   │   ├── components/                     # 6 组件
│   │   ├── pages/                          # 10 page
│   │   ├── api/                            # schema.d.ts + index.ts
│   │   └── stores/                         # pinia
│   └── vitest.setup.ts                     # 全局组件 / stub
├── docs/
│   └── superpowers/
│       ├── specs/2026-07-01-digital-closet-design.md
│       ├── plans/2026-07-01-digital-closet-mvp.md  # 全勾
│       └── handoff.md                      # 本文件
├── .gitignore
└── README.md
```

## 7. 环境配置

### Java / Maven

- Java 21: `C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot`
- Maven 3.9.9: `D:\apache-maven-3.9.9`
- 项目里没装 Maven Wrapper，所有 mvn 命令用 `mvn`，不是 `./mvnw`

### Docker

- Docker Desktop 29.6.1
- dev 容器：`closet-pg-dev` (postgres:16-alpine, healthy, 5432) + `closet-minio-dev` (minio/minio, healthy, 9000/9001) + `deploy-minio-init-1`（已退场，已建 bucket `closet-images`）
- IT 测试用隔离 bucket `closet-it`，`@BeforeEach` 清桶、`@AfterEach` 清对象

## 8. 已知问题与踩坑

### 踩坑 #1：`spring.sql.init.mode: always` 不重置历史数据

- 行为：`schema.sql` 用 `CREATE TABLE IF NOT EXISTS`，`data.sql` 用 `INSERT INTO category VALUES (1, ...)`
- 多次启动后 category 表累积（90 行），clothing 也保留之前的数据
- 长期方案：改成 `mode: never` 手动初始化，或 profile 区分 dev/ci
- **本次修复**：IT 测试在 `@BeforeEach` 用 `TRUNCATE TABLE wear_log, outfit_item, clothing_category, clothing_tag, calendar_entry, outfit, clothing RESTART IDENTITY CASCADE` 把测试用的表清干净

### 踩坑 #2：CategoryPicker / TagPicker 双向 watch 在 vitest 偶发递归更新警告

- 旧实现：`ref + watch(selected, emit) + watch(props.modelValue, set selected)`，组件内有自己的 state
- vitest happy-dom 在某些时点 flush watch 时偶尔触发 "Maximum recursive updates exceeded"
- **本次修复**：改成 `computed({ get, set })` 单向数据流，组件不持有状态，警告消失

### 踩坑 #3：MyBatis-Plus 钉死 3.5.5

3.5.9 的 jar 里没有分页拦截器（拆到独立的 `mybatis-plus-jsqlparser`），`pom.xml` 把 `mybatis-plus.version` 钉到 `3.5.5`。

### 踩坑 #4：MyBatis-Plus `DbType` 枚举名是 `POSTGRE_SQL`（带下划线）

`POSTGRESQL`（无下划线）编译过但运行时报 `Unresolved compilation problem`。

### 踩坑 #5：Testcontainers + Docker Desktop 4.80.0 不兼容

`@Testcontainers MinIOContainer` 起不来（dockerjava 拿不到 `/info` 响应）。`MinioStorageServiceIT` 连 docker-compose 启动的本地 MinIO，`@TestPropertySource` 切到隔离 bucket。

### 踩坑 #6：vitest `stubs: true` 默认不渲染 slot

`global: { stubs: { 'up-checkbox-group': true } }` 简写生成的 stub 是纯标签占位，不传 slot。需要在 `vitest.setup.ts` 写自定义 stub 组件（用 `h()` 透传 slots.default）。

### 踩坑 #7：vite dev 在 Windows 上 sass 缺失

`vite-plugin-uni` 默认要求 sass。如果 `package.json` 没装 `sass`，首次 `npm run dev:h5` 会报 `[plugin:vite:css] Preprocessor dependency "sass" not found`。装 `"sass": "^1.101.0"` 后正常。

## 9. 下一步操作

项目 MVP 已经完成、所有测试通过、服务可达。建议用户：
1. 在浏览器手动走一遍 UI 流程（录入衣物 → 上传图片 → 列表显示 → 创建搭配 → 日历安排 → 统计页 cost-per-wear → 搭配详情分享）
2. 如果要部署到生产，参考 README + `deploy/.env.example` 填密码后 `cd deploy && docker compose up -d --build`

如果后续要新增功能（比如多用户、AI 标签、图片去重），在这个 MVP 基础上继续迭代即可。

## 10. 关键文件位置速查

| 用途 | 路径 |
|------|------|
| 设计文档 | `docs/superpowers/specs/2026-07-01-digital-closet-design.md` |
| 实施计划（已全勾） | `docs/superpowers/plans/2026-07-01-digital-closet-mvp.md` |
| 数据库 schema | `backend/src/main/resources/schema.sql` |
| 数据库种子数据 | `backend/src/main/resources/data.sql` |
| dev Docker Compose | `deploy/docker-compose.dev.yml` |
| prod Docker Compose | `deploy/docker-compose.yml` |
| 后端 Dockerfile | `backend/Dockerfile` |
| Spring Boot 配置 | `backend/src/main/resources/application.yml` |
| Maven 配置 | `backend/pom.xml` |
| API client | `frontend/src/api/index.ts` |
| API schema | `frontend/src/api/schema.d.ts` |
| Pinia stores | `frontend/src/stores/` |
| 前端组件 | `frontend/src/components/` |
| 前端页面 | `frontend/src/pages/` |
| vitest setup | `frontend/vitest.setup.ts` |
| vitest config | `frontend/vitest.config.ts` |
