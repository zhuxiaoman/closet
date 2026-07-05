package com.closet.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * Persisted AI outfit generation history. Each row records the user
 * context (season / occasion / weather) and the 5 outfits produced,
 * along with the user's later verdict (like / dislike / none).
 *
 * <p>{@code seedClothingIds} and {@code resultOutfitIds} are stored as
 * JSON-encoded strings so they ride through MyBatis as plain VARCHAR.
 * Going through {@link com.closet.service.impl.OutfitAiServiceImpl}'s
 * {@code ObjectMapper} keeps the JSON shape stable while sidestepping
 * the MyBatis-Plus 3.5.5 + Postgres JSONB combination where
 * {@code JacksonTypeHandler}'s varchar output is not implicitly cast
 * to {@code jsonb} by the driver. The application treats the column
 * as an opaque JSON payload; query helpers in v2.1 can swap to a
 * native jsonb query without touching this row shape.
 */
@Data
@TableName("outfit_ai_generation")
public class OutfitAiGeneration {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** JSON array of long ids, e.g. {@code [1,7]}. */
    @TableField("seed_clothing_ids")
    private String seedClothingIds;

    private String occasion;

    private String season;

    /** JSON array of arrays of long ids, e.g. {@code [[1,7],[1,8]]}. */
    @TableField("result_outfit_ids")
    private String resultOutfitIds;

    private String feedback;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
}