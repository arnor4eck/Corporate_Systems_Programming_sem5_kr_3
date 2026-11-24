package com.arnor4eck.pks_3.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@jakarta.persistence.Entity
@Table(name = "audit_events")
public class AuditEvent {
    @Id private UUID id = UUID.randomUUID();
    @Column(name = "actor_id") private UUID actorId;
    @Column(name = "actor_login", nullable = false, length = 50) private String actorLogin;
    @Column(nullable = false, length = 40) private String action;
    @Column(name = "entity_type", nullable = false, length = 40) private String entityType;
    @Column(name = "entity_id") private UUID entityId;
    @Column(name = "occurred_at", nullable = false) private Instant occurredAt = Instant.now();
    @Column(name = "correlation_id", nullable = false, length = 64) private String correlationId;
    @Column(nullable = false, length = 300) private String description;
}
