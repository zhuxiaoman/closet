package com.closet.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test for {@code /api/v1/outfits/ai-generate} and
 * {@code /api/v1/outfits/ai-generation/{id}/feedback}. Uses
 * {@code RANDOM_PORT} + {@link TestRestTemplate} so the HTTP layer is
 * exercised end-to-end instead of being shimmed by MockMvc.
 *
 * <p>The AI generator needs the closet to contain at least one active
 * clothing row, so each test seeds clothing through the real
 * {@code POST /api/v1/clothing} endpoint first. AI generation rows
 * are cleaned up in {@link #cleanup()} via a LIKE prefix on the
 * seed id (stored in {@code seed_clothing_ids} JSONB).
 *
 * <p>{@code spring.sql.init.mode=never} skips the dev profile's auto
 * re-run of {@code schema.sql} so we don't fight with concurrent boots
 * (the PG container already has the schema from the last MVP IT run).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@TestPropertySource(properties = {
        "spring.sql.init.mode=never"
})
class OutfitAiControllerIT {

    private static final String CLOTHING_PREFIX = "IT-Ai-Seed-";

    @Autowired
    TestRestTemplate rest;

    @Autowired
    ObjectMapper json;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void cleanupBefore() {
        cleanup();
    }

    @AfterEach
    void cleanup() {
        // AI generation rows have no FK to clothing, so we can wipe
        // them directly. Cascade handles JSONB ids being arrays.
        jdbc.update("DELETE FROM outfit_ai_generation WHERE seed_clothing_ids::text LIKE '%"
                + CLOTHING_PREFIX + "%'");
        jdbc.update("DELETE FROM clothing WHERE name LIKE '" + CLOTHING_PREFIX + "%'");
    }

    /** Convenience: create one clothing row via the real controller. */
    private long createClothing(String name, String season) throws Exception {
        Map<String, Object> body = Map.of(
                "name", name,
                "season", season);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> resp = rest.exchange(
                "/api/v1/clothing",
                HttpMethod.POST,
                new HttpEntity<>(json.writeValueAsString(body), headers),
                String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode(), "clothing create: " + resp.getBody());
        JsonNode root = json.readTree(resp.getBody());
        return root.get("data").get("id").asLong();
    }

    @Test
    void aiGenerate返回5套() throws Exception {
        // Seed three summer items so the AI has something to combine.
        long c1 = createClothing(CLOTHING_PREFIX + "Top", "summer");
        long c2 = createClothing(CLOTHING_PREFIX + "Bottom", "summer");
        long c3 = createClothing(CLOTHING_PREFIX + "Shoes", "summer");

        Map<String, Object> req = Map.of(
                "seedClothingIds", List.of(c1, c2, c3),
                "season", "summer",
                "occasion", "casual");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> resp = rest.exchange(
                "/api/v1/outfits/ai-generate",
                HttpMethod.POST,
                new HttpEntity<>(json.writeValueAsString(req), headers),
                String.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode(), "ai-generate: " + resp.getBody());
        JsonNode body = json.readTree(resp.getBody());
        assertEquals(0, body.get("code").asInt());

        JsonNode data = body.get("data");
        assertNotNull(data);
        long generationId = data.get("generationId").asLong();
        assertTrue(generationId > 0, "generationId should be a positive number");

        JsonNode outfits = data.get("outfits");
        assertNotNull(outfits, "outfits field missing");
        assertTrue(outfits.isArray(), "outfits should be a JSON array");
        assertEquals(5, outfits.size(), "must return exactly 5 outfits, got " + outfits.size());

        // Every outfit must include every seed id.
        for (int i = 0; i < outfits.size(); i++) {
            JsonNode outfit = outfits.get(i);
            assertTrue(outfit.isArray(), "outfit #" + i + " should be an array");
            assertTrue(outfit.size() >= 3,
                    "outfit #" + i + " should have >= 3 items, got " + outfit);
            List<Long> ids = new java.util.ArrayList<>();
            for (JsonNode n : outfit) ids.add(n.asLong());
            assertTrue(ids.contains(c1), "outfit #" + i + " missing seed c1");
            assertTrue(ids.contains(c2), "outfit #" + i + " missing seed c2");
            assertTrue(ids.contains(c3), "outfit #" + i + " missing seed c3");
        }

        // DB sanity: the row was persisted with feedback='none'.
        String feedback = jdbc.queryForObject(
                "SELECT feedback FROM outfit_ai_generation WHERE id = ?",
                String.class, generationId);
        assertEquals("none", feedback, "fresh generation should default to feedback='none'");
    }

    @Test
    void feedback接口能写入like() throws Exception {
        // One active item so the generation has something valid.
        long c1 = createClothing(CLOTHING_PREFIX + "Fb", "summer");

        Map<String, Object> genReq = Map.of(
                "seedClothingIds", List.of(c1),
                "season", "summer");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> genResp = rest.exchange(
                "/api/v1/outfits/ai-generate",
                HttpMethod.POST,
                new HttpEntity<>(json.writeValueAsString(genReq), headers),
                String.class);
        assertEquals(HttpStatus.OK, genResp.getStatusCode());
        long generationId = json.readTree(genResp.getBody())
                .get("data").get("generationId").asLong();

        // Now post feedback.
        Map<String, String> fb = Map.of("feedback", "like");
        ResponseEntity<String> fbResp = rest.exchange(
                "/api/v1/outfits/ai-generation/" + generationId + "/feedback",
                HttpMethod.POST,
                new HttpEntity<>(json.writeValueAsString(fb), headers),
                String.class);

        assertEquals(HttpStatus.OK, fbResp.getStatusCode(),
                "feedback endpoint should accept like, got: " + fbResp.getBody());
        assertEquals(0, json.readTree(fbResp.getBody()).get("code").asInt());

        // DB verification.
        String stored = jdbc.queryForObject(
                "SELECT feedback FROM outfit_ai_generation WHERE id = ?",
                String.class, generationId);
        assertEquals("like", stored, "feedback should be persisted as 'like'");
    }
}