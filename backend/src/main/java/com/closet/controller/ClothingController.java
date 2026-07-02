package com.closet.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.closet.common.Result;
import com.closet.dto.ClothingFilter;
import com.closet.dto.ClothingRequest;
import com.closet.dto.ClothingResponse;
import com.closet.entity.Clothing;
import com.closet.service.ClothingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for {@code /api/v1/clothing}. Mirrors the Category / Tag
 * controllers' style. Pagination defaults are applied here so the page /
 * size query params are optional.
 */
@RestController
@RequestMapping("/api/v1/clothing")
@RequiredArgsConstructor
public class ClothingController {

    private final ClothingService service;

    @GetMapping
    public Result<IPage<ClothingResponse>> list(ClothingFilter filter) {
        if (filter.getPage() == null || filter.getPage() < 1) {
            filter.setPage(1L);
        }
        if (filter.getSize() == null || filter.getSize() < 1) {
            filter.setSize(20L);
        }
        return Result.ok(service.page(filter));
    }

    @GetMapping("/{id}")
    public Result<ClothingResponse> get(@PathVariable Long id) {
        return Result.ok(service.get(id));
    }

    @PostMapping
    public Result<Clothing> create(@Valid @RequestBody ClothingRequest req) {
        return Result.ok(service.create(req));
    }

    @PutMapping("/{id}")
    public Result<Clothing> update(@PathVariable Long id, @Valid @RequestBody ClothingRequest req) {
        return Result.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.softDelete(id);
        return Result.ok();
    }
}