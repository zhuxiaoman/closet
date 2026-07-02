package com.closet.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CRUD integration test for {@code /api/v1/tags}. Mirrors {@link CategoryControllerIT}:
 * uses the locally running PG container, skips {@code spring.sql.init}, and cleans up
 * the rows it created.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@TestPropertySource(properties = {
        "spring.sql.init.mode=never"
})
class TagControllerIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    JdbcTemplate jdbc;

    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM tag WHERE name IN ('通勤','运动')");
    }

    @Test
    void crud_flow() throws Exception {
        String body = json.writeValueAsString(Map.of("name", "通勤"));
        String resp = mvc.perform(post("/api/v1/tags")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value("通勤"))
                .andReturn().getResponse().getContentAsString();

        JsonNode data = json.readTree(resp).get("data");
        long id = data.get("id").asLong();

        mvc.perform(get("/api/v1/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.name=='通勤')]").exists());

        mvc.perform(put("/api/v1/tags/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("name", "运动"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("运动"));

        mvc.perform(delete("/api/v1/tags/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }
}
