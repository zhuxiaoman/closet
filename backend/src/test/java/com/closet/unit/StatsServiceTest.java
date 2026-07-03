package com.closet.unit;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit test for {@link com.closet.service.impl.StatsServiceImpl}.
 * All mappers are mocked so no database is touched - keep these fast and
 * runnable on every save.
 */
class StatsServiceTest {

    private ClothingMapper clothingMapper;
    private OutfitMapper outfitMapper;
    private WearLogMapper wearLogMapper;
    private StatsService service;

    @BeforeEach
    void setUp() {
        clothingMapper = mock(ClothingMapper.class);
        outfitMapper = mock(OutfitMapper.class);
        wearLogMapper = mock(WearLogMapper.class);
        service = new com.closet.service.impl.StatsServiceImpl(
                clothingMapper, outfitMapper, wearLogMapper);
    }

    @Test
    void overview_returns_totals() {
        when(clothingMapper.selectCount(null)).thenReturn(12L);
        when(outfitMapper.selectCount(null)).thenReturn(5L);
        when(wearLogMapper.selectCount(any(QueryWrapper.class))).thenReturn(7L);

        StatsOverview s = service.overview();

        assertEquals(12L, s.getTotalClothing());
        assertEquals(5L, s.getTotalOutfits());
        assertEquals(7L, s.getMonthWears());

        // overview() calls wearLogMapper.selectCount exactly once for the
        // current-month filter.
        verify(wearLogMapper, times(1)).selectCount(any(QueryWrapper.class));
    }

    @Test
    void forClothing_throws_when_not_found() {
        when(clothingMapper.selectById(404L)).thenReturn(null);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.forClothing(404L));
        assertEquals(404, ex.getCode());

        // No wear_log lookup should happen if the clothing itself is missing.
        verify(wearLogMapper, times(0)).selectList(any(QueryWrapper.class));
    }

    @Test
    void forClothing_calculates_cost_per_wear() {
        Clothing c = new Clothing();
        c.setId(1L);
        c.setName("White T-shirt");
        c.setPurchasePrice(new BigDecimal("100.00"));
        when(clothingMapper.selectById(1L)).thenReturn(c);

        WearLog w1 = new WearLog();
        w1.setClothingId(1L);
        w1.setWornAt(LocalDate.of(2026, 1, 10));
        WearLog w2 = new WearLog();
        w2.setClothingId(1L);
        w2.setWornAt(LocalDate.of(2026, 2, 20));
        // forClothing -> buildStat -> orderByAsc("worn_at"). Return asc-sorted.
        when(wearLogMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(List.of(w1, w2));

        ClothingStat s = service.forClothing(1L);

        assertEquals(1L, s.getClothingId());
        assertEquals("White T-shirt", s.getName());
        assertEquals(2L, s.getWearCount());
        assertEquals(LocalDate.of(2026, 1, 10), s.getFirstWorn());
        assertEquals(LocalDate.of(2026, 2, 20), s.getLastWorn());
        // 100 / 2 = 50.00, scale 2 HALF_UP.
        assertEquals(0, new BigDecimal("50.00").compareTo(s.getCostPerWear()));
    }

    @Test
    void forClothing_with_no_logs_has_null_dates_and_no_cost() {
        Clothing c = new Clothing();
        c.setId(2L);
        c.setName("New jacket");
        c.setPurchasePrice(new BigDecimal("300.00"));
        when(clothingMapper.selectById(2L)).thenReturn(c);
        when(wearLogMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(List.of());

        ClothingStat s = service.forClothing(2L);

        assertEquals(0L, s.getWearCount());
        assertNull(s.getFirstWorn());
        assertNull(s.getLastWorn());
        assertNull(s.getCostPerWear());
    }

    @Test
    void mostWorn_returns_top_n_by_count() {
        Map<String, Object> r1 = Map.of("clothing_id", 11L, "cnt", 9L);
        Map<String, Object> r2 = Map.of("clothing_id", 22L, "cnt", 3L);
        when(wearLogMapper.selectMaps(any(QueryWrapper.class)))
                .thenReturn(List.of(r1, r2));

        // forClothing -> selectById -> stub clothing rows by id.
        Clothing c11 = new Clothing();
        c11.setId(11L);
        c11.setName("Jeans");
        Clothing c22 = new Clothing();
        c22.setId(22L);
        c22.setName("Hat");
        when(clothingMapper.selectById(11L)).thenReturn(c11);
        when(clothingMapper.selectById(22L)).thenReturn(c22);

        // buildStat -> wearLogMapper.selectList. Return empty list so we
        // don't have to stub the wear_count computation.
        when(wearLogMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of());

        List<ClothingStat> top = service.mostWorn(2);

        assertEquals(2, top.size());
        assertEquals(11L, top.get(0).getClothingId());
        assertEquals(22L, top.get(1).getClothingId());

        // selectMaps called once (for the aggregated count query).
        verify(wearLogMapper, times(1)).selectMaps(any(QueryWrapper.class));
        // forClothing invoked for each row.
        verify(clothingMapper, times(1)).selectById(11L);
        verify(clothingMapper, times(1)).selectById(22L);
    }

    @Test
    void leastWorn_filters_active_clothing_with_no_recent_wears() {
        Clothing active1 = new Clothing();
        active1.setId(100L);
        active1.setName("Linen shirt");
        Clothing active2 = new Clothing();
        active2.setId(101L);
        active2.setName("Sneakers");
        when(clothingMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(List.of(active1, active2));

        // The first active clothing has a recent wear_log (NOT counted),
        // the second has none (counted as least-worn).
        when(wearLogMapper.selectCount(any(QueryWrapper.class)))
                .thenReturn(1L)   // active1 -> skipped
                .thenReturn(0L);  // active2 -> included

        // buildStat -> selectList for the wear_log history. Empty keeps the
        // assertion focused on filtering.
        when(wearLogMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of());

        List<ClothingStat> stale = service.leastWorn(30);

        assertEquals(1, stale.size());
        assertEquals(101L, stale.get(0).getClothingId());
        assertEquals("Sneakers", stale.get(0).getName());
    }

    @Test
    void mostWorn_appends_limit_to_query_wrapper() {
        when(wearLogMapper.selectMaps(any(QueryWrapper.class)))
                .thenReturn(List.of());

        service.mostWorn(5);

        ArgumentCaptor<QueryWrapper<WearLog>> cap =
                ArgumentCaptor.forClass(QueryWrapper.class);
        verify(wearLogMapper, times(1)).selectMaps(cap.capture());
        String sql = cap.getValue().getSqlSegment();
        // The 'limit N' suffix is set via .last() - make sure it's there so
        // the service can't accidentally return every grouped row.
        if (!sql.contains("limit 5")) {
            throw new AssertionError("expected 'limit 5' in SQL, got: " + sql);
        }
    }
}