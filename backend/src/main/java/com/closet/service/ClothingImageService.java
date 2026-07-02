package com.closet.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.closet.entity.ClothingImage;
import org.springframework.web.multipart.MultipartFile;

/**
 * Image upload / delete for a clothing row. The first uploaded image is
 * automatically marked {@code is_main = true} and the parent clothing row
 * is updated so {@code main_image_id} points at it.
 */
public interface ClothingImageService extends IService<ClothingImage> {

    /**
     * Upload {@code file} for {@code clothingId}. Stores the bytes in MinIO
     * and inserts a {@link ClothingImage} row. Returns the persisted entity.
     *
     * @throws com.closet.common.ApiException 404 if the clothing doesn't exist
     */
    ClothingImage upload(Long clothingId, MultipartFile file);

    /**
     * Delete the image row and its MinIO object. If the deleted image was
     * the main one, the parent clothing's {@code main_image_id} is cleared.
     */
    void delete(Long clothingId, Long imageId);
}