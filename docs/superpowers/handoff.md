# 电子衣橱 MVP 项目状态总结（Handoff）

> 用途：在新 Codex 会话中，把本文档作为背景信息粘贴进去，即可无缝接续当前进度。
> 状态：T1-T28 已完成，T29 待开始（pages/index/index.vue 首页 + Vitest）
> 最后提交：`33e209d`（T28）

## 1. 项目概述

- 名称：电子衣橱（Digital Closet）MVP
- 定位：个人使用，单用户，自托管，跑在用户自己的 PC 上
- 范围：衣物录入 + 搭配组合 + 日历规划 + 穿着统计 + 搭配导出分享
- 不做：多用户 / 社交、AI 打标、电商、推送

## 2. 技术栈

| 层 | 选型 |
|---|---|
| 前端 | uni-app（Vue 3 + TypeScript），uView Plus，Pinia，Vite |
| 前端测试 | Vitest + @vue/test-utils（仅核心组件 + 页面） |
| 后端 | Java 21 + Spring Boot 3.3.4 + MyBatis-Plus 3.5.5（见踩坑 #7） |
| 后端测试 | JUnit 5 + Mockito + Testcontainers |
| 数据库 | PostgreSQL 16 |
| 对象存储 | MinIO（latest） |
| 构建 | Maven（不用 Flyway，schema 走 `schema.sql` + `data.sql` 启动加载） |
| 部署 | Docker Compose（dev 与 prod 两套） |

## 3. 当前进度

### 已完成（36 个 commit，含 7 个 docs）

| 任务 | 提交 | 内容 |
|------|------|------|
| T1  | `ad0ebdd` | chore: project skeleton and gitignore |
| T2  | `3157e0e` | chore: docker compose for local dev (PG + MinIO) |
| T3  | `bbcb1f5` | feat(db): schema and seed data for MVP |
| T4  | `0ec6ae7` | feat(backend): spring boot project init with PG + MinIO deps |
| T5  | `4b3f0e5` | feat(common): Result wrapper and global exception handler |
| T6  | `3ea8146` | feat(storage): MinIO storage service（本地 MinIO + TestPropertySource，见踩坑 #8） |
| T7  | `b981c3f` | feat(backend): MyBatis-Plus pagination and CORS config |
| T8  | `829a96b` | feat(category): CRUD with MyBatis-Plus |
| T9  | `d38e833` | feat(tag): CRUD with MyBatis-Plus |
| T10 | `282334d` | feat(clothing): Clothing 实体 + Mapper + 元数据填充器 |
| T11 | `95f4293` | feat(clothing): ClothingService + DTO + 11 个单元测试 |
| T12 | `dde51fd` | feat(clothing): ClothingController + IT（4 个集成测试） |
| T13 | `385ed90` | feat(clothing): 图片上传/下载（ClothingImage + ImageController + IT） |
| T14 | `122280d` | feat(outfit): entity and mappers |
| T15 | `786cdf8` | feat(outfit): T15 OutfitService + DTO + 8 个 Mockito 单测 |
| T16 | `5b820b7` | feat(outfit): T16 OutfitController + 集成测试 |
| T17 | `a4057fd` | feat(wear-log): T17 手动补登端点 |
| T18 | `ee19c54` | feat(calendar): T18 CalendarEntry 实体 + WearLogSyncService + CalendarService |
| T19 | `7982e5e` | feat(calendar): T19 CalendarController + 集成测试 |
| T20 | `8ff5d67` | feat(stats): T20 StatsService + DTO + 单测 |
| T21 | `412ce21` | feat(stats): T21 StatsController + 集成测试 |
| T22 | `5030995` | feat(frontend): uni-app init with uView Plus + Vitest |
| T23 | `d0c92af` | feat(frontend): OpenAPI client and API wrapper |
| T24 | `308184a` | feat(frontend): pinia stores and routing |
| T25 | `2fe8116` | feat(frontend): ClothingCard with image fallback |
| T26 | `789a285` | feat(frontend): ImageUploader component |
| T27 | `fd03739` | feat(frontend): FilterBar / CategoryPicker / TagPicker components |
| T28 | `33e209d` | feat(frontend): OutfitCanvas with sort-by-order |
| -   | `8b4072f` | docs: project handoff summary for session continuity |
| -   | `b484530` | docs: update handoff with T5 completion |
| -   | `3891081` | docs: update handoff with T6+T7 completion |
| -   | `e08c749` | docs: update handoff with T8 completion |
| -   | `ccf141e` | docs: update handoff with T9 completion |
| -   | `0c1602b` | docs: update handoff with T27 completion |
| -   | `6839fdf`/`a861a4c`/`6c45df6` | 越位的 ClothingForm + ClothingList（已在 `fd03739` 中删除） |

### 待办

**当前：T29**：pages/index/index.vue 首页（统计概览 + Vitest，与 plan T29 对齐）

T28 刚完成：
- `frontend/src/components/OutfitCanvas.vue`：按 sortOrder 升序显示 tile 网格（image + name）；缺失 mainImageKey 用 `/static/placeholder.png` 占位；空 items 显示提示
- 6/6 Vitest 全过（render / sort / default 0 / reactive update / empty / placeholder）
- T27+T28 总计 27/27 全绿

T29 任务（plan 第 4 节 T29）：
- `frontend/src/pages/index/index.vue`：调用 `api.stats.overview()` 拉统计（衣物数 / 搭配数 / 本月穿次数），三张 up-card 展示；两个 up-button 跳到 `/pages/closet/index` 和 `/pages/calendar/index`
- 3 个 Vitest 用例：mount 后 stats 默认 0/0/0、`api.stats.overview` mock 返回正确数字能正确显示、按钮点击触发 `uni.navigateTo`（需要在 setup file 或测试里 stub `uni.navigateTo`）
- `pages.json` 已有首页路径（`pages/index/index`），无需改路由
- 提交信息：`feat(frontend): home page with stats overview`

下一阶段：T29-T36
- T29: pages/index/index.vue 首页（统计概览 + 跳转）
- T30: pages/closet + pages/clothing-form + pages/clothing-detail
- T31: pages/outfits + pages/outfit-form + pages/outfit-detail
- T32: pages/calendar
- T33: pages/stats + pages/settings
- T34-T36: 部署（Backend Dockerfile + 生产 compose + README + 烟测）

### 踩坑提示（重要，subagent 必须知道）

- PowerShell 中 `Set-Content` 不带 -Encoding 默认 GBK，中文会乱码；必须用 `... | Out-File -Encoding UTF8` 写文件
- `git commit -m "/..."` 某些内容会进交互编辑，必须用 `git commit -F <file>` 传 commit message
- vitest 中 `wrapper.find('img')` **找不到**全局注册的组件（全局组件渲染后 tag 名变成实际元素名）；查找组件实例用 `wrapper.findComponent({ name: 'UniXxx' })`；查找 DOM 元素用 `wrapper.html().toContain('xxx')`
- `wrapper.html().toContain('xxx')` 是稳的字符串断言
- 组件 name 用 `UniXxx` 前缀避开 HTML 原生标签冲突
- **vitest `stubs: true` 默认不渲染 slot** —— 见踩坑 #10
- 不要修改任何 backend 文件 / handoff.md / plan 文档 / 别人的子任务产物
- 用中文汇报

## 4. 工作目录与关键路径

```
C:\Users\huchang\Documents\Codex\2026-07-01\new-chat\
├── backend/                                # Spring Boot 工程
│   ├── pom.xml                             # Spring Boot 3.3.4 + Java 21 + MyBatis-Plus 3.5.5
│   ├── src/main/java/com/closet/
│   │   ├── ClosetApplication.java
│   │   ├── common/                         # T5
│   │   │   ├── Result.java
│   │   │   ├── ApiException.java
│   │   │   └── GlobalExceptionHandler.java
│   │   ├── config/                         # T6 + T7
│   │   │   ├── MinioConfig.java
│   │   │   ├── MybatisPlusConfig.java
│   │   │   └── CorsConfig.java
│   │   └── storage/                        # T6
│   │       ├── StorageService.java
│   │       └── MinioStorageService.java
│   ├── src/test/java/com/closet/
│   │   ├── common/ResultTest.java
│   │   └── integration/MinioStorageServiceIT.java
│   └── src/main/resources/
│       ├── application.yml                 # mode: always（见踩坑 #3）
│       ├── application-dev.yml
│       ├── schema.sql                      # 10 张表
│       └── data.sql                        # 5+5 默认分类
├── deploy/
│   └── docker-compose.dev.yml              # PG + MinIO + minio-init
├── frontend/
│   ├── src/
│   │   ├── components/                     # T25-T28
│   │   │   ├── ClothingCard.vue/.test.ts   # T25
│   │   │   ├── ImageUploader.vue/.test.ts  # T26
│   │   │   ├── FilterBar.vue/.test.ts      # T27
│   │   │   ├── CategoryPicker.vue/.test.ts # T27
│   │   │   ├── TagPicker.vue/.test.ts      # T27
│   │   │   └── OutfitCanvas.vue/.test.ts   # T28
│   │   ├── pages/                          # T29 起
│   │   │   └── index/index.vue             # 占位 Hello，T29 替换
│   │   ├── api/                            # schema.d.ts + index.ts
│   │   └── stores/                         # pinia
│   ├── vitest.config.ts
│   └── vitest.setup.ts                     # 全局组件 / stub
├── docs/
│   └── superpowers/
│       ├── specs/2026-07-01-digital-closet-design.md
│       ├── plans/2026-07-01-digital-closet-mvp.md
│       └── handoff.md                      # 本文件
├── .gitignore
└── README.md
```

## 5. 环境配置

### Java / Maven（PATH 已修好）

- Java 21: `C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot`
  - `JAVA_HOME`（user + machine）已设
- Maven 3.9.9: `D:\apache-maven-3.9.9`
  - `MAVEN_HOME`（machine）已改为 3.9.9
- 旧的 Java 8 和 Maven 3.5.4 已从 system PATH 移除

### Docker（已安装并运行）

- Docker Desktop 29.6.1
- 三个容器在跑（`docker ps`）：
  - `closet-pg-dev`（postgres:16-alpine, healthy, 5432）
  - `closet-minio-dev`（minio/minio, healthy, 9000/9001）
  - `deploy-minio-init-1`（exited(0)，bucket `closet-images` 已建）
- IT 测试用隔离 bucket `closet-it`，`@BeforeEach` 建桶 + `@AfterEach` 清对象

### mvn 命令

计划里用 `./mvnw` 但项目还没装 Maven Wrapper，**所有 mvn 命令用 `mvn`，不是 `./mvnw`**。

## 6. 已知问题和踩坑记录

### 踩坑 #1：Subagent 状态报告不可信

Chandrasekhar（T4）报 BLOCKED 说"环境不对、没编译、没提交"，但实际：找到 maven、编译成功、提交了 `0ec6ae7`、启动过应用。

**教训**：永远自己 `git log` / `git status` 验证一遍。

### 踩坑 #2：Subagent 容易卡死在 sleep 操作

Wegener、T2 Poincare 都卡在 `Start-Sleep -Seconds 30`。

**教训**：派 subagent 时显式禁止 `Start-Sleep`、循环等待、置死。

### 踩坑 #3：`spring.sql.init.mode: always` 在 schema 已存在时失败

应用启动报 `ERROR: relation "category" already exists`。

**当前 workaround**：每次重启应用前先 `DROP SCHEMA public CASCADE; CREATE SCHEMA public;`。

**长期方案**（T13 部署前再处理）：改成 `mode: never`，靠手动初始化；或 profile 区分。

### 踩坑 #4：apply_patch 格式

每行内容前必须加 `+` 前缀，包括空行和注释行。用 PowerShell here-string 写大文件时用 `Out-File -Encoding UTF8`。

### 踩坑 #5：Codex 启动子进程用缓存的 system PATH

改 system 环境变量后，当前 Codex 进程内的子进程不会立刻看到。需要重启 Codex。

### 踩坑 #6：vitest `stubs: true` 默认不渲染 slot

`global: { stubs: { 'up-checkbox-group': true, 'up-checkbox': true } }` 简写生成的 stub 是纯标签占位，不传 slot，导致父组件 `v-for` 渲染出的子元素全部丢失。

**正确做法**：在 `vitest.setup.ts` 里写自定义 stub 组件，用 `h()` 透传 attrs 和 slots.default：

```ts
const upCheckboxStub = defineComponent({
  name: 'UpCheckboxStub',
  props: ['label', 'value'],
  setup(props, { slots }) {
    const children = [props.label, slots.default?.()].filter(Boolean);
    return () => h('label', { class: 'up-checkbox-stub', 'data-value': String(props.value ?? '') }, children);
  },
});
config.global.components['up-checkbox'] = upCheckboxStub;
```

测试里不用 `global.stubs`，断言用 `wrapper.findAllComponents({ name: 'UpCheckboxStub' })` 或 `expect(html).toContain('xxx')`。

### 踩坑 #7：MyBatis-Plus 3.5.9 删了 `PaginationInnerInterceptor`

3.5.9 的 jar 里没有分页拦截器（拆到独立的 `mybatis-plus-jsqlparser`）。

**当前 workaround**：`pom.xml` 把 `mybatis-plus.version` 钉到 `3.5.5`。

### 踩坑 #8：Testcontainers + Docker Desktop 4.80.0 不兼容

`@Testcontainers MinIOContainer` 起不来（dockerjava 拿不到 `/info` 响应）。

**当前 workaround**：`MinioStorageServiceIT` 连 docker-compose 启动的本地 MinIO，`@TestPropertySource` 切到隔离 bucket `closet-it`。

### 踩坑 #9：MyBatis-Plus `DbType` 枚举名是 `POSTGRE_SQL`（带下划线）

`POSTGRESQL`（无下划线）编译过但运行时报 `Unresolved compilation problem`，因为 Spring Boot 用 JDT 编译器运行时校验。

## 7. Subagent 派发模板

派 subagent 经常卡死，建议用以下模式：

1. prompt 里显式列出严格约束：
   - 不用 `Start-Sleep`、循环等待、置死
   - apply_patch 每行加 `+` 前缀
   - mvn/psql/docker exec 是同步的，直接等返回
   - 不要多重验证
   - 用中文汇报
2. model 用默认，reasoning_effort 用 `low`
3. type 用 `worker`
4. 3 分钟轮询
5. 派发完自己也要独立验证（踩坑 #1）

## 8. 工作流程约定

- 分支：直接在 main 上工作（用户明确同意）
- 提交：每个 Task 一个 code commit（message 用任务规格里的格式）+ 一个 docs commit（更新 handoff.md）
- 用户偏好：
  - 报告用中文
  - subagent 跑的时候每 3 分钟汇报一次进度
  - 简单任务本地做更快，不用 subagent
  - 复杂任务派 subagent
  - 每个子任务完成都更新 handoff.md

## 9. 下一步操作

**直接做 T29**：写 pages/index/index.vue + .test.ts，然后跑 vitest 验证，最后 code commit + docs commit。

## 10. 关键文件位置速查

| 用途 | 路径 |
|------|------|
| 设计文档 | `docs/superpowers/specs/2026-07-01-digital-closet-design.md` |
| 实施计划 | `docs/superpowers/plans/2026-07-01-digital-closet-mvp.md` |
| 数据库 schema | `backend/src/main/resources/schema.sql` |
| 数据库种子数据 | `backend/src/main/resources/data.sql` |
| Docker Compose | `deploy/docker-compose.dev.yml` |
| Spring Boot 配置 | `backend/src/main/resources/application.yml` |
| Maven 配置 | `backend/pom.xml` |
| Result/ApiException/Handler | `backend/src/main/java/com/closet/common/` |
| MinioConfig/MinioStorageService | `backend/src/main/java/com/closet/config/` + `storage/` |
| MybatisPlus/CORS config | `backend/src/main/java/com/closet/config/` |
| Result 测试 | `backend/src/test/java/com/closet/common/ResultTest.java` |
| MinioStorageService IT | `backend/src/test/java/com/closet/integration/MinioStorageServiceIT.java` |
| API client | `frontend/src/api/index.ts` |
| API schema | `frontend/src/api/schema.d.ts` |
| Pinia stores | `frontend/src/stores/` |
| Frontend 组件 | `frontend/src/components/` |
| Frontend 页面 | `frontend/src/pages/` |
| vitest setup | `frontend/vitest.setup.ts` |
| vitest config | `frontend/vitest.config.ts` |
