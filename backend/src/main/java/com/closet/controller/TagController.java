package com.closet.controller;

import com.closet.common.ApiException;
import com.closet.common.Result;
import com.closet.dto.TagRequest;
import com.closet.entity.Tag;
import com.closet.service.TagService;
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
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService service;

    @GetMapping
    public Result<List<Tag>> list() {
        return Result.ok(service.list());
    }

    @PostMapping
    public Result<Tag> create(@Valid @RequestBody TagRequest req) {
        Tag t = new Tag();
        t.setName(req.getName());
        service.save(t);
        return Result.ok(t);
    }

    @PutMapping("/{id}")
    public Result<Tag> update(@PathVariable Long id, @Valid @RequestBody TagRequest req) {
        Tag exist = service.getById(id);
        if (exist == null) {
            throw new ApiException(404, "tag not found");
        }
        exist.setName(req.getName());
        service.updateById(exist);
        return Result.ok(exist);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.removeById(id);
        return Result.ok();
    }
}
