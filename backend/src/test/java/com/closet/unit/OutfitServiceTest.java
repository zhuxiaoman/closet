package com.closet.unit;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.closet.common.ApiException;
import com.closet.dto.OutfitRequest;
import com.closet.entity.Outfit;
import com.closet.entity.OutfitItem;
import com.closet.mapper.ClothingMapper;
import com.closet.mapper.OutfitItemMapper;
import com.closet.mapper.OutfitMapper;
import com.closet.service.OutfitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit test for {@link com.closet.service.impl.OutfitServiceImpl}.
 * All mappers are mocked so no database is touched - keep these fast
 * and runnable on every save.
 */
class OutfitServiceTest {

    private OutfitMapper outfitMapper;
    private OutfitItemMapper itemMapper;
    private ClothingMapper clothingMapper;
    private OutfitService service;

    @BeforeEach
    void setUp() {
        outfitMapper = mock(OutfitMapper.class);
        itemMapper = mock(OutfitItemMapper.class);
        clothingMapper = mock(ClothingMapper.class);
        service = new com.closet.service.impl.OutfitServiceImpl(
                outfitMapper, itemMapper, clothingMapper);
    }

    @Test
    void create_persists_outfit_and_items() {
        when(outfitMapper.insert(any(Outfit.class))).thenAnswer(inv -> {
            Outfit o = inv.getArgument(0);
            o.setId(1L);
            return 1;
        });

        OutfitRequest req = new OutfitRequest();
        req.setName("周末休闲");
        req.setOccasion("casual");
        req.setSeason("summer");
        req.setIsFavorite(true);
        req.setClothingIds(List.of(10L, 20L, 30L));

        Outfit created = service.create(req);

        assertEquals(1L, created.getId());
        assertEquals("周末休闲", created.getName());
        assertEquals(Boolean.TRUE, created.getIsFavorite());

        verify(outfitMapper, times(1)).insert(any(Outfit.class));

        ArgumentCaptor<OutfitItem> oiCap = ArgumentCaptor.forClass(OutfitItem.class);
        verify(itemMapper, times(3)).insert(oiCap.capture());
        List<OutfitItem> saved = oiCap.getAllValues();
        assertEquals(1L, saved.get(0).getOutfitId());
        assertEquals(10L, saved.get(0).getClothingId());
        assertEquals(0, saved.get(0).getSortOrder());
        assertEquals(20L, saved.get(1).getClothingId());
        assertEquals(1, saved.get(1).getSortOrder());
        assertEquals(30L, saved.get(2).getClothingId());
        assertEquals(2, saved.get(2).getSortOrder());
    }

    @Test
    void create_with_null_clothingIds_works() {
        when(outfitMapper.insert(any(Outfit.class))).thenAnswer(inv -> {
            Outfit o = inv.getArgument(0);
            o.setId(2L);
            return 1;
        });

        OutfitRequest req = new OutfitRequest();
        req.setName("空搭配");
        req.setIsFavorite(null);

        Outfit created = service.create(req);

        assertEquals(2L, created.getId());
        assertEquals(false, created.getIsFavorite());
        verify(outfitMapper, times(1)).insert(any(Outfit.class));
        verify(itemMapper, never()).insert(any(OutfitItem.class));
    }

    @Test
    void create_normalizes_null_favorite_to_false() {
        when(outfitMapper.insert(any(Outfit.class))).thenAnswer(inv -> {
            Outfit o = inv.getArgument(0);
            o.setId(3L);
            return 1;
        });

        OutfitRequest req = new OutfitRequest();
        req.setName("默认收藏=false");
        req.setIsFavorite(null);

        Outfit created = service.create(req);
        assertEquals(Boolean.FALSE, created.getIsFavorite());
    }

    @Test
    void update_throws_when_not_found() {
        when(outfitMapper.selectById(404L)).thenReturn(null);

        OutfitRequest req = new OutfitRequest();
        req.setName("更新");

        ApiException ex = assertThrows(ApiException.class,
                () -> service.update(404L, req));
        assertEquals(404, ex.getCode());

        verify(outfitMapper, never()).updateById(any(Outfit.class));
        verify(itemMapper, never()).delete(any(QueryWrapper.class));
        verify(itemMapper, never()).insert(any(OutfitItem.class));
    }

    @Test
    void update_replaces_all_items() {
        Outfit existing = new Outfit();
        existing.setId(7L);
        existing.setName("旧");
        when(outfitMapper.selectById(7L)).thenReturn(existing);

        OutfitRequest req = new OutfitRequest();
        req.setName("新");
        req.setIsFavorite(true);
        req.setClothingIds(List.of(100L, 200L));

        Outfit updated = service.update(7L, req);

        assertEquals("新", updated.getName());
        assertEquals(Boolean.TRUE, updated.getIsFavorite());

        // Old items wiped, new items inserted in order.
        verify(itemMapper, times(1)).delete(any(QueryWrapper.class));
        ArgumentCaptor<OutfitItem> oiCap = ArgumentCaptor.forClass(OutfitItem.class);
        verify(itemMapper, times(2)).insert(oiCap.capture());
        List<OutfitItem> saved = oiCap.getAllValues();
        assertEquals(7L, saved.get(0).getOutfitId());
        assertEquals(100L, saved.get(0).getClothingId());
        assertEquals(0, saved.get(0).getSortOrder());
        assertEquals(200L, saved.get(1).getClothingId());
        assertEquals(1, saved.get(1).getSortOrder());

        verify(outfitMapper, times(1)).updateById(existing);
    }

    @Test
    void update_with_null_clothingIds_clears_items() {
        Outfit existing = new Outfit();
        existing.setId(8L);
        when(outfitMapper.selectById(8L)).thenReturn(existing);

        OutfitRequest req = new OutfitRequest();
        req.setName("清空");
        req.setClothingIds(null);

        service.update(8L, req);

        verify(itemMapper, times(1)).delete(any(QueryWrapper.class));
        verify(itemMapper, never()).insert(any(OutfitItem.class));
    }

    @Test
    void addItem_inserts_with_sort_order() {
        service.addItem(11L, 99L, 5);

        ArgumentCaptor<OutfitItem> oiCap = ArgumentCaptor.forClass(OutfitItem.class);
        verify(itemMapper, times(1)).insert(oiCap.capture());
        OutfitItem saved = oiCap.getValue();
        assertEquals(11L, saved.getOutfitId());
        assertEquals(99L, saved.getClothingId());
        assertEquals(5, saved.getSortOrder());
    }

    @Test
    void removeItem_uses_query_wrapper_with_both_ids() {
        service.removeItem(12L, 88L);

        ArgumentCaptor<QueryWrapper<OutfitItem>> qCap =
                ArgumentCaptor.forClass(QueryWrapper.class);
        verify(itemMapper, times(1)).delete(qCap.capture());
        String sql = qCap.getValue().getSqlSegment();
        // The wrapper must filter on both columns; otherwise we'd risk
        // wiping the entire join table for the outfit.
        if (!sql.contains("outfit_id =")) {
            throw new AssertionError("expected outfit_id filter, got: " + sql);
        }
        if (!sql.contains("clothing_id =")) {
            throw new AssertionError("expected clothing_id filter, got: " + sql);
        }
    }

    @Test
    void reorderItems_updates_each_sort_order() {
        when(itemMapper.update(any(OutfitItem.class), any(QueryWrapper.class)))
                .thenReturn(1);

        List<OutfitService.ItemOrder> orders = List.of(
                new OutfitService.ItemOrder(10L, 0),
                new OutfitService.ItemOrder(20L, 1),
                new OutfitService.ItemOrder(30L, 2)
        );

        service.reorderItems(13L, orders);

        ArgumentCaptor<OutfitItem> oiCap = ArgumentCaptor.forClass(OutfitItem.class);
        verify(itemMapper, times(3)).update(oiCap.capture(), any(QueryWrapper.class));
        List<OutfitItem> updates = oiCap.getAllValues();
        assertEquals(13L, updates.get(0).getOutfitId());
        assertEquals(10L, updates.get(0).getClothingId());
        assertEquals(0, updates.get(0).getSortOrder());
        assertEquals(20L, updates.get(1).getClothingId());
        assertEquals(1, updates.get(1).getSortOrder());
        assertEquals(30L, updates.get(2).getClothingId());
        assertEquals(2, updates.get(2).getSortOrder());
    }

    @Test
    void reorderItems_with_null_list_is_noop() {
        service.reorderItems(14L, null);
        verify(itemMapper, never()).update(any(OutfitItem.class), any(QueryWrapper.class));
    }

    @Test
    void delete_calls_deleteById() {
        service.delete(15L);
        verify(outfitMapper, times(1)).deleteById(15L);
    }
}