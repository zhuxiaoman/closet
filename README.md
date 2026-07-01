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