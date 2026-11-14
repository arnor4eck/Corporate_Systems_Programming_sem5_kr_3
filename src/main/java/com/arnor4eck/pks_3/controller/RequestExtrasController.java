package com.arnor4eck.pks_3.controller;

import com.arnor4eck.pks_3.dto.*;
import com.arnor4eck.pks_3.dto.RequestExtras.*;
import com.arnor4eck.pks_3.service.RequestExtrasService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController @RequiredArgsConstructor
@Tag(name = "Метки и служебные комментарии заявок")
public class RequestExtrasController {
    private final RequestExtrasService service;
    @GetMapping("/api/tags") @Operation(summary = "Список тематических меток")
    public PageResponse<TagResponse> tags(@RequestParam(defaultValue="0") @Min(0) int page, @RequestParam(defaultValue="20") @Min(1) @Max(100) int size) { return service.tags(page, size); }
    @PostMapping("/api/tags") @ResponseStatus(HttpStatus.CREATED) @Operation(summary = "Создать тематическую метку (MANAGER/ADMIN)")
    public TagResponse createTag(@Valid @RequestBody TagInput input) { return service.createTag(input); }
    @PostMapping("/api/requests/{id}/tags") @Operation(summary = "Добавить метку к заявке с проверкой version")
    public RequestResponse addTag(@PathVariable UUID id, @Valid @RequestBody TagChange input) { return service.changeTag(id, input, true); }
    @DeleteMapping("/api/requests/{id}/tags/{tagId}") @Operation(summary = "Удалить метку у заявки с проверкой version")
    public RequestResponse removeTag(@PathVariable UUID id, @PathVariable UUID tagId, @RequestParam @PositiveOrZero Long version) { return service.changeTag(id, new TagChange(tagId, version), false); }
    @GetMapping("/api/requests/{id}/notes") @Operation(summary = "История служебных комментариев к заявке")
    public PageResponse<Comment> comments(@PathVariable UUID id, @RequestParam(defaultValue="0") @Min(0) int page, @RequestParam(defaultValue="20") @Min(1) @Max(100) int size) { return service.comments(id, page, size); }
    @PostMapping("/api/requests/{id}/notes") @ResponseStatus(HttpStatus.CREATED) @Operation(summary = "Создать комментарий от текущего сотрудника")
    public Comment create(@PathVariable UUID id, @Valid @RequestBody CommentInput input) { return service.createComment(id, input); }
    @PutMapping("/api/requests/{id}/notes/{noteId}") @Operation(summary = "Изменить или закрепить свой комментарий; ADMIN может изменить любой")
    public Comment update(@PathVariable UUID id, @PathVariable UUID noteId, @Valid @RequestBody CommentInput input) { return service.updateComment(id, noteId, input); }
    @DeleteMapping("/api/requests/{id}/notes/{noteId}") @ResponseStatus(HttpStatus.NO_CONTENT) @Operation(summary = "Удалить комментарий с проверкой version")
    public void delete(@PathVariable UUID id, @PathVariable UUID noteId, @RequestParam @PositiveOrZero Long version) { service.deleteComment(id, noteId, version); }
}
