package com.closet.controller;

import com.closet.common.ApiException;
import com.closet.common.Result;
import com.closet.dto.CategoryRequest;
import com.closet.entity.Category;
import com.closet.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService service;

    @GetMapping
    public Result<List<Category>> list() {
        return Result.ok(service.list());
    }

    @PostMapping
    public Result<Category> create(@Valid @RequestBody CategoryRequest req) {
        Category c = new Category();
        c.setName(req.getName());
        c.setParentId(req.getParentId());
        c.setSortOrder(req.getSortOrder() == null ? 0 : req.getSortOrder());
        service.save(c);
        return Result.ok(c);
    }

    @PutMapping("/{id}")
    public Result<Category> update(@PathVariable Long id, @Valid @RequestBody CategoryRequest req) {
        Category exist = service.getById(id);
        if (exist == null) {
            throw new ApiException(404, "category not found");
        }
        exist.setName(req.getName());
        exist.setParentId(req.getParentId());
        exist.setSortOrder(req.getSortOrder() == null ? exist.getSortOrder() : req.getSortOrder());
        service.updateById(exist);
        return Result.ok(exist);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.removeById(id);
        return Result.ok();
    }
}
