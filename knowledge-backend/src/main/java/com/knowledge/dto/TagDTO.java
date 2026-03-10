package com.knowledge.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TagDTO {

    private Long id;
    private String name;
    private int noteCount;
    private LocalDateTime createdAt;
}
