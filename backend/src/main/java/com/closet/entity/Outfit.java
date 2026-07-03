package com.closet.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("outfit")
public class Outfit {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String description;
    private String occasion;
    private String season;

    @TableField("is_favorite")
    private Boolean isFavorite;

    @TableField("cover_image_id")
    private Long coverImageId;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
