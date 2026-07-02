package com.closet.dto;

import lombok.Data;

/**
 * Query parameters for GET /api/v1/clothing. Spring binds the request
 * automatically by matching field names against the URL.
 */
@Data
public class ClothingFilter {

    private Long categoryId;
    private Long tagId;
    private String season;
    private String status;
    private String keyword;
    private Long page;
    private Long size;
}
