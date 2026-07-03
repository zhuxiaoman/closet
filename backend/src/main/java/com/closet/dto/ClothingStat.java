package com.closet.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Per-clothing statistics block. Returned both for a single clothing id
 * (see {@link com.closet.service.StatsService#forClothing(Long)}) and for
 * the most/least-worn aggregations.
 *
 * <ul>
 *   <li>{@code wearCount}    - total number of {@code wear_log} rows for
 *       this clothing.</li>
 *   <li>{@code firstWorn} / {@code lastWorn} - extremes of {@code worn_at}
 *       over those logs (null when there are no logs).</li>
 *   <li>{@code costPerWear}  - {@code purchase_price / wearCount},
 *       rounded HALF_UP at scale 2. Null when the clothing has no
 *       purchase price on record.</li>
 * </ul>
 */
@Data
public class ClothingStat {

    private Long clothingId;
    private String name;
    private long wearCount;
    private LocalDate firstWorn;
    private LocalDate lastWorn;
    private BigDecimal costPerWear;
}
