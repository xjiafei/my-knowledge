package com.knowledge.controller;

import com.knowledge.common.PageResult;
import com.knowledge.common.Result;
import com.knowledge.dto.NoteCreateRequest;
import com.knowledge.dto.NoteDTO;
import com.knowledge.dto.NoteUpdateRequest;
import com.knowledge.service.NoteService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
@Validated
public class NoteController {

    private final NoteService noteService;

    /**
     * GET /api/notes — note list with optional filters and sorting.
     * This mapping is registered before /{id} so static segments take precedence.
     */
    @GetMapping
    public Result<PageResult<NoteDTO>> listNotes(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "updatedAt") String sort,
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) Long categoryId) {
        return Result.success(noteService.listNotes(page, size, sort, order, tagId, categoryId));
    }

    /**
     * GET /api/notes/search — full-text search.
     * Static path /search registered before /{id} path variable — no conflict.
     */
    @GetMapping("/search")
    public Result<PageResult<NoteDTO>> searchNotes(
            @RequestParam @Size(max = 200, message = "关键词不能超过200个字符") String q,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return Result.success(noteService.searchNotes(q, page, size));
    }

    @GetMapping("/{id}")
    public Result<NoteDTO> getNote(@PathVariable Long id) {
        return Result.success(noteService.getNote(id));
    }

    @PostMapping
    public Result<NoteDTO> createNote(@Valid @RequestBody NoteCreateRequest request) {
        return Result.success(noteService.createNote(request));
    }

    @PutMapping("/{id}")
    public Result<NoteDTO> updateNote(@PathVariable Long id,
                                       @Valid @RequestBody NoteUpdateRequest request) {
        return Result.success(noteService.updateNote(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteNote(@PathVariable Long id) {
        noteService.deleteNote(id);
        return Result.success();
    }
}
