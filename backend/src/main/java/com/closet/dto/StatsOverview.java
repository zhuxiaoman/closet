package com.closet.dto;

import lombok.Data;

/**
 * Aggregate wardrobe counters returned by
 * {@link com.closet.service.StatsService#overview()}.
 *
 * <ul>
 *   <li>{@code totalClothing} - all rows in the {@code clothing} table.</li>
 *   <li>{@code totalOutfits} - all rows in the {@code outfit} table.</li>
 *   <li>{@code monthWears}    - count of {@code wear_log} rows with
 *       {@code worn_at >= first day of the current month} (inclusive).</li>
 * </ul>
 */
@Data
public class StatsOverview {

    private long totalClothing;
    private long totalOutfits;
    private long monthWears;
}
