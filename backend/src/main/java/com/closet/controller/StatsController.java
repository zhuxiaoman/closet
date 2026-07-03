package com.closet.controller;

import com.closet.common.Result;
import com.closet.dto.ClothingStat;
import com.closet.dto.StatsOverview;
import com.closet.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-only statistics endpoints under {@code /api/v1/stats}. The
 * service layer derives all counters from clothing / outfit / wear_log
 * at request time; there is no separate stats table to keep in sync.
 *
 * <ul>
 *   <li>{@code GET /overview}        - aggregate counters
 *       (totalClothing / totalOutfits / monthWears).</li>
 *   <li>{@code GET /clothing/{id}}   - per-clothing wear stats block.
 *       Bubbles up {@code ApiException(404)} when the id is unknown.</li>
 *   <li>{@code GET /most-worn}       - top-N most-worn clothing,
 *       ordered by wear count desc; default {@code limit=10}.</li>
 *   <li>{@code GET /least-worn}      - active clothing with no wear in
 *       the last {@code days} days; default {@code days=90}.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService service;

    @GetMapping("/overview")
    public Result<StatsOverview> overview() {
        return Result.ok(service.overview());
    }

    @GetMapping("/clothing/{id}")
    public Result<ClothingStat> forClothing(@PathVariable Long id) {
        return Result.ok(service.forClothing(id));
    }

    @GetMapping("/most-worn")
    public Result<List<ClothingStat>> mostWorn(
            @RequestParam(defaultValue = "10") int limit) {
        return Result.ok(service.mostWorn(limit));
    }

    @GetMapping("/least-worn")
    public Result<List<ClothingStat>> leastWorn(
            @RequestParam(defaultValue = "90") int days) {
        return Result.ok(service.leastWorn(days));
    }
}
