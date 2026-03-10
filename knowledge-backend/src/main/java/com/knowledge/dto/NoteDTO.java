package com.knowledge.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class NoteDTO {

    private Long id;
    private String title;
    private String content;
    private String summary;
    private Long categoryId;
    private String categoryName;
    private List<TagDTO> tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
