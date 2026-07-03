package com.closet.service;

import com.closet.entity.CalendarEntry;

import java.time.LocalDate;
import java.util.List;

/**
 * Business operations for the {@code calendar_entry} table. create/update
 * both delegate to {@link WearLogSyncService} so the {@code wear_log} rows
 * stay in lockstep with the planned outfit; delete tears the log rows
 * down first via the same sync service.
 */
public interface CalendarService {

    /** Range scan ordered by entry_date ascending. */
    List<CalendarEntry> range(LocalDate from, LocalDate to);

    /** Single-row fetch; throws 404 when the id is unknown. */
    CalendarEntry get(Long id);

    /** Insert and regenerate wear_log rows for the chosen outfit. */
    CalendarEntry create(CalendarEntry entry);

    /** Update and regenerate wear_log rows. */
    CalendarEntry update(Long id, CalendarEntry entry);

    /** Drop wear_log rows tied to this entry, then drop the entry itself. */
    void delete(Long id);
}
