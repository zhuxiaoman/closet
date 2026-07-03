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
@TableName("wear_log")
public class WearLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("clothing_id")
    private Long clothingId;

    @TableField("calendar_entry_id")
    private Long calendarEntryId;

    @TableField("worn_at")
    private LocalDate wornAt;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
}
