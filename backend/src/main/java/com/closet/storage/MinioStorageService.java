package com.closet.storage;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageService implements StorageService {

    private final MinioClient client;

    @Value("${minio.bucket}")
    private String bucket;

    @Override
    public String upload(String prefix, String filename, InputStream content, long size, String contentType) {
        String key = "%s/%s-%s".formatted(prefix, UUID.randomUUID(), filename);
        try {
            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket).object(key)
                    .stream(content, size, -1)
                    .contentType(contentType)
                    .build());
            return key;
        } catch (Exception e) {
            throw new RuntimeException("minio upload failed: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream download(String key) {
        try {
            return client.getObject(GetObjectArgs.builder().bucket(bucket).object(key).build());
        } catch (Exception e) {
            throw new RuntimeException("minio download failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build());
        } catch (ErrorResponseException e) {
            if (!"NoSuchKey".equals(e.errorResponse().code())) {
                throw new RuntimeException("minio delete failed: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            throw new RuntimeException("minio delete failed: " + e.getMessage(), e);
        }
    }
}
