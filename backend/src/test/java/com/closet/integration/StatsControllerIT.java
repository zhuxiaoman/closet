package com.closet.integration;

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

import java.time.LocalDate;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for {@code /api/v1/stats}. The controller is
 * read-only - everything is derived from clothing, outfit and
 * wear_log at request time, so this class never writes through the
 * stats endpoints. It seeds clothing and wear_log rows via their
 * own controllers, then asserts on the JSON shapes produced by
 * /overview, /most-worn and /clothing/{id}.
 *
 * <p>Fixtures use the {@code IT-Stats-} / {@code IT-Stats-Clothing-}
 * prefixes so they cannot collide with the {@code IT-Outfit-*} or
 * {@code IT-WearLog-Item-*} fixtures owned by sibling IT classes.
 * The schema is reloaded on each test boot via
 * {@code spring.sql.init.mode=always}, which keeps this class
 * self-contained even if a previous run left rows behind.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@TestPropertySource(properties = {
        "spring.sql.init.mode=always",
        "spring.sql.init.continue-on-error=false"
})
class StatsControllerIT {

    private static final String STATS_CLOTHING_PREFIX = "IT-Stats-Clothing-";
    private static final String STATS_OUTFIT_PREFIX = "IT-Stats-Outfit-";

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void preClean() {
        // TRUNCATE the whole business dataset so this class is
        // self-contained. spring.sql.init.mode=always only reseeds
        // category/tag; clothing, outfit, wear_log, calendar_entry
        // accumulate across runs and would otherwise skew totals
        // assertions (e.g. overview_returns_totals_with_wear_logs).
        // RESTART IDENTITY resets auto-increment ids; CASCADE unwinds
        // the FK graph (wear_log -> clothing, outfit_item -> outfit
        // and clothing, clothing_category/tag -> clothing, etc.).
        jdbc.execute("TRUNCATE TABLE wear_log, outfit_item, clothing_category, clothing_tag, calendar_entry, outfit, clothing RESTART IDENTITY CASCADE");
        cleanup();
    }

    @AfterEach
    void cleanup() {
        // wear_log before clothing because of the FK; outfit_item before
        // both clothing and outfit.
        jdbc.update("DELETE FROM wear_log WHERE clothing_id IN " +
                "(SELECT id FROM clothing WHERE name LIKE '" + STATS_CLOTHING_PREFIX + "%')");
        jdbc.update("DELETE FROM outfit_item WHERE outfit_id IN " +
                "(SELECT id FROM outfit WHERE name LIKE '" + STATS_OUTFIT_PREFIX + "%')");
        jdbc.update("DELETE FROM outfit WHERE name LIKE '" + STATS_OUTFIT_PREFIX + "%'");
        jdbc.update("DELETE FROM clothing_category WHERE clothing_id IN " +
                "(SELECT id FROM clothing WHERE name LIKE '" + STATS_CLOTHING_PREFIX + "%')");
        jdbc.update("DELETE FROM clothing_tag WHERE clothing_id IN " +
                "(SELECT id FROM clothing WHERE name LIKE '" + STATS_CLOTHING_PREFIX + "%')");
        jdbc.update("DELETE FROM clothing WHERE name LIKE '" + STATS_CLOTHING_PREFIX + "%'");
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

    /** Convenience: log one wear entry via the manual wear-log endpoint. */
    private void logWear(long clothingId, LocalDate wornAt) throws Exception {
        String body = json.writeValueAsString(Map.of(
                "clothingId", clothingId,
                "wornAt", wornAt.toString()));
        mvc.perform(post("/api/v1/wear-logs")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void overview_returns_totals_with_wear_logs() throws Exception {
        long a = createClothing(STATS_CLOTHING_PREFIX + "Overview-A");
        long b = createClothing(STATS_CLOTHING_PREFIX + "Overview-B");

        // One wear entry inside the current month; this drives
        // monthWears >= 1 without depending on the exact calendar day.
        LocalDate thisMonth = LocalDate.now().withDayOfMonth(1);
        logWear(a, thisMonth);
        // Sanity that the fixture actually persisted.
        if (b == a) {
            throw new AssertionError("clothing ids collided");
        }

        mvc.perform(get("/api/v1/stats/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.totalClothing").value(2))
                .andExpect(jsonPath("$.data.monthWears").value(1));
    }

    @Test
    void most_worn_returns_clothing_sorted_by_count() throws Exception {
        long a = createClothing(STATS_CLOTHING_PREFIX + "Most-A");
        long b = createClothing(STATS_CLOTHING_PREFIX + "Most-B");

        // Spread the wears within this month so monthWears does not
        // bleed into the assertion for ordering.
        LocalDate base = LocalDate.now().withDayOfMonth(1);
        logWear(a, base);
        logWear(a, base.plusDays(1));
        logWear(b, base.plusDays(2));

        mvc.perform(get("/api/v1/stats/most-worn"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                // The most-worn endpoint returns every active clothing
                // with at least one wear_log in the current month,
                // ordered by wear count desc. A has 2 wears, B has 1,
                // so A must appear at index 0 with wearCount=2 and
                // B must follow with wearCount=1.
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].clothingId").value(a))
                .andExpect(jsonPath("$.data[0].wearCount").value(2))
                .andExpect(jsonPath("$.data[0].name").value(STATS_CLOTHING_PREFIX + "Most-A"))
                .andExpect(jsonPath("$.data[1].clothingId").value(b))
                .andExpect(jsonPath("$.data[1].wearCount").value(1));
    }

    @Test
    void clothing_404_when_not_found() throws Exception {
        // StatsService.forClothing throws ApiException(404) on an
        // unknown id; GlobalExceptionHandler maps that to HTTP 400
        // with code=404 in the JSON body. OutfitControllerIT already
        // pins this pattern; we mirror it for /stats/clothing/{id}.
        mvc.perform(get("/api/v1/stats/clothing/999999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value(containsString("clothing not found")));
    }
}

