package com.closet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.closet.common.ApiException;
import com.closet.dto.AiGenerateRequest;
import com.closet.dto.AiGenerateResponse;
import com.closet.entity.Clothing;
import com.closet.entity.OutfitAiGeneration;
import com.closet.mapper.ClothingMapper;
import com.closet.mapper.OutfitAiGenerationMapper;
import com.closet.service.OutfitAiService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link OutfitAiService}.
 *
 * <p><b>Algorithm (deterministic, no randomness):</b>
 * <ol>
 *   <li>Query the active closet filtered by season
 *       {@code status='active' AND (season='all' OR season={req.season})}.</li>
 *   <li>Resolve seed entities. Any seed id missing from the pool throws
 *       400 \u2014 a "winter" seed in a "summer" generation is a user error.</li>
 *   <li>Sort non-seed candidates by id to make output reproducible.</li>
 *   <li>Build 5 candidate outfits: each starts with all seed ids, then
 *       appends up to 2 non-seed items at offset {@code i % nonSeeds.size()},
 *       walking cyclically. Tests rely on this determinism.</li>
 *   <li>Dedup by set-equality (order-insensitive). When fewer than 5
 *       unique outfits are producible (very small closet), repeat the
 *       first outfit until we reach 5 \u2014 plan explicitly allows this.</li>
 *   <li>Persist the generation row with {@code feedback='none'} and
 *       return the new id alongside the 5 outfits.</li>
 * </ol>
 *
 * <p>JSON columns (seedClothingIds / resultOutfitIds) are stored as
 * plain VARCHAR via {@link ObjectMapper}. MyBatis-Plus 3.5.5 with
 * JacksonTypeHandler does not auto-cast to PG jsonb so we keep the
 * JSON shape ourselves; this also makes the column opaque for v2.1
 * analytics helpers.
 */
@Service
@RequiredArgsConstructor
public class OutfitAiServiceImpl implements OutfitAiService {

    private static final int OUTFIT_COUNT = 5;
    private static final int PER_OUTFIT_PICKS = 2;
    private static final Set<String> ALLOWED_FEEDBACK =
            Set.of("like", "dislike", "none");

    private final ClothingMapper clothingMapper;
    private final OutfitAiGenerationMapper aiMapper;
    private final ObjectMapper objectMapper;

    @Override
    public AiGenerateResponse generate(AiGenerateRequest req) {
        if (req == null || req.getSeedClothingIds() == null
                || req.getSeedClothingIds().isEmpty()) {
            throw new ApiException(400, "seedClothingIds must not be empty");
        }
        List<Long> seedIds = req.getSeedClothingIds();

        // 1. Pull active closet from SQL. Season filter happens in
        //    memory so unit tests can mock the wider pool without
        //    faking QueryWrapper SQL parsing.
        List<Clothing> pool = clothingMapper.selectList(activeOnlyQuery());
        if (pool == null) {
            pool = Collections.emptyList();
        }
        if (req.getSeason() != null && !req.getSeason().isBlank()) {
            final String s = req.getSeason();
            pool = pool.stream()
                    .filter(c -> "all".equals(c.getSeason()) || s.equals(c.getSeason()))
                    .collect(Collectors.toList());
        }

        // 2. Seed validation. Missing seed => 400 so callers can fix.
        Map<Long, Clothing> byId = pool.stream()
                .collect(Collectors.toMap(Clothing::getId, c -> c));
        List<Long> resolvedSeedIds = new ArrayList<>(seedIds.size());
        for (Long sid : seedIds) {
            Clothing sc = byId.get(sid);
            if (sc == null) {
                throw new ApiException(400,
                        "seed clothing id " + sid
                                + " is not available for season "
                                + req.getSeason());
            }
            resolvedSeedIds.add(sc.getId());
        }

        // 3. Non-seed pool sorted by id for deterministic output.
        Set<Long> seedSet = new HashSet<>(resolvedSeedIds);
        List<Clothing> nonSeeds = pool.stream()
                .filter(c -> !seedSet.contains(c.getId()))
                .sorted(Comparator.comparing(Clothing::getId))
                .collect(Collectors.toList());

        // 4. Build 5 candidates with cyclic offsets.
        List<List<Long>> raw = new ArrayList<>(OUTFIT_COUNT);
        if (nonSeeds.isEmpty()) {
            List<Long> only = new ArrayList<>(resolvedSeedIds);
            for (int i = 0; i < OUTFIT_COUNT; i++) {
                raw.add(new ArrayList<>(only));
            }
        } else {
            int take = Math.min(PER_OUTFIT_PICKS, nonSeeds.size());
            for (int i = 0; i < OUTFIT_COUNT; i++) {
                List<Long> outfit = new ArrayList<>(resolvedSeedIds);
                int start = i % nonSeeds.size();
                for (int j = 0; j < take; j++) {
                    Long id = nonSeeds.get((start + j) % nonSeeds.size()).getId();
                    if (!outfit.contains(id)) {
                        outfit.add(id);
                    }
                }
                raw.add(outfit);
            }
        }

        // 5. Dedup by set-equality; pad with the first outfit to reach 5.
        Set<List<Long>> seen = new HashSet<>();
        List<List<Long>> outfits = new ArrayList<>(OUTFIT_COUNT);
        for (List<Long> o : raw) {
            List<Long> key = new ArrayList<>(o);
            Collections.sort(key);
            if (seen.add(key)) {
                outfits.add(o);
            }
        }
        while (outfits.size() < OUTFIT_COUNT) {
            outfits.add(new ArrayList<>(raw.get(0)));
        }

        // 6. Persist generation row. JSON columns are written as strings.
        OutfitAiGeneration gen = new OutfitAiGeneration();
        gen.setSeedClothingIds(toJson(resolvedSeedIds));
        gen.setOccasion(req.getOccasion());
        gen.setSeason(req.getSeason());
        gen.setResultOutfitIds(toJson(outfits));
        gen.setFeedback("none");
        aiMapper.insert(gen);

        AiGenerateResponse resp = new AiGenerateResponse();
        resp.setGenerationId(gen.getId());
        resp.setOutfits(outfits);
        return resp;
    }

    @Override
    public void recordFeedback(Long generationId, String feedback) {
        if (generationId == null) {
            throw new ApiException(400, "generationId is required");
        }
        if (feedback == null || !ALLOWED_FEEDBACK.contains(feedback)) {
            throw new ApiException(400,
                    "feedback must be one of like / dislike / none");
        }
        OutfitAiGeneration existing = aiMapper.selectById(generationId);
        if (existing == null) {
            throw new ApiException(404, "ai generation not found");
        }
        existing.setFeedback(feedback);
        aiMapper.updateById(existing);
    }

    /** Active closet; season scoping is applied in-memory afterwards. */
    private com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Clothing> activeOnlyQuery() {
        QueryWrapper<Clothing> q = new QueryWrapper<>();
        q.eq("status", "active");
        return q;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new ApiException(500, "failed to serialize JSON column: " + ex.getMessage());
        }
    }

    /** Convenience for tests / future readers. */
    List<Long> parseSeedIds(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {});
        } catch (JsonProcessingException ex) {
            throw new ApiException(500, "failed to parse seed ids: " + ex.getMessage());
        }
    }
}