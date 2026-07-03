package com.closet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.closet.common.ApiException;
import com.closet.dto.OutfitRequest;
import com.closet.dto.OutfitResponse;
import com.closet.entity.Clothing;
import com.closet.entity.Outfit;
import com.closet.entity.OutfitItem;
import com.closet.mapper.ClothingMapper;
import com.closet.mapper.OutfitItemMapper;
import com.closet.mapper.OutfitMapper;
import com.closet.service.OutfitService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OutfitServiceImpl implements OutfitService {

    private final OutfitMapper outfitMapper;
    private final OutfitItemMapper itemMapper;
    // Used by toResponse() to resolve outfit_item rows into full Clothing
    // entities. Optional - if null, items list comes back empty.
    private final ClothingMapper clothingMapper;

    @Override
    public IPage<OutfitResponse> page(String season, String occasion, Boolean favorite,
                                      int page, int size) {
        QueryWrapper<Outfit> q = new QueryWrapper<>();
        if (season != null && !season.isBlank()) {
            q.eq("season", season);
        }
        if (occasion != null && !occasion.isBlank()) {
            q.eq("occasion", occasion);
        }
        if (favorite != null) {
            q.eq("is_favorite", favorite);
        }
        q.orderByDesc("created_at");

        long p = page < 1 ? 1 : page;
        long s = size < 1 ? 20 : size;
        IPage<Outfit> result = outfitMapper.selectPage(new Page<>(p, s), q);
        return result.convert(this::toResponse);
    }

    @Override
    public OutfitResponse get(Long id) {
        Outfit o = outfitMapper.selectById(id);
        if (o == null) {
            throw new ApiException(404, "outfit not found");
        }
        return toResponse(o);
    }

    @Override
    @Transactional
    public Outfit create(OutfitRequest req) {
        Outfit o = new Outfit();
        BeanUtils.copyProperties(req, o);
        // Boolean.TRUE.equals(null) == false - safe handling of missing flag.
        o.setIsFavorite(Boolean.TRUE.equals(req.getIsFavorite()));
        outfitMapper.insert(o);

        if (req.getClothingIds() != null) {
            for (int i = 0; i < req.getClothingIds().size(); i++) {
                OutfitItem oi = new OutfitItem();
                oi.setOutfitId(o.getId());
                oi.setClothingId(req.getClothingIds().get(i));
                oi.setSortOrder(i);
                itemMapper.insert(oi);
            }
        }
        return o;
    }

    @Override
    @Transactional
    public Outfit update(Long id, OutfitRequest req) {
        Outfit exist = outfitMapper.selectById(id);
        if (exist == null) {
            throw new ApiException(404, "outfit not found");
        }
        BeanUtils.copyProperties(req, exist, "id", "createdAt");
        exist.setIsFavorite(Boolean.TRUE.equals(req.getIsFavorite()));
        outfitMapper.updateById(exist);

        // Wholly replace the item set so callers can drop rows by simply
        // omitting the clothing id from the request body.
        itemMapper.delete(new QueryWrapper<OutfitItem>().eq("outfit_id", id));
        if (req.getClothingIds() != null) {
            for (int i = 0; i < req.getClothingIds().size(); i++) {
                OutfitItem oi = new OutfitItem();
                oi.setOutfitId(id);
                oi.setClothingId(req.getClothingIds().get(i));
                oi.setSortOrder(i);
                itemMapper.insert(oi);
            }
        }
        return exist;
    }

    @Override
    public void delete(Long id) {
        // outfit_item rows are cleaned up via FK ON DELETE CASCADE on the
        // outfit_id column. calendar_entry.outfit_id is ON DELETE RESTRICT
        // (see schema.sql), so callers MUST detach the outfit from any
        // calendar_entry before invoking this - otherwise Postgres raises
        // a FK violation.
        outfitMapper.deleteById(id);
    }

    @Override
    public void addItem(Long outfitId, Long clothingId, int sortOrder) {
        OutfitItem oi = new OutfitItem();
        oi.setOutfitId(outfitId);
        oi.setClothingId(clothingId);
        oi.setSortOrder(sortOrder);
        itemMapper.insert(oi);
    }

    @Override
    public void removeItem(Long outfitId, Long clothingId) {
        itemMapper.delete(new QueryWrapper<OutfitItem>()
                .eq("outfit_id", outfitId)
                .eq("clothing_id", clothingId));
    }

    @Override
    @Transactional
    public void reorderItems(Long outfitId, List<ItemOrder> orders) {
        if (orders == null) {
            return;
        }
        for (ItemOrder o : orders) {
            OutfitItem oi = new OutfitItem();
            oi.setOutfitId(outfitId);
            oi.setClothingId(o.clothingId());
            oi.setSortOrder(o.sortOrder());
            // outfit_item has no PK - updateById() is wrong here. Use
            // conditional update against (outfit_id, clothing_id).
            itemMapper.update(oi, new QueryWrapper<OutfitItem>()
                    .eq("outfit_id", outfitId)
                    .eq("clothing_id", o.clothingId()));
        }
    }

    private OutfitResponse toResponse(Outfit o) {
        OutfitResponse r = new OutfitResponse();
        BeanUtils.copyProperties(o, r);

        if (clothingMapper == null) {
            r.setItems(Collections.emptyList());
            return r;
        }

        QueryWrapper<OutfitItem> q = new QueryWrapper<OutfitItem>()
                .eq("outfit_id", o.getId())
                .orderByAsc("sort_order");
        List<OutfitItem> items = itemMapper.selectList(q);
        if (items.isEmpty()) {
            r.setItems(Collections.emptyList());
            return r;
        }

        List<Long> clothingIds = items.stream()
                .map(OutfitItem::getClothingId)
                .collect(Collectors.toList());
        List<Clothing> clothings = clothingMapper.selectBatchIds(clothingIds);
        // Preserve the sort_order from outfit_item, since selectBatchIds
        // returns rows in PK order which may not match.
        List<Clothing> ordered = new ArrayList<>(clothings.size());
        for (OutfitItem oi : items) {
            for (Clothing c : clothings) {
                if (c.getId().equals(oi.getClothingId())) {
                    ordered.add(c);
                    break;
                }
            }
        }
        r.setItems(ordered);
        return r;
    }
}