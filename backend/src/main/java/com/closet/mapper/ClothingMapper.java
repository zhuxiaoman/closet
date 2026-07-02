package com.closet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.closet.entity.Clothing;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ClothingMapper extends BaseMapper<Clothing> {
}
