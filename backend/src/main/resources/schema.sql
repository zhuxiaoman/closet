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
