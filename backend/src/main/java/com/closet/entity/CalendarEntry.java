package com.closet.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@TableName("calendar_entry")
public class CalendarEntry {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("entry_date")
    private LocalDate entryDate;

    private String slot;

    @TableField("outfit_id")
    private Long outfitId;

    private String notes;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
