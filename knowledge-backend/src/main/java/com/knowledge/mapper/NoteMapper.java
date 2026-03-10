package com.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledge.dto.NoteDTO;
import com.knowledge.entity.Note;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NoteMapper extends BaseMapper<Note> {

    IPage<NoteDTO> selectNotePage(Page<NoteDTO> page,
                                  @Param("tagId") Long tagId,
                                  @Param("categoryId") Long categoryId,
                                  @Param("sort") String sort,
                                  @Param("order") String order);

    IPage<NoteDTO> searchNotes(Page<NoteDTO> page,
                               @Param("keyword") String keyword);

    NoteDTO selectNoteDetail(@Param("id") Long id);
}
