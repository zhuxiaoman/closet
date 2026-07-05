package com.closet.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@TableName(value = "outfit_ai_generation", autoResultMap = true)
public class OutfitAiGeneration {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(value = "seed_clothing_ids", typeHandler = JacksonTypeHandler.class)
    private List<Long> seedClothingIds;

    private String occasion;

    private String season;

    @TableField(value = "result_outfit_ids", typeHandler = JacksonTypeHandler.class)
    private List<List<Long>> resultOutfitIds;

    private String feedback;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
}