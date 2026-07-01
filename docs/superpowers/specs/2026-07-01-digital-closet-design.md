# Digital Closet (电子衣橱) Design Spec

- **Date:** 2026-07-01
- **Status:** Approved (pending user review of this written spec)
- **Scope:** MVP for personal use — single user, self-hosted on user's PC.

## 1. Overview

A digital closet (电子衣橱) that lets the owner catalog clothing, compose outfits, plan what to wear via a calendar, view usage statistics, and share outfits as images. First version is single-user, self-hosted, with no authentication.

### Goals

- Catalog every clothing item with photos, category, color, season, tags, purchase price.
- Combine items into outfits and edit/save them.
- Plan daily outfits on a calendar; support multiple outfits per day.
- Surface useful stats (most/least worn, cost-per-wear, items not worn in N days).
- Share an outfit by exporting it as an image (handled by the frontend canvas-to-image export; no backend work needed).

### Non-Goals (YAGNI for MVP)

- Multi-user / social features (friend system, comments, public profiles).
- AI auto-tagging / category detection from images.
- E-commerce / shopping integrations.
- Native iOS/Android push notifications.

## 2. Tech Stack

| Layer | Choice |
|-------|--------|
| Frontend framework | uni-app (Vue 3 + TypeScript) |
| Frontend UI library | uView Plus |
| Frontend state | Pinia |
| Frontend build | Vite |
| Frontend testing | Vitest + @vue/test-utils (core components only) |
| Backend language | Java 21 |
| Backend framework | Spring Boot 3.3+ (Spring Web) |
| ORM | MyBatis-Plus (`mybatis-plus-boot-starter`) |
| Database | PostgreSQL 16 |
| DB schema management | `schema.sql` + `data.sql` loaded by Spring Boot on startup (no Flyway) |
| Object storage | MinIO (S3-compatible) |
| Image SDK | MinIO Java SDK |
| Build | Maven |
| Backend testing | JUnit 5 + Mockito + Testcontainers (real PG + MinIO via Docker) |
| API docs | springdoc-openapi → generated OpenAPI 3 spec |

## 3. Architecture

```
┌──────────────────────────────┐
│ Client (uni-app, single code)│
│  - WeChat Mini Program       │
│  - H5 (mobile / desktop)     │
│  - iOS / Android App         │
└─────────────┬────────────────┘
              │ HTTPS REST (no auth in MVP)
              ▼
┌──────────────────────────────┐
│ Backend (Spring Boot)        │
│  - REST controllers          │
│  - service layer             │
│  - MyBatis-Plus mappers      │
│  - MinIO client              │
└────┬──────────────────┬──────┘
     │ SQL              │ S3 protocol
     ▼                  ▼
┌──────────┐      ┌──────────┐
│PostgreSQL│      │  MinIO   │
└──────────┘      └──────────┘
```

### Data flow — image upload

1. Client picks image → `POST /api/v1/clothing/{id}/images` (multipart).
2. Backend uploads to MinIO, stores resulting `storage_key` in `clothing_image` table, returns the image record.
3. Client gets the image record back; subsequent reads use `GET /api/v1/images/{key}` which the backend proxies from MinIO.

### Data flow — image download

- Client requests `GET /api/v1/images/{key}`.
- Backend fetches the object from MinIO and streams the bytes back (with `ETag`, `Cache-Control` for client caching). No Redis cache in MVP.

### Data flow — calendar entry create

1. Client `POST /api/v1/calendar` with `{ date, slot, outfitId, notes }`.
2. Backend writes `calendar_entry`.
3. Backend reads `outfit_item` rows for that outfit and writes one `wear_log` per item with `worn_at = entry.date`.
4. Backend returns the saved entry.

## 4. Data Model

### Tables

**`category`** — clothing categories (supports two levels)

| Column | Type | Notes |
|--------|------|-------|
| id | bigserial PK | |
| name | varchar(64) | not null |
| parent_id | bigint FK→category.id nullable | for sub-categories |
| sort_order | int | default 0 |

**`clothing`** — clothing items

| Column | Type | Notes |
|--------|------|-------|
| id | bigserial PK | |
| name | varchar(128) | not null |
| brand | varchar(64) | nullable |
| color_primary | varchar(32) | hex or named |
| color_secondary | varchar(32) | nullable |
| size | varchar(32) | nullable |
| purchase_price | numeric(10,2) | nullable |
| purchase_date | date | nullable |
| season | varchar(16) | enum: spring/summer/fall/winter/all |
| notes | text | nullable |
| status | varchar(16) | enum: active/discarded/donated/sold, default active |
| main_image_id | bigint | nullable, points to clothing_image.id |
| created_at | timestamptz | |
| updated_at | timestamptz | |

**`clothing_image`** — images for a clothing item

| Column | Type | Notes |
|--------|------|-------|
| id | bigserial PK | |
| clothing_id | bigint FK→clothing.id | not null, on delete cascade |
| storage_key | varchar(255) | MinIO object key |
| sort_order | int | default 0 |
| is_main | boolean | default false |
| created_at | timestamptz | |

**`tag`** — free-form tags

| Column | Type | Notes |
|--------|------|-------|
| id | bigserial PK | |
| name | varchar(64) | unique |

**`clothing_category`** — M:N clothing ↔ category

| Column | Type |
|--------|------|
| clothing_id | bigint FK→clothing.id |
| category_id | bigint FK→category.id |
| PK | (clothing_id, category_id) |

**`clothing_tag`** — M:N clothing ↔ tag

| Column | Type |
|--------|------|
| clothing_id | bigint FK |
| tag_id | bigint FK |
| PK | (clothing_id, tag_id) |

**`outfit`** — saved outfit

| Column | Type | Notes |
|--------|------|-------|
| id | bigserial PK | |
| name | varchar(128) | not null |
| description | text | nullable |
| occasion | varchar(64) | nullable (casual/work/sport/...) |
| season | varchar(16) | nullable |
| is_favorite | boolean | default false |
| cover_image_id | bigint | nullable |
| created_at | timestamptz | |
| updated_at | timestamptz | |

**`outfit_item`** — M:N outfit ↔ clothing with ordering

| Column | Type |
|--------|------|
| outfit_id | bigint FK→outfit.id |
| clothing_id | bigint FK→clothing.id |
| sort_order | int | default 0 (layer order, e.g., base=0, mid=1, outer=2) |
| PK | (outfit_id, clothing_id) |

**`calendar_entry`** — one row per planned outfit for a date+slot

| Column | Type | Notes |
|--------|------|-------|
| id | bigserial PK | |
| entry_date | date | not null |
| slot | varchar(16) | enum: morning/afternoon/evening/all_day, default all_day |
| outfit_id | bigint FK→outfit.id | not null |
| notes | text | nullable |
| created_at | timestamptz | |
| updated_at | timestamptz | |

Multiple entries per date are allowed (different `slot`s or repeated slots).

**`wear_log`** — append-only log of each wear

| Column | Type | Notes |
|--------|------|-------|
| id | bigserial PK | |
| clothing_id | bigint FK→clothing.id | not null |
| calendar_entry_id | bigint FK→calendar_entry.id | nullable (null = manual log) |
| worn_at | date | not null |
| created_at | timestamptz | |

### wear_log rules

- **Auto-generated** when a `calendar_entry` is created/updated: one log row per `outfit_item` in the entry's outfit, with `worn_at = entry.entry_date`.
- **Cascade delete** when the source `calendar_entry` is deleted.
- **Preserved** when the outfit itself is deleted (the wear actually happened).
- **Manual insert** supported via `POST /api/v1/wear-logs` for items worn outside any planned outfit.
- **Stats queries** read this table directly (denormalized on purpose for fast reads).

## 5. Module Structure

### Frontend (uni-app)

```
src/
├── pages/
│   ├── index/                # Home: stats summary + recent
│   ├── closet/               # Clothing list w/ filter
│   ├── clothing-detail/
│   ├── clothing-form/        # Create / edit + image upload
│   ├── outfits/              # Outfit list
│   ├── outfit-detail/
│   ├── outfit-form/          # Compose outfit from clothing
│   ├── calendar/             # Calendar view + day detail
│   ├── stats/
│   └── settings/             # Manage categories, tags, data export
├── components/
│   ├── ClothingCard.vue
│   ├── OutfitCanvas.vue      # Compose / preview outfit
│   ├── ImageUploader.vue
│   ├── FilterBar.vue
│   ├── CategoryPicker.vue
│   └── TagPicker.vue
├── api/                      # Generated from OpenAPI (openapi-typescript)
├── stores/                   # Pinia stores
└── utils/
```

### Backend (Spring Boot, package `com.closet`)

```
src/main/java/com/closet/
├── controller/   # REST controllers
├── service/      # Business logic incl. WearLogSyncService
├── mapper/       # MyBatis-Plus mappers
├── entity/       # DB entities
├── dto/          # Request / response objects
├── config/       # MyBatis-PlusConfig, MinioConfig, CorsConfig
├── common/       # GlobalExceptionHandler, Result wrapper, PageRequest
└── storage/      # MinioStorageService wrapper
```

## 6. API (versioned `/api/v1`)

### Clothing

| Method | Path | Description |
|--------|------|-------------|
| GET | `/clothing` | List with filters: `categoryId`, `tagId`, `season`, `status`, `keyword`, `page`, `size` |
| GET | `/clothing/{id}` | Detail |
| POST | `/clothing` | Create |
| PUT | `/clothing/{id}` | Update |
| DELETE | `/clothing/{id}` | Soft-delete (set `status=discarded`) by default |
| POST | `/clothing/{id}/images` | Multipart upload (single image per request) |
| DELETE | `/clothing/{id}/images/{imageId}` | Remove image and MinIO object |

### Category / Tag

Standard CRUD: `GET/POST/PUT/DELETE /categories`, `GET/POST/PUT/DELETE /tags`.

### Outfit

| Method | Path | Description |
|--------|------|-------------|
| GET | `/outfits` | List with `season`, `occasion`, `favorite` filters |
| GET | `/outfits/{id}` | Detail (includes items) |
| POST | `/outfits` | Create |
| PUT | `/outfits/{id}` | Update |
| DELETE | `/outfits/{id}` | Delete (does not touch wear_log) |
| POST | `/outfits/{id}/items` | Add clothing; body: `{ clothingId, sortOrder }` |
| PUT | `/outfits/{id}/items/reorder` | Reorder all items; body: `[{ clothingId, sortOrder }, ...]` |
| DELETE | `/outfits/{id}/items/{clothingId}` | Remove clothing from outfit |

### Calendar

| Method | Path | Description |
|--------|------|-------------|
| GET | `/calendar?from=YYYY-MM-DD&to=YYYY-MM-DD` | Entries in range |
| GET | `/calendar/{id}` | Detail |
| POST | `/calendar` | Create — triggers wear_log generation |
| PUT | `/calendar/{id}` | Update — diffs items to adjust wear_log |
| DELETE | `/calendar/{id}` | Delete — cascades wear_log for this entry |

### Wear log

| Method | Path | Description |
|--------|------|-------------|
| POST | `/wear-logs` | Manual log: `{ clothingId, wornAt }` |
| DELETE | `/wear-logs/{id}` | Remove a manual log |

### Images

| Method | Path | Description |
|--------|------|-------------|
| GET | `/images/{key}` | Backend streams from MinIO |

### Statistics

| Method | Path | Description |
|--------|------|-------------|
| GET | `/stats/overview` | Totals: clothing, outfits, this-month wears |
| GET | `/stats/clothing/{id}` | Per-item: worn count, first/last worn, cost-per-wear |
| GET | `/stats/most-worn?limit=10` | Top worn items |
| GET | `/stats/least-worn?days=90` | Items not worn in 90 days |

### Conventions

- All responses wrapped in `{ code: 0|!0, data, message }`.
- Errors throw a typed exception handled by `GlobalExceptionHandler` to produce the same envelope.
- Pagination via `?page=1&size=20` returning `{ list, total, page, size }`.
- OpenAPI spec published at `/v3/api-docs` for client codegen.

## 7. Key Flows

### Add clothing

1. User opens `clothing-form`, picks images from phone.
2. Frontend uploads each image via `POST /clothing/{id}/images` after creating the clothing row first (or via draft pattern — TBD in plan phase).
3. Frontend sets categories, tags, season, price, etc.
4. Save → `POST /clothing` with metadata + image keys.

### Compose outfit

1. User opens `outfit-form`, picks clothing from list.
2. Drag to reorder (sets `sort_order`).
3. Pick cover image, fill name/occasion/season.
4. Save → `POST /outfits` then `POST /outfits/{id}/items` per clothing.

### Plan calendar

1. User opens `calendar` page, taps a date.
2. Picks slot + outfit.
3. Save → `POST /calendar` → backend writes wear_log for each outfit_item.

### Share outfit

1. User opens outfit detail, taps "Share".
2. Frontend renders `OutfitCanvas` to image via `<canvas>` `toDataURL`.
3. Native share API (or WeChat `wx.shareAppMessage` on the MP).

## 8. Deployment

### Local dev

```bash
cd deploy
docker compose -f docker-compose.dev.yml up -d   # PG + MinIO
cd ../backend
./mvnw spring-boot:run
# in another shell
cd ../frontend
npm install
npm run dev:h5           # browser
npm run dev:mp-weixin    # WeChat devtools
```

`docker-compose.dev.yml` exposes PG on `5432` and MinIO on `9000`/`9001` for direct local access.

### Production (self-hosted on user's PC)

`deploy/docker-compose.yml` runs three services:

- `postgres` (PostgreSQL 16-alpine) with named volume `pgdata` and init script `postgres/init.sql` mounted to `/docker-entrypoint-initdb.d/`.
- `minio` (latest) with named volume `miniodata`, console on `:9001`, API on `:9000`.
- `backend` (built from `backend/Dockerfile`) depending on both; exposed on `:8080`.

Environment variables (loaded from `.env`):

```
DB_PASSWORD=...
MINIO_USER=...
MINIO_PASSWORD=...
```

### Networking

- H5 access on local network: just `http://<pc-ip>:8080`. Frontend dev points at that.
- WeChat Mini Program: requires a public HTTPS domain. For MVP, use **Cloudflare Tunnel** (`cloudflared`) to expose `localhost:8080` as `https://closet.example.com` without port-forwarding. Add the domain to WeChat MP's "server domain" whitelist.

### Storage init

On first MinIO start, a one-shot init container creates the `closet-images` bucket. Implementation detail in plan phase.

### Backups

**Out of scope for MVP.** Data sits in Docker named volumes. User can manually back up via `docker run --rm -v closet_pgdata:/data -v $PWD:/backup alpine tar czf /backup/pg-$(date +%F).tar.gz /data` if/when they want.

## 9. Testing

### Backend

- **Unit:** service layer with JUnit 5 + Mockito. Target ≥ 80% line coverage on `service/`.
- **Integration:** `@SpringBootTest` + Testcontainers spinning real PostgreSQL + MinIO. Cover all controllers end-to-end (mapper → service → controller).
- **Test layout:** `src/test/java/com/closet/{unit,integration}`.

### Frontend

- **Component tests** for `ClothingCard`, `OutfitCanvas`, `ImageUploader`, `FilterBar`, `CategoryPicker`, `TagPicker` via Vitest + @vue/test-utils + happy-dom.
- **API client** validated against the OpenAPI spec at build time (TypeScript types).
- **E2E:** deferred. Will revisit after MVP ships.

### CI

Out of scope for MVP. The user will run tests manually.

## 10. Open Questions / Future Work

1. **Image processing pipeline** — should the backend auto-generate thumbnails on upload (saves bandwidth on the list view)? Not in MVP, but cheap to add with `thumbnailator`.
2. **Multi-user later** — when expanding to friends / community, the schema needs `users` and an `owner_id` column on every table. Easy to retrofit since no rows exist yet.
3. **AI tagging** — auto-detect category / color / season from image. Pluggable later as a service called from `ClothingService.create`.
4. **Backup automation** — when the user feels data is valuable enough, add a cron / scheduled task.
5. **H5 ↔ WeChat MP behavior parity** — test both targets; some uni-app APIs differ (image picker, share, login).

## 11. Out-of-Scope Confirmation

The following were considered and explicitly deferred:

- PIN / password lock on app open.
- Redis caching of images.
- Cron-based automated backups.
- Frontend E2E tests.
- Multi-user / social features.