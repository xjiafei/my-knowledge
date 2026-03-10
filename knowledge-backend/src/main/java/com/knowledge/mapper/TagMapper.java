package com.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.dto.TagDTO;
import com.knowledge.entity.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TagMapper extends BaseMapper<Tag> {

    @Select("SELECT t.id, t.name, t.created_at, COUNT(nt.note_id) AS note_count " +
            "FROM tag t LEFT JOIN note_tag nt ON t.id = nt.tag_id " +
            "GROUP BY t.id, t.name, t.created_at " +
            "ORDER BY note_count DESC")
    List<TagDTO> selectAllWithNoteCount();

    @Select("SELECT t.id, t.name, t.created_at, COUNT(nt.note_id) AS note_count " +
            "FROM tag t LEFT JOIN note_tag nt ON t.id = nt.tag_id " +
            "WHERE t.id = #{tagId} " +
            "GROUP BY t.id, t.name, t.created_at")
    TagDTO selectWithNoteCountById(Long tagId);
}
