package com.closet.integration;

import com.closet.storage.MinioStorageService;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for MinioStorageService against the locally running MinIO container
 * (started by docker-compose.dev.yml). Uses an isolated test bucket to avoid touching
 * dev data.
 *
 * <p>Testcontainers is intentionally not used here because Docker Desktop 4.80.0 on
 * Windows + Testcontainers 1.21.x has a known socket-routing incompatibility (the
 * dockerjava client receives an empty {@code /info} response). Once that is fixed,
 * this test can be migrated back to a {@code @Testcontainers} setup.
 */
@SpringBootTest
@ActiveProfiles("dev")
@TestPropertySource(properties = {
        "minio.bucket=closet-it",
        "spring.sql.init.mode=never"
})
class MinioStorageServiceIT {

    @Autowired
    MinioStorageService storage;

    @Autowired
    MinioClient client;

    @Value("${minio.bucket}")
    String bucket;

    private final List<String> createdKeys = new ArrayList<>();

    @BeforeEach
    void ensureBucket() throws Exception {
        boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
        createdKeys.clear();
    }

    @AfterEach
    void cleanupKeys() {
        for (String key : createdKeys) {
            try {
                storage.delete(key);
            } catch (Exception ignored) {
                // best-effort cleanup
            }
        }
    }

    @Test
    void upload_and_download_roundtrip() throws Exception {
        byte[] data = "hello-minio".getBytes();
        String key = storage.upload("test", "x.txt",
                new ByteArrayInputStream(data), data.length, "text/plain");
        createdKeys.add(key);

        try (var in = storage.download(key)) {
            byte[] read = in.readAllBytes();
            assertThat(read).isEqualTo(data);
        }

        storage.delete(key);
        createdKeys.remove(key);
    }

    @Test
    void delete_missing_key_is_noop() {
        // NoSuchKey should be silently swallowed by the service
        storage.delete("does/not/exist.txt");
    }
}
