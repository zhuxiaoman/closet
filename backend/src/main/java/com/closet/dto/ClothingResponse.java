package com.closet.dto;

import com.closet.entity.Clothing;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Response shape for GET / POST / PUT /api/v1/clothing*. Mirrors
 * {@link Clothing} and additionally exposes the resolved category / tag
 * names plus the image list. Image wiring is added in T13; the field is
 * declared now so the response shape stays stable.
 */
@Data
public class ClothingResponse {

    private Long id;
    private String name;
    private String brand;
    private String colorPrimary;
    private String colorSecondary;
    private String size;
    private BigDecimal purchasePrice;
    private LocalDate purchaseDate;
    private String season;
    private String notes;
    private String status;
    private Long mainImageId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    private List<String> categories;
    private List<String> tags;
    private List<String> imageKeys;
}
