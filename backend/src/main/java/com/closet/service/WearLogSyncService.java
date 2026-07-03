package com.closet.service;

import com.closet.entity.CalendarEntry;

/**
 * Keeps the {@code wear_log} table in sync with calendar entries: each
 * time an outfit is scheduled on a date, one wear_log row per
 * outfit_item is written with the same {@code worn_at} value.
 */
public interface WearLogSyncService {

    /**
     * Regenerate the wear_log set for {@code entry}. Any previously
     * generated logs for the same calendar_entry_id are removed first
     * so the new set fully replaces the old.
     */
    void generateForEntry(CalendarEntry entry);

    /**
     * Remove every wear_log row produced by the given calendar_entry.
     * Used when the entry itself is deleted.
     */
    void deleteForEntry(Long calendarEntryId);
}
