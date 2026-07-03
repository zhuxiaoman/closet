
package com.closet.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for {@code /api/v1/outfits}. Runs against the
 * locally running PostgreSQL container started by
 * {@code deploy/docker-compose.dev.yml}. The schema is reloaded on
 * each test boot via {@code spring.sql.init.mode=always}, so we are
 * free to leave rows behind between tests - cleanup still runs in
 * {@link #cleanup()} to keep things tidy.
 *
 * <p>Outfit fixtures are created by first POSTing to
 * {@code /api/v1/clothing} so the FK on {@code outfit_item.clothing_id}
 * resolves. Clothing and outfit rows use ASCII-only names with an
 * {@code IT-Outfit-} prefix so they can be wiped by simple DELETE
 * statements without negotiating the server encoding.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@TestPropertySource(properties = {
        "spring.sql.init.mode=always",
        "spring.sql.init.continue-on-error=false"
})
class OutfitControllerIT {

    private static final String OUTFIT_PREFIX = "IT-Outfit-";
    private static final String CLOTHING_PREFIX = "IT-Outfit-Item-";

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void preClean() {
        cleanup();
    }

    @AfterEach
    void cleanup() {
        // outfit_item first because both outfit_id and clothing_id are FKs.
        jdbc.update("DELETE FROM outfit_item WHERE outfit_id IN " +
                "(SELECT id FROM outfit WHERE name LIKE '" + OUTFIT_PREFIX + "%')");
        jdbc.update("DELETE FROM outfit WHERE name LIKE '" + OUTFIT_PREFIX + "%'");
        jdbc.update("DELETE FROM clothing WHERE name LIKE '" + CLOTHING_PREFIX + "%'");
    }

    /** Convenience: create one clothing row via the real controller. */
    private long createClothing(String name) throws Exception {
        String body = json.writeValueAsString(Map.of("name", name));
        String resp = mvc.perform(post("/api/v1/clothing")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(resp).get("data").get("id").asLong();
    }

    /** Convenience: create one outfit via the real controller, return its id. */
    private long createOutfit(String name, String season, boolean favorite,
                              List<Long> clothingIds) throws Exception {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("name", name);
        body.put("season", season);
        body.put("isFavorite", favorite);
        if (clothingIds != null) {
            body.put("clothingIds", clothingIds);
        }
        String resp = mvc.perform(post("/api/v1/outfits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(resp).get("data").get("id").asLong();
    }

    @Test
    void create_then_get_returns_outfit_with_items() throws Exception {
        long c1 = createClothing(CLOTHING_PREFIX + "A");
        long c2 = createClothing(CLOTHING_PREFIX + "B");

        long outfitId = createOutfit(OUTFIT_PREFIX + "Create-Get", "summer", false,
                List.of(c1, c2));

        // GET /{id} should resolve clothing entities in sort_order 0, 1.
        mvc.perform(get("/api/v1/outfits/" + outfitId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(outfitId))
                .andExpect(jsonPath("$.data.name").value(OUTFIT_PREFIX + "Create-Get"))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].id").value(c1))
                .andExpect(jsonPath("$.data.items[1].id").value(c2));
    }

    @Test
    void page_filters_by_season_and_favorite() throws Exception {
        // Three outfits across seasons / favorite flag.
        createOutfit(OUTFIT_PREFIX + "Summer-Fav", "summer", true, null);
        createOutfit(OUTFIT_PREFIX + "Summer-Normal", "summer", false, null);
        createOutfit(OUTFIT_PREFIX + "Winter-Normal", "winter", false, null);

        // All three visible with no filter.
        mvc.perform(get("/api/v1/outfits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.records.length()").value(3));

        // season=summer narrows to two.
        mvc.perform(get("/api/v1/outfits?season=summer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records[?(@.name=='" + OUTFIT_PREFIX + "Summer-Fav')]").exists())
                .andExpect(jsonPath("$.data.records[?(@.name=='" + OUTFIT_PREFIX + "Summer-Normal')]").exists())
                .andExpect(jsonPath("$.data.records[?(@.name=='" + OUTFIT_PREFIX + "Winter-Normal')]").doesNotExist());

        // favorite=true narrows to one.
        mvc.perform(get("/api/v1/outfits?favorite=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].name").value(OUTFIT_PREFIX + "Summer-Fav"))
                .andExpect(jsonPath("$.data.records[0].isFavorite").value(true));

        // Combined: season=winter&favorite=false -> one.
        mvc.perform(get("/api/v1/outfits?season=winter&favorite=false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));

        // Pagination: size=2 on page=1 has 2 rows, page=2 has 1.
        mvc.perform(get("/api/v1/outfits?page=1&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records.length()").value(2))
                .andExpect(jsonPath("$.data.total").value(3));
        mvc.perform(get("/api/v1/outfits?page=2&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.total").value(3));
    }

    @Test
    void update_replaces_all_items() throws Exception {
        long c1 = createClothing(CLOTHING_PREFIX + "U1");
        long c2 = createClothing(CLOTHING_PREFIX + "U2");
        long c3 = createClothing(CLOTHING_PREFIX + "U3");
        long c4 = createClothing(CLOTHING_PREFIX + "U4");

        long outfitId = createOutfit(OUTFIT_PREFIX + "Update", "spring", false,
                List.of(c1, c2, c3));

        // Sanity: initial items list = [c1, c2, c3].
        mvc.perform(get("/api/v1/outfits/" + outfitId))
                .andExpect(jsonPath("$.data.items.length()").value(3))
                .andExpect(jsonPath("$.data.items[0].id").value(c1))
                .andExpect(jsonPath("$.data.items[2].id").value(c3));

        // PUT with a wholly different clothing list (c2 dropped, c4 added).
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("name", OUTFIT_PREFIX + "Update-2");
        body.put("season", "fall");
        body.put("isFavorite", true);
        body.put("clothingIds", List.of(c1, c4));
        mvc.perform(put("/api/v1/outfits/" + outfitId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value(OUTFIT_PREFIX + "Update-2"));

        // Re-fetch: items fully replaced, no c2 or c3, c4 present.
        mvc.perform(get("/api/v1/outfits/" + outfitId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value(OUTFIT_PREFIX + "Update-2"))
                .andExpect(jsonPath("$.data.season").value("fall"))
                .andExpect(jsonPath("$.data.isFavorite").value(true))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].id").value(c1))
                .andExpect(jsonPath("$.data.items[1].id").value(c4));

        // DB-level: only the two new (outfit_id, clothing_id) rows remain.
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM outfit_item WHERE outfit_id = ?",
                Integer.class, outfitId);
        if (count == null || count != 2) {
            throw new AssertionError("expected 2 outfit_item rows, got " + count);
        }
    }

    @Test
    void reorder_items_changes_sort_order() throws Exception {
        long a = createClothing(CLOTHING_PREFIX + "RA");
        long b = createClothing(CLOTHING_PREFIX + "RB");
        long c = createClothing(CLOTHING_PREFIX + "RC");

        long outfitId = createOutfit(OUTFIT_PREFIX + "Reorder", "all", false,
                List.of(a, b, c));

        // Initial order: a(0), b(1), c(2).
        mvc.perform(get("/api/v1/outfits/" + outfitId))
                .andExpect(jsonPath("$.data.items[0].id").value(a))
                .andExpect(jsonPath("$.data.items[1].id").value(b))
                .andExpect(jsonPath("$.data.items[2].id").value(c));

        // Reverse via reorder endpoint.
        List<Map<String, Object>> orders = List.of(
                Map.of("clothingId", a, "sortOrder", 2),
                Map.of("clothingId", b, "sortOrder", 1),
                Map.of("clothingId", c, "sortOrder", 0)
        );
        mvc.perform(put("/api/v1/outfits/" + outfitId + "/items/reorder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(orders)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // GET now reflects new order: c(0), b(1), a(2).
        mvc.perform(get("/api/v1/outfits/" + outfitId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value(c))
                .andExpect(jsonPath("$.data.items[1].id").value(b))
                .andExpect(jsonPath("$.data.items[2].id").value(a));
    }

    @Test
    void delete_removes_outfit_and_items_cascade() throws Exception {
        long c1 = createClothing(CLOTHING_PREFIX + "D1");
        long outfitId = createOutfit(OUTFIT_PREFIX + "Delete", "summer", false,
                List.of(c1));

        // Sanity: item row exists before delete.
        Integer before = jdbc.queryForObject(
                "SELECT COUNT(*) FROM outfit_item WHERE outfit_id = ?",
                Integer.class, outfitId);
        if (before == null || before != 1) {
            throw new AssertionError("pre-delete expected 1 item row, got " + before);
        }

        mvc.perform(delete("/api/v1/outfits/" + outfitId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // outfit_item cascaded away with the outfit row.
        Integer after = jdbc.queryForObject(
                "SELECT COUNT(*) FROM outfit_item WHERE outfit_id = ?",
                Integer.class, outfitId);
        if (after == null || after != 0) {
            throw new AssertionError("post-delete expected 0 item rows, got " + after);
        }

        // GET /{id} -> 404 via ApiException -> status 400 + code 404.
        mvc.perform(get("/api/v1/outfits/" + outfitId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("outfit not found")));
    }
}
