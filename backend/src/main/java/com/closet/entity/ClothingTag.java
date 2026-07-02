package com.closet.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * Junction table linking {@link Clothing} to {@link Tag}. See {@link ClothingCategory}
 * for the composite-key note.
 */
@Data
@TableName("clothing_tag")
public class ClothingTag {

    @TableField("clothing_id")
    private Long clothingId;

    @TableField("tag_id")
    private Long tagId;
}
