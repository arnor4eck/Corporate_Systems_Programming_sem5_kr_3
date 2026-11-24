package com.arnor4eck.pks_3.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;

public final class RequestExtras {
    private RequestExtras() {}
    public record TagInput(@NotBlank @Size(max = 50) String name) {}
    public record TagChange(@NotNull UUID tagId, @NotNull @PositiveOrZero Long version) {}
    public record CommentInput(@NotBlank @Size(max = 4000) String content, Boolean pinned, @PositiveOrZero Long version) {}
    public record Comment(UUID id, UUID requestId, UUID authorId, String authorName, String content, boolean pinned,
                          Instant createdAt, Instant updatedAt, Long version) {}
    public record Enabled(@NotNull Boolean enabled, @NotNull @PositiveOrZero Long version) {}
}
