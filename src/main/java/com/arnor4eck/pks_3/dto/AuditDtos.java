package com.arnor4eck.pks_3.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;

public final class AuditDtos {
    private AuditDtos() {}
    public record Event(UUID id, UUID actorId, String actorLogin, String action, String entityType,
                        UUID entityId, Instant occurredAt, String correlationId, String description) {}
    public record Query(@Size(max = 200) String q, @Size(max = 40) String action,
                        @Size(max = 40) String entityType, UUID entityId, UUID actorId,
                        Instant from, Instant to, @Size(max = 64) String correlationId,
                        @Min(0) @Max(1000000) Integer page, @Min(1) @Max(100) Integer size,
                        String sort, String direction) {}
}
