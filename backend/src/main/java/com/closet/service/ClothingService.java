package com.closet.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.closet.dto.ClothingFilter;
import com.closet.dto.ClothingRequest;
import com.closet.dto.ClothingResponse;
import com.closet.entity.Clothing;

/**
 * Business operations for the {@code clothing} table and its M:N joins.
 * Implementation lives in {@code ClothingServiceImpl}.
 */
public interface ClothingService {

    IPage<ClothingResponse> page(ClothingFilter filter);

    ClothingResponse get(Long id);

    Clothing create(ClothingRequest req);

    Clothing update(Long id, ClothingRequest req);

    /**
     * Marks the row as discarded (soft delete). The row stays in the table
     * so historical wear logs and outfit items still resolve.
     */
    void softDelete(Long id);
}
