package com.closet.service;

import com.closet.dto.AiGenerateRequest;
import com.closet.dto.AiGenerateResponse;

/**
 * AI outfit generator. Given 1+ seed clothing ids and optional context
 * (season / occasion / weather), it produces 5 distinct outfit
 * candidates and persists the generation record for later feedback.
 *
 * <p>Implementation lives in {@code OutfitAiServiceImpl}.
 */
public interface OutfitAiService {

    /**
     * Builds 5 outfits that all contain every seed id. Season filtering
     * is applied against {@code clothing.status='active'} AND
     * {@code clothing.season='all' OR = req.season}. Off-season seeds
     * cause an {@code ApiException} so callers get a clear error.
     */
    AiGenerateResponse generate(AiGenerateRequest req);

    /**
     * Records the user's verdict on a previous generation. {@code feedback}
     * must be one of "like", "dislike" or "none". Persisted on the
     * {@code outfit_ai_generation} row.
     */
    void recordFeedback(Long generationId, String feedback);
}