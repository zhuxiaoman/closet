package com.closet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.closet.entity.CalendarEntry;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CalendarEntryMapper extends BaseMapper<CalendarEntry> {
}
