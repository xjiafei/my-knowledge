package com.knowledge.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    @Value("${app.upload.dir}")
    private String uploadDir;

    /**
     * Generate a relative storage path in the format: yyyy/MM/{uuid}.{ext}
     */
    public String generatePath(String ext) {
        String safeExt = ext.replaceAll("[^a-zA-Z0-9]", "");
        String yearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return yearMonth + "/" + uuid + "." + safeExt.toLowerCase();
    }

    /**
     * Store a file to disk at the given relative path.
     */
    public void store(MultipartFile file, String relativePath) throws IOException {
        Path target = resolveAndValidate(relativePath);
        Files.createDirectories(target.getParent());
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Resolve a relative path to an absolute Path.
     */
    public Path resolve(String relativePath) {
        return resolveAndValidate(relativePath);
    }

    /**
     * Delete a file from disk. Logs a warning if deletion fails.
     */
    public boolean delete(String relativePath) {
        try {
            return Files.deleteIfExists(resolveAndValidate(relativePath));
        } catch (IOException e) {
            log.warn("删除磁盘文件失败: {}", relativePath, e);
            return false;
        }
    }

    private Path resolveAndValidate(String relativePath) {
        Path base = Paths.get(uploadDir).normalize().toAbsolutePath();
        Path target = base.resolve(relativePath).normalize();
        if (!target.startsWith(base)) {
            throw new com.knowledge.common.BusinessException(400, "非法文件路径");
        }
        return target;
    }
}
