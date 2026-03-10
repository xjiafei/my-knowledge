package com.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("knowledge_file")
public class KnowledgeFile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String originalName;

    private String storedName;

    private String storagePath;

    private String mimeType;

    private Long fileSize;

    private String fileCategory;

    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
