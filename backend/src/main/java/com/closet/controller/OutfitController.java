
package com.closet.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.closet.common.Result;
import com.closet.dto.OutfitRequest;
import com.closet.dto.OutfitResponse;
import com.closet.entity.Outfit;
import com.closet.service.OutfitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoints for {@code /api/v1/outfits}. CRUD is mirrored on
 * {@code outfit}; outfit_item rows are managed through the three
 * {@code /{id}/items*} sub-routes. {@code page}/{@code size} default to
 * 1/20 so the list endpoint can be hit with no query string at all.
 */
@RestController
@RequestMapping("/api/v1/outfits")
@RequiredArgsConstructor
public class OutfitController {

    private final OutfitService service;

    @GetMapping
    public Result<IPage<OutfitResponse>> list(
            @RequestParam(required = false) String season,
            @RequestParam(required = false) String occasion,
            @RequestParam(required = false) Boolean favorite,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(service.page(season, occasion, favorite, page, size));
    }

    @GetMapping("/{id}")
    public Result<OutfitResponse> get(@PathVariable Long id) {
        return Result.ok(service.get(id));
    }

    @PostMapping
    public Result<Outfit> create(@Valid @RequestBody OutfitRequest req) {
        return Result.ok(service.create(req));
    }

    @PutMapping("/{id}")
    public Result<Outfit> update(@PathVariable Long id,
                                 @Valid @RequestBody OutfitRequest req) {
        return Result.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.ok();
    }

    @PostMapping("/{id}/items")
    public Result<Void> addItem(@PathVariable Long id,
                                @RequestParam Long clothingId,
                                @RequestParam(defaultValue = "0") int sortOrder) {
        service.addItem(id, clothingId, sortOrder);
        return Result.ok();
    }

    @DeleteMapping("/{id}/items/{clothingId}")
    public Result<Void> removeItem(@PathVariable Long id,
                                   @PathVariable Long clothingId) {
        service.removeItem(id, clothingId);
        return Result.ok();
    }

    @PutMapping("/{id}/items/reorder")
    public Result<Void> reorder(@PathVariable Long id,
                                @RequestBody List<OutfitService.ItemOrder> orders) {
        service.reorderItems(id, orders);
        return Result.ok();
    }
}
