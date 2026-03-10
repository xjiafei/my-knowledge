package com.knowledge.service;

import com.knowledge.common.BusinessException;
import com.knowledge.dto.CategoryCreateRequest;
import com.knowledge.dto.CategoryDTO;
import com.knowledge.dto.CategoryUpdateRequest;
import com.knowledge.entity.Category;
import com.knowledge.mapper.CategoryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryService categoryService;

    private Category rootCategory;
    private Category childCategory;
    private Category grandChildCategory;

    @BeforeEach
    void setUp() {
        rootCategory = new Category();
        rootCategory.setId(1L);
        rootCategory.setName("技术");
        rootCategory.setParentId(null);
        rootCategory.setCreatedAt(LocalDateTime.now());

        childCategory = new Category();
        childCategory.setId(2L);
        childCategory.setName("前端");
        childCategory.setParentId(1L);
        childCategory.setCreatedAt(LocalDateTime.now());

        grandChildCategory = new Category();
        grandChildCategory.setId(3L);
        grandChildCategory.setName("Vue");
        grandChildCategory.setParentId(2L);
        grandChildCategory.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void buildTree_withFlatList_buildsCorrectHierarchy() {
        when(categoryMapper.countNotesByCategory(1L)).thenReturn(2);
        when(categoryMapper.countNotesByCategory(2L)).thenReturn(3);

        List<Category> all = Arrays.asList(rootCategory, childCategory);
        List<CategoryDTO> tree = categoryService.buildTree(all);

        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).getId()).isEqualTo(1L);
        assertThat(tree.get(0).getChildren()).hasSize(1);
        assertThat(tree.get(0).getChildren().get(0).getId()).isEqualTo(2L);
    }

    @Test
    void buildTree_withEmptyList_returnsEmptyList() {
        List<CategoryDTO> tree = categoryService.buildTree(Collections.emptyList());
        assertThat(tree).isEmpty();
    }

    @Test
    void createCategory_withValidTopLevel_createsSuccessfully() {
        CategoryCreateRequest request = new CategoryCreateRequest();
        request.setName("后端");
        request.setParentId(null);

        when(categoryMapper.insert(any(Category.class))).thenReturn(1);

        CategoryDTO result = categoryService.createCategory(request);

        assertThat(result).isNotNull();
        assertThat(result.getParentId()).isNull();
        verify(categoryMapper).insert(any(Category.class));
    }

    @Test
    void createCategory_withNonExistentParent_throwsNotFoundException() {
        CategoryCreateRequest request = new CategoryCreateRequest();
        request.setName("子分类");
        request.setParentId(999L);

        when(categoryMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> categoryService.createCategory(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode()).isEqualTo(404));
    }

    @Test
    void createCategory_exceedingMaxDepth_throwsBadRequestException() {
        CategoryCreateRequest request = new CategoryCreateRequest();
        request.setName("Level 4");
        request.setParentId(3L);

        // grandChildCategory is at depth 3 (root -> child -> grandChild)
        when(categoryMapper.selectById(3L)).thenReturn(grandChildCategory);
        when(categoryMapper.selectById(2L)).thenReturn(childCategory);
        when(categoryMapper.selectById(1L)).thenReturn(rootCategory);

        assertThatThrownBy(() -> categoryService.createCategory(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode()).isEqualTo(400))
                .hasMessageContaining("3级");
    }

    @Test
    void updateCategory_withNonExistentId_throwsNotFoundException() {
        when(categoryMapper.selectById(999L)).thenReturn(null);

        CategoryUpdateRequest request = new CategoryUpdateRequest();
        request.setName("New Name");

        assertThatThrownBy(() -> categoryService.updateCategory(999L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode()).isEqualTo(404));
    }

    @Test
    void updateCategory_withValidRequest_updatesName() {
        when(categoryMapper.selectById(1L)).thenReturn(rootCategory);
        when(categoryMapper.updateById(any(Category.class))).thenReturn(1);
        when(categoryMapper.countNotesByCategory(1L)).thenReturn(0);

        CategoryUpdateRequest request = new CategoryUpdateRequest();
        request.setName("编程技术");

        CategoryDTO result = categoryService.updateCategory(1L, request);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("编程技术");
    }

    @Test
    void deleteCategory_withNonExistentId_throwsNotFoundException() {
        when(categoryMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> categoryService.deleteCategory(999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode()).isEqualTo(404));
    }

    @Test
    void deleteCategory_withChildCategories_deletesAllCascade() {
        when(categoryMapper.selectById(1L)).thenReturn(rootCategory);
        when(categoryMapper.selectChildIds(1L)).thenReturn(Arrays.asList(2L));
        when(categoryMapper.selectChildIds(2L)).thenReturn(Collections.emptyList());
        doNothing().when(categoryMapper).clearNoteCategories(anyList());
        when(categoryMapper.delete(any())).thenReturn(3);

        assertThatCode(() -> categoryService.deleteCategory(1L)).doesNotThrowAnyException();

        verify(categoryMapper).clearNoteCategories(anyList());
        verify(categoryMapper).delete(any());
    }
}
