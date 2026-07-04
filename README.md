# 电子衣橱 MVP (Digital Closet)

个人电子衣橱应用。单用户、自托管、不鉴权。技术栈：

| 层 | 选型 |
|---|---|
| 前端 | uni-app (Vue 3 + TypeScript), uView Plus, Pinia, Vite, Vitest |
| 后端 | Java 21 + Spring Boot 3.3.4 + MyBatis-Plus 3.5.5 |
| 数据库 | PostgreSQL 16 |
| 对象存储 | MinIO (latest) |
| 部署 | Docker Compose (dev 与 prod 两套) |

## 功能

- 衣物录入（名称 / 品牌 / 颜色 / 尺码 / 入手日期 / 季节 / 备注 / 分类 / 标签 / 图片）
- 搭配组合（衣物选择器 + 季节 + 场合 + 收藏）
- 日历规划（按日期 + 时段安排搭配）
- 穿着统计（总览 / 最常穿 / 最少穿）
- 搭配分享（uni.share 集成）
- 数据导出（JSON 下载）

## 项目结构

```
.
├── backend/                          # Spring Boot 工程
│   ├── src/main/java/com/closet/
│   │   ├── common/                   # Result / ApiException / 全局异常处理
│   │   ├── config/                   # MyBatis-Plus / MinIO / CORS
│   │   ├── controller/               # 9 个 REST 控制器
│   │   ├── service/                  # 9 个 service
│   │   ├── mapper/                   # MyBatis-Plus mapper
│   │   ├── entity/                   # 9 个实体
│   │   ├── dto/                      # 请求/响应 DTO
│   │   └── storage/                  # MinioStorageService
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   ├── application-prod.yml
│   │   ├── schema.sql                # 10 张表
│   │   └── data.sql                  # 5+5 默认分类
│   ├── src/test/java/com/closet/
│   │   ├── unit/                     # Mockito 单测
│   │   └── integration/              # Testcontainers / 本地 MinIO 集成测试
│   ├── pom.xml
│   └── Dockerfile
├── frontend/                         # uni-app 工程
│   ├── src/
│   │   ├── pages/                    # 10 个页面
│   │   ├── components/               # 6 个核心组件
│   │   ├── api/                      # OpenAPI 客户端
│   │   ├── stores/                   # Pinia
│   │   └── utils/
│   ├── package.json
│   ├── vitest.config.ts
│   └── vitest.setup.ts
├── deploy/
│   ├── docker-compose.dev.yml        # 本地开发: PG + MinIO + minio-init
│   ├── docker-compose.yml            # 生产: 上述 + backend
│   └── .env.example                  # 生产环境变量模板
├── docs/
│   └── superpowers/                  # 设计 / 计划 / 交接文档
└── README.md
```

## 本地开发

### 前置条件

- Windows / macOS / Linux
- Java 21
- Node.js 22+
- Maven 3.9+
- Docker Desktop (推荐) 或本地 PostgreSQL + MinIO

### 启动后端 + 数据库

```bash
cd deploy
docker compose -f docker-compose.dev.yml up -d
```

启动后 PostgreSQL 在 5432、MinIO 在 9000 / 控制台 9001。`closet-images` bucket 已建。

### 启动后端服务（开发模式）

```bash
cd backend
mvn spring-boot:run
```

后端跑在 `http://localhost:8080`，API 前缀 `/api/v1`。

### 启动前端

```bash
cd frontend
npm install
npm run dev:h5
```

前端开发服务器默认跑在 `http://localhost:5173`。

### 跑测试

```bash
# 后端单测 + 集成测试
cd backend
mvn test

# 前端组件 + page 测试
cd frontend
npm test
```

## 生产部署（自托管）

```bash
cd deploy
cp .env.example .env
# 编辑 .env 填入真实密码

docker compose up -d --build
```

等约 30 秒，验证：

```bash
curl http://localhost:8080/api/v1/categories
# 期望返回默认分类列表
```

## 微信小程序发布

不在 MVP 范围。后端已对 CORS 开放，前端可按 uni-app 文档做多端编译。

## 测试

- 后端: `mvn test` 跑 JUnit 5 + Mockito + Testcontainers
- 前端: `npm test` 跑 Vitest（happy-dom）

## 备份（手动）

- PostgreSQL 数据卷：`deploy_pgdata`
- MinIO 数据卷：`deploy_miniodata`

## 许可证

仅供个人使用。

## 验收清单

参见 `docs/superpowers/plans/2026-07-01-digital-closet-mvp.md` 末尾的验收清单。
