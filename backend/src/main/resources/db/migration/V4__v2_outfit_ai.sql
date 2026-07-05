-- V4: AI 智能搭配生成历史表 (v2.0)
-- 用于保存 5 套搭配的生成记录和用户反馈

CREATE TABLE IF NOT EXISTS outfit_ai_generation (
  id                 BIGSERIAL PRIMARY KEY,
  seed_clothing_ids  JSONB NOT NULL,
  occasion           VARCHAR(32),
  season             VARCHAR(16),
  result_outfit_ids  JSONB,
  feedback           VARCHAR(16) NOT NULL DEFAULT 'none',
  created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ai_generation_created ON outfit_ai_generation(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ai_generation_feedback ON outfit_ai_generation(feedback) WHERE feedback != 'none';