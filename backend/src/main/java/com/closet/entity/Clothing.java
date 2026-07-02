package com.closet.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@TableName("clothing")
public class Clothing {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String brand;

    @TableField("color_primary")
    private String colorPrimary;

    @TableField("color_secondary")
    private String colorSecondary;

    private String size;

    @TableField("purchase_price")
    private BigDecimal purchasePrice;

    @TableField("purchase_date")
    private LocalDate purchaseDate;

    private String season;
    private String status;
    private String notes;

    @TableField("main_image_id")
    private Long mainImageId;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
