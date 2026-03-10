package com.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CategoryMapper extends BaseMapper<Category> {

    @Select("SELECT id FROM category WHERE parent_id = #{parentId}")
    List<Long> selectChildIds(@Param("parentId") Long parentId);

    void clearNoteCategories(@Param("ids") List<Long> ids);

    @Select("SELECT COUNT(*) FROM note WHERE category_id = #{categoryId}")
    int countNotesByCategory(@Param("categoryId") Long categoryId);
}
