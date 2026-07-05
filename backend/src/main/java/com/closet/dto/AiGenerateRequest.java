package com.closet.dto;

import lombok.Data;

import java.util.List;

/**
 * Request body for {@code POST /api/v1/outfits/ai-generate}.
 *
 * <p>{@code seedClothingIds} is the user-selected starting item(s) that
 * the algorithm must include in every generated outfit. {@code occasion}
 * and {@code season} are optional context tags persisted alongside the
 * result so later analytics can bucket generations by purpose.
 * {@code weatherCode} is reserved for the Phase 1 weather integration
 * (e.g. "sunny", "rainy") and is stored verbatim when present.
 */
@Data
public class AiGenerateRequest {

    /** 1+ seed clothing ids. Validation lives in the service layer. */
    private List<Long> seedClothingIds;

    /** Optional. casual / work / date / sport / home ... */
    private String occasion;

    /** Optional. spring / summer / fall / winter / all. */
    private String season;

    /** Optional. Free-form weather tag for the v2.0 weather integration. */
    private String weatherCode;
}