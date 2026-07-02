package com.closet.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Request body for POST /api/v1/clothing and PUT /api/v1/clothing/{id}.
 * Only {@code name} is mandatory; everything else may be left null and
 * falls back to a sensible default (see {@code ClothingServiceImpl#applyRequest}).
 */
@Data
public class ClothingRequest {

    @NotBlank
    private String name;

    private String brand;
    private String colorPrimary;
    private String colorSecondary;
    private String size;
    private BigDecimal purchasePrice;
    private LocalDate purchaseDate;
    private String season;
    private String notes;

    /** Optional M:N category links, fully replaced on update. */
    private List<Long> categoryIds;

    /** Optional M:N tag links, fully replaced on update. */
    private List<Long> tagIds;
}
