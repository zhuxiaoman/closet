# 电子衣橱 MVP 项目状态总结（Handoff）

> 用途：在新 Codex 会话中，把本文档作为背景信息粘贴进去，即可无缝接续当前进度。
> 截止：T1-T5 已完成，T6 待开始
> 最后提交：`4b3f0e5`

## 1. 项目概述

- **名称**：电子衣橱（Digital Closet）MVP
- **定位**：个人使用，单用户，自托管在用户自己的 PC 上
- **范围**：衣物录入 + 搭配组合 + 日历规划 + 穿着统计 + 搭配导出分享
- **不做**：多用户 / 社交、AI 打标签、电商、推送

## 2. 技术栈

| 层 | 选型 |
|----|------|
| 前端 | uni-app（Vue 3 + TypeScript），uView Plus，Pinia，Vite |
| 前端测试 | Vitest + @vue/test-utils（仅核心组件） |
| 后端 | Java 21 + Spring Boot 3.3.4 + MyBatis-Plus 3.5.9 |
| 后端测试 | JUnit 5 + Mockito + Testcontainers |
| 数据库 | PostgreSQL 16 |
| 对象存储 | MinIO（latest） |
| 构建 | Maven（不用 Flyway，schema 走 `schema.sql` + `data.sql` 启动加载） |
| 部署 | Docker Compose（dev 与 prod 两套） |

## 3. 当前进度

### 已完成（5 个 commit，含 docs）

| 任务 | 提交 | 内容 |
|------|------|------|
| T1 | `ad0ebdd` | chore: project skeleton and gitignore |
| T2 | `3157e0e` | chore: docker compose for local dev (PG + MinIO) |
| T3 | `bbcb1f5` | feat(db): schema and seed data for MVP |
| T4 | `0ec6ae7` | feat(backend): spring boot project init with PG + MinIO deps |
| T5 | `4b3f0e5` | feat(common): Result wrapper and global exception handler |
| -    | `8b4072f` | docs: project handoff summary for session continuity |

### 待办

**下一步：T6**（MinIO 配置与存储服务）
- 配置 MinIO 客户端（endpoint、access-key、secret-key、bucket）
- 实现 `MinioStorageService`（上传、下载、删除对象）
- 单元测试
- 路径：`backend/src/main/java/com/closet/config/MinioConfig.java` 和 `backend/src/main/java/com/closet/storage/MinioStorageService.java`

后续 T7-T36 详见 `docs/superpowers/plans/2026-07-01-digital-closet-mvp.md`。

## 4. 工作目录与关键路径

```
C:\Users\huchang\Documents\Codex\2026-07-01\new-chat\
├── backend/                                # Spring Boot 工程
│   ├── pom.xml                             # Spring Boot 3.3.4 + Java 21
│   ├── src/main/java/com/closet/
│   │   ├── ClosetApplication.java
│   │   └── common/                         # T5 新增
│   │       ├── Result.java
│   │       ├── ApiException.java
│   │       └── GlobalExceptionHandler.java
│   ├── src/test/java/com/closet/common/
│   │   └── ResultTest.java                 # T5 测试
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

- **Java 21**：`C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot`
  - `JAVA_HOME`（user + machine）已设
- **Maven 3.9.9**：`D:\apache-maven-3.9.9`
  - `MAVEN_HOME`（machine）已改为 3.9.9
- 旧的 Java 8（`D:\Program Files\Java\jdk1.8.0_271`）和 Maven 3.5.4 已从 system PATH 移除

验证（新 PowerShell 进程）：
```powershell
mvn -version    # 应显示 Maven 3.9.9 + Java 21.0.11 Microsoft
java -version   # 应显示 openjdk 21.0.11
```

### Docker（已安装并运行）

- Docker Desktop 29.6.1
- 三个容器在跑（`docker ps`）：
  - `closet-pg-dev`（postgres:16-alpine, healthy, 5432）
  - `closet-minio-dev`（minio/minio, healthy, 9000/9001）
  - `deploy-minio-init-1`（exited(0)，bucket `closet-images` 已建）

### 计划里用 `./mvnw` 但项目还没装 Maven Wrapper

**T6 及之后所有 mvn 命令用 `mvn`，不是 `./mvnw`**。

## 6. 已知问题和踩坑记录

### 坑 1：Subagent 状态报告不可信

Chandrasekhar（T4）报 BLOCKED 说"环境不对、没编译、没提交"，但实际：
- 它找到了 `D:\apache-maven-3.9.9` 并成功编译（生成了 `target/closet-backend-0.1.0.jar`）
- 它提交了 `0ec6ae7`
- 它启动过应用并跑了 schema 初始化（这导致 PG 里已有表）

**教训**：永远自己 `git log` / `git status` 验证一遍，不要只看 subagent 的报告。

### 坑 2：Subagent 容易卡死在 sleep 操作

Wegener、T2 第二次尝试的 Poincare 都卡在 `Start-Sleep -Seconds 30` 不返回。

**教训**：派 subagent 时显式禁止 `Start-Sleep`、循环等待、轮询。如果需要等命令完成，mvn/psql/docker exec 这些是同步的，直接等返回即可。

### 坑 3：`spring.sql.init.mode: always` 在 schema 已存在时失败

T4 启动验证时，应用报 `ERROR: relation "category" already exists`。

**原因**：`mode: always` 每次启动都跑 schema.sql，第二次启动时表已存在。

**当前 workaround**：每次重启应用前先 `DROP SCHEMA public CASCADE; CREATE SCHEMA public;`。

**长期方案**（T6+ 或部署前再处理）：
- 把 `mode: always` 改成 `mode: never`，靠手动初始化
- 或者加 profile 区分（dev 用 always，prod 用 never）
- 或者给 schema.sql 加 `IF NOT EXISTS`

### 坑 4：apply_patch 格式

每行内容前必须加 `+` 前缀，包括空行和注释行。例：
```
*** Add File: path/to/file.java
+package com.closet;
+
+// 注释
+public class Foo {}
```
如果用 PowerShell here-string 写大文件，注意 `Get-Content | Add-Content` 会丢换行，必须用 `Get-Content -Raw` 或 `Set-Content`。

### 坑 5：Codex 启动子进程用缓存的 system PATH

改 system 环境变量后，当前 Codex 进程内的子进程不会立即看到。需要重启 Codex。
**当前状态**：用户已手动改好 system 环境变量并重启，PATH 已正常。

### 坑 6：PowerShell 多行字符串替换复杂

`-replace` 操作符处理带特殊字符（`(`, `)`, `[`, `]`, `*`, `?`, `` ` `` 等）的中文文本容易失败。处理多行内容时直接用 `Set-Content` 重写整个文件更可靠。

## 7. Subagent 派发模板

之前派 subagent 经常卡死。建议以后用以下模式：

1. **prompt 里显式列出严格约束**：
   - 不用 `Start-Sleep`、循环等待、轮询
   - apply_patch 每行加 `+` 前缀
   - mvn/psql/docker exec 是同步的，直接等返回
   - 不要多轮验证
   - 用中文报告

2. **model 用默认**，reasoning_effort 用 `low`

3. **type 用 `worker`**

4. **3 分钟轮询**：`wait_agent(timeout=180000)` 循环，每次超时报告"还在跑"，完成后报告最终结果

5. **派发后自己也要独立验证**（坑 1）

## 8. 工作流约定

- **分支**：直接在 main 上工作（用户明确同意）
- **提交**：每 Task 一个 commit，message 用任务规格里的格式
- **用户偏好**：
  - 报告用中文
  - subagent 跑的时候每 3 分钟汇报一次进度
  - 简单任务（写静态文件）本地做更快，不用 subagent
  - 复杂任务（多文件、编译、测试）派 subagent

## 9. 下一步操作

**直接做 T6**，按以下顺序：

1. 读 `docs/superpowers/plans/2026-07-01-digital-closet-mvp.md` 里 T6 的完整规格
2. 派 subagent 执行（带严格约束），或本地直接做
3. 完成后用 mvn 跑测试验证
4. commit

**T6 提示**：
- 用 `mvn` 不是 `./mvnw`
- MinIO 端点：`http://localhost:9000`，bucket：`closet-images`，access-key：`closet_dev`，secret-key：`closet_dev_secret`
- 容器已在运行（`closet-minio-dev` healthy, `closet-images` bucket 已建好）

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
| Result 测试 | `backend/src/test/java/com/closet/common/ResultTest.java` |
