package com.closet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.closet.common.ApiException;
import com.closet.dto.ClothingStat;
import com.closet.dto.StatsOverview;
import com.closet.entity.Clothing;
import com.closet.entity.WearLog;
import com.closet.mapper.ClothingMapper;
import com.closet.mapper.OutfitMapper;
import com.closet.mapper.WearLogMapper;
import com.closet.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final ClothingMapper clothingMapper;
    private final OutfitMapper outfitMapper;
    private final WearLogMapper wearLogMapper;

    @Override
    public StatsOverview overview() {
        StatsOverview s = new StatsOverview();
        s.setTotalClothing(clothingMapper.selectCount(null));
        s.setTotalOutfits(outfitMapper.selectCount(null));
        LocalDate firstOfMonth = LocalDate.now().withDayOfMonth(1);
        s.setMonthWears(wearLogMapper.selectCount(
                new QueryWrapper<WearLog>().ge("worn_at", firstOfMonth)));
        return s;
    }

    @Override
    public ClothingStat forClothing(Long clothingId) {
        Clothing c = clothingMapper.selectById(clothingId);
        if (c == null) {
            throw new ApiException(404, "clothing not found");
        }
        return buildStat(c);
    }

    @Override
    public List<ClothingStat> mostWorn(int limit) {
        // Group wear_log by clothing_id, count desc, take top N.
        List<Map<String, Object>> rows = wearLogMapper.selectMaps(
                new QueryWrapper<WearLog>()
                        .select("clothing_id", "count(*) as cnt")
                        .groupBy("clothing_id")
                        .orderByDesc("cnt")
                        .last("limit " + limit));
        return rows.stream().map(this::rowToStat).toList();
    }

    @Override
    public List<ClothingStat> leastWorn(int days) {
        LocalDate cutoff = LocalDate.now().minusDays(days);
        // Find status='active' clothings with no wear_log since `cutoff`.
        List<Clothing> all = clothingMapper.selectList(
                new QueryWrapper<Clothing>().eq("status", "active"));
        return all.stream()
                .filter(c -> wearLogMapper.selectCount(
                        new QueryWrapper<WearLog>()
                                .eq("clothing_id", c.getId())
                                .ge("worn_at", cutoff)) == 0)
                .map(this::buildStat)
                .toList();
    }

    private ClothingStat buildStat(Clothing c) {
        List<WearLog> logs = wearLogMapper.selectList(
                new QueryWrapper<WearLog>()
                        .eq("clothing_id", c.getId())
                        .orderByAsc("worn_at"));
        ClothingStat s = new ClothingStat();
        s.setClothingId(c.getId());
        s.setName(c.getName());
        s.setWearCount(logs.size());
        if (!logs.isEmpty()) {
            s.setFirstWorn(logs.get(0).getWornAt());
            s.setLastWorn(logs.get(logs.size() - 1).getWornAt());
            if (c.getPurchasePrice() != null) {
                s.setCostPerWear(c.getPurchasePrice()
                        .divide(BigDecimal.valueOf(logs.size()), 2, RoundingMode.HALF_UP));
            }
        }
        return s;
    }

    private ClothingStat rowToStat(Map<String, Object> row) {
        Long id = ((Number) row.get("clothing_id")).longValue();
        return forClothing(id);
    }
}