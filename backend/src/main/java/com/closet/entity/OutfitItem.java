package com.closet.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("outfit_item")
public class OutfitItem {

    @TableField("outfit_id")
    private Long outfitId;

    @TableField("clothing_id")
    private Long clothingId;

    @TableField("sort_order")
    private Integer sortOrder;
}
