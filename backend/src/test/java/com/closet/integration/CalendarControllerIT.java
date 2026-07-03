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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for {@code /api/v1/calendar}. Runs against the
 * locally running PostgreSQL container started by
 * {@code deploy/docker-compose.dev.yml}. The schema is reloaded on
 * each test boot via {@code spring.sql.init.mode=always}.
 *
 * <p>All fixtures are namespaced with the {@code IT-Cal-} prefix so
 * this test class never collides with {@code OutfitControllerIT} or
 * {@code WearLogControllerIT}. The {@code @AfterEach} cleanup
 * respects the FK chain declared in {@code schema.sql}: wear_log
 * rows reference both clothing and calendar_entry, calendar_entry
 * holds an ON DELETE RESTRICT FK back to outfit, and outfit_item
 * chains outfit and clothing. So the safe order is
 * wear_log -> calendar_entry -> outfit_item -> outfit -> clothing.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@TestPropertySource(properties = {
        "spring.sql.init.mode=always",
        "spring.sql.init.continue-on-error=false"
})
class CalendarControllerIT {

    private static final String CAL_PREFIX = "IT-Cal-Entry-";
    private static final String OUTFIT_PREFIX = "IT-Cal-Outfit-";
    private static final String OUTFIT_ITEM_PREFIX = "IT-Cal-OutfitItem-";
    private static final String CLOTHING_PREFIX = "IT-Cal-Clothing-";

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
        // wear_log first: it holds FKs to both clothing and calendar_entry.
        jdbc.update("DELETE FROM wear_log WHERE clothing_id IN " +
                "(SELECT id FROM clothing WHERE name LIKE '" + CLOTHING_PREFIX + "%')");
        // calendar_entry second: it has ON DELETE RESTRICT to outfit, so
        // we must remove it before any cascade of outfit.
        jdbc.update("DELETE FROM calendar_entry WHERE notes LIKE '" + CAL_PREFIX + "%'");
        jdbc.update("DELETE FROM wear_log WHERE calendar_entry_id IN " +
                "(SELECT id FROM calendar_entry WHERE notes LIKE '" + CAL_PREFIX + "%')");
        // outfit_item then outfit (cascade takes care of outfit_item if
        // we drop the outfit, but listing it first keeps the cleanup
        // intent obvious in case the cascade rule ever changes).
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
    private long createOutfit(String name, List<Long> clothingIds) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("season", "all");
        body.put("isFavorite", false);
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

    /** Convenience: POST a calendar_entry with notes carry the test prefix. */
    private long createCalendarEntry(String entryDate, String slot,
                                     long outfitId, String notesTag) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("entryDate", entryDate);
        body.put("slot", slot);
        body.put("outfitId", outfitId);
        body.put("notes", CAL_PREFIX + notesTag);
        String resp = mvc.perform(post("/api/v1/calendar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(resp).get("data").get("id").asLong();
    }

    @Test
    void post_creates_calendar_entry_and_syncs_wear_logs() throws Exception {
        long c1 = createClothing(CLOTHING_PREFIX + "Sync-A");
        long c2 = createClothing(CLOTHING_PREFIX + "Sync-B");
        long outfitId = createOutfit(OUTFIT_PREFIX + "Sync", List.of(c1, c2));

        // POST /api/v1/calendar should come back with the new entry and
        // the side effect of two wear_log rows tied to the two outfit
        // items, both stamped with the entry date.
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("entryDate", "2026-07-03");
        body.put("slot", "all_day");
        body.put("outfitId", outfitId);
        body.put("notes", CAL_PREFIX + "Sync-Post");
        String resp = mvc.perform(post("/api/v1/calendar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.outfitId").value(outfitId))
                .andExpect(jsonPath("$.data.entryDate").value("2026-07-03"))
                .andReturn().getResponse().getContentAsString();

        JsonNode dataNode = json.readTree(resp).get("data");
        long entryId = dataNode.get("id").asLong();

        // Exactly 2 wear_log rows for this entry, one per outfit_item.
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM wear_log WHERE calendar_entry_id = ?",
                Integer.class, entryId);
        assertNotNull(count, "wear_log COUNT should not be null");
        assertEquals(2, count.intValue(),
                "expected 2 wear_log rows synced from the outfit, got " + count);

        // The two rows are tied to c1 and c2, and share the entry's date.
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT clothing_id, worn_at FROM wear_log " +
                "WHERE calendar_entry_id = ? ORDER BY clothing_id", entryId);
        assertEquals(2, rows.size(), "wear_log should return 2 rows ordered by clothing_id");
        assertEquals(((Number) rows.get(0).get("clothing_id")).longValue(),
                Math.min(c1, c2),
                "first wear_log.clothing_id should be the smaller of c1, c2");
        assertEquals(((Number) rows.get(1).get("clothing_id")).longValue(),
                Math.max(c1, c2),
                "second wear_log.clothing_id should be the larger of c1, c2");
        for (Map<String, Object> row : rows) {
            // PG returns DATE as java.sql.Date; compare via toString.
            assertEquals("2026-07-03", row.get("worn_at").toString(),
                    "wear_log.worn_at should match the entry_date");
        }
    }

    @Test
    void get_range_returns_entries_in_date_range() throws Exception {
        long outfitId = createOutfit(OUTFIT_PREFIX + "Range", null);

        // Three entries: one before the window, two inside, none after.
        createCalendarEntry("2026-06-30", "all_day", outfitId, "Range-Before");
        long inWindowA = createCalendarEntry("2026-07-01", "morning", outfitId, "Range-A");
        long inWindowB = createCalendarEntry("2026-07-02", "evening", outfitId, "Range-B");

        // Query the 2026-07-01..2026-07-02 window.
        mvc.perform(get("/api/v1/calendar?from=2026-07-01&to=2026-07-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(inWindowA))
                .andExpect(jsonPath("$.data[0].entryDate").value("2026-07-01"))
                .andExpect(jsonPath("$.data[0].slot").value("morning"))
                .andExpect(jsonPath("$.data[1].id").value(inWindowB))
                .andExpect(jsonPath("$.data[1].entryDate").value("2026-07-02"))
                .andExpect(jsonPath("$.data[1].slot").value("evening"));

        // A wider window pulls in the before-window entry too.
        mvc.perform(get("/api/v1/calendar?from=2026-06-01&to=2026-07-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3));

        // A window that misses everything returns an empty list.
        mvc.perform(get("/api/v1/calendar?from=2026-09-01&to=2026-09-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void delete_removes_wear_logs_first() throws Exception {
        long c1 = createClothing(CLOTHING_PREFIX + "Del-A");
        long c2 = createClothing(CLOTHING_PREFIX + "Del-B");
        long outfitId = createOutfit(OUTFIT_PREFIX + "Del", List.of(c1, c2));

        long entryId = createCalendarEntry("2026-07-04", "all_day", outfitId, "Del-Post");

        // Sanity: the POST already synced 2 wear_log rows.
        Integer before = jdbc.queryForObject(
                "SELECT COUNT(*) FROM wear_log WHERE calendar_entry_id = ?",
                Integer.class, entryId);
        assertNotNull(before, "wear_log COUNT should not be null");
        assertEquals(2, before.intValue(),
                "pre-delete expected 2 wear_log rows, got " + before);

        mvc.perform(delete("/api/v1/calendar/" + entryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // wear_log rows for this entry are gone (delete-for-entry fired
        // before the calendar_entry DELETE).
        Integer after = jdbc.queryForObject(
                "SELECT COUNT(*) FROM wear_log WHERE calendar_entry_id = ?",
                Integer.class, entryId);
        assertNotNull(after, "wear_log COUNT should not be null");
        assertEquals(0, after.intValue(),
                "post-delete expected 0 wear_log rows, got " + after);

        // The calendar_entry row itself is gone too.
        Integer entryCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM calendar_entry WHERE id = ?",
                Integer.class, entryId);
        assertNotNull(entryCount, "calendar_entry COUNT should not be null");
        assertEquals(0, entryCount.intValue(),
                "post-delete expected calendar_entry to be gone, got " + entryCount);

        // A follow-up GET surfaces the 404 from CalendarServiceImpl.
        mvc.perform(get("/api/v1/calendar/" + entryId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(404));
    }
}
