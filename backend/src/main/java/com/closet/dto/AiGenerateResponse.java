package com.closet.dto;

import lombok.Data;

import java.util.List;

/**
 * Response body for {@code POST /api/v1/outfits/ai-generate}.
 *
 * <p>The service guarantees exactly 5 outfits per generation. Each
 * outfit is a list of clothing ids that already exist in the user's
 * closet; the front-end resolves them via {@code GET /clothing/{id}}
 * to render thumbnails.
 *
 * <p>{@code generationId} is the primary key of the persisted
 * {@code outfit_ai_generation} row. The client later calls
 * {@code POST /outfits/ai-generation/{id}/feedback} to record like /
 * dislike signals, which feed the future ranking model.
 */
@Data
public class AiGenerateResponse {

    private Long generationId;

    /** Always exactly 5 entries. Each entry is a list of clothing ids. */
    private List<List<Long>> outfits;
}