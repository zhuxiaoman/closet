package com.closet.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for clothing image upload / download / delete.
 *
 * <p>Reuses the locally running PostgreSQL and MinIO containers started by
 * docker-compose.dev.yml. Uses an isolated bucket {@code closet-it-image} so
 * we don't touch real dev objects. Cleans up the rows and MinIO objects it
 * creates.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@TestPropertySource(properties = {
        "minio.bucket=closet-it-image",
        "spring.sql.init.mode=always",
        "spring.sql.init.continue-on-error=false"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClothingImageIT {

    private static final String[] TEST_NAMES = {"IT-Img-Shirt-A", "IT-Img-Shirt-B"};

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    MinioClient minioClient;

    @Value("${minio.bucket}")
    String bucket;

    @BeforeAll
    void ensureBucket() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }

    @AfterEach
    void cleanup() throws Exception {
        String inList = "'" + String.join("','", TEST_NAMES) + "'";
        var keys = jdbc.queryForList(
                "SELECT storage_key FROM clothing_image WHERE clothing_id IN " +
                        "(SELECT id FROM clothing WHERE name IN (" + inList + "))", String.class);
        jdbc.update("DELETE FROM clothing_image WHERE clothing_id IN " +
                "(SELECT id FROM clothing WHERE name IN (" + inList + "))");
        jdbc.update("DELETE FROM clothing_category WHERE clothing_id IN " +
                "(SELECT id FROM clothing WHERE name IN (" + inList + "))");
        jdbc.update("DELETE FROM clothing WHERE name IN (" + inList + ")");

        for (String key : keys) {
            try {
                minioClient.removeObject(RemoveObjectArgs.builder()
                        .bucket(bucket).object(key).build());
            } catch (Exception ignored) {
                // best-effort
            }
        }
    }

    @Test
    void upload_list_download_delete_roundtrip() throws Exception {
        String body = json.writeValueAsString(Map.of("name", "IT-Img-Shirt-A"));
        String cr = mvc.perform(post("/api/v1/clothing")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long clothingId = json.readTree(cr).get("data").get("id").asLong();

        byte[] payload = "fake-jpeg-bytes".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file", "shirt.jpg", "image/jpeg", payload);
        String upResp = mvc.perform(multipart("/api/v1/clothing/" + clothingId + "/images")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.clothingId").value(clothingId))
                .andExpect(jsonPath("$.data.isMain").value(true))
                .andExpect(jsonPath("$.data.storageKey").exists())
                .andReturn().getResponse().getContentAsString();
        JsonNode img = json.readTree(upResp).get("data");
        long imageId = img.get("id").asLong();
        String key = img.get("storageKey").asText();

        mvc.perform(get("/api/v1/clothing/" + clothingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mainImageId").value(imageId));

        mvc.perform(get("/api/v1/images/" + key))
                .andExpect(status().isOk())
                .andExpect(content().bytes(payload));

        mvc.perform(delete("/api/v1/clothing/" + clothingId + "/images/" + imageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mvc.perform(get("/api/v1/clothing/" + clothingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mainImageId").doesNotExist());
    }

    @Test
    void upload_to_missing_clothing_returns_400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "x.jpg", "image/jpeg", new byte[]{1, 2, 3});
        mvc.perform(multipart("/api/v1/clothing/999999/images").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("clothing not found")));
    }
}