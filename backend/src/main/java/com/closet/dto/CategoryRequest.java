package com.closet.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoryRequest {

    @NotBlank
    private String name;

    private Long parentId;

    private Integer sortOrder;
}
