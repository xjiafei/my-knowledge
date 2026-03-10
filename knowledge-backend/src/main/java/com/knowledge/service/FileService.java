package com.knowledge.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledge.common.BusinessException;
import com.knowledge.common.PageResult;
import com.knowledge.dto.FileDTO;
import com.knowledge.dto.TagDTO;
import com.knowledge.entity.KnowledgeFile;
import com.knowledge.mapper.FileTagMapper;
import com.knowledge.mapper.KnowledgeFileMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final KnowledgeFileMapper knowledgeFileMapper;
    private final FileTagMapper fileTagMapper;
    private final FileStorageService fileStorageService;

    @Value("${app.upload.dir}")
    private String uploadDir;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "jpg", "jpeg", "png", "gif", "webp", "txt", "md", "zip"
    );

    private static final Map<String, String> EXTENSION_MIME_MAP = Map.ofEntries(
            Map.entry("pdf",  "application/pdf"),
            Map.entry("doc",  "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("xls",  "application/vnd.ms-excel"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("ppt",  "application/vnd.ms-powerpoint"),
            Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            Map.entry("jpg",  "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("png",  "image/png"),
            Map.entry("gif",  "image/gif"),
            Map.entry("webp", "image/webp"),
            Map.entry("txt",  "text/plain"),
            Map.entry("md",   "text/markdown"),
            Map.entry("zip",  "application/zip")
    );

    private static final Map<String, String> EXT_TO_CATEGORY = Map.ofEntries(
            Map.entry("pdf",  "PDF"),
            Map.entry("doc",  "DOCUMENT"),
            Map.entry("docx", "DOCUMENT"),
            Map.entry("xls",  "DOCUMENT"),
            Map.entry("xlsx", "DOCUMENT"),
            Map.entry("ppt",  "DOCUMENT"),
            Map.entry("pptx", "DOCUMENT"),
            Map.entry("jpg",  "IMAGE"),
            Map.entry("jpeg", "IMAGE"),
            Map.entry("png",  "IMAGE"),
            Map.entry("gif",  "IMAGE"),
            Map.entry("webp", "IMAGE"),
            Map.entry("txt",  "TEXT"),
            Map.entry("md",   "TEXT"),
            Map.entry("zip",  "ARCHIVE")
    );

    private static final long MAX_FILE_SIZE = 100L * 1024 * 1024;
    private static final long MAX_DISK_USAGE = 10L * 1024 * 1024 * 1024;
    private static final int MAX_TAG_COUNT = 20;

    @Transactional
    public FileDTO uploadFile(MultipartFile file, String description, List<Long> tagIds) throws IOException {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("文件不能为空");
        }

        // 1. Extension validation
        String ext = validateAndGetExtension(file);

        // 2. File size validation
        if (file.getSize() > MAX_FILE_SIZE) {
            throw BusinessException.badRequest("文件大小不能超过 100MB");
        }

        // 3. Description length validation
        if (description != null && description.length() > 500) {
            throw BusinessException.badRequest("文件描述不能超过 500 字符");
        }

        // 4. Tag count validation
        if (tagIds != null && tagIds.size() > MAX_TAG_COUNT) {
            throw BusinessException.badRequest("标签数量不能超过 20 个");
        }

        // 5. Disk space check
        checkDiskSpace(file.getSize());

        // 6. Determine file category
        String fileCategory = EXT_TO_CATEGORY.getOrDefault(ext, "OTHER");

        // 7. Generate storage path
        String storagePath = fileStorageService.generatePath(ext);
        String storedName = Paths.get(storagePath).getFileName().toString();

        // 8. Save metadata (in transaction)
        KnowledgeFile entity = new KnowledgeFile();
        entity.setOriginalName(file.getOriginalFilename());
        entity.setStoredName(storedName);
        entity.setStoragePath(storagePath);
        entity.setMimeType(file.getContentType());
        entity.setFileSize(file.getSize());
        entity.setFileCategory(fileCategory);
        entity.setDescription(description);
        knowledgeFileMapper.insert(entity);

        // 9. Save tag associations (in transaction)
        if (tagIds != null && !tagIds.isEmpty()) {
            fileTagMapper.batchInsert(entity.getId(), tagIds);
        }

        // 10. Write to disk (still in @Transactional - IOException triggers rollback)
        fileStorageService.store(file, storagePath);

        log.info("File uploaded: id={}, name={}, size={}, category={}",
                entity.getId(), entity.getOriginalName(), entity.getFileSize(), fileCategory);

        List<TagDTO> tags = tagIds != null && !tagIds.isEmpty()
                ? fileTagMapper.selectTagsByFileId(entity.getId())
                : Collections.emptyList();
        return FileDTO.fromEntity(entity, tags);
    }

    public PageResult<FileDTO> listFiles(int page, int size, String fileCategory, Long tagId, String keyword) {
        if (keyword != null && keyword.length() > 100) {
            throw BusinessException.badRequest("搜索关键词不能超过 100 字符");
        }

        Page<KnowledgeFile> pageParam = new Page<>(page, size);
        IPage<KnowledgeFile> result = knowledgeFileMapper.selectFilePage(
                pageParam,
                (fileCategory != null && !fileCategory.isBlank()) ? fileCategory : null,
                (keyword != null && !keyword.isBlank()) ? keyword : null,
                tagId
        );

        List<FileDTO> records = result.getRecords().stream()
                .map(entity -> {
                    List<TagDTO> tags = fileTagMapper.selectTagsByFileId(entity.getId());
                    return FileDTO.fromEntity(entity, tags);
                })
                .toList();

        return new PageResult<>(records, result.getTotal(), page, size);
    }

    public FileDTO getFile(Long id) {
        KnowledgeFile entity = knowledgeFileMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("文件不存在: id=" + id);
        }
        List<TagDTO> tags = fileTagMapper.selectTagsByFileId(id);
        return FileDTO.fromEntity(entity, tags);
    }

    public void downloadFile(Long id, HttpServletResponse response) throws IOException {
        KnowledgeFile entity = knowledgeFileMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("文件不存在: id=" + id);
        }

        Path filePath = fileStorageService.resolve(entity.getStoragePath());
        if (!Files.exists(filePath)) {
            log.warn("磁盘文件丢失，元数据存在但物理文件不存在: id={}, path={}", id, entity.getStoragePath());
            throw BusinessException.notFound("文件已损坏或丢失: id=" + id);
        }

        String encoded = URLEncoder.encode(entity.getOriginalName(), StandardCharsets.UTF_8)
                .replace("+", "%20");

        response.setContentType(entity.getMimeType());
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
        response.setContentLengthLong(entity.getFileSize());

        try (OutputStream out = response.getOutputStream()) {
            Files.copy(filePath, out);
        }
    }

    @Transactional
    public void deleteFile(Long id) {
        KnowledgeFile entity = knowledgeFileMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("文件不存在: id=" + id);
        }
        // file_tag ON DELETE CASCADE handles associations automatically
        knowledgeFileMapper.deleteById(id);
        log.info("File metadata deleted: id={}", id);

        // Delete from disk after transaction commit
        // Note: This runs after the @Transactional method completes.
        // We use TransactionSynchronizationManager to run after commit.
        String storagePath = entity.getStoragePath();
        org.springframework.transaction.support.TransactionSynchronizationManager
                .registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        fileStorageService.delete(storagePath);
                        log.info("File deleted from disk after commit: path={}", storagePath);
                    }
                });
    }

    private String validateAndGetExtension(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw BusinessException.badRequest("不支持该文件格式");
        }

        String ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw BusinessException.badRequest("不支持该文件格式: " + ext);
        }

        // Content-Type validation
        String contentType = file.getContentType();
        if (contentType != null) {
            String expectedMime = EXTENSION_MIME_MAP.get(ext);
            if (expectedMime != null && !contentType.contains(expectedMime) && !contentType.startsWith(expectedMime)) {
                // Allow octet-stream as fallback (some clients send this)
                if (!"application/octet-stream".equals(contentType)) {
                    throw BusinessException.badRequest("文件类型与扩展名不匹配");
                }
            }
        }

        return ext;
    }

    private void checkDiskSpace(long incomingSize) {
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                return; // Directory doesn't exist yet, no usage
            }
            long usedBytes = Files.walk(uploadPath)
                    .filter(Files::isRegularFile)
                    .mapToLong(p -> p.toFile().length())
                    .sum();
            if (usedBytes + incomingSize > MAX_DISK_USAGE) {
                throw new BusinessException(507, "存储空间不足，请清理磁盘后重试");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            log.warn("磁盘空间检查失败，忽略检查: {}", e.getMessage());
        }
    }
}
