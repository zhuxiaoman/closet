-- V5: relax outfit_ai_generation JSONB columns to TEXT.
-- MyBatis-Plus 3.5.5 + JacksonTypeHandler (and even plain String + PG
-- driver) does not auto-cast varchar to jsonb on INSERT, so the column
-- type is dropped to TEXT. The application layer still treats the
-- value as opaque JSON (encoded via ObjectMapper in OutfitAiServiceImpl)
-- so query helpers in v2.1 can switch back to a native jsonb column
-- without changing business semantics.
ALTER TABLE outfit_ai_generation
    ALTER COLUMN seed_clothing_ids  TYPE TEXT USING seed_clothing_ids::TEXT,
    ALTER COLUMN result_outfit_ids TYPE TEXT USING result_outfit_ids::TEXT;