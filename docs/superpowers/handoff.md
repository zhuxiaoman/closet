# 电子衣橱 MVP 项目状态总结（Handoff）

> 用途：在新 Codex 会话中，把本文档作为背景信息粘贴进去，即可无缝接续当前进度。
> 截止：T1-T18 已完成，T19 待开始（CalendarController + IT）
> 最后提交：`ee19c54`（T18）

## 1. 项目概述

- 名称：电子衣橱（Digital Closet）MVP
- 定位：个人使用，单用户，自托管在用户自己的 PC 上
- 范围：衣物录入 + 搭配组合 + 日历规划 + 穿着统计 + 搭配导出分享
- 不做：多用户 / 社交、AI 打标签、电商、推送

## 2. 技术栈

| 层 | 选型 |
|----|------|
| 前端 | uni-app（Vue 3 + TypeScript），uView Plus，Pinia，Vite |
| 前端测试 | Vitest + @vue/test-utils（仅核心组件） |
| 后端 | Java 21 + Spring Boot 3.3.4 + MyBatis-Plus 3.5.5（见踩坑 #7） |
| 后端测试 | JUnit 5 + Mockito + Testcontainers |
| 数据库 | PostgreSQL 16 |
| 对象存储 | MinIO（latest） |
| 构建 | Maven（不用 Flyway，schema 走 `schema.sql` + `data.sql` 启动加载） |
| 部署 | Docker Compose（dev 与 prod 两套） |

## 3. 当前进度

### 已完成（23 个 commit，含 5 个 docs）

| 任务 | 提交 | 内容 |
|------|------|------|
| T1 | `ad0ebdd` | chore: project skeleton and gitignore |
| T2 | `3157e0e` | chore: docker compose for local dev (PG + MinIO) |
| T3 | `bbcb1f5` | feat(db): schema and seed data for MVP |
| T4 | `0ec6ae7` | feat(backend): spring boot project init with PG + MinIO deps |
| T5 | `4b3f0e5` | feat(common): Result wrapper and global exception handler |
| T6 | `3ea8146` | feat(storage): MinIO storage service（用本地 MinIO + TestPropertySource，详见踩坑 #8） |
| T7 | `b981c3f` | feat(backend): MyBatis-Plus pagination and CORS config |
| T8 | `829a96b` | feat(category): CRUD with MyBatis-Plus |
| T9 | `d38e833` | feat(tag): CRUD with MyBatis-Plus |
| T10 | `282334d` | feat(clothing): Clothing 实体 + Mapper + 元数据填充器 |
| T11 | `95f4293` | feat(clothing): ClothingService + DTO + 11 个单元测试 |
| T12 | `dde51fd` | feat(clothing): ClothingController + IT（4 个集成测试） |
| T13 | `385ed90` | feat(clothing): 图片上传/下载（ClothingImage + ImageController + IT） |
| T14 | `122280d` | feat(outfit): entity and mappers |
| T15 | `786cdf8` | feat(outfit): T15 OutfitService + DTO + 8 个 Mockito 单测 |
| T16 | `5b820b7` | feat(outfit): T16 OutfitController + 集成测试 |
| T17 | `a4057fd` | feat(wear-log): T17 手动补登端点 |
| T18 | `ee19c54` | feat(calendar): T18 CalendarEntry 实体 + WearLogSyncService + CalendarService |
| -    | `8b4072f` | docs: project handoff summary for session continuity |
| -    | `b484530` | docs: update handoff with T5 completion |
| -    | `3891081` | docs: update handoff with T6+T7 completion |
| -    | `e08c749` | docs: update handoff with T8 completion |
| -    | `ccf141e` | docs: update handoff with T9 completion |

### 待办

**当前：T19**（CalendarController + IT）

T18 已完成：CalendarEntry + WearLogSyncService + CalendarService + 3/3 单测全绿 9.3s，commit `ee19c54`。

T19 任务（计划文档 §4 第 6 项，详见 2595-2640 行）：
- controller/CalendarController.java（5 端点）：
  - GET /api/v1/calendar?from&to（范围查询，@DateTimeFormat ISO.DATE）
  - GET /api/v1/calendar/{id}
  - POST /api/v1/calendar（创建，触发 sync.generateForEntry）
  - PUT /api/v1/calendar/{id}（更新 + 重生 wear_log）
  - DELETE /api/v1/calendar/{id}（先 sync.deleteForEntry 再 delete）
- integration/CalendarControllerIT.java：建 outfit+clothing → POST calendar → GET range 验证 wear_log 副作用 → PUT → DELETE
- 提交信息：feat(calendar): T19 CalendarController + 集成测试（中文详细）
- 跑 mvn -o -Dtest=CalendarControllerIT test 验证

后续 T20：StatsService（统计 DTO + service + 单测）。T21 起 Phase 6 完成进 Phase 7。

## 4. 工作目录与关键路径

```
C:\Users\huchang\Documents\Codex\2026-07-01\new-chat\
├── backend/                                # Spring Boot 工程
│   ├── pom.xml                             # Spring Boot 3.3.4 + Java 21 + MyBatis-Plus 3.5.5
│   ├── src/main/java/com/closet/
│   │   ├── ClosetApplication.java
│   │   ├── common/                         # T5 新增
│   │   │   ├── Result.java
│   │   │   ├── ApiException.java
│   │   │   └── GlobalExceptionHandler.java
│   │   ├── config/                         # T6 + T7 新增
│   │   │   ├── MinioConfig.java            # MinioClient bean
│   │   │   ├── MybatisPlusConfig.java      # 分页拦截器
│   │   │   └── CorsConfig.java             # CORS
│   │   └── storage/                        # T6 新增
│   │       ├── StorageService.java         # 接口
│   │       └── MinioStorageService.java    # 实现（NoSuchKey 静默吞掉）
│   ├── src/test/java/com/closet/
│   │   ├── common/ResultTest.java          # T5 测试（2 个）
│   │   └── integration/
│   │       └── MinioStorageServiceIT.java  # T6 测试（2 个，跑本地 MinIO + PG）
│   ├── src/main/resources/
│   │   ├── application.yml                 # mode: always（见踩坑 #3）
│   │   ├── application-dev.yml
│   │   ├── schema.sql                      # 10 张表
│   │   └── data.sql                        # 5+5 默认分类
│   └── target/                             # 构建产物（gitignore）
├── deploy/
│   └── docker-compose.dev.yml              # PG + MinIO + minio-init
├── docs/
│   └── superpowers/
│       ├── specs/2026-07-01-digital-closet-design.md
│       ├── plans/2026-07-01-digital-closet-mvp.md  # 36 个 Task 的实施计划
│       └── handoff.md                      # 本文档
├── .gitignore
└── README.md
```

## 5. 环境配置

### Java / Maven（PATH 已修好）

- Java 21：`C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot`
  - `JAVA_HOME`（user + machine）已设
- Maven 3.9.9：`D:\apache-maven-3.9.9`
  - `MAVEN_HOME`（machine）已改为 3.9.9
- 旧的 Java 8（`D:\Program Files\Java\jdk1.8.0_271`）和 Maven 3.5.4 已从 system PATH 移除

### Docker（已安装并运行）

- Docker Desktop 29.6.1
- 三个容器在跑（`docker ps`）：
  - `closet-pg-dev`（postgres:16-alpine, healthy, 5432）
  - `closet-minio-dev`（minio/minio, healthy, 9000/9001）
  - `deploy-minio-init-1`（exited(0)，bucket `closet-images` 已建）
- IT 测试用隔离 bucket `closet-it`，用 `@BeforeEach` 创建 + `@AfterEach` 清

### mvn 命令

计划里用 `./mvnw` 但项目还没装 Maven Wrapper，**所有 mvn 命令用 `mvn`，不是 `./mvnw`**。

## 6. 已知问题和踩坑记录

### 坑 1：Subagent 状态报告不可信

Chandrasekhar（T4）报 BLOCKED 说"环境不对、没编译、没提交"，但实际：
- 它找到了 `D:\apache-maven-3.9.9` 并成功编译
- 它提交了 `0ec6ae7`
- 它启动过应用并跑了 schema 初始化（这导致 PG 里已有表）

**教训**：永远自己 `git log` / `git status` 验证一遍。

### 坑 2：Subagent 容易卡死在 sleep 操作

Wegener、T2 Poincare 都卡在 `Start-Sleep -Seconds 30` 不返回。

**教训**：派 subagent 时显式禁止 `Start-Sleep`、循环等待、轮询。mvn/psql/docker exec 是同步的，直接等返回即可。

### 坑 3：`spring.sql.init.mode: always` 在 schema 已存在时失败

应用启动报 `ERROR: relation "category" already exists`。

**当前 workaround**：每次重启应用前先 `DROP SCHEMA public CASCADE; CREATE SCHEMA public;`。

**长期方案**（T13 部署前再处理）：
- 改成 `mode: never`，靠手动初始化
- 或 profile 区分（dev 用 always，prod 用 never）
- 或 schema.sql 加 `IF NOT EXISTS`

### 坑 4：apply_patch 格式

每行内容前必须加 `+` 前缀，包括空行和注释行。
如果用 PowerShell here-string 写大文件，注意 `Get-Content | Add-Content` 会丢换行，必须用 `Get-Content -Raw` 或 `Set-Content`。

### 坑 5：Codex 启动子进程用缓存的 system PATH

改 system 环境变量后，当前 Codex 进程内的子进程不会立即看到。需要重启 Codex。

### 坑 6：PowerShell 多行字符串替换复杂

`-replace` 处理带特殊字符的中文文本容易失败。处理多行内容时直接用 `Set-Content` 重写整个文件更可靠。

### 坑 7：MyBatis-Plus 3.5.9 删了 `PaginationInnerInterceptor`

3.5.9 的 `mybatis-plus-extension` jar 里只有 `InnerInterceptor` 接口和 `DynamicTableName/OptimisticLocker/ReplacePlaceholder` 三个实现，**没有 `PaginationInnerInterceptor`**（3.5.5 里有）。原因：3.5.9 把分页拦截器拆到独立的 `mybatis-plus-jsqlparser` 模块。

**当前 workaround**：`pom.xml` 把 `mybatis-plus.version` 钉到 `3.5.5`。

**长期方案**（可选）：加 `com.baomidou:mybatis-plus-jsqlparser` 依赖后升回 3.5.9。

### 坑 8：Testcontainers + Docker Desktop 4.80.0 不兼容

按 T6 计划用 `@Testcontainers MinIOContainer`，但 dockerjava 客户端拿不到 `/info` 响应（空响应），容器起不来。

**当前 workaround**：`MinioStorageServiceIT` 直接连 docker-compose 启动的本地 MinIO，用 `@TestPropertySource` 切到隔离 bucket `closet-it`，`@BeforeEach` 建桶 + `@AfterEach` 清对象。代码里有注释解释。

### 坑 9：MyBatis-Plus `DbType` 枚举名是 `POSTGRE_SQL`（带下划线）

计划里写 `DbType.POSTGRE_SQL` 是对的，反编译 `mybatis-plus-annotation-3.5.5.jar` 确认。
一开始误写 `POSTGRESQL`（无下划线）→ 编译过但运行时报 `Unresolved compilation problem: POSTGRESQL cannot be resolved`，因为 Spring Boot 用 JDT 编译器运行时校验。

## 7. Subagent 派发模板

派 subagent 经常卡死，建议用以下模式：

1. prompt 里显式列出严格约束：
   - 不用 `Start-Sleep`、循环等待、轮询
   - apply_patch 每行加 `+` 前缀
   - mvn/psql/docker exec 是同步的，直接等返回
   - 不要多轮验证
   - 用中文报告
2. model 用默认，reasoning_effort 用 `low`
3. type 用 `worker`
4. 3 分钟轮询
5. 派发后自己也要独立验证（坑 1）

## 8. 工作流约定

- 分支：直接在 main 上工作（用户明确同意）
- 提交：每 Task 一个 commit，message 用任务规格里的格式
- 用户偏好：
  - 报告用中文
  - subagent 跑的时候每 3 分钟汇报一次进度
  - 简单任务（写静态文件）本地做更快，不用 subagent
  - 复杂任务（多文件、编译、测试）派 subagent
  - 每个子任务完成都更新 handoff.md（AGENTS.md 要求）

## 9. 下一步操作

**直接做 T8**，按以下顺序：

1. 读 `docs/superpowers/plans/2026-07-01-digital-closet-mvp.md` 里 T8 的完整规格
2. Clothing 比 Category/Tag 复杂：还含 image 列表 + 多对多关联（），按 TDD 顺序写测试 + 实现
3. 完成后用 `mvn -o test` 跑测试验证
4. commit

**T10 提示**：
- 用 `mvn` 不是 `./mvnw`
- 实体类放 backend/src/main/java/com/closet/entity/Clothing.java
- Mapper 接口继承 `BaseMapper<Category>`（来自 `com.baomidou.mybatisplus.core.mapper`）
- 注意 `schema.sql` 里 `category` 表的列名（`id`, `name`, `icon`, `sort_order`, `parent_id NULLABLE`, `created_at`, `updated_at`）
- 默认 dev profile 跑全链路 IT 会连本地 PG（容器已起）；单元测试不需要 DB

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
