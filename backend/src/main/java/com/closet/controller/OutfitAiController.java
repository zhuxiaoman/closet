package com.closet.controller;

import com.closet.common.Result;
import com.closet.dto.AiGenerateRequest;
import com.closet.dto.AiGenerateResponse;
import com.closet.service.OutfitAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST endpoints for the AI outfit generator. The two routes sit under
 * {@code /api/v1/outfits/} so they share a base with the regular outfit
 * CRUD ({@code /api/v1/outfits/{id}}):
 *
 * <ul>
 *   <li>{@code POST /ai-generate} \u2014 build 5 outfits and persist the
 *       generation row, returning the new id alongside the 5 outfits.</li>
 *   <li>{@code POST /ai-generation/{id}/feedback} \u2014 record a user's
 *       verdict ({@code like|dislike|none}) on a prior generation.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/outfits")
@RequiredArgsConstructor
public class OutfitAiController {

    private final OutfitAiService outfitAiService;

    @PostMapping("/ai-generate")
    public Result<AiGenerateResponse> generate(@RequestBody AiGenerateRequest req) {
        return Result.ok(outfitAiService.generate(req));
    }

    @PostMapping("/ai-generation/{id}/feedback")
    public Result<Void> feedback(@PathVariable Long id,
                                 @RequestBody Map<String, String> body) {
        String feedback = body == null ? null : body.get("feedback");
        outfitAiService.recordFeedback(id, feedback);
        return Result.ok();
    }
}