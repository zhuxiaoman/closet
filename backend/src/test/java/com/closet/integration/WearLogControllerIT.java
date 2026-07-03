package com.closet.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.closet.entity.WearLog;
import com.closet.mapper.WearLogMapper;
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

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for {@code /api/v1/wear-logs}. Runs against the
 * locally running PostgreSQL container started by
 * {@code deploy/docker-compose.dev.yml}. The schema is reloaded on
 * each test boot via {@code spring.sql.init.mode=always}, so we are
 * free to leave rows behind between tests - cleanup still runs in
 * {@link #cleanup()} so this test class never pollutes others.
 *
 * <p>Clothing fixtures use the {@code IT-WearLog-Item-} prefix so the
 * {@link OutfitControllerIT} fixtures (which use {@code IT-Outfit-Item-})
 * are unaffected.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@TestPropertySource(properties = {
        "spring.sql.init.mode=always",
        "spring.sql.init.continue-on-error=false"
})
class WearLogControllerIT {

    private static final String CLOTHING_PREFIX = "IT-WearLog-Item-";

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    WearLogMapper wearLogMapper;

    @BeforeEach
    void preClean() {
        cleanup();
    }

    @AfterEach
    void cleanup() {
        // wear_log before clothing because of the FK.
        jdbc.update("DELETE FROM wear_log WHERE clothing_id IN " +
                "(SELECT id FROM clothing WHERE name LIKE '" + CLOTHING_PREFIX + "%')");
        jdbc.update("DELETE FROM clothing_category WHERE clothing_id IN " +
                "(SELECT id FROM clothing WHERE name LIKE '" + CLOTHING_PREFIX + "%')");
        jdbc.update("DELETE FROM clothing_tag WHERE clothing_id IN " +
                "(SELECT id FROM clothing WHERE name LIKE '" + CLOTHING_PREFIX + "%')");
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

    @Test
    void post_creates_wear_log_when_clothing_exists() throws Exception {
        long clothingId = createClothing(CLOTHING_PREFIX + "Create");

        String body = json.writeValueAsString(Map.of(
                "clothingId", clothingId,
                "wornAt", "2026-07-03"));
        String resp = mvc.perform(post("/api/v1/wear-logs")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.clothingId").value(clothingId))
                .andExpect(jsonPath("$.data.wornAt").value("2026-07-03"))
                .andReturn().getResponse().getContentAsString();

        long wearLogId = json.readTree(resp).get("data").get("id").asLong();

        // Verify the row actually persisted by selecting it back.
        WearLog stored = wearLogMapper.selectById(wearLogId);
        assertNotNull(stored, "wear_log should be persisted after POST");
        assertEquals(clothingId, stored.getClothingId());
    }

    @Test
    void post_returns_404_when_clothing_not_found() throws Exception {
        String body = json.writeValueAsString(Map.of(
                "clothingId", 999999,
                "wornAt", "2026-07-03"));
        // GlobalExceptionHandler maps ApiException -> HTTP 400, with the
        // ApiException code preserved in the body.
        mvc.perform(post("/api/v1/wear-logs")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value(containsString("clothing not found")));
    }

    @Test
    void delete_removes_wear_log() throws Exception {
        long clothingId = createClothing(CLOTHING_PREFIX + "Delete");

        String createBody = json.writeValueAsString(Map.of(
                "clothingId", clothingId,
                "wornAt", "2026-07-03"));
        String resp = mvc.perform(post("/api/v1/wear-logs")
                        .contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long wearLogId = json.readTree(resp).get("data").get("id").asLong();

        // Row exists before DELETE.
        WearLog before = wearLogMapper.selectById(wearLogId);
        assertNotNull(before, () -> "wear_log " + wearLogId + " should exist before DELETE");

        mvc.perform(delete("/api/v1/wear-logs/" + wearLogId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // Row is gone after DELETE.
        WearLog after = wearLogMapper.selectById(wearLogId);
        assertNull(after, () -> "wear_log " + wearLogId + " should be deleted");
    }
}