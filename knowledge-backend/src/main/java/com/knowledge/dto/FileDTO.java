package com.knowledge.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.knowledge.entity.KnowledgeFile;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class FileDTO {

    private Long id;
    private String originalName;
    private String mimeType;
    private Long fileSize;
    private String fileSizeDisplay;
    private String fileCategory;
    private String description;
    private List<TagDTO> tags;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    public static FileDTO fromEntity(KnowledgeFile entity, List<TagDTO> tags) {
        FileDTO dto = new FileDTO();
        dto.setId(entity.getId());
        dto.setOriginalName(entity.getOriginalName());
        dto.setMimeType(entity.getMimeType());
        dto.setFileSize(entity.getFileSize());
        dto.setFileSizeDisplay(formatFileSize(entity.getFileSize()));
        dto.setFileCategory(entity.getFileCategory());
        dto.setDescription(entity.getDescription());
        dto.setTags(tags);
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    private static String formatFileSize(long size) {
        if (size < 1024L * 1024L) {
            long kb = (size + 1023) / 1024;
            return kb + " KB";
        } else {
            double mb = size / (1024.0 * 1024.0);
            return String.format("%.1f MB", mb);
        }
    }
}
