# 电子衣橱 设计文档

- **日期：** 2026-07-01
- **状态：** 已通过设计阶段（待用户对本文档做最后审阅）
- **范围：** 个人使用 MVP——单用户，自托管在用户自己的电脑上。

## 1. 概述

一个电子衣橱（digital closet），让主人记录衣物、组合搭配、通过日历规划每日穿搭、查看穿着统计，并把搭配导出成图片分享。第一版面向单用户、自托管，**不**做登录鉴权。

### 目标

- 录入每一件衣物：照片、分类、颜色、季节、标签、购入价格。
- 把多件衣物组合成搭配并保存、编辑。
- 通过日历规划每天穿什么；支持同一天多条记录。
- 提供有用的统计：最常穿/最少穿、单件 cost-per-wear、N 天没穿过的衣物。
- 一键把搭配导出成图片分享（前端 canvas → 图片，不需要后端参与）。

### 非目标（MVP 不做）

- 多用户 / 社交功能（好友、评论、公开主页）。
- AI 自动识别图片打标签 / 分类。
- 电商 / 购物整合。
- 原生 iOS / Android 推送通知。

## 2. 技术选型

| 层 | 选择 |
|----|------|
| 前端框架 | uni-app（Vue 3 + TypeScript） |
| 前端 UI 库 | uView Plus |
| 前端状态 | Pinia |
| 前端构建 | Vite |
| 前端测试 | Vitest + @vue/test-utils（仅核心组件） |
| 后端语言 | Java 21 |
| 后端框架 | Spring Boot 3.3+（Spring Web） |
| ORM | MyBatis-Plus（`mybatis-plus-boot-starter`） |
| 数据库 | PostgreSQL 16 |
| 数据库 schema 管理 | Spring Boot 启动时加载 `schema.sql` + `data.sql`（**不**用 Flyway） |
| 对象存储 | MinIO（S3 兼容） |
| 图片 SDK | MinIO Java SDK |
| 构建工具 | Maven |
| 后端测试 | JUnit 5 + Mockito + Testcontainers（Docker 起真实 PG 和 MinIO） |
| API 文档 | springdoc-openapi → 生成 OpenAPI 3 规范 |

## 3. 架构

```
┌──────────────────────────────┐
│ 客户端（uni-app 一套代码）     │
│  - 微信小程序                  │
│  - H5（手机 / 电脑浏览器）      │
│  - iOS / Android App          │
└─────────────┬────────────────┘
              │ HTTPS REST（MVP 无鉴权）
              ▼
┌──────────────────────────────┐
│ 后端（Spring Boot）            │
│  - REST 控制器                 │
│  - 业务逻辑层                  │
│  - MyBatis-Plus mappers       │
│  - MinIO 客户端                │
└────┬──────────────────┬──────┘
     │ SQL              │ S3 协议
     ▼                  ▼
┌──────────┐      ┌──────────┐
│PostgreSQL│      │  MinIO   │
└──────────┘      └──────────┘
```

### 数据流 —— 图片上传

1. 客户端选图 → `POST /api/v1/clothing/{id}/images`（multipart）。
2. 后端把图片上传到 MinIO，把返回的 `storage_key` 写入 `clothing_image` 表，返回图片记录。
3. 客户端拿到图片记录。后续查看图片时调用 `GET /api/v1/images/{key}`，由后端从 MinIO 代理取出。

### 数据流 —— 图片下载

- 客户端请求 `GET /api/v1/images/{key}`。
- 后端从 MinIO 取出对象，字节流直接回写（带 `ETag`、`Cache-Control` 让客户端缓存）。MVP 不上 Redis 缓存。

### 数据流 —— 创建日历条目

1. 客户端 `POST /api/v1/calendar`，body：`{ date, slot, outfitId, notes }`。
2. 后端写入 `calendar_entry`。
3. 后端读取该搭配的 `outfit_item` 列表，为每件衣物写一条 `wear_log`，`worn_at = entry.date`。
4. 返回保存后的条目。

## 4. 数据模型

### 表结构

**`category`** —— 衣物分类（支持两级：父分类 → 子分类）

| 列 | 类型 | 说明 |
|----|------|------|
| id | bigserial PK | |
| name | varchar(64) | 非空 |
| parent_id | bigint FK→category.id 可空 | 二级分类的父节点 |
| sort_order | int | 默认 0 |

**`clothing`** —— 衣物单品

| 列 | 类型 | 说明 |
|----|------|------|
| id | bigserial PK | |
| name | varchar(128) | 非空 |
| brand | varchar(64) | 可空 |
| color_primary | varchar(32) | hex 或色名 |
| color_secondary | varchar(32) | 可空 |
| size | varchar(32) | 可空 |
| purchase_price | numeric(10,2) | 可空 |
| purchase_date | date | 可空 |
| season | varchar(16) | 枚举：spring / summer / fall / winter / all |
| notes | text | 可空 |
| status | varchar(16) | 枚举：active / discarded / donated / sold，默认 active |
| main_image_id | bigint | 可空，指向 clothing_image.id |
| created_at | timestamptz | |
| updated_at | timestamptz | |

**`clothing_image`** —— 单件衣物的多张图片

| 列 | 类型 | 说明 |
|----|------|------|
| id | bigserial PK | |
| clothing_id | bigint FK→clothing.id | 非空，on delete cascade |
| storage_key | varchar(255) | MinIO 对象 key |
| sort_order | int | 默认 0 |
| is_main | boolean | 默认 false |
| created_at | timestamptz | |

**`tag`** —— 自由标签

| 列 | 类型 | 说明 |
|----|------|------|
| id | bigserial PK | |
| name | varchar(64) | unique |

**`clothing_category`** —— 衣物 ↔ 分类 多对多

| 列 | 类型 |
|----|------|
| clothing_id | bigint FK→clothing.id |
| category_id | bigint FK→category.id |
| PK | (clothing_id, category_id) |

**`clothing_tag`** —— 衣物 ↔ 标签 多对多

| 列 | 类型 |
|----|------|
| clothing_id | bigint FK |
| tag_id | bigint FK |
| PK | (clothing_id, tag_id) |

**`outfit`** —— 保存的搭配

| 列 | 类型 | 说明 |
|----|------|------|
| id | bigserial PK | |
| name | varchar(128) | 非空 |
| description | text | 可空 |
| occasion | varchar(64) | 可空（casual / work / sport / ...） |
| season | varchar(16) | 可空 |
| is_favorite | boolean | 默认 false |
| cover_image_id | bigint | 可空 |
| created_at | timestamptz | |
| updated_at | timestamptz | |

**`outfit_item`** —— 搭配 ↔ 衣物 多对多，带叠放顺序

| 列 | 类型 |
|----|------|
| outfit_id | bigint FK→outfit.id |
| clothing_id | bigint FK→clothing.id |
| sort_order | int | 默认 0（叠放层次：内层=0、中间=1、外套=2 ...） |
| PK | (outfit_id, clothing_id) |

**`calendar_entry`** —— 每天每个 slot 一条记录

| 列 | 类型 | 说明 |
|----|------|------|
| id | bigserial PK | |
| entry_date | date | 非空 |
| slot | varchar(16) | 枚举：morning / afternoon / evening / all_day，默认 all_day |
| outfit_id | bigint FK→outfit.id | 非空 |
| notes | text | 可空 |
| created_at | timestamptz | |
| updated_at | timestamptz | |

同一天允许多条记录（不同 slot，或同一 slot 多次添加）。

**`wear_log`** —— 穿着流水，只追加不修改

| 列 | 类型 | 说明 |
|----|------|------|
| id | bigserial PK | |
| clothing_id | bigint FK→clothing.id | 非空 |
| calendar_entry_id | bigint FK→calendar_entry.id | 可空（null 表示手动记录） |
| worn_at | date | 非空 |
| created_at | timestamptz | |

### wear_log 规则

- **自动生成** —— 当 `calendar_entry` 创建/更新时，给该搭配 `outfit_item` 里的每件衣物各写一条，`worn_at = entry.entry_date`。
- **级联删除** —— 删除来源 `calendar_entry` 时同步删除其产生的 wear_log。
- **保留不变** —— 删除搭配本身不动 wear_log（衣服确实穿过的事实不能抹掉）。
- **支持手动补登** —— 通过 `POST /api/v1/wear-logs` 给没在搭配里的穿着补一条记录。
- **统计直接读 wear_log** —— 反范式化设计是有意为之，统计查询不需要 JOIN 多张表。

## 5. 模块结构

### 前端（uni-app）

```
src/
├── pages/
│   ├── index/                # 首页：统计概览 + 最近穿着
│   ├── closet/               # 衣物列表 + 筛选
│   ├── clothing-detail/      # 衣物详情
│   ├── clothing-form/        # 新增/编辑衣物 + 图片上传
│   ├── outfits/              # 搭配列表
│   ├── outfit-detail/        # 搭配详情
│   ├── outfit-form/          # 从衣物里挑选组合搭配
│   ├── calendar/             # 日历视图 + 当日详情
│   ├── stats/                # 统计页
│   └── settings/             # 设置（分类管理、标签管理、数据导出）
├── components/
│   ├── ClothingCard.vue
│   ├── OutfitCanvas.vue      # 搭配编辑画布
│   ├── ImageUploader.vue
│   ├── FilterBar.vue
│   ├── CategoryPicker.vue
│   └── TagPicker.vue
├── api/                      # 由 OpenAPI 自动生成（openapi-typescript）
├── stores/                   # Pinia stores
└── utils/
```

### 后端（Spring Boot，包名 `com.closet`）

```
src/main/java/com/closet/
├── controller/   # REST 控制器
├── service/      # 业务逻辑，含 WearLogSyncService
├── mapper/       # MyBatis-Plus mappers
├── entity/       # 数据库实体
├── dto/          # 请求 / 响应对象
├── config/       # MyBatis-PlusConfig、MinioConfig、CorsConfig
├── common/       # GlobalExceptionHandler、Result 包装、PageRequest
└── storage/      # MinioStorageService 封装
```

## 6. API（统一前缀 `/api/v1`）

### 衣物

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/clothing` | 列表，支持筛选 `categoryId`、`tagId`、`season`、`status`、`keyword`、`page`、`size` |
| GET | `/clothing/{id}` | 详情 |
| POST | `/clothing` | 创建 |
| PUT | `/clothing/{id}` | 更新 |
| DELETE | `/clothing/{id}` | 软删除（默认把 `status` 置为 `discarded`） |
| POST | `/clothing/{id}/images` | multipart 上传（每次一张） |
| DELETE | `/clothing/{id}/images/{imageId}` | 删除图片记录 + MinIO 对象 |

### 分类 / 标签

标准 CRUD：`GET/POST/PUT/DELETE /categories`、`GET/POST/PUT/DELETE /tags`。

### 搭配

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/outfits` | 列表，支持筛选 `season`、`occasion`、`favorite` |
| GET | `/outfits/{id}` | 详情（含 items） |
| POST | `/outfits` | 创建 |
| PUT | `/outfits/{id}` | 更新 |
| DELETE | `/outfits/{id}` | 删除（**不**影响 wear_log） |
| POST | `/outfits/{id}/items` | 添加衣物到搭配；body：`{ clothingId, sortOrder }` |
| PUT | `/outfits/{id}/items/reorder` | 整体重排；body：`[{ clothingId, sortOrder }, ...]` |
| DELETE | `/outfits/{id}/items/{clothingId}` | 从搭配里移除该衣物 |

### 日历

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/calendar?from=YYYY-MM-DD&to=YYYY-MM-DD` | 区间内所有条目 |
| GET | `/calendar/{id}` | 详情 |
| POST | `/calendar` | 创建 —— 触发 wear_log 自动生成 |
| PUT | `/calendar/{id}` | 更新 —— diff items 调整 wear_log |
| DELETE | `/calendar/{id}` | 删除 —— 级联删除该条目产生的 wear_log |

### 穿着流水

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/wear-logs` | 手动补登；body：`{ clothingId, wornAt }` |
| DELETE | `/wear-logs/{id}` | 删除一条手动记录 |

### 图片代理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/images/{key}` | 后端从 MinIO 拉取并流式回写 |

### 统计

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/stats/overview` | 总览：衣物数、搭配数、本月穿着次数 |
| GET | `/stats/clothing/{id}` | 单件：穿着次数、首次/最近穿着、cost-per-wear |
| GET | `/stats/most-worn?limit=10` | 穿着次数最多的 TOP 10 |
| GET | `/stats/least-worn?days=90` | N 天内没穿过的衣物（清理参考） |

### 通用约定

- 所有响应包装成 `{ code: 0|!0, data, message }`。
- 异常由 `GlobalExceptionHandler` 统一处理，输出同样的 envelope。
- 分页参数 `?page=1&size=20`，响应 `{ list, total, page, size }`。
- OpenAPI 规范挂在 `/v3/api-docs`，给前端做客户端代码生成。

## 7. 关键流程

### 新增衣物

1. 用户进 `clothing-form`，从相册选图。
2. 先创建衣物拿到 id，再循环 `POST /clothing/{id}/images` 上传每张图（草稿模式待计划阶段细化）。
3. 设置分类、标签、季节、价格等元数据。
4. 提交 → `POST /clothing`（一次性写入元数据 + 图片 key）。

### 组搭配

1. 用户进 `outfit-form`，从衣物列表里挑衣服。
2. 拖拽调整顺序（写入 `sort_order`）。
3. 选封面图、填名称 / 场合 / 季节。
4. 保存 → `POST /outfits`，再循环 `POST /outfits/{id}/items` 添加每件衣物。

### 规划日历

1. 用户进 `calendar` 页，点某个日期。
2. 选 slot + 搭配。
3. 保存 → `POST /calendar` → 后端自动给 `outfit_item` 里每件衣物写一条 wear_log。

### 分享搭配

1. 用户在搭配详情页点「分享」。
2. 前端用 `OutfitCanvas` 把搭配渲染到 `<canvas>`，调 `toDataURL` 出图。
3. 调用系统分享 API（H5：`navigator.share`；微信小程序：`wx.shareAppMessage`）。

## 8. 部署

### 本地开发

```bash
cd deploy
docker compose -f docker-compose.dev.yml up -d   # 起 PG + MinIO
cd ../backend
./mvnw spring-boot:run
# 另开一个 shell
cd ../frontend
npm install
npm run dev:h5           # 浏览器调试
npm run dev:mp-weixin    # 微信开发者工具调试
```

`docker-compose.dev.yml` 把 PG 暴露在 `5432`、MinIO 暴露在 `9000`（API）/ `9001`（控制台），方便本地直接连。

### 生产部署（自托管在用户自己的 PC）

`deploy/docker-compose.yml` 跑三个服务：

- `postgres`：PostgreSQL 16-alpine，命名卷 `pgdata`，init 脚本 `postgres/init.sql` 挂到 `/docker-entrypoint-initdb.d/`。
- `minio`：latest，命名卷 `miniodata`，控制台 `:9001`、API `:9000`。
- `backend`：由 `backend/Dockerfile` 构建，依赖前两者，对外暴露 `:8080`。

`.env` 文件提供环境变量：

```
DB_PASSWORD=...
MINIO_USER=...
MINIO_PASSWORD=...
```

### 网络

- **H5 在局域网内访问**：直接用 `http://<本机 IP>:8080`，前端开发连这个。
- **微信小程序**：必须有公网 HTTPS 域名。推荐用 **Cloudflare Tunnel**（`cloudflared`）把 `localhost:8080` 暴露成 `https://closet.example.com`，不需要做端口映射。然后在微信公众平台的「服务器域名」里把这个域名加白。

### 存储初始化

第一次启动 MinIO 时用一个一次性 init 容器创建 `closet-images` bucket。具体实现细节在计划阶段确定。

### 备份

**MVP 不做。** 数据放在 Docker 命名卷里。用户需要时手动备份：

```bash
docker run --rm -v closet_pgdata:/data -v $PWD:/backup alpine tar czf /backup/pg-$(date +%F).tar.gz /data
```

## 9. 测试

### 后端

- **单元测试**：service 层，JUnit 5 + Mockito。`service/` 目录行覆盖率目标 ≥ 80%。
- **集成测试**：`@SpringBootTest` + Testcontainers 起真实 PG 和 MinIO，跑全链路（mapper → service → controller）。
- **测试目录**：`src/test/java/com/closet/{unit,integration}`。

### 前端

- **组件测试**：`ClothingCard`、`OutfitCanvas`、`ImageUploader`、`FilterBar`、`CategoryPicker`、`TagPicker` 用 Vitest + @vue/test-utils + happy-dom。
- **API 客户端**：构建时用 OpenAPI 规范校验 TypeScript 类型。
- **E2E**：暂不做，MVP 上线后再考虑。

### CI

MVP 不做 CI，用户本地手动跑测试。

## 10. 开放问题 / 未来工作

1. **图片处理管线** —— 上传时是否后端自动生成缩略图（列表页能省流量）？MVP 不做，加 `thumbnailator` 很容易。
2. **未来多用户** —— 扩展到好友/社区时，需要 `users` 表和每张业务表加 `owner_id`。现在还没数据，回填成本极低。
3. **AI 打标签** —— 自动识别类别 / 颜色 / 季节。作为一个可插拔的服务挂在 `ClothingService.create` 里即可。
4. **自动备份** —— 等用户觉得数据够值钱时再加 cron / 定时任务。
5. **H5 ↔ 微信小程序行为差异** —— 两端都要测。uni-app 在选图、分享、登录这些 API 上有差异。

## 11. 暂缓实现（未来可能补上）


以下功能本次 MVP 不实现，但都是后续版本可能补上的候选，与第 10 节（开放问题 / 未来工作）相互呼应：

- 打开 App 时的 PIN / 密码锁。
- 图片的 Redis 缓存。
- 基于 cron 的自动备份。
- 前端 E2E 测试。
- 多用户 / 社交功能。
