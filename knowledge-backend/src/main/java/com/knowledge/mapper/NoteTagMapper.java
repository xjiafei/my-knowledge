package com.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.dto.TagDTO;
import com.knowledge.entity.NoteTag;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface NoteTagMapper extends BaseMapper<NoteTag> {

    @Delete("DELETE FROM note_tag WHERE note_id = #{noteId}")
    int deleteByNoteId(@Param("noteId") Long noteId);

    @Select("SELECT t.id, t.name, t.created_at, 0 AS note_count " +
            "FROM tag t INNER JOIN note_tag nt ON t.id = nt.tag_id " +
            "WHERE nt.note_id = #{noteId}")
    List<TagDTO> selectTagsByNoteId(@Param("noteId") Long noteId);
}
