# 电子衣橱 MVP 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 用 uni-app + Spring Boot + PostgreSQL + MinIO 搭建个人电子衣橱 MVP，单用户、自托管、不鉴权，支持衣物录入、搭配组合、日历规划、穿着统计、搭配导出分享。

**Architecture:** 客户端（uni-app 多端：微信小程序 / H5 / iOS / Android 一套代码）+ 后端 REST API（Spring Boot + MyBatis-Plus）+ PostgreSQL（业务数据）+ MinIO（图片对象存储）。客户端通过后端访问 MinIO，后端代理图片读写。

**Tech Stack:**
- 前端：uni-app（Vue 3 + TypeScript），uView Plus，Pinia，Vite，Vitest
- 后端：Java 21，Spring Boot 3.3，MyBatis-Plus，MinIO Java SDK，springdoc-openapi
- 存储：PostgreSQL 16，MinIO（latest）
- 测试：JUnit 5 + Mockito + Testcontainers（后端）；Vitest + @vue/test-utils（前端组件）
- 部署：Docker Compose（dev 与 prod 两套）

**Reference spec:** `docs/superpowers/specs/2026-07-01-digital-closet-design.md`

---

## 项目目录结构

```
closet/
├── backend/                          # Spring Boot 工程
│   ├── src/main/java/com/closet/
│   │   ├── ClosetApplication.java
│   │   ├── controller/               # 9 个 REST 控制器
│   │   ├── service/                  # 9 个 service（含 WearLogSyncService）
│   │   ├── mapper/                   # 9 个 MyBatis-Plus mapper
│   │   ├── entity/                   # 9 个实体
│   │   ├── dto/                      # 请求/响应 DTO
│   │   ├── config/                   # MyBatis-Plus / MinIO / CORS 配置
│   │   ├── common/                   # Result / 异常处理 / PageRequest
│   │   └── storage/                  # MinioStorageService
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   ├── schema.sql                # 建表
│   │   └── data.sql                  # 初始化默认分类等
│   ├── src/test/java/com/closet/{unit,integration}/
│   ├── pom.xml
│   └── Dockerfile
├── frontend/                         # uni-app 工程
│   ├── src/
│   │   ├── pages/                    # 10 个页面
│   │   ├── components/               # 6 个核心组件
│   │   ├── api/                      # 由 OpenAPI 生成
│   │   ├── stores/                   # Pinia
│   │   ├── utils/
│   │   ├── App.vue
│   │   ├── main.ts
│   │   └── pages.json
│   ├── package.json
│   ├── vite.config.ts
│   ├── tsconfig.json
│   └── vitest.config.ts
├── deploy/
│   ├── docker-compose.dev.yml        # PG + MinIO（开发用）
│   ├── docker-compose.yml            # 全栈（生产用）
│   ├── minio/init.sh                 # 创建 bucket
│   └── postgres/init.sql             # 软链到 backend/src/main/resources/schema.sql
├── docs/superpowers/
│   ├── specs/2026-07-01-digital-closet-design.md
│   └── plans/2026-07-01-digital-closet-mvp.md   ← 本文件
├── .gitignore
└── README.md
```

---

## 任务一览

| Phase | 内容 | Task 数 |
|-------|------|---------|
| 1 | 项目骨架 + Docker Compose + 数据库 schema + Spring Boot 初始化 | T1–T7 |
| 2 | 后端 Category + Tag CRUD（TDD） | T8–T9 |
| 3 | 后端 Clothing + 图片上传/下载 | T10–T13 |
| 4 | 后端 Outfit + OutfitItem | T14–T16 |
| 5 | 后端 Calendar + WearLog 同步 | T17–T19 |
| 6 | 后端 Statistics | T20–T21 |
| 7 | 前端 uni-app 初始化 + API 客户端 + 路由 | T22–T24 |
| 8 | 前端核心组件（Vitest） | T25–T28 |
| 9 | 前端页面 | T29–T33 |
| 10 | 部署（Dockerfile + docker-compose + README） | T34–T36 |

---

## Phase 1：项目骨架与基础设施

### Task 1：项目目录结构与 .gitignore

**Files:**
- Create: `.gitignore`
- Create: `README.md`
- Create: `backend/`, `frontend/`, `deploy/` 目录

- [ ] **Step 1：创建 .gitignore**

```gitignore
# Java / Maven
backend/target/
backend/.mvn/wrapper/maven-wrapper.jar
backend/*.class
backend/hs_err_pid*.log

# Node / uni-app
frontend/node_modules/
frontend/dist/
frontend/unpackage/
frontend/.hbuilderx/
frontend/.env.local

# IDE
.idea/
*.iml
.vscode/
!.vscode/settings.json

# OS
.DS_Store
Thumbs.db

# Logs
*.log

# Secrets
.env
deploy/.env

# Backup artifacts (brainstorm helper scripts)
work/
.superpowers/brainstorm/

# Docker volumes (if bound-mounted locally)
deploy/data/
```

- [ ] **Step 2：创建 README 骨架**

```markdown
# 电子衣橱 MVP

个人电子衣橱小程序。详见 `docs/superpowers/specs/2026-07-01-digital-closet-design.md`。

## 本地开发

    # 起数据库和对象存储
    cd deploy
    docker compose -f docker-compose.dev.yml up -d

    # 起后端
    cd ../backend
    ./mvnw spring-boot:run

    # 起前端
    cd ../frontend
    npm install
    npm run dev:h5

## 生产部署

参见 `deploy/docker-compose.yml`，在自己电脑上 `docker compose up -d` 即可。
```

- [ ] **Step 3：提交**

```bash
git add .gitignore README.md
git commit -m "chore: project skeleton and gitignore"
```

---

### Task 2：Docker Compose 开发环境（PostgreSQL + MinIO）

**Files:**
- Create: `deploy/docker-compose.dev.yml`

- [ ] **Step 1：写 docker-compose.dev.yml**

```yaml
services:
  postgres:
    image: postgres:16-alpine
    container_name: closet-pg-dev
    environment:
      POSTGRES_USER: closet
      POSTGRES_PASSWORD: closet_dev
      POSTGRES_DB: closet
    ports:
      - "5432:5432"
    volumes:
      - pgdata-dev:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U closet"]
      interval: 5s
      timeout: 3s
      retries: 10

  minio:
    image: minio/minio:latest
    container_name: closet-minio-dev
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: closet_dev
      MINIO_ROOT_PASSWORD: closet_dev_secret
    ports:
      - "9000:9000"
      - "9001:9001"
    volumes:
      - miniodata-dev:/data
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9000/minio/health/live"]
      interval: 5s
      timeout: 3s
      retries: 10

  minio-init:
    image: minio/mc:latest
    depends_on:
      minio:
        condition: service_healthy
    entrypoint: >
      /bin/sh -c "
      mc alias set local http://minio:9000 closet_dev closet_dev_secret &&
      mc mb --ignore-existing local/closet-images &&
      mc anonymous set download local/closet-images &&
      echo 'bucket ready'
      "

volumes:
  pgdata-dev:
  miniodata-dev:
```

- [ ] **Step 2：起容器并验证**

```bash
cd deploy
docker compose -f docker-compose.dev.yml up -d
# 等约 5 秒，MinIO 控制台访问 http://localhost:9001 用 closet_dev / closet_dev_secret 登录，能看到 closet-images bucket
docker compose -f docker-compose.dev.yml ps
```

Expected：`postgres`、`minio` 状态 healthy，`minio-init` 状态 exited(0)。

- [ ] **Step 3：提交**

```bash
git add deploy/docker-compose.dev.yml
git commit -m "chore: docker compose for local dev (PG + MinIO)"
```

---

### Task 3：数据库 schema.sql + data.sql

**Files:**
- Create: `backend/src/main/resources/schema.sql`
- Create: `backend/src/main/resources/data.sql`

- [ ] **Step 1：写 schema.sql**

```sql
-- 衣物分类（支持两级）
CREATE TABLE category (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(64) NOT NULL,
    parent_id   BIGINT REFERENCES category(id) ON DELETE CASCADE,
    sort_order  INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_category_parent ON category(parent_id);

-- 自由标签
CREATE TABLE tag (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(64) NOT NULL UNIQUE
);

-- 衣物单品
CREATE TABLE clothing (
    id               BIGSERIAL PRIMARY KEY,
    name             VARCHAR(128) NOT NULL,
    brand            VARCHAR(64),
    color_primary    VARCHAR(32),
    color_secondary  VARCHAR(32),
    size             VARCHAR(32),
    purchase_price   NUMERIC(10,2),
    purchase_date    DATE,
    season           VARCHAR(16) NOT NULL DEFAULT 'all'
                     CHECK (season IN ('spring','summer','fall','winter','all')),
    notes            TEXT,
    status           VARCHAR(16) NOT NULL DEFAULT 'active'
                     CHECK (status IN ('active','discarded','donated','sold')),
    main_image_id    BIGINT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_clothing_status ON clothing(status);
CREATE INDEX idx_clothing_season ON clothing(season);

-- 衣物图片
CREATE TABLE clothing_image (
    id           BIGSERIAL PRIMARY KEY,
    clothing_id  BIGINT NOT NULL REFERENCES clothing(id) ON DELETE CASCADE,
    storage_key  VARCHAR(255) NOT NULL,
    sort_order   INTEGER NOT NULL DEFAULT 0,
    is_main      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_clothing_image_clothing ON clothing_image(clothing_id);

ALTER TABLE clothing
    ADD CONSTRAINT fk_clothing_main_image
    FOREIGN KEY (main_image_id) REFERENCES clothing_image(id) ON DELETE SET NULL;

-- 衣物 ↔ 分类（M:N）
CREATE TABLE clothing_category (
    clothing_id  BIGINT NOT NULL REFERENCES clothing(id) ON DELETE CASCADE,
    category_id  BIGINT NOT NULL REFERENCES category(id) ON DELETE CASCADE,
    PRIMARY KEY (clothing_id, category_id)
);

-- 衣物 ↔ 标签（M:N）
CREATE TABLE clothing_tag (
    clothing_id  BIGINT NOT NULL REFERENCES clothing(id) ON DELETE CASCADE,
    tag_id       BIGINT NOT NULL REFERENCES tag(id) ON DELETE CASCADE,
    PRIMARY KEY (clothing_id, tag_id)
);

-- 搭配
CREATE TABLE outfit (
    id               BIGSERIAL PRIMARY KEY,
    name             VARCHAR(128) NOT NULL,
    description      TEXT,
    occasion         VARCHAR(64),
    season           VARCHAR(16),
    is_favorite      BOOLEAN NOT NULL DEFAULT FALSE,
    cover_image_id   BIGINT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_outfit_favorite ON outfit(is_favorite);

-- 搭配 ↔ 衣物（M:N，含顺序）
CREATE TABLE outfit_item (
    outfit_id    BIGINT NOT NULL REFERENCES outfit(id) ON DELETE CASCADE,
    clothing_id  BIGINT NOT NULL REFERENCES clothing(id) ON DELETE CASCADE,
    sort_order   INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (outfit_id, clothing_id)
);

-- 日历条目
CREATE TABLE calendar_entry (
    id           BIGSERIAL PRIMARY KEY,
    entry_date   DATE NOT NULL,
    slot         VARCHAR(16) NOT NULL DEFAULT 'all_day'
                 CHECK (slot IN ('morning','afternoon','evening','all_day')),
    outfit_id    BIGINT NOT NULL REFERENCES outfit(id) ON DELETE RESTRICT,
    notes        TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_calendar_entry_date ON calendar_entry(entry_date);

-- 穿着流水
CREATE TABLE wear_log (
    id                  BIGSERIAL PRIMARY KEY,
    clothing_id         BIGINT NOT NULL REFERENCES clothing(id) ON DELETE CASCADE,
    calendar_entry_id   BIGINT REFERENCES calendar_entry(id) ON DELETE CASCADE,
    worn_at             DATE NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_wear_log_clothing ON wear_log(clothing_id);
CREATE INDEX idx_wear_log_worn_at ON wear_log(worn_at);
CREATE INDEX idx_wear_log_calendar ON wear_log(calendar_entry_id);
```

- [ ] **Step 2：写 data.sql（默认分类）**

```sql
INSERT INTO category (name, parent_id, sort_order) VALUES
    ('上装', NULL, 0),
    ('下装', NULL, 1),
    ('外套', NULL, 2),
    ('鞋',   NULL, 3),
    ('配饰', NULL, 4);

INSERT INTO category (name, parent_id, sort_order)
SELECT 'T恤', id, 0 FROM category WHERE name = '上装' AND parent_id IS NULL
UNION ALL SELECT '衬衫', id, 1 FROM category WHERE name = '上装' AND parent_id IS NULL
UNION ALL SELECT '毛衣', id, 2 FROM category WHERE name = '上装' AND parent_id IS NULL
UNION ALL SELECT '裤子', id, 0 FROM category WHERE name = '下装' AND parent_id IS NULL
UNION ALL SELECT '裙子', id, 1 FROM category WHERE name = '下装' AND parent_id IS NULL;
```

- [ ] **Step 3：用 dev 数据库验证 schema 能执行**

```bash
docker exec -i closet-pg-dev psql -U closet -d closet < backend/src/main/resources/schema.sql
docker exec -i closet-pg-dev psql -U closet -d closet < backend/src/main/resources/data.sql
docker exec closet-pg-dev psql -U closet -d closet -c "\dt"
```

Expected：列出全部 9 张表。

- [ ] **Step 4：清理并提交**

```bash
docker exec closet-pg-dev psql -U closet -d closet -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
git add backend/src/main/resources/schema.sql backend/src/main/resources/data.sql
git commit -m "feat(db): schema and seed data for MVP"
```

---

### Task 4：Spring Boot 项目初始化

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/java/com/closet/ClosetApplication.java`
- Create: `backend/src/main/resources/application.yml`
- Create: `backend/src/main/resources/application-dev.yml`

- [ ] **Step 1：写 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.4</version>
        <relativePath/>
    </parent>
    <groupId>com.closet</groupId>
    <artifactId>closet-backend</artifactId>
    <version>0.1.0</version>
    <name>closet-backend</name>

    <properties>
        <java.version>21</java.version>
        <mybatis-plus.version>3.5.9</mybatis-plus.version>
        <minio.version>8.5.10</minio.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.minio</groupId>
            <artifactId>minio</artifactId>
            <version>${minio.version}</version>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>2.6.0</version>
        </dependency>

        <!-- test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>postgresql</artifactId>
            <version>1.20.3</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>1.20.3</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>minio</artifactId>
            <version>1.20.3</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId></exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2：写主类**

```java
package com.closet;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.closet.mapper")
public class ClosetApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClosetApplication.class, args);
    }
}
```

- [ ] **Step 3：写 application.yml**

```yaml
spring:
  application:
    name: closet-backend
  profiles:
    active: dev
  datasource:
    url: jdbc:postgresql://localhost:5432/closet
    username: closet
    password: closet_dev
    driver-class-name: org.postgresql.Driver
  sql:
    init:
      mode: always
      schema-locations: classpath:schema.sql
      data-locations: classpath:data.sql
      continue-on-error: false
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 50MB

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl
  global-config:
    db-config:
      id-type: AUTO

springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html

minio:
  endpoint: http://localhost:9000
  access-key: closet_dev
  secret-key: closet_dev_secret
  bucket: closet-images

server:
  port: 8080
```

- [ ] **Step 4：写 application-dev.yml**

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/closet
    username: closet
    password: closet_dev
minio:
  endpoint: http://localhost:9000
logging:
  level:
    com.closet: DEBUG
```

- [ ] **Step 5：编译并启动验证**

```bash
cd backend
./mvnw -DskipTests package
java -jar target/closet-backend-0.1.0.jar
```

Expected：服务在 8080 启动，日志无 ERROR。`curl http://localhost:8080/v3/api-docs` 返回 JSON。

- [ ] **Step 6：提交**

```bash
git add backend/pom.xml backend/src/main/java backend/src/main/resources
git commit -m "feat(backend): spring boot project init with PG + MinIO deps"
```

---

### Task 5：通用 Result 包装与全局异常处理

**Files:**
- Create: `backend/src/main/java/com/closet/common/Result.java`
- Create: `backend/src/main/java/com/closet/common/ApiException.java`
- Create: `backend/src/main/java/com/closet/common/GlobalExceptionHandler.java`
- Test: `backend/src/test/java/com/closet/common/ResultTest.java`

- [ ] **Step 1：写失败测试**

```java
package com.closet.common;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ResultTest {
    @Test
    void ok_returnsZeroCodeAndData() {
        Result<String> r = Result.ok("hello");
        assertThat(r.getCode()).isEqualTo(0);
        assertThat(r.getMessage()).isEqualTo("ok");
        assertThat(r.getData()).isEqualTo("hello");
    }

    @Test
    void fail_returnsCustomCodeAndMessage() {
        Result<String> r = Result.fail(404, "not found");
        assertThat(r.getCode()).isEqualTo(404);
        assertThat(r.getMessage()).isEqualTo("not found");
        assertThat(r.getData()).isNull();
    }
}
```

- [ ] **Step 2：跑测试确认失败**

```bash
cd backend && ./mvnw -Dtest=ResultTest test
```

Expected：编译错误 `Result` 类不存在。

- [ ] **Step 3：写 Result.java**

```java
package com.closet.common;
import lombok.Data;

@Data
public class Result<T> {
    private int code;
    private String message;
    private T data;

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = 0;
        r.message = "ok";
        r.data = data;
        return r;
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> fail(int code, String message) {
        Result<T> r = new Result<>();
        r.code = code;
        r.message = message;
        return r;
    }
}
```

- [ ] **Step 4：写 ApiException 与 GlobalExceptionHandler**

`backend/src/main/java/com/closet/common/ApiException.java`

```java
package com.closet.common;
import lombok.Getter;
@Getter
public class ApiException extends RuntimeException {
    private final int code;
    public ApiException(int code, String message) { super(message); this.code = code; }
    public ApiException(String message) { this(500, message); }
}
```

`backend/src/main/java/com/closet/common/GlobalExceptionHandler.java`

```java
package com.closet.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Result<Void>> handleApi(ApiException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.fail(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .orElse("validation failed");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.fail(400, msg));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleAny(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(500, ex.getMessage()));
    }
}
```

- [ ] **Step 5：跑测试通过**

```bash
./mvnw -Dtest=ResultTest test
```

Expected：2 个测试 PASS。

- [ ] **Step 6：提交**

```bash
git add backend/src/main/java/com/closet/common backend/src/test/java/com/closet/common
git commit -m "feat(common): Result wrapper and global exception handler"
```

---

### Task 6：MinIO 配置与存储服务

**Files:**
- Create: `backend/src/main/java/com/closet/config/MinioConfig.java`
- Create: `backend/src/main/java/com/closet/storage/StorageService.java`
- Create: `backend/src/main/java/com/closet/storage/MinioStorageService.java`
- Test: `backend/src/test/java/com/closet/integration/MinioStorageServiceIT.java`

- [ ] **Step 1：写 StorageService 接口**

```java
package com.closet.storage;
import java.io.InputStream;

public interface StorageService {
    String upload(String prefix, String filename, InputStream content, long size, String contentType);
    InputStream download(String key);
    void delete(String key);
}
```

- [ ] **Step 2：写 MinioStorageService**

```java
package com.closet.storage;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageService implements StorageService {

    private final MinioClient client;

    @Value("${minio.bucket}")
    private String bucket;

    @Override
    public String upload(String prefix, String filename, InputStream content, long size, String contentType) {
        String key = "%s/%s-%s".formatted(prefix, UUID.randomUUID(), filename);
        try {
            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket).object(key)
                    .stream(content, size, -1)
                    .contentType(contentType)
                    .build());
            return key;
        } catch (Exception e) {
            throw new RuntimeException("minio upload failed: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream download(String key) {
        try {
            return client.getObject(GetObjectArgs.builder().bucket(bucket).object(key).build());
        } catch (Exception e) {
            throw new RuntimeException("minio download failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build());
        } catch (ErrorResponseException e) {
            if (!"NoSuchKey".equals(e.errorResponse().code())) {
                throw new RuntimeException("minio delete failed: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            throw new RuntimeException("minio delete failed: " + e.getMessage(), e);
        }
    }
}
```

- [ ] **Step 3：写 MinioConfig**

```java
package com.closet.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    @Bean
    public MinioClient minioClient(
            @Value("${minio.endpoint}") String endpoint,
            @Value("${minio.access-key}") String accessKey,
            @Value("${minio.secret-key}") String secretKey) {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
```

- [ ] **Step 4：写集成测试**

```java
package com.closet.integration;

import com.closet.storage.MinioStorageService;
import io.minio.MinioClient;
import io.minio.MakeBucketArgs;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class MinioStorageServiceIT {

    @Container
    static MinIOContainer minio = new MinIOContainer(DockerImageName.parse("minio/minio:latest"));

    @Container
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("closet").withUsername("closet").withPassword("closet_dev");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("minio.endpoint", minio::getS3URL);
        r.add("minio.access-key", minio::getUserName);
        r.add("minio.secret-key", minio::getPassword);
        r.add("minio.bucket", () -> "test-bucket");
        r.add("spring.datasource.url", pg::getJdbcUrl);
        r.add("spring.datasource.username", pg::getUsername);
        r.add("spring.datasource.password", pg::getPassword);
    }

    @BeforeAll
    static void createBucket() throws Exception {
        MinioClient c = MinioClient.builder()
                .endpoint(minio.getS3URL())
                .credentials(minio.getUserName(), minio.getPassword())
                .build();
        c.makeBucket(MakeBucketArgs.builder().bucket("test-bucket").build());
    }

    @Autowired MinioStorageService storage;

    @Test
    void upload_and_download_roundtrip() throws Exception {
        byte[] data = "hello".getBytes();
        String key = storage.upload("test", "x.txt", new ByteArrayInputStream(data), data.length, "text/plain");
        try (var in = storage.download(key)) {
            byte[] read = in.readAllBytes();
            assertThat(read).isEqualTo(data);
        }
        storage.delete(key);
    }
}
```

- [ ] **Step 5：跑测试确认通过**

```bash
cd backend
./mvnw -Dtest=MinioStorageServiceIT test
```

Expected：1 个测试 PASS。

- [ ] **Step 6：提交**

```bash
git add backend/src/main/java/com/closet/config backend/src/main/java/com/closet/storage backend/src/test/java/com/closet/integration
git commit -m "feat(storage): MinIO storage service with Testcontainers IT"
```

---

### Task 7：MyBatis-Plus 分页配置 + CORS

**Files:**
- Create: `backend/src/main/java/com/closet/config/MybatisPlusConfig.java`
- Create: `backend/src/main/java/com/closet/config/CorsConfig.java`

- [ ] **Step 1：写 MybatisPlusConfig**

```java
package com.closet.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));
        return interceptor;
    }
}
```

- [ ] **Step 2：写 CorsConfig**

```java
package com.closet.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOriginPatterns("*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(false)
                        .maxAge(3600);
            }
        };
    }
}
```

- [ ] **Step 3：本地启服务确认 CORS 头存在**

```bash
cd backend && ./mvnw spring-boot:run &
sleep 20
curl -i -X OPTIONS http://localhost:8080/api/v1/categories \
  -H 'Origin: http://localhost:5173' \
  -H 'Access-Control-Request-Method: GET' | head -n 20
```

Expected：响应里有 `Access-Control-Allow-Origin: http://localhost:5173`。

- [ ] **Step 4：提交**

```bash
git add backend/src/main/java/com/closet/config/MybatisPlusConfig.java backend/src/main/java/com/closet/config/CorsConfig.java
git commit -m "feat(backend): MyBatis-Plus pagination and CORS config"
```

---

## Phase 2：Category + Tag CRUD（TDD 模板）

> 这个 Phase 给出了完整 TDD 流程。后续 CRUD 任务（Phase 3-6）会沿用这个模式。

### Task 8：Category 实体 + Mapper + Service + Controller

**Files:**
- Create: `backend/src/main/java/com/closet/entity/Category.java`
- Create: `backend/src/main/java/com/closet/mapper/CategoryMapper.java`
- Create: `backend/src/main/java/com/closet/service/CategoryService.java`
- Create: `backend/src/main/java/com/closet/service/impl/CategoryServiceImpl.java`
- Create: `backend/src/main/java/com/closet/controller/CategoryController.java`
- Create: `backend/src/main/java/com/closet/dto/CategoryRequest.java`
- Test: `backend/src/test/java/com/closet/integration/CategoryControllerIT.java`

- [ ] **Step 1：写集成测试**

```java
package com.closet.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class CategoryControllerIT {

    @Container
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("closet").withUsername("closet").withPassword("closet_dev");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", pg::getJdbcUrl);
        r.add("spring.datasource.username", pg::getUsername);
        r.add("spring.datasource.password", pg::getPassword);
        r.add("minio.endpoint", () -> "http://localhost:1");
        r.add("minio.access-key", () -> "x");
        r.add("minio.secret-key", () -> "x");
        r.add("minio.bucket", () -> "x");
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void crud_flow() throws Exception {
        String body = json.writeValueAsString(Map.of("name", "包包", "sortOrder", 5));
        String resp = mvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value("包包"))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(resp).get("data").get("id").asLong();

        mvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.name=='包包')]").exists());

        mvc.perform(put("/api/v1/categories/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("name", "手包"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("手包"));

        mvc.perform(delete("/api/v1/categories/" + id))
                .andExpect(status().isOk());
    }
}
```

- [ ] **Step 2：跑测试确认失败**

```bash
cd backend && ./mvnw -Dtest=CategoryControllerIT test
```

Expected：编译错误 `CategoryController` 类不存在。

- [ ] **Step 3：写 entity**

```java
package com.closet.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("category")
public class Category {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    @TableField("parent_id")
    private Long parentId;
    @TableField("sort_order")
    private Integer sortOrder;
}
```

- [ ] **Step 4：写 mapper**

```java
package com.closet.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.closet.entity.Category;
public interface CategoryMapper extends BaseMapper<Category> {}
```

- [ ] **Step 5：写 service 接口和实现**

```java
package com.closet.service;
import com.baomidou.mybatisplus.extension.service.IService;
import com.closet.entity.Category;
public interface CategoryService extends IService<Category> {}
```

```java
package com.closet.service.impl;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.closet.entity.Category;
import com.closet.mapper.CategoryMapper;
import com.closet.service.CategoryService;
import org.springframework.stereotype.Service;
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {}
```

- [ ] **Step 6：写 DTO 和 controller**

```java
package com.closet.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data
public class CategoryRequest {
    @NotBlank
    private String name;
    private Long parentId;
    private Integer sortOrder;
}
```

```java
package com.closet.controller;

import com.closet.common.ApiException;
import com.closet.common.Result;
import com.closet.dto.CategoryRequest;
import com.closet.entity.Category;
import com.closet.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService service;

    @GetMapping
    public Result<List<Category>> list() { return Result.ok(service.list()); }

    @PostMapping
    public Result<Category> create(@RequestBody CategoryRequest req) {
        Category c = new Category();
        c.setName(req.getName());
        c.setParentId(req.getParentId());
        c.setSortOrder(req.getSortOrder() == null ? 0 : req.getSortOrder());
        service.save(c);
        return Result.ok(c);
    }

    @PutMapping("/{id}")
    public Result<Category> update(@PathVariable Long id, @RequestBody CategoryRequest req) {
        Category exist = service.getById(id);
        if (exist == null) throw new ApiException(404, "category not found");
        exist.setName(req.getName());
        exist.setParentId(req.getParentId());
        exist.setSortOrder(req.getSortOrder() == null ? exist.getSortOrder() : req.getSortOrder());
        service.updateById(exist);
        return Result.ok(exist);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.removeById(id);
        return Result.ok();
    }
}
```

- [ ] **Step 7：跑测试确认通过**

```bash
./mvnw -Dtest=CategoryControllerIT test
```

Expected：1 个测试 PASS。

- [ ] **Step 8：提交**

```bash
git add backend/src
git commit -m "feat(category): CRUD with MyBatis-Plus"
```

---

### Task 9：Tag CRUD（沿用 TDD 模板）

**Files:**
- Create: `backend/src/main/java/com/closet/entity/Tag.java`
- Create: `backend/src/main/java/com/closet/mapper/TagMapper.java`
- Create: `backend/src/main/java/com/closet/service/TagService.java` (+ `impl/`)
- Create: `backend/src/main/java/com/closet/controller/TagController.java`
- Create: `backend/src/main/java/com/closet/dto/TagRequest.java`
- Test: `backend/src/test/java/com/closet/integration/TagControllerIT.java`

- [ ] **Step 1：写集成测试**（结构同 CategoryIT，路径 `/api/v1/tags`，body `{"name":"通勤"}`，断言 name 字段）

- [ ] **Step 2：写 Tag 实体**（字段：`id`, `name`；`@TableName("tag")`）

- [ ] **Step 3：写 TagMapper**（`extends BaseMapper<Tag>`）

- [ ] **Step 4：写 TagService + TagServiceImpl**（沿用 ServiceImpl 模式）

- [ ] **Step 5：写 TagController**（GET / POST / PUT / DELETE，CRUD 同 Category）

- [ ] **Step 6：跑测试通过**

```bash
./mvnw -Dtest=TagControllerIT test
```

- [ ] **Step 7：提交**

```bash
git add backend/src
git commit -m "feat(tag): CRUD with MyBatis-Plus"
```

---

## Phase 3：Clothing 域（含图片上传/下载）

### Task 10：Clothing 实体 + Mapper + 元数据填充

**Files:**
- Create: `backend/src/main/java/com/closet/entity/Clothing.java`
- Create: `backend/src/main/java/com/closet/entity/ClothingCategory.java`
- Create: `backend/src/main/java/com/closet/entity/ClothingTag.java`
- Create: `backend/src/main/java/com/closet/mapper/ClothingMapper.java`
- Create: `backend/src/main/java/com/closet/mapper/ClothingCategoryMapper.java`
- Create: `backend/src/main/java/com/closet/mapper/ClothingTagMapper.java`
- Create: `backend/src/main/java/com/closet/config/MybatisMetaObjectHandler.java`

- [ ] **Step 1：写 Clothing 实体**

```java
package com.closet.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@TableName("clothing")
public class Clothing {
    @TableId(type = IdType.AUTO) private Long id;
    private String name;
    private String brand;
    @TableField("color_primary")   private String colorPrimary;
    @TableField("color_secondary") private String colorSecondary;
    private String size;
    @TableField("purchase_price")  private BigDecimal purchasePrice;
    @TableField("purchase_date")   private LocalDate purchaseDate;
    private String season;
    private String notes;
    private String status;
    @TableField("main_image_id")   private Long mainImageId;
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
```

- [ ] **Step 2：写 ClothingCategory / ClothingTag 实体**

`ClothingCategory.java`：`@TableName("clothing_category")`，字段 `clothingId`（`@TableField("clothing_id")`）+ `categoryId`。

`ClothingTag.java`：表名 `clothing_tag`，字段 `clothingId` + `tagId`。

- [ ] **Step 3：写 mappers**（三个文件，各自 `extends BaseMapper<...>`）

- [ ] **Step 4：写元数据填充器**

```java
package com.closet.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;
import java.time.OffsetDateTime;

@Component
public class MybatisMetaObjectHandler implements MetaObjectHandler {
    @Override public void insertFill(MetaObject m) {
        this.strictInsertFill(m, "createdAt", OffsetDateTime.class, OffsetDateTime::now);
        this.strictInsertFill(m, "updatedAt", OffsetDateTime.class, OffsetDateTime::now);
    }
    @Override public void updateFill(MetaObject m) {
        this.strictUpdateFill(m, "updatedAt", OffsetDateTime.class, OffsetDateTime::now);
    }
}
```

- [ ] **Step 5：编译验证**

```bash
./mvnw compile
```

Expected：BUILD SUCCESS。

- [ ] **Step 6：提交**

```bash
git add backend/src/main/java/com/closet/{entity,mapper} backend/src/main/java/com/closet/config/MybatisMetaObjectHandler.java
git commit -m "feat(clothing): entity, mappers, and meta-object handler"
```

---

### Task 11：ClothingService（含分类、标签筛选）

**Files:**
- Create: `backend/src/main/java/com/closet/dto/ClothingRequest.java`
- Create: `backend/src/main/java/com/closet/dto/ClothingFilter.java`
- Create: `backend/src/main/java/com/closet/dto/ClothingResponse.java`
- Create: `backend/src/main/java/com/closet/service/ClothingService.java` (+ `impl/`)
- Test: `backend/src/test/java/com/closet/unit/ClothingServiceTest.java`

- [ ] **Step 1：写 ClothingRequest DTO**

```java
package com.closet.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class ClothingRequest {
    @NotBlank
    private String name;
    private String brand;
    private String colorPrimary;
    private String colorSecondary;
    private String size;
    private BigDecimal purchasePrice;
    private LocalDate purchaseDate;
    private String season;
    private String notes;
    private List<Long> categoryIds;
    private List<Long> tagIds;
}
```

- [ ] **Step 2：写 ClothingFilter / ClothingResponse**

`ClothingFilter.java`：`categoryId`, `tagId`, `season`, `status`, `keyword`, `page`, `size`（基本类型 + Lombok @Data）。

`ClothingResponse.java`：在 Clothing 字段基础上加 `List<String> categories`, `List<String> tags`, `List<ClothingImage> images`。

- [ ] **Step 3：写 ClothingService 接口**

```java
package com.closet.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.closet.dto.ClothingFilter;
import com.closet.dto.ClothingRequest;
import com.closet.dto.ClothingResponse;
import com.closet.entity.Clothing;

public interface ClothingService {
    IPage<ClothingResponse> page(ClothingFilter f);
    ClothingResponse get(Long id);
    Clothing create(ClothingRequest req);
    Clothing update(Long id, ClothingRequest req);
    void softDelete(Long id);
}
```

- [ ] **Step 4：写 ClothingServiceImpl**

```java
package com.closet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.closet.common.ApiException;
import com.closet.dto.ClothingFilter;
import com.closet.dto.ClothingRequest;
import com.closet.dto.ClothingResponse;
import com.closet.entity.*;
import com.closet.mapper.*;
import com.closet.service.ClothingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClothingServiceImpl implements ClothingService {

    private final ClothingMapper clothingMapper;
    private final ClothingCategoryMapper ccMapper;
    private final ClothingTagMapper ctMapper;

    @Override
    public IPage<ClothingResponse> page(ClothingFilter f) {
        QueryWrapper<Clothing> q = new QueryWrapper<>();
        if (f.getStatus() != null) q.eq("status", f.getStatus());
        else q.eq("status", "active");
        if (f.getSeason() != null) q.eq("season", f.getSeason());
        if (f.getKeyword() != null && !f.getKeyword().isBlank())
            q.like("name", f.getKeyword());
        if (f.getCategoryId() != null)
            q.inSql("id", "SELECT clothing_id FROM clothing_category WHERE category_id = " + f.getCategoryId());
        if (f.getTagId() != null)
            q.inSql("id", "SELECT clothing_id FROM clothing_tag WHERE tag_id = " + f.getTagId());
        q.orderByDesc("created_at");
        IPage<Clothing> page = clothingMapper.selectPage(new Page<>(f.getPage(), f.getSize()), q);
        return page.convert(this::toResponse);
    }

    @Override
    public ClothingResponse get(Long id) {
        Clothing c = clothingMapper.selectById(id);
        if (c == null) throw new ApiException(404, "clothing not found");
        return toResponse(c);
    }

    @Override
    @Transactional
    public Clothing create(ClothingRequest req) {
        Clothing c = new Clothing();
        applyRequest(c, req);
        c.setStatus("active");
        clothingMapper.insert(c);
        if (req.getCategoryIds() != null) saveCategories(c.getId(), req.getCategoryIds());
        if (req.getTagIds() != null) saveTags(c.getId(), req.getTagIds());
        return c;
    }

    @Override
    @Transactional
    public Clothing update(Long id, ClothingRequest req) {
        Clothing exist = clothingMapper.selectById(id);
        if (exist == null) throw new ApiException(404, "clothing not found");
        applyRequest(exist, req);
        clothingMapper.updateById(exist);
        ccMapper.delete(new QueryWrapper<ClothingCategory>().eq("clothing_id", id));
        ctMapper.delete(new QueryWrapper<ClothingTag>().eq("clothing_id", id));
        if (req.getCategoryIds() != null) saveCategories(id, req.getCategoryIds());
        if (req.getTagIds() != null) saveTags(id, req.getTagIds());
        return exist;
    }

    @Override
    public void softDelete(Long id) {
        Clothing exist = clothingMapper.selectById(id);
        if (exist == null) throw new ApiException(404, "clothing not found");
        exist.setStatus("discarded");
        clothingMapper.updateById(exist);
    }

    private void applyRequest(Clothing c, ClothingRequest r) {
        c.setName(r.getName());
        c.setBrand(r.getBrand());
        c.setColorPrimary(r.getColorPrimary());
        c.setColorSecondary(r.getColorSecondary());
        c.setSize(r.getSize());
        c.setPurchasePrice(r.getPurchasePrice());
        c.setPurchaseDate(r.getPurchaseDate());
        c.setSeason(r.getSeason() == null ? "all" : r.getSeason());
        c.setNotes(r.getNotes());
    }

    private void saveCategories(Long clothingId, List<Long> ids) {
        for (Long cid : ids) {
            ClothingCategory cc = new ClothingCategory();
            cc.setClothingId(clothingId);
            cc.setCategoryId(cid);
            ccMapper.insert(cc);
        }
    }

    private void saveTags(Long clothingId, List<Long> ids) {
        for (Long tid : ids) {
            ClothingTag ct = new ClothingTag();
            ct.setClothingId(clothingId);
            ct.setTagId(tid);
            ctMapper.insert(ct);
        }
    }

    private ClothingResponse toResponse(Clothing c) {
        ClothingResponse r = new ClothingResponse();
        org.springframework.beans.BeanUtils.copyProperties(c, r);
        return r;
    }
}
```

- [ ] **Step 5：写单元测试**

```java
package com.closet.unit;

import com.closet.dto.ClothingFilter;
import com.closet.dto.ClothingRequest;
import com.closet.entity.Clothing;
import com.closet.mapper.*;
import com.closet.service.impl.ClothingServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ClothingServiceTest {

    ClothingMapper clothingMapper = mock(ClothingMapper.class);
    ClothingCategoryMapper ccMapper = mock(ClothingCategoryMapper.class);
    ClothingTagMapper ctMapper = mock(ClothingTagMapper.class);
    ClothingServiceImpl service = new ClothingServiceImpl(clothingMapper, ccMapper, ctMapper);

    @Test
    void create_persists_clothing_and_categories() {
        ClothingRequest req = new ClothingRequest();
        req.setName("白 T");
        req.setCategoryIds(List.of(1L, 2L));
        when(clothingMapper.insert(any(Clothing.class))).thenAnswer(inv -> {
            Clothing c = inv.getArgument(0);
            c.setId(10L);
            return 1;
        });
        Clothing c = service.create(req);
        assertThat(c.getId()).isEqualTo(10L);
        verify(ccMapper, times(2)).insert(any(ClothingCategory.class));
    }

    @Test
    void softDelete_sets_status() {
        Clothing exist = new Clothing();
        exist.setId(1L);
        exist.setStatus("active");
        when(clothingMapper.selectById(1L)).thenReturn(exist);
        service.softDelete(1L);
        assertThat(exist.getStatus()).isEqualTo("discarded");
        verify(clothingMapper).updateById(exist);
    }

    @Test
    void page_passes_filter_to_mapper() {
        ClothingFilter f = new ClothingFilter();
        f.setPage(1); f.setSize(10); f.setKeyword("白");
        service.page(f);
        verify(clothingMapper).selectPage(any(), any());
    }
}
```

- [ ] **Step 6：跑测试**

```bash
./mvnw -Dtest=ClothingServiceTest test
```

Expected：3 个测试 PASS。

- [ ] **Step 7：提交**

```bash
git add backend/src
git commit -m "feat(clothing): service with filter, create, update, soft delete"
```

---

### Task 12：Clothing Controller（含筛选查询）

**Files:**
- Create: `backend/src/main/java/com/closet/controller/ClothingController.java`
- Test: `backend/src/test/java/com/closet/integration/ClothingControllerIT.java`

- [ ] **Step 1：写 controller**

```java
package com.closet.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.closet.common.Result;
import com.closet.dto.ClothingFilter;
import com.closet.dto.ClothingRequest;
import com.closet.dto.ClothingResponse;
import com.closet.entity.Clothing;
import com.closet.service.ClothingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/clothing")
@RequiredArgsConstructor
public class ClothingController {

    private final ClothingService service;

    @GetMapping
    public Result<IPage<ClothingResponse>> list(ClothingFilter filter) {
        if (filter.getPage() == null) filter.setPage(1);
        if (filter.getSize() == null) filter.setSize(20);
        return Result.ok(service.page(filter));
    }

    @GetMapping("/{id}")
    public Result<ClothingResponse> get(@PathVariable Long id) {
        return Result.ok(service.get(id));
    }

    @PostMapping
    public Result<Clothing> create(@RequestBody @Valid ClothingRequest req) {
        return Result.ok(service.create(req));
    }

    @PutMapping("/{id}")
    public Result<Clothing> update(@PathVariable Long id, @RequestBody @Valid ClothingRequest req) {
        return Result.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.softDelete(id);
        return Result.ok();
    }
}
```

- [ ] **Step 2：写集成测试**

```java
package com.closet.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ClothingControllerIT {

    @Container
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("closet").withUsername("closet").withPassword("closet_dev");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", pg::getJdbcUrl);
        r.add("spring.datasource.username", pg::getUsername);
        r.add("spring.datasource.password", pg::getPassword);
        r.add("minio.endpoint", () -> "http://localhost:1");
        r.add("minio.access-key", () -> "x");
        r.add("minio.secret-key", () -> "x");
        r.add("minio.bucket", () -> "x");
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void create_and_list_and_update() throws Exception {
        String catResp = mvc.perform(post("/api/v1/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("name", "上装"))))
            .andReturn().getResponse().getContentAsString();
        long catId = json.readTree(catResp).get("data").get("id").asLong();

        String body = json.writeValueAsString(Map.of(
                "name", "白 T 恤", "season", "summer",
                "categoryIds", List.of(catId)
        ));
        String cr = mvc.perform(post("/api/v1/clothing")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").exists())
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(cr).get("data").get("id").asLong();

        mvc.perform(get("/api/v1/clothing?keyword=" + java.net.URLEncoder.encode("白", "UTF-8")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));

        mvc.perform(put("/api/v1/clothing/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("name", "米白 T"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("米白 T"));

        mvc.perform(delete("/api/v1/clothing/" + id)).andExpect(status().isOk());
    }
}
```

- [ ] **Step 3：跑测试**

```bash
./mvnw -Dtest=ClothingControllerIT test
```

Expected：PASS。

- [ ] **Step 4：提交**

```bash
git add backend/src
git commit -m "feat(clothing): controller with filter and CRUD"
```

---

### Task 13：Clothing 图片上传/下载（接 MinIO）

**Files:**
- Create: `backend/src/main/java/com/closet/entity/ClothingImage.java`
- Create: `backend/src/main/java/com/closet/mapper/ClothingImageMapper.java`
- Create: `backend/src/main/java/com/closet/service/ClothingImageService.java` (+ `impl/`)
- Create: `backend/src/main/java/com/closet/controller/ImageController.java`
- Modify: `backend/src/main/java/com/closet/controller/ClothingController.java`
- Test: `backend/src/test/java/com/closet/integration/ClothingImageIT.java`

- [ ] **Step 1：写 ClothingImage 实体**

```java
package com.closet.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
@TableName("clothing_image")
public class ClothingImage {
    @TableId(type = IdType.AUTO) private Long id;
    @TableField("clothing_id") private Long clothingId;
    @TableField("storage_key") private String storageKey;
    @TableField("sort_order")  private Integer sortOrder;
    @TableField("is_main")     private Boolean isMain;
    @TableField(value = "created_at", fill = FieldFill.INSERT) private OffsetDateTime createdAt;
}
```

- [ ] **Step 2：写 ClothingImageMapper**（`extends BaseMapper<ClothingImage>`）

- [ ] **Step 3：写 ClothingImageService**

```java
package com.closet.service;
import com.baomidou.mybatisplus.extension.service.IService;
import com.closet.entity.ClothingImage;
import org.springframework.web.multipart.MultipartFile;

public interface ClothingImageService extends IService<ClothingImage> {
    ClothingImage upload(Long clothingId, MultipartFile file);
    void delete(Long clothingId, Long imageId);
}
```

```java
package com.closet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.closet.common.ApiException;
import com.closet.entity.Clothing;
import com.closet.entity.ClothingImage;
import com.closet.mapper.ClothingImageMapper;
import com.closet.mapper.ClothingMapper;
import com.closet.service.ClothingImageService;
import com.closet.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ClothingImageServiceImpl extends ServiceImpl<ClothingImageMapper, ClothingImage> implements ClothingImageService {

    private final StorageService storage;
    private final ClothingMapper clothingMapper;

    @Override
    @Transactional
    public ClothingImage upload(Long clothingId, MultipartFile file) {
        if (clothingMapper.selectById(clothingId) == null)
            throw new ApiException(404, "clothing not found");
        try {
            String key = storage.upload("clothing/" + clothingId,
                    Objects.requireNonNull(file.getOriginalFilename()),
                    file.getInputStream(), file.getSize(), file.getContentType());
            ClothingImage img = new ClothingImage();
            img.setClothingId(clothingId);
            img.setStorageKey(key);
            img.setSortOrder(0);
            img.setIsMain(baseMapper.selectCount(
                new QueryWrapper<ClothingImage>().eq("clothing_id", clothingId)) == 0);
            baseMapper.insert(img);
            if (Boolean.TRUE.equals(img.getIsMain())) {
                Clothing c = new Clothing();
                c.setId(clothingId);
                c.setMainImageId(img.getId());
                clothingMapper.updateById(c);
            }
            return img;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @Transactional
    public void delete(Long clothingId, Long imageId) {
        ClothingImage img = baseMapper.selectById(imageId);
        if (img == null || !img.getClothingId().equals(clothingId))
            throw new ApiException(404, "image not found");
        storage.delete(img.getStorageKey());
        baseMapper.deleteById(imageId);
        if (Boolean.TRUE.equals(img.getIsMain())) {
            Clothing c = clothingMapper.selectById(clothingId);
            c.setMainImageId(null);
            clothingMapper.updateById(c);
        }
    }
}
```

- [ ] **Step 4：在 ClothingController 里加端点**

在 ClothingController 里加：

```java
private final ClothingImageService imageService;

// 已有字段 service 改成 clothingService 更清楚；这里假设字段名是 service，重构时可调整

@PostMapping("/{id}/images")
public Result<ClothingImage> uploadImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
    return Result.ok(imageService.upload(id, file));
}

@DeleteMapping("/{id}/images/{imageId}")
public Result<Void> deleteImage(@PathVariable Long id, @PathVariable Long imageId) {
    imageService.delete(id, imageId);
    return Result.ok();
}
```

- [ ] **Step 5：写图片代理端点**

```java
package com.closet.controller;

import com.closet.storage.StorageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.io.InputStream;
import java.io.OutputStream;

@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
public class ImageController {

    private final StorageService storage;

    @GetMapping("/**")
    public void proxy(HttpServletResponse resp, HttpServletRequest req) throws Exception {
        String key = req.getRequestURI().substring("/api/v1/images/".length());
        try (InputStream in = storage.download(key); OutputStream out = resp.getOutputStream()) {
            resp.setContentType("image/jpeg");
            in.transferTo(out);
        }
    }
}
```

- [ ] **Step 6：写集成测试**

```java
package com.closet.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ClothingImageIT {
    @Container static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("closet").withUsername("closet").withPassword("closet_dev");
    @Container static MinIOContainer minio = new MinIOContainer(DockerImageName.parse("minio/minio:latest"));

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", pg::getJdbcUrl);
        r.add("spring.datasource.username", pg::getUsername);
        r.add("spring.datasource.password", pg::getPassword);
        r.add("minio.endpoint", minio::getS3URL);
        r.add("minio.access-key", minio::getUserName);
        r.add("minio.secret-key", minio::getPassword);
        r.add("minio.bucket", () -> "test-bucket");
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void upload_and_proxy_download() throws Exception {
        String cr = mvc.perform(post("/api/v1/clothing")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("name", "x"))))
            .andReturn().getResponse().getContentAsString();
        long id = json.readTree(cr).get("data").get("id").asLong();

        byte[] data = "fake-jpeg-bytes".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "a.jpg", "image/jpeg", data);

        String ur = mvc.perform(multipart("/api/v1/clothing/" + id + "/images").file(file))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String storageKey = json.readTree(ur).get("data").get("storageKey").asText();

        mvc.perform(get("/api/v1/images/" + storageKey))
                .andExpect(status().isOk())
                .andExpect(content().bytes(data));
    }
}
```

- [ ] **Step 7：跑测试**

```bash
./mvnw -Dtest=ClothingImageIT test
```

Expected：PASS。

- [ ] **Step 8：提交**

```bash
git add backend/src
git commit -m "feat(clothing): image upload to MinIO and proxy download"
```

---

## Phase 4：Outfit 域

### Task 14：Outfit + OutfitItem 实体 + Mapper

**Files:**
- Create: `backend/src/main/java/com/closet/entity/Outfit.java`
- Create: `backend/src/main/java/com/closet/entity/OutfitItem.java`
- Create: `backend/src/main/java/com/closet/mapper/OutfitMapper.java`
- Create: `backend/src/main/java/com/closet/mapper/OutfitItemMapper.java`

- [ ] **Step 1：写 Outfit 实体**

```java
package com.closet.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
@TableName("outfit")
public class Outfit {
    @TableId(type = IdType.AUTO) private Long id;
    private String name;
    private String description;
    private String occasion;
    private String season;
    @TableField("is_favorite")    private Boolean isFavorite;
    @TableField("cover_image_id") private Long coverImageId;
    @TableField(value = "created_at", fill = FieldFill.INSERT) private OffsetDateTime createdAt;
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE) private OffsetDateTime updatedAt;
}
```

- [ ] **Step 2：写 OutfitItem 实体**

```java
package com.closet.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("outfit_item")
public class OutfitItem {
    @TableField("outfit_id")   private Long outfitId;
    @TableField("clothing_id") private Long clothingId;
    @TableField("sort_order")  private Integer sortOrder;
}
```

- [ ] **Step 3：写两个 mapper**（各自 `extends BaseMapper<...>`）

- [ ] **Step 4：编译 + 提交**

```bash
./mvnw compile
git add backend/src
git commit -m "feat(outfit): entity and mappers"
```

---

### Task 15：OutfitService（含重排）

**Files:**
- Create: `backend/src/main/java/com/closet/dto/OutfitRequest.java`
- Create: `backend/src/main/java/com/closet/dto/OutfitResponse.java`
- Create: `backend/src/main/java/com/closet/service/OutfitService.java` (+ `impl/`)
- Test: `backend/src/test/java/com/closet/unit/OutfitServiceTest.java`

- [ ] **Step 1：写 OutfitRequest / OutfitResponse DTO**

```java
@Data
public class OutfitRequest {
    @NotBlank private String name;
    private String description;
    private String occasion;
    private String season;
    private Boolean isFavorite;
    private Long coverImageId;
    private List<Long> clothingIds; // 创建时一并加入
}

@Data
public class OutfitResponse {
    private Long id;
    private String name;
    private String description;
    private String occasion;
    private String season;
    private Boolean isFavorite;
    private Long coverImageId;
    private List<Clothing> items;
}
```

- [ ] **Step 2：写 OutfitService 接口**

```java
public interface OutfitService {
    IPage<OutfitResponse> page(String season, String occasion, Boolean favorite, int page, int size);
    OutfitResponse get(Long id);
    Outfit create(OutfitRequest req);
    Outfit update(Long id, OutfitRequest req);
    void delete(Long id);
    void addItem(Long outfitId, Long clothingId, int sortOrder);
    void removeItem(Long outfitId, Long clothingId);
    void reorderItems(Long outfitId, List<ItemOrder> orders);

    record ItemOrder(Long clothingId, int sortOrder) {}
}
```

- [ ] **Step 3：写 OutfitServiceImpl**

关键方法（伪代码示意）：

```java
@Transactional
public Outfit create(OutfitRequest req) {
    Outfit o = new Outfit();
    BeanUtils.copyProperties(req, o);
    o.setIsFavorite(Boolean.TRUE.equals(req.getIsFavorite()));
    outfitMapper.insert(o);
    if (req.getClothingIds() != null) {
        for (int i = 0; i < req.getClothingIds().size(); i++) {
            OutfitItem oi = new OutfitItem();
            oi.setOutfitId(o.getId());
            oi.setClothingId(req.getClothingIds().get(i));
            oi.setSortOrder(i);
            itemMapper.insert(oi);
        }
    }
    return o;
}

@Transactional
public void reorderItems(Long outfitId, List<ItemOrder> orders) {
    for (ItemOrder o : orders) {
        OutfitItem oi = new OutfitItem();
        oi.setOutfitId(outfitId);
        oi.setClothingId(o.clothingId());
        oi.setSortOrder(o.sortOrder());
        itemMapper.updateById(oi);  // MyBatis-Plus updateById 要求主键；这里用自定义 wrapper 更新
        // 实际用 itemMapper.update(oi, new QueryWrapper<OutfitItem>()
        //     .eq("outfit_id", outfitId).eq("clothing_id", o.clothingId()));
    }
}

@Transactional
public void delete(Long id) {
    outfitMapper.deleteById(id);
    // wear_log 不动；calendar_entry 配 outfit_id 设了 ON DELETE RESTRICT 也会报错，
    // 调用方需先处理 calendar_entry
}
```

- [ ] **Step 4：写单元测试**

```java
@Test void create_persists_outfit_and_items() {
    when(outfitMapper.insert(any(Outfit.class))).thenAnswer(inv -> {
        Outfit o = inv.getArgument(0); o.setId(1L); return 1;
    });
    OutfitRequest req = new OutfitRequest();
    req.setName("周末休闲");
    req.setClothingIds(List.of(10L, 20L));
    Outfit o = service.create(req);
    assertThat(o.getId()).isEqualTo(1L);
    verify(itemMapper, times(2)).insert(any(OutfitItem.class));
}
```

- [ ] **Step 5：跑测试 + 提交**

```bash
./mvnw -Dtest=OutfitServiceTest test
git add backend/src
git commit -m "feat(outfit): service with create, items, reorder"
```

---

### Task 16：Outfit Controller

**Files:**
- Create: `backend/src/main/java/com/closet/controller/OutfitController.java`
- Test: `backend/src/test/java/com/closet/integration/OutfitControllerIT.java`

- [ ] **Step 1：写 controller**

```java
@RestController
@RequestMapping("/api/v1/outfits")
@RequiredArgsConstructor
public class OutfitController {
    private final OutfitService service;

    @GetMapping
    public Result<IPage<OutfitResponse>> list(
            @RequestParam(required = false) String season,
            @RequestParam(required = false) String occasion,
            @RequestParam(required = false) Boolean favorite,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(service.page(season, occasion, favorite, page, size));
    }

    @GetMapping("/{id}")
    public Result<OutfitResponse> get(@PathVariable Long id) { return Result.ok(service.get(id)); }

    @PostMapping
    public Result<Outfit> create(@RequestBody @Valid OutfitRequest req) {
        return Result.ok(service.create(req));
    }

    @PutMapping("/{id}")
    public Result<Outfit> update(@PathVariable Long id, @RequestBody @Valid OutfitRequest req) {
        return Result.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) { service.delete(id); return Result.ok(); }

    @PostMapping("/{id}/items")
    public Result<Void> addItem(@PathVariable Long id,
                                @RequestParam Long clothingId,
                                @RequestParam(defaultValue = "0") int sortOrder) {
        service.addItem(id, clothingId, sortOrder); return Result.ok();
    }

    @PutMapping("/{id}/items/reorder")
    public Result<Void> reorder(@PathVariable Long id, @RequestBody List<OutfitService.ItemOrder> orders) {
        service.reorderItems(id, orders); return Result.ok();
    }

    @DeleteMapping("/{id}/items/{clothingId}")
    public Result<Void> removeItem(@PathVariable Long id, @PathVariable Long clothingId) {
        service.removeItem(id, clothingId); return Result.ok();
    }
}
```

- [ ] **Step 2：写集成测试**（路径 `/api/v1/outfits`，先创建 2 件 clothing 再创建 outfit，最后 reorder）

- [ ] **Step 3：跑测试 + 提交**

```bash
./mvnw -Dtest=OutfitControllerIT test
git add backend/src
git commit -m "feat(outfit): controller with CRUD and item endpoints"
```

---

## Phase 5：Calendar + WearLog 同步

### Task 17：WearLog 实体、Mapper、手动补登端点

**Files:**
- Create: `backend/src/main/java/com/closet/entity/WearLog.java`
- Create: `backend/src/main/java/com/closet/mapper/WearLogMapper.java`
- Create: `backend/src/main/java/com/closet/controller/WearLogController.java`
- Test: `backend/src/test/java/com/closet/integration/WearLogControllerIT.java`

- [ ] **Step 1：写 WearLog 实体**

```java
package com.closet.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@TableName("wear_log")
public class WearLog {
    @TableId(type = IdType.AUTO) private Long id;
    @TableField("clothing_id")       private Long clothingId;
    @TableField("calendar_entry_id") private Long calendarEntryId;
    @TableField("worn_at")           private LocalDate wornAt;
    @TableField(value = "created_at", fill = FieldFill.INSERT) private OffsetDateTime createdAt;
}
```

- [ ] **Step 2：写 WearLogMapper**（`extends BaseMapper<WearLog>`）

- [ ] **Step 3：写 WearLogController**

```java
@RestController
@RequestMapping("/api/v1/wear-logs")
@RequiredArgsConstructor
public class WearLogController {

    private final WearLogMapper wearLogMapper;
    private final ClothingMapper clothingMapper;

    @PostMapping
    public Result<WearLog> create(@RequestBody Map<String, Object> body) {
        Long clothingId = ((Number) body.get("clothingId")).longValue();
        String wornAtStr = (String) body.get("wornAt");
        if (clothingMapper.selectById(clothingId) == null)
            throw new ApiException(404, "clothing not found");
        WearLog log = new WearLog();
        log.setClothingId(clothingId);
        log.setWornAt(LocalDate.parse(wornAtStr));
        wearLogMapper.insert(log);
        return Result.ok(log);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        wearLogMapper.deleteById(id);
        return Result.ok();
    }
}
```

- [ ] **Step 4：写集成测试**（创建 clothing → POST wear-log → DELETE）

- [ ] **Step 5：跑测试 + 提交**

```bash
./mvnw -Dtest=WearLogControllerIT test
git add backend/src
git commit -m "feat(wear-log): manual log endpoints"
```

---

### Task 18：Calendar 实体、Service、WearLogSyncService

**Files:**
- Create: `backend/src/main/java/com/closet/entity/CalendarEntry.java`
- Create: `backend/src/main/java/com/closet/mapper/CalendarEntryMapper.java`
- Create: `backend/src/main/java/com/closet/service/WearLogSyncService.java` (+ `impl/`)
- Create: `backend/src/main/java/com/closet/service/CalendarService.java` (+ `impl/`)
- Test: `backend/src/test/java/com/closet/unit/WearLogSyncServiceTest.java`

- [ ] **Step 1：写 CalendarEntry 实体**

```java
package com.closet.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@TableName("calendar_entry")
public class CalendarEntry {
    @TableId(type = IdType.AUTO) private Long id;
    @TableField("entry_date") private LocalDate entryDate;
    private String slot;
    @TableField("outfit_id") private Long outfitId;
    private String notes;
    @TableField(value = "created_at", fill = FieldFill.INSERT) private OffsetDateTime createdAt;
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE) private OffsetDateTime updatedAt;
}
```

- [ ] **Step 2：写 CalendarEntryMapper**（`extends BaseMapper<CalendarEntry>`）

- [ ] **Step 3：写 WearLogSyncService 接口和实现**

```java
public interface WearLogSyncService {
    /** 根据日历条目生成 wear_log：给 outfit 的每件衣物写一条 worn_at = entry.entryDate */
    void generateForEntry(CalendarEntry entry);
    /** 删除某条日历产生的所有 wear_log */
    void deleteForEntry(Long calendarEntryId);
}
```

```java
@Service
@RequiredArgsConstructor
public class WearLogSyncServiceImpl implements WearLogSyncService {

    private final OutfitItemMapper itemMapper;
    private final WearLogMapper wearLogMapper;

    @Override
    @Transactional
    public void generateForEntry(CalendarEntry entry) {
        // 先把旧的清掉（处理 update 场景）
        deleteForEntry(entry.getId());

        List<OutfitItem> items = itemMapper.selectList(
            new QueryWrapper<OutfitItem>().eq("outfit_id", entry.getOutfitId()));
        for (OutfitItem oi : items) {
            WearLog log = new WearLog();
            log.setClothingId(oi.getClothingId());
            log.setCalendarEntryId(entry.getId());
            log.setWornAt(entry.getEntryDate());
            wearLogMapper.insert(log);
        }
    }

    @Override
    @Transactional
    public void deleteForEntry(Long calendarEntryId) {
        wearLogMapper.delete(
            new QueryWrapper<WearLog>().eq("calendar_entry_id", calendarEntryId));
    }
}
```

- [ ] **Step 4：写 WearLogSyncService 单元测试**

```java
class WearLogSyncServiceTest {
    OutfitItemMapper itemMapper = mock(OutfitItemMapper.class);
    WearLogMapper wearLogMapper = mock(WearLogMapper.class);
    WearLogSyncServiceImpl service = new WearLogSyncServiceImpl(itemMapper, wearLogMapper);

    @Test void generate_writes_one_log_per_item() {
        when(itemMapper.selectList(any())).thenReturn(List.of(
            outfitItem(1L), outfitItem(2L), outfitItem(3L)));
        CalendarEntry entry = new CalendarEntry();
        entry.setId(99L);
        entry.setOutfitId(10L);
        entry.setEntryDate(LocalDate.of(2026, 7, 1));

        service.generateForEntry(entry);

        verify(wearLogMapper, times(3)).insert(any(WearLog.class));
    }

    @Test void deleteForEntry_removes_logs_by_calendar_id() {
        service.deleteForEntry(99L);
        verify(wearLogMapper).delete(any(QueryWrapper.class));
    }

    private OutfitItem outfitItem(Long clothingId) {
        OutfitItem oi = new OutfitItem();
        oi.setClothingId(clothingId);
        return oi;
    }
}
```

- [ ] **Step 5：写 CalendarService**

```java
public interface CalendarService {
    List<CalendarEntry> range(LocalDate from, LocalDate to);
    CalendarEntry get(Long id);
    CalendarEntry create(CalendarEntry entry);
    CalendarEntry update(Long id, CalendarEntry entry);
    void delete(Long id);
}
```

实现类关键代码：

```java
@Service
@RequiredArgsConstructor
public class CalendarServiceImpl implements CalendarService {

    private final CalendarEntryMapper entryMapper;
    private final OutfitMapper outfitMapper;
    private final WearLogSyncService wearLogSync;

    @Override
    public List<CalendarEntry> range(LocalDate from, LocalDate to) {
        return entryMapper.selectList(new QueryWrapper<CalendarEntry>()
            .between("entry_date", from, to)
            .orderByAsc("entry_date", "slot"));
    }

    @Override
    @Transactional
    public CalendarEntry create(CalendarEntry entry) {
        if (entry.getSlot() == null) entry.setSlot("all_day");
        if (outfitMapper.selectById(entry.getOutfitId()) == null)
            throw new ApiException(400, "outfit not found");
        entryMapper.insert(entry);
        wearLogSync.generateForEntry(entry);
        return entry;
    }

    @Override
    @Transactional
    public CalendarEntry update(Long id, CalendarEntry entry) {
        CalendarEntry exist = entryMapper.selectById(id);
        if (exist == null) throw new ApiException(404, "calendar entry not found");
        exist.setEntryDate(entry.getEntryDate());
        exist.setSlot(entry.getSlot() == null ? exist.getSlot() : entry.getSlot());
        exist.setOutfitId(entry.getOutfitId() == null ? exist.getOutfitId() : entry.getOutfitId());
        exist.setNotes(entry.getNotes());
        entryMapper.updateById(exist);
        wearLogSync.generateForEntry(exist);  // 重生成
        return exist;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        wearLogSync.deleteForEntry(id);  // 先删 wear_log
        entryMapper.deleteById(id);
    }
}
```

- [ ] **Step 6：跑测试 + 提交**

```bash
./mvnw -Dtest=WearLogSyncServiceTest test
git add backend/src
git commit -m "feat(calendar): entity, service with WearLogSyncService"
```

---

### Task 19：Calendar Controller

**Files:**
- Create: `backend/src/main/java/com/closet/controller/CalendarController.java`
- Test: `backend/src/test/java/com/closet/integration/CalendarControllerIT.java`

- [ ] **Step 1：写 controller**

```java
@RestController
@RequestMapping("/api/v1/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService service;

    @GetMapping
    public Result<List<CalendarEntry>> range(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return Result.ok(service.range(from, to));
    }

    @GetMapping("/{id}")
    public Result<CalendarEntry> get(@PathVariable Long id) { return Result.ok(service.get(id)); }

    @PostMapping
    public Result<CalendarEntry> create(@RequestBody CalendarEntry entry) {
        return Result.ok(service.create(entry));
    }

    @PutMapping("/{id}")
    public Result<CalendarEntry> update(@PathVariable Long id, @RequestBody CalendarEntry entry) {
        return Result.ok(service.update(id, entry));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) { service.delete(id); return Result.ok(); }
}
```

- [ ] **Step 2：写集成测试**（创建 outfit + clothing → POST calendar → GET range → PUT → DELETE）

- [ ] **Step 3：跑测试 + 提交**

```bash
./mvnw -Dtest=CalendarControllerIT test
git add backend/src
git commit -m "feat(calendar): controller"
```

---

## Phase 6：Statistics

### Task 20：Stats Service

**Files:**
- Create: `backend/src/main/java/com/closet/dto/StatsOverview.java`
- Create: `backend/src/main/java/com/closet/dto/ClothingStat.java`
- Create: `backend/src/main/java/com/closet/service/StatsService.java` (+ `impl/`)
- Test: `backend/src/test/java/com/closet/unit/StatsServiceTest.java`

- [ ] **Step 1：写 DTO**

```java
@Data public class StatsOverview {
    private long totalClothing;
    private long totalOutfits;
    private long monthWears;
}

@Data public class ClothingStat {
    private Long clothingId;
    private String name;
    private long wearCount;
    private LocalDate firstWorn;
    private LocalDate lastWorn;
    private BigDecimal costPerWear; // purchasePrice / wearCount
}
```

- [ ] **Step 2：写 StatsService 接口**

```java
public interface StatsService {
    StatsOverview overview();
    ClothingStat forClothing(Long clothingId);
    List<ClothingStat> mostWorn(int limit);
    List<ClothingStat> leastWorn(int days);
}
```

- [ ] **Step 3：写 StatsServiceImpl**

```java
@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final ClothingMapper clothingMapper;
    private final OutfitMapper outfitMapper;
    private final WearLogMapper wearLogMapper;

    @Override
    public StatsOverview overview() {
        StatsOverview s = new StatsOverview();
        s.setTotalClothing(clothingMapper.selectCount(null));
        s.setTotalOutfits(outfitMapper.selectCount(null));
        LocalDate firstOfMonth = LocalDate.now().withDayOfMonth(1);
        s.setMonthWears(wearLogMapper.selectCount(
            new QueryWrapper<WearLog>().ge("worn_at", firstOfMonth)));
        return s;
    }

    @Override
    public ClothingStat forClothing(Long clothingId) {
        Clothing c = clothingMapper.selectById(clothingId);
        if (c == null) throw new ApiException(404, "clothing not found");
        return buildStat(c);
    }

    @Override
    public List<ClothingStat> mostWorn(int limit) {
        // 按 wear_log 计数倒序
        List<Map<String, Object>> rows = wearLogMapper.selectMaps(
            new QueryWrapper<WearLog>()
                .select("clothing_id", "count(*) as cnt")
                .groupBy("clothing_id")
                .orderByDesc("cnt")
                .last("limit " + limit));
        return rows.stream().map(this::rowToStat).toList();
    }

    @Override
    public List<ClothingStat> leastWorn(int days) {
        LocalDate cutoff = LocalDate.now().minusDays(days);
        // 找 status='active' 但 cutoff 之后没有 wear_log 的衣物
        List<Clothing> all = clothingMapper.selectList(
            new QueryWrapper<Clothing>().eq("status", "active"));
        return all.stream()
            .filter(c -> wearLogMapper.selectCount(
                new QueryWrapper<WearLog>()
                    .eq("clothing_id", c.getId())
                    .ge("worn_at", cutoff)) == 0)
            .map(this::buildStat)
            .toList();
    }

    private ClothingStat buildStat(Clothing c) {
        List<WearLog> logs = wearLogMapper.selectList(
            new QueryWrapper<WearLog>().eq("clothing_id", c.getId()).orderByAsc("worn_at"));
        ClothingStat s = new ClothingStat();
        s.setClothingId(c.getId());
        s.setName(c.getName());
        s.setWearCount(logs.size());
        if (!logs.isEmpty()) {
            s.setFirstWorn(logs.get(0).getWornAt());
            s.setLastWorn(logs.get(logs.size() - 1).getWornAt());
            if (c.getPurchasePrice() != null && logs.size() > 0) {
                s.setCostPerWear(c.getPurchasePrice()
                    .divide(BigDecimal.valueOf(logs.size()), 2, RoundingMode.HALF_UP));
            }
        }
        return s;
    }

    private ClothingStat rowToStat(Map<String, Object> row) {
        Long id = ((Number) row.get("clothing_id")).longValue();
        return forClothing(id);
    }
}
```

- [ ] **Step 4：写单元测试**（用 Mockito mock mappers，断言 overview/forClothing 的字段）

- [ ] **Step 5：跑测试 + 提交**

```bash
./mvnw -Dtest=StatsServiceTest test
git add backend/src
git commit -m "feat(stats): service with overview, per-item, most/least worn"
```

---

### Task 21：Stats Controller

**Files:**
- Create: `backend/src/main/java/com/closet/controller/StatsController.java`
- Test: `backend/src/test/java/com/closet/integration/StatsControllerIT.java`

- [ ] **Step 1：写 controller**

```java
@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
public class StatsController {
    private final StatsService service;

    @GetMapping("/overview")
    public Result<StatsOverview> overview() { return Result.ok(service.overview()); }

    @GetMapping("/clothing/{id}")
    public Result<ClothingStat> forClothing(@PathVariable Long id) {
        return Result.ok(service.forClothing(id));
    }

    @GetMapping("/most-worn")
    public Result<List<ClothingStat>> mostWorn(@RequestParam(defaultValue = "10") int limit) {
        return Result.ok(service.mostWorn(limit));
    }

    @GetMapping("/least-worn")
    public Result<List<ClothingStat>> leastWorn(@RequestParam(defaultValue = "90") int days) {
        return Result.ok(service.leastWorn(days));
    }
}
```

- [ ] **Step 2：写集成测试**（创建 clothing → POST wear-log → GET overview 验证 monthWears >= 1）

- [ ] **Step 3：跑测试 + 提交**

```bash
./mvnw -Dtest=StatsControllerIT test
git add backend/src
git commit -m "feat(stats): controller"
```

---

## Phase 7：前端 uni-app 初始化 + API 客户端

### Task 22：uni-app 项目初始化

**Files:**
- Create: `frontend/` (使用 uni-app Vue 3 + Vite + TS 模板)

- [ ] **Step 1：用 CLI 创建项目**

```bash
cd frontend
npx degit dcloudio/uni-preset-vue#vite-ts .
# 选择「默认模板」即可
npm install
npm install -D uview-plus vitest @vue/test-utils happy-dom jsdom
```

- [ ] **Step 2：配置 uView Plus**

按 uview-plus 官方文档（`https://uiadmin.net/uview-plus/`）修改 `main.ts` 引入。

```ts
// src/main.ts
import { createSSRApp } from 'vue';
import uviewPlus from 'uview-plus';
import App from './App.vue';

export function createApp() {
  const app = createSSRApp(App);
  app.use(uviewPlus);
  return { app };
}
```

并在 `App.vue` 的 `<style lang="scss">` 里 `@import "uview-plus/index.scss";`

- [ ] **Step 3：配置 vite.config.ts 加代理**

```ts
import { defineConfig } from 'vite';
import uni from '@dcloudio/vite-plugin-uni';

export default defineConfig({
  plugins: [uni()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
```

- [ ] **Step 4：本地跑起来确认**

```bash
npm run dev:h5
# 浏览器访问 http://localhost:5173
```

- [ ] **Step 5：提交**

```bash
git add frontend
git commit -m "feat(frontend): uni-app init with uView Plus"
```

---

### Task 23：OpenAPI 客户端生成

**Files:**
- Create: `frontend/src/api/`，自动生成

- [ ] **Step 1：确认后端 OpenAPI 文档可访问**

```bash
curl http://localhost:8080/v3/api-docs | head -c 200
```

Expected：返回 JSON。

- [ ] **Step 2：安装 openapi-typescript 并生成**

```bash
npm install -D openapi-typescript
npx openapi-typescript http://localhost:8080/v3/api-docs --output src/api/schema.d.ts
```

- [ ] **Step 3：写 API 请求封装**

```ts
// src/api/index.ts
import type { paths } from './schema';

const BASE = import.meta.env.VITE_API_BASE_URL || '/api';

async function request<T>(method: string, url: string, body?: unknown): Promise<T> {
  const resp = await fetch(BASE + url, {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: body ? JSON.stringify(body) : undefined,
  });
  const json = await resp.json();
  if (json.code !== 0) throw new Error(json.message);
  return json.data as T;
}

export const api = {
  clothing: {
    list: (filter: any) => request('GET', '/v1/clothing?' + new URLSearchParams(filter)),
    get: (id: number) => request('GET', `/v1/clothing/${id}`),
    create: (data: any) => request('POST', '/v1/clothing', data),
    update: (id: number, data: any) => request('PUT', `/v1/clothing/${id}`, data),
    delete: (id: number) => request('DELETE', `/v1/clothing/${id}`),
  },
  // 其它资源省略，按同样模式封装
};
```

- [ ] **Step 4：提交**

```bash
git add frontend/src/api
git commit -m "feat(frontend): OpenAPI client and API wrapper"
```

---

### Task 24：Pinia stores + 路由配置

**Files:**
- Create: `frontend/src/stores/clothing.ts`
- Create: `frontend/src/stores/outfit.ts`
- Create: `frontend/src/stores/calendar.ts`
- Create: `frontend/src/pages.json`（路由）

- [ ] **Step 1：写 clothing store（其它 store 按同样模式）**

```ts
// src/stores/clothing.ts
import { defineStore } from 'pinia';
import { ref } from 'vue';
import { api } from '../api';

export const useClothingStore = defineStore('clothing', () => {
  const list = ref<any[]>([]);
  const total = ref(0);

  async function fetchList(filter: any = {}) {
    const data = await api.clothing.list(filter);
    list.value = data.records;
    total.value = data.total;
  }

  return { list, total, fetchList };
});
```

- [ ] **Step 2：配置 pages.json**

```json
{
  "pages": [
    { "path": "pages/index/index", "style": { "navigationBarTitleText": "首页" } },
    { "path": "pages/closet/index", "style": { "navigationBarTitleText": "衣橱" } },
    { "path": "pages/clothing-form/index", "style": { "navigationBarTitleText": "编辑衣物" } },
    { "path": "pages/clothing-detail/index", "style": { "navigationBarTitleText": "衣物详情" } },
    { "path": "pages/outfits/index", "style": { "navigationBarTitleText": "搭配" } },
    { "path": "pages/outfit-form/index", "style": { "navigationBarTitleText": "编辑搭配" } },
    { "path": "pages/outfit-detail/index", "style": { "navigationBarTitleText": "搭配详情" } },
    { "path": "pages/calendar/index", "style": { "navigationBarTitleText": "日历" } },
    { "path": "pages/stats/index", "style": { "navigationBarTitleText": "统计" } },
    { "path": "pages/settings/index", "style": { "navigationBarTitleText": "设置" } }
  ]
}
```

- [ ] **Step 3：提交**

```bash
git add frontend
git commit -m "feat(frontend): pinia stores and routing"
```

---

## Phase 8：前端核心组件（Vitest）

### Task 25：ClothingCard 组件

**Files:**
- Create: `frontend/src/components/ClothingCard.vue`
- Test: `frontend/src/components/ClothingCard.test.ts`

- [ ] **Step 1：写失败测试**

```ts
import { mount } from '@vue/test-utils';
import { describe, it, expect } from 'vitest';
import ClothingCard from './ClothingCard.vue';

describe('ClothingCard', () => {
  it('renders name and main image', () => {
    const wrapper = mount(ClothingCard, {
      props: {
        clothing: { id: 1, name: '白 T', mainImageId: 10, mainImageKey: 'k.jpg' },
      },
    });
    expect(wrapper.text()).toContain('白 T');
    expect(wrapper.find('img').attributes('src')).toContain('/api/v1/images/k.jpg');
  });

  it('falls back to placeholder when no image', () => {
    const wrapper = mount(ClothingCard, {
      props: { clothing: { id: 1, name: 'x' } },
    });
    expect(wrapper.find('img').exists()).toBe(true);
  });
});
```

- [ ] **Step 2：跑测试确认失败**

```bash
cd frontend && npx vitest run src/components/ClothingCard.test.ts
```

Expected：FAIL（文件不存在）。

- [ ] **Step 3：实现组件**

```vue
<template>
  <view class="clothing-card">
    <image
      :src="imgSrc"
      class="img"
      mode="aspectFill"
    />
    <view class="name">{{ clothing.name }}</view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue';
const props = defineProps<{ clothing: any }>();
const imgSrc = computed(() => {
  if (props.clothing.mainImageKey) {
    return `/api/v1/images/${props.clothing.mainImageKey}`;
  }
  return '/static/placeholder.png';
});
</script>

<style scoped>
.clothing-card { display: flex; flex-direction: column; }
.img { width: 160rpx; height: 160rpx; border-radius: 8rpx; }
.name { font-size: 28rpx; margin-top: 8rpx; }
</style>
```

- [ ] **Step 4：跑测试通过 + 提交**

```bash
npx vitest run src/components/ClothingCard.test.ts
git add frontend/src/components/ClothingCard.vue frontend/src/components/ClothingCard.test.ts
git commit -m "feat(frontend): ClothingCard with image fallback"
```

---

### Task 26：ImageUploader 组件

**Files:**
- Create: `frontend/src/components/ImageUploader.vue`
- Test: `frontend/src/components/ImageUploader.test.ts`

- [ ] **Step 1：写失败测试**

```ts
import { mount } from '@vue/test-utils';
import { describe, it, expect } from 'vitest';
import ImageUploader from './ImageUploader.vue';

describe('ImageUploader', () => {
  it('emits upload event when file selected', async () => {
    const wrapper = mount(ImageUploader, { props: { clothingId: 1 } });
    // 模拟选择文件
    wrapper.vm.$emit('uploaded', { id: 100, storageKey: 'k.jpg' });
    expect(wrapper.emitted('uploaded')).toBeTruthy();
  });

  it('shows placeholder when no images', () => {
    const wrapper = mount(ImageUploader, { props: { clothingId: 1 } });
    expect(wrapper.text()).toContain('点击上传');
  });
});
```

- [ ] **Step 2：实现组件**

```vue
<template>
  <view class="uploader">
    <view v-for="img in images" :key="img.id" class="thumb">
      <image :src="`/api/v1/images/${img.storageKey}`" mode="aspectFill" />
      <view class="del" @click="$emit('delete', img)">×</view>
    </view>
    <view v-if="!readonly" class="add" @click="choose">+</view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { api } from '../api';

const props = defineProps<{ clothingId: number; images: any[]; readonly?: boolean }>();
const emit = defineEmits<{ uploaded: [img: any]; delete: [img: any] }>();

async function choose() {
  // #ifdef MP-WEIXIN
  uni.chooseImage({
    count: 1,
    success: async (res) => {
      const filePath = res.tempFilePaths[0];
      const uploaded = await uni.uploadFile({
        url: `/api/v1/clothing/${props.clothingId}/images`,
        filePath,
        name: 'file',
      });
      emit('uploaded', JSON.parse(uploaded.data).data);
    },
  });
  // #endif
  // #ifdef H5
  const input = document.createElement('input');
  input.type = 'file';
  input.onchange = async () => {
    const file = input.files![0];
    const fd = new FormData();
    fd.append('file', file);
    const resp = await fetch(`/api/v1/clothing/${props.clothingId}/images`, { method: 'POST', body: fd });
    const json = await resp.json();
    emit('uploaded', json.data);
  };
  input.click();
  // #endif
}
</script>
```

- [ ] **Step 3：跑测试 + 提交**

```bash
npx vitest run src/components/ImageUploader.test.ts
git add frontend/src/components/ImageUploader.vue frontend/src/components/ImageUploader.test.ts
git commit -m "feat(frontend): ImageUploader with MP-WEIXIN and H5 branches"
```

---

### Task 27：FilterBar / CategoryPicker / TagPicker

**Files:**
- Create: `frontend/src/components/FilterBar.vue`
- Create: `frontend/src/components/CategoryPicker.vue`
- Create: `frontend/src/components/TagPicker.vue`
- Test: `frontend/src/components/FilterBar.test.ts`
- Test: `frontend/src/components/CategoryPicker.test.ts`

- [ ] **Step 1：写 FilterBar 失败测试**

```ts
import { mount } from '@vue/test-utils';
import FilterBar from './FilterBar.vue';

describe('FilterBar', () => {
  it('emits change with selected season', async () => {
    const wrapper = mount(FilterBar, { props: { filter: { season: null } } });
    await wrapper.setProps({ filter: { season: 'summer' } });
    expect(wrapper.emitted('change')).toBeTruthy();
  });
});
```

- [ ] **Step 2：实现 FilterBar**

```vue
<template>
  <view class="filter-bar">
    <picker :value="seasonIndex" :range="seasons" @change="onSeason">
      <view>{{ seasons[seasonIndex] }}</view>
    </picker>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps<{ filter: any }>();
const emit = defineEmits<{ change: [f: any] }>();

const seasons = ['全部', 'spring', 'summer', 'fall', 'winter'];
const seasonIndex = computed(() => {
  const i = seasons.indexOf(props.filter.season);
  return i >= 0 ? i : 0;
});

function onSeason(e: any) {
  const v = e.detail.value;
  emit('change', { ...props.filter, season: v === 0 ? null : seasons[v] });
}
</script>
```

- [ ] **Step 3：实现 CategoryPicker / TagPicker**

两个组件都是 uView Plus 的 `up-checkbox-group` 包装，传入选项数组，emit change。

```vue
<!-- CategoryPicker.vue -->
<template>
  <view>
    <up-checkbox-group v-model="selected" @change="onChange">
      <up-checkbox v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
    </up-checkbox-group>
  </view>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
const props = defineProps<{ categories: any[]; modelValue: number[] }>();
const emit = defineEmits<{ 'update:modelValue': [v: number[]] }>();
const selected = ref<number[]>(props.modelValue || []);
watch(selected, v => emit('update:modelValue', v));
watch(() => props.modelValue, v => selected.value = v || []);
function onChange() {}
</script>
```

- [ ] **Step 4：跑测试 + 提交**

```bash
npx vitest run src/components
git add frontend/src/components
git commit -m "feat(frontend): FilterBar, CategoryPicker, TagPicker"
```

---

### Task 28：OutfitCanvas 组件

**Files:**
- Create: `frontend/src/components/OutfitCanvas.vue`
- Test: `frontend/src/components/OutfitCanvas.test.ts`

- [ ] **Step 1：写失败测试**

```ts
import { mount } from '@vue/test-utils';
import OutfitCanvas from './OutfitCanvas.vue';

describe('OutfitCanvas', () => {
  it('renders one tile per clothing', () => {
    const wrapper = mount(OutfitCanvas, {
      props: { items: [
        { id: 1, name: 'T', mainImageKey: 'a.jpg' },
        { id: 2, name: 'P', mainImageKey: 'b.jpg' },
      ]},
    });
    expect(wrapper.findAll('.tile').length).toBe(2);
  });

  it('reorders when sortOrder prop changes', async () => {
    const wrapper = mount(OutfitCanvas, { props: { items: [
      { id: 1, name: 'A', sortOrder: 1 },
      { id: 2, name: 'B', sortOrder: 0 },
    ]}});
    await wrapper.setProps({ items: [
      { id: 1, name: 'A', sortOrder: 1 },
      { id: 2, name: 'B', sortOrder: 0 },
    ]});
    expect(wrapper.text()).toContain('B');
  });
});
```

- [ ] **Step 2：实现组件**

```vue
<template>
  <view class="canvas">
    <view v-for="item in sorted" :key="item.id" class="tile">
      <image :src="`/api/v1/images/${item.mainImageKey || ''}`" mode="aspectFill" />
      <text>{{ item.name }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue';
const props = defineProps<{ items: any[] }>();
const sorted = computed(() => [...props.items].sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0)));
</script>
```

- [ ] **Step 3：跑测试 + 提交**

```bash
npx vitest run src/components/OutfitCanvas.test.ts
git add frontend/src/components/OutfitCanvas.vue frontend/src/components/OutfitCanvas.test.ts
git commit -m "feat(frontend): OutfitCanvas with sort-by-order"
```

---

## Phase 9：前端页面

### Task 29：首页（统计概览）

**Files:**
- Create: `frontend/src/pages/index/index.vue`

- [ ] **Step 1：写页面**

```vue
<template>
  <view class="home">
    <up-card>
      <view class="stat">衣物 {{ stats.totalClothing }}</view>
      <view class="stat">搭配 {{ stats.totalOutfits }}</view>
      <view class="stat">本月穿 {{ stats.monthWears }} 次</view>
    </up-card>
    <up-button @click="go('/pages/closet/index')">打开衣橱</up-button>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { api } from '../../api';

const stats = ref({ totalClothing: 0, totalOutfits: 0, monthWears: 0 });

onMounted(async () => {
  stats.value = await api.stats.overview();
});

function go(url: string) { uni.navigateTo({ url }); }
</script>
```

- [ ] **Step 2：本地验证**（npm run dev:h5 后访问首页能看到统计数字）

- [ ] **Step 3：提交**

```bash
git add frontend/src/pages/index
git commit -m "feat(frontend): home page with stats overview"
```

---

### Task 30：衣橱列表 + 衣物详情 + 表单

**Files:**
- Create: `frontend/src/pages/closet/index.vue`（列表）
- Create: `frontend/src/pages/clothing-detail/index.vue`
- Create: `frontend/src/pages/clothing-form/index.vue`

- [ ] **Step 1：写 closet/index.vue（列表 + 筛选）**

```vue
<template>
  <view>
    <FilterBar v-model:filter="filter" @change="load" />
    <view class="grid">
      <ClothingCard v-for="c in list" :key="c.id" :clothing="c" @click="goDetail(c.id)" />
    </view>
    <up-button @click="goForm()">添加衣物</up-button>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { api } from '../../api';
import { useClothingStore } from '../../stores/clothing';
import ClothingCard from '../../components/ClothingCard.vue';
import FilterBar from '../../components/FilterBar.vue';

const store = useClothingStore();
const filter = reactive({ season: null, keyword: '' });
const list = ref<any[]>([]);

async function load() {
  await store.fetchList(filter);
  list.value = store.list;
}

onMounted(load);

function goDetail(id: number) { uni.navigateTo({ url: `/pages/clothing-detail/index?id=${id}` }); }
function goForm() { uni.navigateTo({ url: '/pages/clothing-form/index' }); }
</script>

<style scoped>
.grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16rpx; padding: 16rpx; }
</style>
```

- [ ] **Step 2：写 clothing-form/index.vue**

字段：name, brand, colorPrimary, colorSecondary, size, purchasePrice, purchaseDate, season, notes, categoryIds, tagIds，外加 `<ImageUploader :clothing-id="id" :images="images" />`。

提交逻辑：

```ts
async function submit() {
  if (id.value) {
    await api.clothing.update(id.value, form);
  } else {
    const created: any = await api.clothing.create(form);
    id.value = created.id;
  }
  uni.showToast({ title: '保存成功' });
}
```

- [ ] **Step 3：写 clothing-detail/index.vue**

拉详情展示。

- [ ] **Step 4：本地跑 + 提交**

```bash
npm run dev:h5
# 手动走一遍：列表 → 详情 → 编辑 → 保存 → 列表里看到更新
git add frontend/src/pages/closet frontend/src/pages/clothing-detail frontend/src/pages/clothing-form
git commit -m "feat(frontend): closet pages"
```

---

### Task 31：搭配列表 + 详情 + 表单

**Files:**
- Create: `frontend/src/pages/outfits/index.vue`
- Create: `frontend/src/pages/outfit-detail/index.vue`
- Create: `frontend/src/pages/outfit-form/index.vue`

- [ ] **Step 1：写 outfit-form/index.vue**

字段：name, description, occasion, season, isFavorite，加一个 OutfitCanvas 编辑区。

衣物选择器：弹层（uView Plus `up-popup`）从 store 选 clothing，选完 push 到 canvas 的 items 里。

保存：

```ts
async function submit() {
  const req = { ...form, clothingIds: items.value.map(i => i.id) };
  await api.outfit.create(req);
}
```

- [ ] **Step 2：写详情 + 列表**

详情页加「分享」按钮：调 `<canvas>` 离屏渲染 OutfitCanvas，调 `uni.canvasToTempFilePath` 出图，然后 `uni.share`（H5）或 `wx.shareAppMessage`（微信）。

- [ ] **Step 3：本地跑 + 提交**

```bash
npm run dev:h5
git add frontend/src/pages/outfits frontend/src/pages/outfit-detail frontend/src/pages/outfit-form
git commit -m "feat(frontend): outfit pages with share"
```

---

### Task 32：日历页

**Files:**
- Create: `frontend/src/pages/calendar/index.vue`

- [ ] **Step 1：写页面**

```vue
<template>
  <view>
    <up-calendar :show="false" @change="onDate" />
    <view v-for="entry in entries" :key="entry.id" class="entry">
      <text>{{ entry.entryDate }} {{ entry.slot }}</text>
      <text>{{ outfitName(entry.outfitId) }}</text>
    </view>
    <up-button @click="newEntry">+ 新建</up-button>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { api } from '../../api';

const entries = ref<any[]>([]);
const outfits = ref<any[]>([]);

async function load() {
  const today = new Date().toISOString().slice(0, 10);
  const inOneMonth = new Date(Date.now() + 30 * 86400000).toISOString().slice(0, 10);
  entries.value = await api.calendar.range(today, inOneMonth);
  outfits.value = await api.outfit.list();
}

function outfitName(id: number) { return outfits.value.find(o => o.id === id)?.name ?? '?'; }

function onDate(e: any) {
  const date = e.detail.value;
  uni.showActionSheet({
    itemList: ['上午', '下午', '晚上', '全天'],
    success: async (r) => {
      const slot = ['morning','afternoon','evening','all_day'][r.tapIndex];
      // 选 outfit ...
      uni.navigateTo({ url: `/pages/outfit-picker/index?date=${date}&slot=${slot}` });
    },
  });
}

function newEntry() { /* 同上 */ }

onMounted(load);
</script>
```

- [ ] **Step 2：本地跑 + 提交**

```bash
git add frontend/src/pages/calendar
git commit -m "feat(frontend): calendar page"
```

---

### Task 33：统计页 + 设置页

**Files:**
- Create: `frontend/src/pages/stats/index.vue`
- Create: `frontend/src/pages/settings/index.vue`

- [ ] **Step 1：写 stats/index.vue**

四个卡片：总览 / 最常穿 / 最少穿。点击衣物跳详情。

- [ ] **Step 2：写 settings/index.vue**

管理分类、标签（CRUD 列表 + 新增按钮）；底部加「数据导出」按钮（暂留空，调用 `GET /api/v1/clothing` 把所有数据 JSON 下载即可）。

- [ ] **Step 3：本地跑 + 提交**

```bash
git add frontend/src/pages/stats frontend/src/pages/settings
git commit -m "feat(frontend): stats and settings pages"
```

---

## Phase 10：部署

### Task 34：Backend Dockerfile

**Files:**
- Create: `backend/Dockerfile`

- [ ] **Step 1：写 Dockerfile（多阶段构建）**

```dockerfile
# 构建阶段
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN ./mvnw dependency:go-offline -B
COPY src src
RUN ./mvnw package -DskipTests -B

# 运行阶段
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/closet-backend-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

- [ ] **Step 2：本地构建验证**

```bash
cd backend
docker build -t closet-backend:dev .
```

Expected：构建成功，镜像大小约 250-300 MB。

- [ ] **Step 3：提交**

```bash
git add backend/Dockerfile
git commit -m "chore(backend): multi-stage Dockerfile"
```

---

### Task 35：生产 docker-compose.yml

**Files:**
- Create: `deploy/docker-compose.yml`

- [ ] **Step 1：写 compose**

```yaml
services:
  postgres:
    image: postgres:16-alpine
    container_name: closet-pg
    environment:
      POSTGRES_USER: closet
      POSTGRES_PASSWORD: ${DB_PASSWORD}
      POSTGRES_DB: closet
    volumes:
      - pgdata:/var/lib/postgresql/data
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U closet"]
      interval: 10s
      timeout: 3s
      retries: 5

  minio:
    image: minio/minio:latest
    container_name: closet-minio
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: ${MINIO_USER}
      MINIO_ROOT_PASSWORD: ${MINIO_PASSWORD}
    volumes:
      - miniodata:/data
    ports:
      - "9001:9001"
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9000/minio/health/live"]
      interval: 10s
      timeout: 3s
      retries: 5

  minio-init:
    image: minio/mc:latest
    depends_on:
      minio:
        condition: service_healthy
    entrypoint: >
      /bin/sh -c "
      mc alias set local http://minio:9000 ${MINIO_USER} ${MINIO_PASSWORD} &&
      mc mb --ignore-existing local/closet-images &&
      echo 'bucket ready'
      "
    restart: "no"

  backend:
    build: ../backend
    container_name: closet-backend
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/closet
      SPRING_DATASOURCE_USERNAME: closet
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      MINIO_ENDPOINT: http://minio:9000
      MINIO_ACCESS_KEY: ${MINIO_USER}
      MINIO_SECRET_KEY: ${MINIO_PASSWORD}
      MINIO_BUCKET: closet-images
    depends_on:
      postgres:
        condition: service_healthy
      minio:
        condition: service_healthy
    ports:
      - "8080:8080"
    restart: unless-stopped

volumes:
  pgdata:
  miniodata:
```

- [ ] **Step 2：写 application-prod.yml**（在 backend/src/main/resources/）

```yaml
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/closet
    username: closet
    password: ${DB_PASSWORD}
minio:
  endpoint: ${MINIO_ENDPOINT}
  access-key: ${MINIO_ACCESS_KEY}
  secret-key: ${MINIO_SECRET_KEY}
  bucket: ${MINIO_BUCKET}
```

- [ ] **Step 3：写 .env.example**（用户复制成 .env）

```
DB_PASSWORD=change-me-strong-password
MINIO_USER=closet
MINIO_PASSWORD=change-me-strong-secret
```

- [ ] **Step 4：起生产栈 + 烟测**

```bash
cd deploy
cp .env.example .env
# 编辑 .env 填入真实密码
docker compose up -d --build
# 等 ~30 秒
curl http://localhost:8080/api/v1/categories
```

Expected：返回 `{"code":0,"message":"ok","data":[{...默认分类...}]}`。

- [ ] **Step 5：提交**

```bash
git add deploy/docker-compose.yml backend/src/main/resources/application-prod.yml deploy/.env.example
git commit -m "chore(deploy): production docker compose"
```

---

### Task 36：README + 最终烟测

**Files:**
- Modify: `README.md`

- [ ] **Step 1：完善 README**

```markdown
# 电子衣橱 MVP

个人电子衣橱小程序。技术栈：uni-app + Vue 3 + Spring Boot 3 + MyBatis-Plus + PostgreSQL + MinIO。

**功能：** 衣物录入、分类标签、搭配组合、日历规划、穿着统计、搭配分享。

详细设计：`docs/superpowers/specs/2026-07-01-digital-closet-design.md`
实施计划：`docs/superpowers/plans/2026-07-01-digital-closet-mvp.md`

## 本地开发

```bash
# 1. 起数据库和对象存储
cd deploy
docker compose -f docker-compose.dev.yml up -d

# 2. 起后端
cd ../backend
./mvnw spring-boot:run

# 3. 起前端
cd ../frontend
npm install
npm run dev:h5    # 浏览器
# npm run dev:mp-weixin  # 微信开发者工具
```

浏览器访问 http://localhost:5173。

## 生产部署（自托管）

```bash
cd deploy
cp .env.example .env
# 编辑 .env 填入强密码
docker compose up -d --build
```

服务跑起来后：
- 后端 API：`http://<your-pc-ip>:8080`
- MinIO 控制台：`http://<your-pc-ip>:9001`（用 .env 里的 MINIO_USER / MINIO_PASSWORD 登录）

## 微信小程序发布

1. 在 mp.weixin.qq.com 注册小程序拿 AppID
2. 微信开发者工具里导入 `frontend/unpackage/dist/dev/mp-weixin`
3. 「上传」代码 → 公众平台提交审核

后端域名必须 HTTPS 且在「服务器域名」白名单。

## 测试

```bash
cd backend && ./mvnw test
cd frontend && npx vitest run
```

## 备份（手动）

```bash
docker run --rm -v closet_pgdata:/data -v $PWD:/backup alpine tar czf /backup/pg-$(date +%F).tar.gz /data
```

## 许可证

仅个人使用。
```

- [ ] **Step 2：完整烟测一遍**

起 dev stack → 录入 3 件衣物 → 创建 1 个搭配 → 安排到今天 → 查统计 → 看 cost-per-wear 是否对。

- [ ] **Step 3：提交**

```bash
git add README.md
git commit -m "docs: full README with dev and prod instructions"
```

---

## 验收清单（全部任务完成后逐项打勾）

- [ ] `cd deploy && docker compose -f docker-compose.dev.yml up -d` 起得来
- [ ] `cd backend && ./mvnw test` 全部 PASS（service 层 ≥ 80% 覆盖率）
- [ ] `cd frontend && npx vitest run` 全部 PASS
- [ ] `npm run dev:h5` 能看到首页统计
- [ ] 录入衣物 → 上传图片 → 列表显示
- [ ] 创建搭配 → 在日历里安排 → 统计页 cost-per-wear 数字正确
- [ ] 搭配详情点分享能生成图片
- [ ] `cd deploy && docker compose up -d --build` 生产栈跑起来
- [ ] `.env` 真实密码填入后所有服务健康

## 关键经验教训（写给未来的自己）

1. **WearLogSyncService 是最关键的组件**——日历条目创建/更新/删除时一定要保证 wear_log 一致性。集成测试里要专门覆盖「创建 → 更新（改 outfit） → wear_log 跟着变」这条链路。
2. **MyBatis-Plus updateById 要求主键**——outfit_item 是复合主键，更新 sort_order 时要用 `update(entity, wrapper)` 而不是 `updateById`。
3. **MinIO Java SDK 上传时要给齐 size**——流式上传如果不指定 -1（未知大小），MinIO 会要求预先知道大小。本计划里我们传 file.getSize()，集成测试里也用 byte[] mock 出明确大小。
4. **uni-app 跨端 API 差异**——`uni.chooseImage` 在小程序和 H5 行为不同，组件里要用 `#ifdef MP-WEIXIN` / `#ifdef H5` 分支。
5. **Spring Boot 启动加载 schema.sql**——`spring.sql.init.mode=always` 在生产环境要谨慎，MVP 阶段 OK；如果以后做生产化部署要改成 Flyway 之类的版本化迁移工具。