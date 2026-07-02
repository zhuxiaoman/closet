package com.closet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.closet.common.ApiException;
import com.closet.entity.Clothing;
import com.closet.entity.ClothingImage;
import com.closet.mapper.ClothingImageMapper;
import com.closet.mapper.ClothingMapper;
import com.closet.service.ClothingImageService;
import com.closet.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ClothingImageServiceImpl extends ServiceImpl<ClothingImageMapper, ClothingImage>
        implements ClothingImageService {

    private final StorageService storage;
    private final ClothingMapper clothingMapper;

    @Override
    @Transactional
    public ClothingImage upload(Long clothingId, MultipartFile file) {
        if (clothingMapper.selectById(clothingId) == null) {
            throw new ApiException(404, "clothing not found");
        }
        try {
            String original = Objects.requireNonNullElse(file.getOriginalFilename(), "image.jpg");
            String key = storage.upload(
                    "clothing/" + clothingId,
                    original,
                    file.getInputStream(),
                    file.getSize(),
                    file.getContentType());

            ClothingImage img = new ClothingImage();
            img.setClothingId(clothingId);
            img.setStorageKey(key);
            img.setSortOrder(0);
            // First image for this clothing is the main one.
            long existing = baseMapper.selectCount(
                    new QueryWrapper<ClothingImage>().eq("clothing_id", clothingId));
            img.setIsMain(existing == 0L);
            baseMapper.insert(img);

            if (Boolean.TRUE.equals(img.getIsMain())) {
                Clothing c = new Clothing();
                c.setId(clothingId);
                c.setMainImageId(img.getId());
                clothingMapper.updateById(c);
            }
            return img;
        } catch (IOException e) {
            throw new RuntimeException("failed to read upload stream: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void delete(Long clothingId, Long imageId) {
        ClothingImage img = baseMapper.selectById(imageId);
        if (img == null || !img.getClothingId().equals(clothingId)) {
            throw new ApiException(404, "image not found");
        }
        storage.delete(img.getStorageKey());
        baseMapper.deleteById(imageId);
        if (Boolean.TRUE.equals(img.getIsMain())) {
            Clothing c = clothingMapper.selectById(clothingId);
            if (c != null) {
                c.setMainImageId(null);
                clothingMapper.updateById(c);
            }
        }
    }
}