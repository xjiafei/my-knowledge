package com.knowledge.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledge.common.BusinessException;
import com.knowledge.common.PageResult;
import com.knowledge.dto.NoteCreateRequest;
import com.knowledge.dto.NoteDTO;
import com.knowledge.dto.NoteUpdateRequest;
import com.knowledge.dto.TagDTO;
import com.knowledge.entity.Category;
import com.knowledge.entity.Note;
import com.knowledge.entity.NoteTag;
import com.knowledge.mapper.CategoryMapper;
import com.knowledge.mapper.NoteMapper;
import com.knowledge.mapper.NoteTagMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteMapper noteMapper;
    private final NoteTagMapper noteTagMapper;
    private final CategoryMapper categoryMapper;

    public PageResult<NoteDTO> listNotes(int page, int size, String sort, String order,
                                         Long tagId, Long categoryId) {
        // Validate sort field
        if (!isValidSortField(sort)) {
            sort = "updatedAt";
        }
        if (!isValidOrder(order)) {
            order = "desc";
        }

        Page<NoteDTO> pageParam = new Page<>(page, size);
        IPage<NoteDTO> result = noteMapper.selectNotePage(pageParam, tagId, categoryId, sort, order);

        // Load tags for each note in the list
        List<NoteDTO> records = result.getRecords();
        for (NoteDTO note : records) {
            List<TagDTO> tags = noteTagMapper.selectTagsByNoteId(note.getId());
            note.setTags(tags);
        }

        return new PageResult<>(records, result.getTotal(), page, size);
    }

    @Transactional
    public NoteDTO createNote(NoteCreateRequest request) {
        if (request.getCategoryId() != null) {
            Category category = categoryMapper.selectById(request.getCategoryId());
            if (category == null) {
                throw BusinessException.notFound("分类不存在: id=" + request.getCategoryId());
            }
        }

        Note note = new Note();
        note.setTitle(request.getTitle().trim());
        note.setContent(request.getContent());
        note.setSummary(generateSummary(request.getContent()));
        note.setCategoryId(request.getCategoryId());
        noteMapper.insert(note);

        // Save tag associations
        saveNoteTags(note.getId(), request.getTagIds());

        log.info("Created note: id={}, title={}", note.getId(), note.getTitle());
        return getNoteDetail(note.getId());
    }

    public NoteDTO getNote(Long id) {
        NoteDTO note = getNoteDetail(id);
        if (note == null) {
            throw BusinessException.notFound("笔记不存在: id=" + id);
        }
        return note;
    }

    @Transactional
    public NoteDTO updateNote(Long id, NoteUpdateRequest request) {
        Note note = noteMapper.selectById(id);
        if (note == null) {
            throw BusinessException.notFound("笔记不存在: id=" + id);
        }

        if (request.getCategoryId() != null) {
            Category category = categoryMapper.selectById(request.getCategoryId());
            if (category == null) {
                throw BusinessException.notFound("分类不存在: id=" + request.getCategoryId());
            }
        }

        note.setTitle(request.getTitle().trim());
        note.setContent(request.getContent());
        note.setSummary(generateSummary(request.getContent()));
        note.setCategoryId(request.getCategoryId());
        noteMapper.updateById(note);

        // Update tag associations
        noteTagMapper.deleteByNoteId(id);
        saveNoteTags(id, request.getTagIds());

        log.info("Updated note: id={}, title={}", note.getId(), note.getTitle());
        return getNoteDetail(id);
    }

    @Transactional
    public void deleteNote(Long id) {
        Note note = noteMapper.selectById(id);
        if (note == null) {
            throw BusinessException.notFound("笔记不存在: id=" + id);
        }
        // note_tag ON DELETE CASCADE handles the associations
        noteMapper.deleteById(id);
        log.info("Deleted note: id={}", id);
    }

    public PageResult<NoteDTO> searchNotes(String keyword, int page, int size) {
        if (!StringUtils.hasText(keyword)) {
            throw BusinessException.badRequest("搜索关键词不能为空");
        }
        if (keyword.length() > 200) {
            throw BusinessException.badRequest("搜索关键词不能超过200个字符");
        }

        Page<NoteDTO> pageParam = new Page<>(page, size);
        IPage<NoteDTO> result = noteMapper.searchNotes(pageParam, keyword);

        List<NoteDTO> records = result.getRecords();
        for (NoteDTO note : records) {
            List<TagDTO> tags = noteTagMapper.selectTagsByNoteId(note.getId());
            note.setTags(tags);
        }

        return new PageResult<>(records, result.getTotal(), page, size);
    }

    private NoteDTO getNoteDetail(Long id) {
        NoteDTO note = noteMapper.selectNoteDetail(id);
        if (note != null) {
            List<TagDTO> tags = noteTagMapper.selectTagsByNoteId(id);
            note.setTags(tags);
        }
        return note;
    }

    private void saveNoteTags(Long noteId, List<Long> tagIds) {
        if (CollectionUtils.isEmpty(tagIds)) {
            return;
        }
        List<NoteTag> noteTags = new ArrayList<>();
        for (Long tagId : tagIds) {
            noteTags.add(new NoteTag(noteId, tagId));
        }
        for (NoteTag noteTag : noteTags) {
            noteTagMapper.insert(noteTag);
        }
    }

    String generateSummary(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        return content.length() <= 100 ? content : content.substring(0, 100);
    }

    private boolean isValidSortField(String sort) {
        return "updatedAt".equals(sort) || "createdAt".equals(sort) || "title".equals(sort);
    }

    private boolean isValidOrder(String order) {
        return "asc".equals(order) || "desc".equals(order);
    }
}
