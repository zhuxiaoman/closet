package com.closet.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request body for POST /api/v1/outfit and PUT /api/v1/outfit/{id}.
 * Only {@code name} is mandatory. {@code clothingIds} are persisted as
 * outfit_item rows in order (index becomes sort_order). On update the
 * existing items are fully replaced with whatever is supplied here.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OutfitRequest {

    @NotBlank
    private String name;

    private String description;
    private String occasion;
    private String season;
    private Boolean isFavorite;
    private Long coverImageId;

    /** Optional list of clothing ids; index becomes the sort_order (0-based). */
    private List<Long> clothingIds;
}