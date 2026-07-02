package com.closet.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
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
 * CRUD integration test for {@code /api/v1/categories}. Reuses the locally running
 * PostgreSQL container from {@code deploy/docker-compose.dev.yml}; the schema is
 * already there so we skip {@code spring.sql.init}. Created rows are cleaned up in
 * {@link #cleanup()} so the test is repeatable.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@TestPropertySource(properties = {
        "spring.sql.init.mode=never"
})
class CategoryControllerIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    JdbcTemplate jdbc;

    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM category WHERE name IN ('包包','手包')");
    }

    @Test
    void crud_flow() throws Exception {
        String body = json.writeValueAsString(Map.of("name", "包包", "sortOrder", 5));
        String resp = mvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value("包包"))
                .andReturn().getResponse().getContentAsString();

        JsonNode data = json.readTree(resp).get("data");
        long id = data.get("id").asLong();

        mvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.name=='包包')]").exists());

        mvc.perform(put("/api/v1/categories/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("name", "手包"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("手包"));

        mvc.perform(delete("/api/v1/categories/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }
}
