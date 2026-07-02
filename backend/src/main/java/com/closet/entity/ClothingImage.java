package com.closet.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * Image attached to a {@link Clothing}. The actual bytes live in MinIO; this
 * row stores only the object key plus display flags. The {@code created_at}
 * field is filled by {@link com.closet.config.MybatisMetaObjectHandler}.
 */
@Data
@TableName("clothing_image")
public class ClothingImage {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("clothing_id")
    private Long clothingId;

    @TableField("storage_key")
    private String storageKey;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("is_main")
    private Boolean isMain;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
}