package com.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.common.BusinessException;
import com.knowledge.dto.CategoryCreateRequest;
import com.knowledge.dto.CategoryDTO;
import com.knowledge.dto.CategoryUpdateRequest;
import com.knowledge.entity.Category;
import com.knowledge.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryMapper categoryMapper;

    public List<CategoryDTO> getTree() {
        List<Category> all = categoryMapper.selectList(null);
        return buildTree(all);
    }

    public List<CategoryDTO> buildTree(List<Category> all) {
        // Build noteCount map
        Map<Long, Integer> noteCountMap = all.stream()
                .collect(Collectors.toMap(
                        Category::getId,
                        c -> categoryMapper.countNotesByCategory(c.getId()),
                        (a, b) -> a
                ));

        Map<Long, CategoryDTO> dtoMap = all.stream()
                .collect(Collectors.toMap(Category::getId, c -> toDTO(c, noteCountMap.getOrDefault(c.getId(), 0))));

        List<CategoryDTO> roots = new ArrayList<>();
        for (CategoryDTO dto : dtoMap.values()) {
            if (dto.getParentId() == null) {
                roots.add(dto);
            } else {
                CategoryDTO parent = dtoMap.get(dto.getParentId());
                if (parent != null) {
                    parent.getChildren().add(dto);
                }
            }
        }
        return roots;
    }

    @Transactional(rollbackFor = Exception.class)
    public CategoryDTO createCategory(CategoryCreateRequest request) {
        String name = request.getName().trim();

        if (request.getParentId() != null) {
            Category parent = categoryMapper.selectById(request.getParentId());
            if (parent == null) {
                throw BusinessException.notFound("父分类不存在: id=" + request.getParentId());
            }
            // Check depth: max 3 levels
            int depth = calculateDepth(request.getParentId());
            if (depth >= 3) {
                throw BusinessException.badRequest("分类层级不能超过3级");
            }
        }

        Category category = new Category();
        category.setName(name);
        category.setParentId(request.getParentId());
        categoryMapper.insert(category);

        log.info("Created category: id={}, name={}, parentId={}", category.getId(), category.getName(), category.getParentId());

        CategoryDTO dto = new CategoryDTO();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setParentId(category.getParentId());
        dto.setNoteCount(0);
        dto.setCreatedAt(category.getCreatedAt());
        return dto;
    }

    @Transactional(rollbackFor = Exception.class)
    public CategoryDTO updateCategory(Long id, CategoryUpdateRequest request) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw BusinessException.notFound("分类不存在: id=" + id);
        }

        category.setName(request.getName().trim());
        categoryMapper.updateById(category);

        log.info("Updated category: id={}, name={}", category.getId(), category.getName());

        CategoryDTO dto = new CategoryDTO();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setParentId(category.getParentId());
        dto.setNoteCount(categoryMapper.countNotesByCategory(id));
        dto.setCreatedAt(category.getCreatedAt());
        return dto;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw BusinessException.notFound("分类不存在: id=" + id);
        }

        // Recursively collect all descendant IDs including self
        List<Long> allIds = new ArrayList<>();
        collectAllDescendantIds(id, allIds);
        allIds.add(id);

        log.info("Deleting categories: ids={}", allIds);

        // Clear category_id on associated notes
        categoryMapper.clearNoteCategories(allIds);

        // Delete all categories (cascade won't handle children if FK is SET NULL, so delete manually)
        categoryMapper.delete(new LambdaQueryWrapper<Category>()
                .in(Category::getId, allIds));
    }

    private void collectAllDescendantIds(Long parentId, List<Long> result) {
        List<Long> childIds = categoryMapper.selectChildIds(parentId);
        for (Long childId : childIds) {
            result.add(childId);
            collectAllDescendantIds(childId, result);
        }
    }

    private int calculateDepth(Long categoryId) {
        int depth = 1;
        Category current = categoryMapper.selectById(categoryId);
        while (current != null && current.getParentId() != null) {
            depth++;
            current = categoryMapper.selectById(current.getParentId());
        }
        return depth;
    }

    private CategoryDTO toDTO(Category category, int noteCount) {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setParentId(category.getParentId());
        dto.setNoteCount(noteCount);
        dto.setCreatedAt(category.getCreatedAt());
        return dto;
    }
}
