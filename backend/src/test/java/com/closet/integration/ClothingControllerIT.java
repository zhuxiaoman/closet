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

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CRUD integration test for {@code /api/v1/clothing}. Reuses the locally
 * running PostgreSQL container.
 *
 * <p>Uses {@code spring.sql.init.mode=always} so the schema is reloaded on
 * each test boot - this makes the test self-contained (no manual
 * {@code DROP SCHEMA} dance). Cleanup uses ASCII-only row names so the
 * JDBC client doesn't have to negotiate the server encoding.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@TestPropertySource(properties = {
        "spring.sql.init.mode=always",
        "spring.sql.init.continue-on-error=false"
})
class ClothingControllerIT {

    private static final String[] TEST_NAMES = {
            "IT-Shirt-A", "IT-Shirt-B", "IT-Shirt-C",
            "IT-Updated", "IT-Pants-A"
    };

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    JdbcTemplate jdbc;

    @AfterEach
    void cleanup() {
        // Junction rows first because of FKs.
        String inList = "'" + String.join("','", TEST_NAMES) + "'";
        jdbc.update("DELETE FROM clothing_category WHERE clothing_id IN " +
                "(SELECT id FROM clothing WHERE name IN (" + inList + "))");
        jdbc.update("DELETE FROM clothing_tag WHERE clothing_id IN " +
                "(SELECT id FROM clothing WHERE name IN (" + inList + "))");
        jdbc.update("DELETE FROM clothing WHERE name IN (" + inList + ")");
    }

    @Test
    void crud_flow_with_category_link() throws Exception {
        // Create a category first so the FK in clothing_category resolves.
        String catBody = json.writeValueAsString(Map.of("name", "IT-Cat-Shirt"));
        String catResp = mvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON).content(catBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long catId = json.readTree(catResp).get("data").get("id").asLong();

        try {
            // Create clothing with category link.
            String body = json.writeValueAsString(Map.of(
                    "name", "IT-Shirt-A",
                    "season", "summer",
                    "categoryIds", List.of(catId)
            ));
            String cr = mvc.perform(post("/api/v1/clothing")
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.id").exists())
                    .andExpect(jsonPath("$.data.name").value("IT-Shirt-A"))
                    .andExpect(jsonPath("$.data.season").value("summer"))
                    .andReturn().getResponse().getContentAsString();
            long id = json.readTree(cr).get("data").get("id").asLong();

            // List with keyword search.
            mvc.perform(get("/api/v1/clothing?keyword=IT-Shirt-A"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.records[?(@.name=='IT-Shirt-A')]").exists())
                    .andExpect(jsonPath("$.data.total").value(1));

            // Get one.
            mvc.perform(get("/api/v1/clothing/" + id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value("IT-Shirt-A"));

            // Update name.
            mvc.perform(put("/api/v1/clothing/" + id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of(
                                    "name", "IT-Updated",
                                    "season", "summer",
                                    "categoryIds", List.of(catId)))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value("IT-Updated"));

            // Update with empty categoryIds clears the link.
            mvc.perform(put("/api/v1/clothing/" + id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of(
                                    "name", "IT-Updated",
                                    "categoryIds", List.of()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value("IT-Updated"));

            // Soft delete (status -> discarded).
            mvc.perform(delete("/api/v1/clothing/" + id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            // Default page filter excludes discarded, so keyword search now empty.
            mvc.perform(get("/api/v1/clothing?keyword=IT-Updated"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.total").value(0));
        } finally {
            jdbc.update("DELETE FROM category WHERE name = 'IT-Cat-Shirt'");
        }
    }

    @Test
    void create_rejects_blank_name() throws Exception {
        String body = json.writeValueAsString(Map.of("name", ""));
        mvc.perform(post("/api/v1/clothing")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void get_404_for_missing() throws Exception {
        mvc.perform(get("/api/v1/clothing/999999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("clothing not found")));
    }

    @Test
    void page_paginates() throws Exception {
        for (String n : new String[]{"IT-Shirt-A", "IT-Shirt-B", "IT-Shirt-C"}) {
            String body = json.writeValueAsString(Map.of("name", n));
            mvc.perform(post("/api/v1/clothing")
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk());
        }
        mvc.perform(get("/api/v1/clothing?page=1&size=2&keyword=IT-Shirt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records.length()").value(2))
                .andExpect(jsonPath("$.data.total").value(3));
        mvc.perform(get("/api/v1/clothing?page=2&size=2&keyword=IT-Shirt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records.length()").value(1));
    }
}