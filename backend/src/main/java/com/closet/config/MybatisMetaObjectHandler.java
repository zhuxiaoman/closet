package com.closet.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * Auto-fills {@code created_at} on insert and {@code updated_at} on insert + update for
 * MyBatis-Plus entities that declare the corresponding {@code @TableField(fill = ...)}.
 * Wired automatically via {@link Component}.
 *
 * <p>Uses the {@code (MetaObject, String, Supplier, Class)} overload of
 * {@code strictInsertFill} / {@code strictUpdateFill}; the other overload
 * {@code (MetaObject, String, Class, E)} requires an already-resolved value
 * and cannot accept a method reference like {@code OffsetDateTime::now}.
 */
@Component
public class MybatisMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createdAt", OffsetDateTime::now, OffsetDateTime.class);
        this.strictInsertFill(metaObject, "updatedAt", OffsetDateTime::now, OffsetDateTime.class);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updatedAt", OffsetDateTime::now, OffsetDateTime.class);
    }
}
