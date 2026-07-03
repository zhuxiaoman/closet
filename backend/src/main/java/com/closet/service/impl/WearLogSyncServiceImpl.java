package com.closet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.closet.entity.CalendarEntry;
import com.closet.entity.OutfitItem;
import com.closet.entity.WearLog;
import com.closet.mapper.OutfitItemMapper;
import com.closet.mapper.WearLogMapper;
import com.closet.service.WearLogSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WearLogSyncServiceImpl implements WearLogSyncService {

    private final OutfitItemMapper itemMapper;
    private final WearLogMapper wearLogMapper;

    @Override
    @Transactional
    public void generateForEntry(CalendarEntry entry) {
        // Wipe logs from a previous generate() call so this is idempotent
        // across create + update.
        deleteForEntry(entry.getId());

        List<OutfitItem> items = itemMapper.selectList(
                new QueryWrapper<OutfitItem>().eq("outfit_id", entry.getOutfitId()));
        for (OutfitItem oi : items) {
            WearLog log = new WearLog();
            log.setClothingId(oi.getClothingId());
            log.setCalendarEntryId(entry.getId());
            log.setWornAt(entry.getEntryDate());
            wearLogMapper.insert(log);
        }
    }

    @Override
    @Transactional
    public void deleteForEntry(Long calendarEntryId) {
        wearLogMapper.delete(
                new QueryWrapper<WearLog>().eq("calendar_entry_id", calendarEntryId));
    }
}
