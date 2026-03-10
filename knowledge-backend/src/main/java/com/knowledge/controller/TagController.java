package com.knowledge.controller;

import com.knowledge.common.Result;
import com.knowledge.dto.TagDTO;
import com.knowledge.dto.TagRequest;
import com.knowledge.service.TagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @GetMapping
    public Result<List<TagDTO>> listTags() {
        return Result.success(tagService.listAll());
    }

    @PostMapping
    public Result<TagDTO> createTag(@Valid @RequestBody TagRequest request) {
        return Result.success(tagService.createTag(request));
    }

    @PutMapping("/{id}")
    public Result<TagDTO> updateTag(@PathVariable Long id, @Valid @RequestBody TagRequest request) {
        return Result.success(tagService.updateTag(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteTag(@PathVariable Long id) {
        tagService.deleteTag(id);
        return Result.success();
    }
}
