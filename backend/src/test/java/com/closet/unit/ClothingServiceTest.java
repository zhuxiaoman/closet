package com.closet.unit;

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
import com.closet.service.impl.ClothingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit test for {@link ClothingServiceImpl}. All mappers are mocked so
 * no database is touched - keep these fast and runnable on every save.
 */
class ClothingServiceTest {

    private ClothingMapper clothingMapper;
    private ClothingCategoryMapper ccMapper;
    private ClothingTagMapper ctMapper;
    private ClothingServiceImpl service;

    @BeforeEach
    void setUp() {
        clothingMapper = mock(ClothingMapper.class);
        ccMapper = mock(ClothingCategoryMapper.class);
        ctMapper = mock(ClothingTagMapper.class);
        service = new ClothingServiceImpl(clothingMapper, ccMapper, ctMapper);
    }

    @Test
    void create_inserts_clothing_and_links() {
        ClothingRequest req = new ClothingRequest();
        req.setName("白 T 恤");
        req.setSeason("summer");
        req.setPurchasePrice(new BigDecimal("99.00"));
        req.setCategoryIds(List.of(10L, 11L));
        req.setTagIds(List.of(20L));

        when(clothingMapper.insert(any(Clothing.class))).thenAnswer(inv -> {
            Clothing c = inv.getArgument(0);
            c.setId(1L);
            return 1;
        });

        Clothing created = service.create(req);

        assertEquals(1L, created.getId());
        assertEquals("白 T 恤", created.getName());
        assertEquals("summer", created.getSeason());
        assertEquals("active", created.getStatus());

        ArgumentCaptor<ClothingCategory> ccCap = ArgumentCaptor.forClass(ClothingCategory.class);
        verify(ccMapper, times(2)).insert(ccCap.capture());
        assertEquals(1L, ccCap.getAllValues().get(0).getClothingId());
        assertEquals(10L, ccCap.getAllValues().get(0).getCategoryId());
        assertEquals(11L, ccCap.getAllValues().get(1).getCategoryId());

        ArgumentCaptor<ClothingTag> ctCap = ArgumentCaptor.forClass(ClothingTag.class);
        verify(ctMapper, times(1)).insert(ctCap.capture());
        assertEquals(20L, ctCap.getValue().getTagId());
    }

    @Test
    void create_without_links_does_not_touch_junction_tables() {
        ClothingRequest req = new ClothingRequest();
        req.setName("夹克");

        when(clothingMapper.insert(any(Clothing.class))).thenAnswer(inv -> {
            Clothing c = inv.getArgument(0);
            c.setId(2L);
            return 1;
        });

        service.create(req);

        verify(ccMapper, never()).insert(any(ClothingCategory.class));
        verify(ctMapper, never()).insert(any(ClothingTag.class));
    }

    @Test
    void create_defaults_season_to_all_when_blank() {
        ClothingRequest req = new ClothingRequest();
        req.setName("袜子");

        when(clothingMapper.insert(any(Clothing.class))).thenAnswer(inv -> {
            Clothing c = inv.getArgument(0);
            c.setId(3L);
            return 1;
        });

        Clothing created = service.create(req);
        assertEquals("all", created.getSeason());
    }

    @Test
    void get_returns_404_when_missing() {
        when(clothingMapper.selectById(99L)).thenReturn(null);
        ApiException ex = assertThrows(ApiException.class, () -> service.get(99L));
        assertEquals(404, ex.getCode());
    }

    @Test
    void get_maps_entity_to_response() {
        Clothing c = new Clothing();
        c.setId(5L);
        c.setName("牛仔");
        c.setSeason("all");
        c.setStatus("active");
        when(clothingMapper.selectById(5L)).thenReturn(c);

        ClothingResponse resp = service.get(5L);
        assertEquals(5L, resp.getId());
        assertEquals("牛仔", resp.getName());
    }

    @Test
    void update_replaces_category_and_tag_links() {
        Clothing existing = new Clothing();
        existing.setId(7L);
        existing.setName("旧");
        when(clothingMapper.selectById(7L)).thenReturn(existing);

        ClothingRequest req = new ClothingRequest();
        req.setName("新");
        req.setSeason("fall");
        req.setCategoryIds(List.of(99L));
        req.setTagIds(List.of());

        service.update(7L, req);

        verify(ccMapper).delete(any(QueryWrapper.class));
        verify(ccMapper).insert(any(ClothingCategory.class));

        verify(ctMapper).delete(any(QueryWrapper.class));
        verify(ctMapper, never()).insert(any(ClothingTag.class));

        verify(clothingMapper).updateById(existing);
        assertEquals("新", existing.getName());
        assertEquals("fall", existing.getSeason());
    }

    @Test
    void softDelete_marks_status_and_persists() {
        Clothing existing = new Clothing();
        existing.setId(11L);
        existing.setStatus("active");
        when(clothingMapper.selectById(11L)).thenReturn(existing);

        service.softDelete(11L);

        assertEquals("discarded", existing.getStatus());
        verify(clothingMapper).updateById(existing);
    }

    @Test
    void page_uses_default_paging_when_missing() {
        ClothingFilter f = new ClothingFilter();
        f.setKeyword("白");

        @SuppressWarnings("unchecked")
        Page<Clothing> emptyPage = new Page<>(1, 20);
        emptyPage.setRecords(List.of());
        when(clothingMapper.selectPage(any(Page.class), any(QueryWrapper.class)))
                .thenReturn(emptyPage);

        IPage<ClothingResponse> resp = service.page(f);
        assertNotNull(resp);
        assertEquals(0L, resp.getTotal());

        // MyBatis-Plus renders the literal status via a parameter placeholder
        // (#{ew.paramNameValuePairs.MPGENVAL1}) in getSqlSegment(), so we assert
        // on the column name + the param-mapping infrastructure instead.
        ArgumentCaptor<QueryWrapper<Clothing>> qCap = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(clothingMapper).selectPage(any(Page.class), qCap.capture());
        String sql = qCap.getValue().getSqlSegment();
        if (!sql.contains("status =")) {
            throw new AssertionError("expected status column to be filtered, got: " + sql);
        }
        if (!sql.contains("name LIKE")) {
            throw new AssertionError("expected keyword to apply LIKE, got: " + sql);
        }
    }

    @Test
    void page_with_category_id_adds_in_sql_filter() {
        ClothingFilter f = new ClothingFilter();
        f.setCategoryId(42L);

        when(clothingMapper.selectPage(any(Page.class), any(QueryWrapper.class)))
                .thenReturn(new Page<>(1, 20));

        service.page(f);

        ArgumentCaptor<QueryWrapper<Clothing>> qCap = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(clothingMapper).selectPage(any(Page.class), qCap.capture());
        String sql = qCap.getValue().getSqlSegment();
        if (!sql.contains("clothing_category")) {
            throw new AssertionError("expected inSql join to clothing_category, got: " + sql);
        }
    }

    @Test
    void update_404_when_missing() {
        when(clothingMapper.selectById(123L)).thenReturn(null);
        ClothingRequest req = new ClothingRequest();
        req.setName("x");
        assertThrows(ApiException.class, () -> service.update(123L, req));
        verify(clothingMapper, never()).updateById(any(Clothing.class));
    }

    @Test
    void create_persists_brand_and_color() {
        ClothingRequest req = new ClothingRequest();
        req.setName("风衣");
        req.setBrand("优衣库");
        req.setColorPrimary("卡其");
        req.setColorSecondary("米白");
        req.setSize("L");

        when(clothingMapper.insert(any(Clothing.class))).thenAnswer(inv -> {
            Clothing c = inv.getArgument(0);
            c.setId(50L);
            return 1;
        });

        Clothing created = service.create(req);
        assertEquals("优衣库", created.getBrand());
        assertEquals("卡其", created.getColorPrimary());
        assertEquals("米白", created.getColorSecondary());
        assertEquals("L", created.getSize());
    }

    @SuppressWarnings("unused")
    private static <T> T identity(T t) { assertSame(t, t); return t; }

    @SuppressWarnings("unused")
    private static <T> T nullCheck(T t) { assertNull(t); return t; }
}