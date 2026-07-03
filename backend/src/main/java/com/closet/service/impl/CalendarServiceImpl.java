package com.closet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.closet.common.ApiException;
import com.closet.entity.CalendarEntry;
import com.closet.mapper.CalendarEntryMapper;
import com.closet.service.CalendarService;
import com.closet.service.WearLogSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CalendarServiceImpl implements CalendarService {

    private final CalendarEntryMapper entryMapper;
    private final WearLogSyncService syncService;

    @Override
    public List<CalendarEntry> range(LocalDate from, LocalDate to) {
        return entryMapper.selectList(new QueryWrapper<CalendarEntry>()
                .between("entry_date", from, to)
                .orderByAsc("entry_date"));
    }

    @Override
    public CalendarEntry get(Long id) {
        CalendarEntry entry = entryMapper.selectById(id);
        if (entry == null) {
            throw new ApiException(404, "calendar entry not found");
        }
        return entry;
    }

    @Override
    @Transactional
    public CalendarEntry create(CalendarEntry entry) {
        entryMapper.insert(entry);
        syncService.generateForEntry(entry);
        return entry;
    }

    @Override
    @Transactional
    public CalendarEntry update(Long id, CalendarEntry entry) {
        CalendarEntry exist = entryMapper.selectById(id);
        if (exist == null) {
            throw new ApiException(404, "calendar entry not found");
        }
        exist.setEntryDate(entry.getEntryDate());
        exist.setSlot(entry.getSlot());
        exist.setOutfitId(entry.getOutfitId());
        exist.setNotes(entry.getNotes());
        entryMapper.updateById(exist);
        // The outfit (or date) may have changed, so drop the old logs and
        // regenerate against the new outfit_item set.
        syncService.generateForEntry(exist);
        return exist;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        syncService.deleteForEntry(id);
        entryMapper.deleteById(id);
    }
}
