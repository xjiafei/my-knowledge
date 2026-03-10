package com.knowledge.controller;

import com.knowledge.common.PageResult;
import com.knowledge.common.Result;
import com.knowledge.dto.FileDTO;
import com.knowledge.service.FileService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Validated
public class FileController {

    private final FileService fileService;

    /**
     * POST /api/files — Upload a file
     */
    @PostMapping
    public Result<FileDTO> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "tagIds", required = false) List<Long> tagIds
    ) throws IOException {
        FileDTO result = fileService.uploadFile(file, description, tagIds);
        return Result.success(result);
    }

    /**
     * GET /api/files — List files with optional filters
     */
    @GetMapping
    public Result<PageResult<FileDTO>> listFiles(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String fileCategory,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) String keyword
    ) {
        return Result.success(fileService.listFiles(page, size, fileCategory, tagId, keyword));
    }

    /**
     * GET /api/files/{id}/download — Download a file
     */
    @GetMapping("/{id}/download")
    public void downloadFile(@PathVariable Long id, HttpServletResponse response) throws IOException {
        fileService.downloadFile(id, response);
    }

    /**
     * DELETE /api/files/{id} — Delete a file
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteFile(@PathVariable Long id) {
        fileService.deleteFile(id);
        return Result.success();
    }
}
