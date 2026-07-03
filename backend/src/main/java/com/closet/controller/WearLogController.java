package com.closet.controller;

import com.closet.common.ApiException;
import com.closet.common.Result;
import com.closet.entity.WearLog;
import com.closet.mapper.ClothingMapper;
import com.closet.mapper.WearLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

/**
 * Manual wear-log endpoints under {@code /api/v1/wear-logs}. The POST
 * route covers the "I forgot to log it on the day" case; the DELETE
 * route lets the user undo a mistaken manual entry. Calendar-driven
 * wear_log rows are created by {@code WearLogSyncService} (Task 18),
 * not here.
 */
@RestController
@RequestMapping("/api/v1/wear-logs")
@RequiredArgsConstructor
public class WearLogController {

    private final WearLogMapper wearLogMapper;
    private final ClothingMapper clothingMapper;

    @PostMapping
    public Result<WearLog> create(@RequestBody Map<String, Object> body) {
        Long clothingId = ((Number) body.get("clothingId")).longValue();
        String wornAtStr = (String) body.get("wornAt");
        if (clothingMapper.selectById(clothingId) == null) {
            throw new ApiException(404, "clothing not found");
        }
        WearLog log = new WearLog();
        log.setClothingId(clothingId);
        log.setWornAt(LocalDate.parse(wornAtStr));
        wearLogMapper.insert(log);
        return Result.ok(log);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        wearLogMapper.deleteById(id);
        return Result.ok();
    }
}
