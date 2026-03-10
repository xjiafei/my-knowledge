package com.knowledge.controller;

import com.knowledge.common.Result;
import com.knowledge.dto.CategoryCreateRequest;
import com.knowledge.dto.CategoryDTO;
import com.knowledge.dto.CategoryUpdateRequest;
import com.knowledge.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public Result<List<CategoryDTO>> getTree() {
        return Result.success(categoryService.getTree());
    }

    @PostMapping
    public Result<CategoryDTO> createCategory(@Valid @RequestBody CategoryCreateRequest request) {
        return Result.success(categoryService.createCategory(request));
    }

    @PutMapping("/{id}")
    public Result<CategoryDTO> updateCategory(@PathVariable Long id,
                                               @Valid @RequestBody CategoryUpdateRequest request) {
        return Result.success(categoryService.updateCategory(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return Result.success();
    }
}
