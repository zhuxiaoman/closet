package com.closet.service;

import com.closet.dto.ClothingStat;
import com.closet.dto.StatsOverview;

import java.util.List;

// Read-only statistics over the wardrobe. Implementation lives in
// com.closet.service.impl.StatsServiceImpl. All counters are derived from
// clothing, outfit and wear_log - there is no separate stats table to keep
// in sync.
public interface StatsService {

    // Aggregate counters: total clothing, total outfits, wears this month.
    StatsOverview overview();

    // Per-clothing stats block for the given id. Throws ApiException(404)
    // if the clothing does not exist.
    ClothingStat forClothing(Long clothingId);

    // Top-N most-worn clothing items, ordered by wear_log count desc.
    // limit is appended to a SQL LIMIT clause; caller must keep it sane.
    List<ClothingStat> mostWorn(int limit);

    // Active clothing items with no wear_log entry in the last `days` days,
    // i.e. things collecting dust since today - days. days must be >= 0.
    List<ClothingStat> leastWorn(int days);
}
