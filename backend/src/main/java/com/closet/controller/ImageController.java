package com.closet.controller;

import com.closet.storage.StorageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Proxies MinIO downloads back to the browser / uni-app client so the
 * client doesn't need direct access to the MinIO endpoint and we keep a
 * single auth surface. The mapping strips the {@code /api/v1/images/}
 * prefix and uses the remainder as the storage object key.
 */
@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
public class ImageController {

    private static final String PREFIX = "/api/v1/images/";

    private final StorageService storage;

    @GetMapping("/**")
    public void proxy(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String key = req.getRequestURI();
        if (!key.startsWith(PREFIX)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid image path");
            return;
        }
        String objectKey = key.substring(PREFIX.length());
        resp.setContentType("image/jpeg");
        try (InputStream in = storage.download(objectKey); OutputStream out = resp.getOutputStream()) {
            in.transferTo(out);
        }
    }
}