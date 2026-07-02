package com.closet.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.closet.entity.Category;
import com.closet.mapper.CategoryMapper;
import com.closet.service.CategoryService;
import org.springframework.stereotype.Service;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {
}
