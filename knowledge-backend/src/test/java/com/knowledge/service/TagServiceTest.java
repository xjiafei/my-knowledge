package com.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.common.BusinessException;
import com.knowledge.dto.TagDTO;
import com.knowledge.dto.TagRequest;
import com.knowledge.entity.Tag;
import com.knowledge.mapper.TagMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock
    private TagMapper tagMapper;

    @InjectMocks
    private TagService tagService;

    private Tag testTag;
    private TagDTO testTagDTO;

    @BeforeEach
    void setUp() {
        testTag = new Tag();
        testTag.setId(1L);
        testTag.setName("Java");
        testTag.setCreatedAt(LocalDateTime.now());

        testTagDTO = new TagDTO();
        testTagDTO.setId(1L);
        testTagDTO.setName("Java");
        testTagDTO.setNoteCount(5);
        testTagDTO.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void listAll_returnsAllTagsOrderedByNoteCount() {
        List<TagDTO> tags = Arrays.asList(testTagDTO);
        when(tagMapper.selectAllWithNoteCount()).thenReturn(tags);

        List<TagDTO> result = tagService.listAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Java");
        verify(tagMapper).selectAllWithNoteCount();
    }

    @Test
    void createTag_withUniqueName_createsSuccessfully() {
        TagRequest request = new TagRequest();
        request.setName("Docker");

        when(tagMapper.selectCount(any())).thenReturn(0L);
        when(tagMapper.insert(any(Tag.class))).thenReturn(1);

        TagDTO result = tagService.createTag(request);

        assertThat(result).isNotNull();
        assertThat(result.getNoteCount()).isEqualTo(0);
        verify(tagMapper).insert(any(Tag.class));
    }

    @Test
    void createTag_withDuplicateName_throwsConflictException() {
        TagRequest request = new TagRequest();
        request.setName("Java");

        when(tagMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> tagService.createTag(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode()).isEqualTo(409));
    }

    @Test
    void updateTag_withNonExistentId_throwsNotFoundException() {
        when(tagMapper.selectById(999L)).thenReturn(null);

        TagRequest request = new TagRequest();
        request.setName("NewName");

        assertThatThrownBy(() -> tagService.updateTag(999L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode()).isEqualTo(404));
    }

    @Test
    void updateTag_withDuplicateName_throwsConflictException() {
        when(tagMapper.selectById(1L)).thenReturn(testTag);
        when(tagMapper.selectCount(any())).thenReturn(1L);

        TagRequest request = new TagRequest();
        request.setName("Python"); // different from current "Java"

        assertThatThrownBy(() -> tagService.updateTag(1L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode()).isEqualTo(409));
    }

    @Test
    void updateTag_withSameName_updatesSuccessfully() {
        when(tagMapper.selectById(1L)).thenReturn(testTag);
        when(tagMapper.updateById(any(Tag.class))).thenReturn(1);
        when(tagMapper.selectWithNoteCountById(1L)).thenReturn(testTagDTO);

        TagRequest request = new TagRequest();
        request.setName("Java"); // same name - no duplicate check triggered

        TagDTO result = tagService.updateTag(1L, request);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Java");
    }

    @Test
    void deleteTag_withNonExistentId_throwsNotFoundException() {
        when(tagMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> tagService.deleteTag(999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode()).isEqualTo(404));
    }

    @Test
    void deleteTag_withExistingId_deletesSuccessfully() {
        when(tagMapper.selectById(1L)).thenReturn(testTag);
        when(tagMapper.deleteById(1L)).thenReturn(1);

        assertThatCode(() -> tagService.deleteTag(1L)).doesNotThrowAnyException();
        verify(tagMapper).deleteById(1L);
    }
}
