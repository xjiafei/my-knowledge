package com.knowledge.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledge.common.BusinessException;
import com.knowledge.dto.NoteCreateRequest;
import com.knowledge.dto.NoteDTO;
import com.knowledge.dto.NoteUpdateRequest;
import com.knowledge.dto.TagDTO;
import com.knowledge.entity.Category;
import com.knowledge.entity.Note;
import com.knowledge.mapper.CategoryMapper;
import com.knowledge.mapper.NoteMapper;
import com.knowledge.mapper.NoteTagMapper;
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
class NoteServiceTest {

    @Mock
    private NoteMapper noteMapper;

    @Mock
    private NoteTagMapper noteTagMapper;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private NoteService noteService;

    private Note testNote;
    private NoteDTO testNoteDTO;

    @BeforeEach
    void setUp() {
        testNote = new Note();
        testNote.setId(1L);
        testNote.setTitle("Test Note");
        testNote.setContent("Test content");
        testNote.setSummary("Test content");
        testNote.setCreatedAt(LocalDateTime.now());
        testNote.setUpdatedAt(LocalDateTime.now());

        testNoteDTO = new NoteDTO();
        testNoteDTO.setId(1L);
        testNoteDTO.setTitle("Test Note");
        testNoteDTO.setSummary("Test content");
        testNoteDTO.setTags(Collections.emptyList());
        testNoteDTO.setCreatedAt(LocalDateTime.now());
        testNoteDTO.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void generateSummary_withShortContent_returnsFullContent() {
        String content = "Short content";
        String summary = noteService.generateSummary(content);
        assertThat(summary).isEqualTo(content);
    }

    @Test
    void generateSummary_withLongContent_returns100Chars() {
        String content = "A".repeat(200);
        String summary = noteService.generateSummary(content);
        assertThat(summary).hasSize(100);
    }

    @Test
    void generateSummary_withNull_returnsEmpty() {
        String summary = noteService.generateSummary(null);
        assertThat(summary).isEmpty();
    }

    @Test
    void generateSummary_withExactly100Chars_returnsFullContent() {
        String content = "B".repeat(100);
        String summary = noteService.generateSummary(content);
        assertThat(summary).isEqualTo(content);
    }

    @Test
    void createNote_withValidRequest_returnsNoteDTO() {
        NoteCreateRequest request = new NoteCreateRequest();
        request.setTitle("Test Note");
        request.setContent("Test content");
        request.setTagIds(Collections.emptyList());

        when(noteMapper.insert(any(Note.class))).thenReturn(1);
        when(noteMapper.selectNoteDetail(any())).thenReturn(testNoteDTO);
        when(noteTagMapper.selectTagsByNoteId(any())).thenReturn(Collections.emptyList());

        NoteDTO result = noteService.createNote(request);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Test Note");
        verify(noteMapper).insert(any(Note.class));
    }

    @Test
    void createNote_withNonExistentCategory_throwsBusinessException() {
        NoteCreateRequest request = new NoteCreateRequest();
        request.setTitle("Test Note");
        request.setCategoryId(999L);

        when(categoryMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> noteService.createNote(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("分类不存在");
    }

    @Test
    void getNote_withNonExistentId_throwsBusinessException() {
        when(noteMapper.selectNoteDetail(999L)).thenReturn(null);

        assertThatThrownBy(() -> noteService.getNote(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("笔记不存在");
    }

    @Test
    void getNote_withExistingId_returnsNoteDTO() {
        when(noteMapper.selectNoteDetail(1L)).thenReturn(testNoteDTO);
        when(noteTagMapper.selectTagsByNoteId(1L)).thenReturn(Collections.emptyList());

        NoteDTO result = noteService.getNote(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void updateNote_withNonExistentId_throwsBusinessException() {
        when(noteMapper.selectById(999L)).thenReturn(null);

        NoteUpdateRequest request = new NoteUpdateRequest();
        request.setTitle("Updated");

        assertThatThrownBy(() -> noteService.updateNote(999L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("笔记不存在");
    }

    @Test
    void deleteNote_withNonExistentId_throwsBusinessException() {
        when(noteMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> noteService.deleteNote(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("笔记不存在");
    }

    @Test
    void deleteNote_withExistingId_deletesSuccessfully() {
        when(noteMapper.selectById(1L)).thenReturn(testNote);
        when(noteMapper.deleteById(1L)).thenReturn(1);

        assertThatCode(() -> noteService.deleteNote(1L)).doesNotThrowAnyException();
        verify(noteMapper).deleteById(1L);
    }

    @Test
    void searchNotes_withEmptyKeyword_throwsBusinessException() {
        assertThatThrownBy(() -> noteService.searchNotes("", 1, 20))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("搜索关键词不能为空");
    }

    @Test
    void searchNotes_withKeywordTooLong_throwsBusinessException() {
        String longKeyword = "A".repeat(201);

        assertThatThrownBy(() -> noteService.searchNotes(longKeyword, 1, 20))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("搜索关键词不能超过200个字符");
    }

    @Test
    void searchNotes_withValidKeyword_returnsPageResult() {
        String keyword = "test";
        IPage<NoteDTO> mockPage = new Page<>(1, 20);
        ((Page<NoteDTO>) mockPage).setRecords(Collections.singletonList(testNoteDTO));
        ((Page<NoteDTO>) mockPage).setTotal(1);

        when(noteMapper.searchNotes(any(Page.class), eq(keyword))).thenReturn(mockPage);
        when(noteTagMapper.selectTagsByNoteId(any())).thenReturn(Collections.emptyList());

        var result = noteService.searchNotes(keyword, 1, 20);

        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getRecords()).hasSize(1);
    }
}
