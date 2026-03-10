package com.knowledge.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class CategoryDTO {

    private Long id;
    private String name;
    private Long parentId;
    private int noteCount;
    private LocalDateTime createdAt;
    private List<CategoryDTO> children = new ArrayList<>();
}
