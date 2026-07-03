package com.closet.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.closet.dto.OutfitRequest;
import com.closet.dto.OutfitResponse;
import com.closet.entity.Outfit;

import java.util.List;

/**
 * Business operations for the {@code outfit} table and its {@code outfit_item}
 * join rows. Implementation lives in {@code OutfitServiceImpl}.
 */
public interface OutfitService {

    IPage<OutfitResponse> page(String season, String occasion, Boolean favorite, int page, int size);

    OutfitResponse get(Long id);

    /** Creates an outfit and (optionally) its initial outfit_item rows. */
    Outfit create(OutfitRequest req);

    /** Updates an outfit and fully replaces its outfit_item rows. */
    Outfit update(Long id, OutfitRequest req);

    /**
     * Hard-deletes the outfit and cascades deletion of its outfit_item rows.
     * NOTE: schema wires calendar_entry.outfit_id with ON DELETE RESTRICT,
     * so callers must detach the outfit from calendar_entry first.
     */
    void delete(Long id);

    void addItem(Long outfitId, Long clothingId, int sortOrder);

    void removeItem(Long outfitId, Long clothingId);

    /**
     * Bulk reorder. Each {@link ItemOrder} maps to an existing
     * outfit_item row identified by (outfitId, clothingId); the sort_order
     * on that row is updated to the value carried by the entry.
     */
    void reorderItems(Long outfitId, List<ItemOrder> orders);

    record ItemOrder(Long clothingId, int sortOrder) {}
}