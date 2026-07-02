package com.closet.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * Junction table linking {@link Clothing} to {@link Category}. Composite primary key
 * (clothing_id, category_id) is enforced by the database; MyBatis-Plus treats it as a
 * regular bean so {@code insert}/{@code delete-by-wrapper} from T11 just work.
 */
@Data
@TableName("clothing_category")
public class ClothingCategory {

    @TableField("clothing_id")
    private Long clothingId;

    @TableField("category_id")
    private Long categoryId;
}
