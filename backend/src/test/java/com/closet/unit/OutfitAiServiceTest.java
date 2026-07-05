package com.closet.unit;

import com.closet.common.ApiException;
import com.closet.dto.AiGenerateRequest;
import com.closet.dto.AiGenerateResponse;
import com.closet.entity.Clothing;
import com.closet.entity.OutfitAiGeneration;
import com.closet.mapper.ClothingMapper;
import com.closet.mapper.OutfitAiGenerationMapper;
import com.closet.service.impl.OutfitAiServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit test for {@link OutfitAiServiceImpl}. Both mappers are
 * mocked so the test stays fast and database-free. The four cases
 * mirror the plan's safety net: count guarantee, season filter,
 * outfit uniqueness, seed inclusion. Plus a feedback round-trip
 * for the second public method.
 */
class OutfitAiServiceTest {

    private ClothingMapper clothingMapper;
    private OutfitAiGenerationMapper aiMapper;
    private OutfitAiServiceImpl service;

    @BeforeEach
    void setUp() {
        clothingMapper = mock(ClothingMapper.class);
        aiMapper = mock(OutfitAiGenerationMapper.class);
        service = new OutfitAiServiceImpl(clothingMapper, aiMapper);

        // Stub insert() so the persisted entity gains an id, mirroring
        // MyBatis-Plus AUTO behaviour.
        when(aiMapper.insert(any(OutfitAiGeneration.class))).thenAnswer(inv -> {
            OutfitAiGeneration g = inv.getArgument(0);
            g.setId(System.nanoTime());
            return 1;
        });
    }

    /** Tiny helper: build a Clothing row with the given id / season / color. */
    private static Clothing mk(Long id, String season, String color) {
        Clothing c = new Clothing();
        c.setId(id);
        c.setName("item-" + id);
        c.setSeason(season);
        c.setColorPrimary(color);
        return c;
    }

    @Test
    void 生成结果必须是5套() {
        when(clothingMapper.selectList(any())).thenReturn(List.of(
                mk(1L, "summer", "#ffffff"),
                mk(2L, "summer", "#d4b896"),
                mk(3L, "summer", "#a8c8d8"),
                mk(4L, "summer", "#5a4032"),
                mk(5L, "summer", "#88aa66")
        ));

        AiGenerateRequest req = new AiGenerateRequest();
        req.setSeedClothingIds(List.of(1L));
        req.setSeason("summer");

        AiGenerateResponse resp = service.generate(req);

        assertNotNull(resp);
        assertNotNull(resp.getOutfits());
        assertEquals(5, resp.getOutfits().size(),
                "OutfitAiService must always return exactly 5 outfits");
        assertNotNull(resp.getGenerationId());
    }

    @Test
    void 非当季衣物必须被过滤() {
        when(clothingMapper.selectList(any())).thenReturn(List.of(
                mk(1L, "summer", "#ffffff"),
                mk(2L, "winter", "#5a4032")
        ));

        AiGenerateRequest req = new AiGenerateRequest();
        req.setSeedClothingIds(List.of(1L));
        req.setSeason("summer");

        AiGenerateResponse resp = service.generate(req);

        // Winter item (id 2) must never appear in any of the 5 outfits.
        for (List<Long> outfit : resp.getOutfits()) {
            assertFalse(outfit.contains(2L),
                    "outfit should not contain off-season id 2: " + outfit);
        }
    }

    @Test
    void 五套之间不能完全重复() {
        // 20 candidate items so the algorithm has plenty of variety to pick from.
        List<Clothing> pool = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            pool.add(mk((long) i, "summer", "#ffffff"));
        }
        when(clothingMapper.selectList(any())).thenReturn(pool);

        AiGenerateRequest req = new AiGenerateRequest();
        req.setSeedClothingIds(List.of(0L));
        req.setSeason("summer");

        AiGenerateResponse resp = service.generate(req);

        Set<Set<Long>> unique = new HashSet<>();
        for (List<Long> outfit : resp.getOutfits()) {
            unique.add(new HashSet<>(outfit));
        }
        assertEquals(5, unique.size(),
                "5 outfits must be pairwise distinct, got " + unique);
    }

    @Test
    void 起点衣物必须出现在每套结果中() {
        when(clothingMapper.selectList(any())).thenReturn(List.of(
                mk(1L, "summer", "#ffffff"),
                mk(2L, "summer", "#d4b896"),
                mk(3L, "summer", "#a8c8d8")
        ));

        AiGenerateRequest req = new AiGenerateRequest();
        req.setSeedClothingIds(List.of(1L));
        req.setSeason("summer");

        AiGenerateResponse resp = service.generate(req);

        for (int i = 0; i < resp.getOutfits().size(); i++) {
            List<Long> outfit = resp.getOutfits().get(i);
            assertTrue(outfit.contains(1L),
                    "outfit #" + i + " missing seed id 1: " + outfit);
        }
    }

    @Test
    void 持久化会把生成结果写到表里() {
        when(clothingMapper.selectList(any())).thenReturn(List.of(
                mk(1L, "summer", "#ffffff"),
                mk(2L, "summer", "#a8c8d8")
        ));

        AiGenerateRequest req = new AiGenerateRequest();
        req.setSeedClothingIds(List.of(1L));
        req.setOccasion("casual");
        req.setSeason("summer");

        service.generate(req);

        ArgumentCaptor<OutfitAiGeneration> cap =
                ArgumentCaptor.forClass(OutfitAiGeneration.class);
        verify(aiMapper, times(1)).insert(cap.capture());
        OutfitAiGeneration saved = cap.getValue();
        assertEquals("casual", saved.getOccasion());
        assertEquals("summer", saved.getSeason());
        assertEquals("none", saved.getFeedback());
        assertNotNull(saved.getSeedClothingIds());
        assertTrue(saved.getSeedClothingIds().contains(1L));
        assertNotNull(saved.getResultOutfitIds());
        assertEquals(5, saved.getResultOutfitIds().size());
    }

    @Test
    void seed_为空_抛ApiException() {
        AiGenerateRequest req = new AiGenerateRequest();
        req.setSeedClothingIds(List.of());

        ApiException ex = assertThrows(ApiException.class,
                () -> service.generate(req));
        assertEquals(400, ex.getCode());
        verify(aiMapper, never()).insert(any(OutfitAiGeneration.class));
    }

    @Test
    void seed_不在当季_抛ApiException() {
        when(clothingMapper.selectList(any())).thenReturn(List.of(
                mk(2L, "summer", "#ffffff")
        ));

        AiGenerateRequest req = new AiGenerateRequest();
        req.setSeedClothingIds(List.of(99L));
        req.setSeason("summer");

        assertThrows(ApiException.class, () -> service.generate(req));
        verify(aiMapper, never()).insert(any(OutfitAiGeneration.class));
    }

    @Test
    void recordFeedback_更新行() {
        OutfitAiGeneration existing = new OutfitAiGeneration();
        existing.setId(42L);
        existing.setFeedback("none");
        when(aiMapper.selectById(42L)).thenReturn(existing);

        service.recordFeedback(42L, "like");

        ArgumentCaptor<OutfitAiGeneration> cap =
                ArgumentCaptor.forClass(OutfitAiGeneration.class);
        verify(aiMapper, times(1)).updateById(cap.capture());
        assertEquals("like", cap.getValue().getFeedback());
    }

    @Test
    void recordFeedback_行不存在_抛404() {
        when(aiMapper.selectById(7L)).thenReturn(null);
        ApiException ex = assertThrows(ApiException.class,
                () -> service.recordFeedback(7L, "like"));
        assertEquals(404, ex.getCode());
        verify(aiMapper, never()).updateById(any(OutfitAiGeneration.class));
    }

    @Test
    void recordFeedback_非法值_抛400() {
        OutfitAiGeneration existing = new OutfitAiGeneration();
        existing.setId(8L);
        when(aiMapper.selectById(8L)).thenReturn(existing);

        assertThrows(ApiException.class,
                () -> service.recordFeedback(8L, "thumbs_up"));
        verify(aiMapper, never()).updateById(any(OutfitAiGeneration.class));
    }
}
