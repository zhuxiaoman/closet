package com.closet.unit;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.closet.entity.CalendarEntry;
import com.closet.entity.OutfitItem;
import com.closet.entity.WearLog;
import com.closet.mapper.OutfitItemMapper;
import com.closet.mapper.WearLogMapper;
import com.closet.service.impl.WearLogSyncServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit test for {@link WearLogSyncServiceImpl}. Both mappers are
 * mocked so no database is touched - keep these fast and runnable on
 * every save.
 */
class WearLogSyncServiceTest {

    private OutfitItemMapper itemMapper;
    private WearLogMapper wearLogMapper;
    private WearLogSyncServiceImpl service;

    @BeforeEach
    void setUp() {
        itemMapper = mock(OutfitItemMapper.class);
        wearLogMapper = mock(WearLogMapper.class);
        service = new WearLogSyncServiceImpl(itemMapper, wearLogMapper);
    }

    @Test
    void generate_writes_one_log_per_item() {
        when(itemMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(
                outfitItem(10L),
                outfitItem(20L),
                outfitItem(30L)));

        CalendarEntry entry = new CalendarEntry();
        entry.setId(99L);
        entry.setOutfitId(7L);
        entry.setEntryDate(LocalDate.of(2026, 7, 1));

        service.generateForEntry(entry);

        ArgumentCaptor<WearLog> logCap = ArgumentCaptor.forClass(WearLog.class);
        verify(wearLogMapper, times(3)).insert(logCap.capture());
        List<WearLog> logs = logCap.getAllValues();
        // Each log row must carry the calendar_entry_id of the entry and
        // the worn_at date the entry scheduled.
        for (WearLog log : logs) {
            assertEquals(99L, log.getCalendarEntryId());
            assertEquals(LocalDate.of(2026, 7, 1), log.getWornAt());
        }
        assertEquals(10L, logs.get(0).getClothingId());
        assertEquals(20L, logs.get(1).getClothingId());
        assertEquals(30L, logs.get(2).getClothingId());
    }

    @Test
    void generate_clears_old_logs_first() {
        when(itemMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(
                outfitItem(1L)));

        CalendarEntry entry = new CalendarEntry();
        entry.setId(5L);
        entry.setOutfitId(1L);
        entry.setEntryDate(LocalDate.of(2026, 7, 1));

        service.generateForEntry(entry);

        // Old logs for this calendar_entry must be removed before the
        // new ones are written - otherwise re-running generate() would
        // double-count wears.
        InOrder ordered = inOrder(wearLogMapper);
        ordered.verify(wearLogMapper).delete(any(QueryWrapper.class));
        ordered.verify(wearLogMapper).insert(any(WearLog.class));
    }

    @Test
    void deleteForEntry_uses_calendar_id_query_wrapper() {
        service.deleteForEntry(42L);

        ArgumentCaptor<QueryWrapper<WearLog>> qCap =
                ArgumentCaptor.forClass(QueryWrapper.class);
        verify(wearLogMapper, times(1)).delete(qCap.capture());
        String sql = qCap.getValue().getSqlSegment();
        if (!sql.contains("calendar_entry_id =")) {
            throw new AssertionError("expected calendar_entry_id filter, got: " + sql);
        }
    }

    private OutfitItem outfitItem(Long clothingId) {
        OutfitItem oi = new OutfitItem();
        oi.setClothingId(clothingId);
        return oi;
    }
}
