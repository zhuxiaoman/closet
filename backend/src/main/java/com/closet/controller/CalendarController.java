package com.closet.controller;

import com.closet.common.Result;
import com.closet.entity.CalendarEntry;
import com.closet.service.CalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * REST endpoints for {@code /api/v1/calendar}. Mirrors the CRUD surface
 * of {@code outfit} and {@code clothing}: a range scan, a single-row
 * fetch, and create/update/delete for individual entries. The service
 * layer is responsible for the side effect of regenerating the matching
 * {@code wear_log} rows via {@code WearLogSyncService} on create /
 * update, and tearing them down on delete.
 *
 * <p>{@code from} / {@code to} on the range endpoint are inclusive and
 * are parsed in ISO-8601 ({@code yyyy-MM-dd}) form, matching the
 * {@code entry_date} column.
 */
@RestController
@RequestMapping("/api/v1/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService service;

    @GetMapping
    public Result<List<CalendarEntry>> range(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return Result.ok(service.range(from, to));
    }

    @GetMapping("/{id}")
    public Result<CalendarEntry> get(@PathVariable Long id) {
        return Result.ok(service.get(id));
    }

    @PostMapping
    public Result<CalendarEntry> create(@RequestBody CalendarEntry entry) {
        return Result.ok(service.create(entry));
    }

    @PutMapping("/{id}")
    public Result<CalendarEntry> update(@PathVariable Long id,
                                        @RequestBody CalendarEntry entry) {
        return Result.ok(service.update(id, entry));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.ok();
    }
}
