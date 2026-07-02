package com.closet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.closet.common.ApiException;
import com.closet.dto.ClothingFilter;
import com.closet.dto.ClothingRequest;
import com.closet.dto.ClothingResponse;
import com.closet.entity.Clothing;
import com.closet.entity.ClothingCategory;
import com.closet.entity.ClothingTag;
import com.closet.mapper.ClothingCategoryMapper;
import com.closet.mapper.ClothingMapper;
import com.closet.mapper.ClothingTagMapper;
import com.closet.service.ClothingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClothingServiceImpl implements ClothingService {

    private final ClothingMapper clothingMapper;
    private final ClothingCategoryMapper ccMapper;
    private final ClothingTagMapper ctMapper;

    @Override
    public IPage<ClothingResponse> page(ClothingFilter f) {
        QueryWrapper<Clothing> q = new QueryWrapper<>();
        // Default to active items unless caller explicitly asks for another status.
        if (f.getStatus() != null && !f.getStatus().isBlank()) {
            q.eq("status", f.getStatus());
        } else {
            q.eq("status", "active");
        }
        if (f.getSeason() != null && !f.getSeason().isBlank()) {
            q.eq("season", f.getSeason());
        }
        if (f.getKeyword() != null && !f.getKeyword().isBlank()) {
            q.like("name", f.getKeyword().trim());
        }
        if (f.getCategoryId() != null) {
            q.inSql("id", "SELECT clothing_id FROM clothing_category WHERE category_id = "
                    + f.getCategoryId());
        }
        if (f.getTagId() != null) {
            q.inSql("id", "SELECT clothing_id FROM clothing_tag WHERE tag_id = "
                    + f.getTagId());
        }
        q.orderByDesc("created_at");

        long pageNum = f.getPage() == null || f.getPage() < 1 ? 1 : f.getPage();
        long pageSize = f.getSize() == null || f.getSize() < 1 ? 20 : f.getSize();
        IPage<Clothing> page = clothingMapper.selectPage(new Page<>(pageNum, pageSize), q);
        return page.convert(this::toResponse);
    }

    @Override
    public ClothingResponse get(Long id) {
        Clothing c = clothingMapper.selectById(id);
        if (c == null) {
            throw new ApiException(404, "clothing not found");
        }
        return toResponse(c);
    }

    @Override
    @Transactional
    public Clothing create(ClothingRequest req) {
        Clothing c = new Clothing();
        applyRequest(c, req);
        c.setStatus("active");
        clothingMapper.insert(c);
        if (req.getCategoryIds() != null) {
            saveCategories(c.getId(), req.getCategoryIds());
        }
        if (req.getTagIds() != null) {
            saveTags(c.getId(), req.getTagIds());
        }
        return c;
    }

    @Override
    @Transactional
    public Clothing update(Long id, ClothingRequest req) {
        Clothing exist = clothingMapper.selectById(id);
        if (exist == null) {
            throw new ApiException(404, "clothing not found");
        }
        applyRequest(exist, req);
        clothingMapper.updateById(exist);

        // Replace the M:N links wholesale so callers can drop categories
        // by simply not including the id in the request.
        ccMapper.delete(new QueryWrapper<ClothingCategory>().eq("clothing_id", id));
        ctMapper.delete(new QueryWrapper<ClothingTag>().eq("clothing_id", id));
        if (req.getCategoryIds() != null) {
            saveCategories(id, req.getCategoryIds());
        }
        if (req.getTagIds() != null) {
            saveTags(id, req.getTagIds());
        }
        return exist;
    }

    @Override
    public void softDelete(Long id) {
        Clothing exist = clothingMapper.selectById(id);
        if (exist == null) {
            throw new ApiException(404, "clothing not found");
        }
        exist.setStatus("discarded");
        clothingMapper.updateById(exist);
    }

    private void applyRequest(Clothing c, ClothingRequest r) {
        c.setName(r.getName());
        c.setBrand(r.getBrand());
        c.setColorPrimary(r.getColorPrimary());
        c.setColorSecondary(r.getColorSecondary());
        c.setSize(r.getSize());
        c.setPurchasePrice(r.getPurchasePrice());
        c.setPurchaseDate(r.getPurchaseDate());
        c.setSeason(r.getSeason() == null || r.getSeason().isBlank() ? "all" : r.getSeason());
        c.setNotes(r.getNotes());
    }

    private void saveCategories(Long clothingId, List<Long> ids) {
        for (Long cid : ids) {
            ClothingCategory cc = new ClothingCategory();
            cc.setClothingId(clothingId);
            cc.setCategoryId(cid);
            ccMapper.insert(cc);
        }
    }

    private void saveTags(Long clothingId, List<Long> ids) {
        for (Long tid : ids) {
            ClothingTag ct = new ClothingTag();
            ct.setClothingId(clothingId);
            ct.setTagId(tid);
            ctMapper.insert(ct);
        }
    }

    private ClothingResponse toResponse(Clothing c) {
        ClothingResponse r = new ClothingResponse();
        org.springframework.beans.BeanUtils.copyProperties(c, r);
        return r;
    }
}
