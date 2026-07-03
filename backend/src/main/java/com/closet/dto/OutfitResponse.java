package com.closet.dto;

import com.closet.entity.Clothing;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Response shape for GET / POST / PUT /api/v1/outfit*. Mirrors the
 * {@code outfit} table and additionally exposes the resolved clothing
 * items in the order defined by {@code outfit_item.sort_order}.
 */
@Data
public class OutfitResponse {

    private Long id;
    private String name;
    private String description;
    private String occasion;
    private String season;
    private Boolean isFavorite;
    private Long coverImageId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    private List<Clothing> items;
}