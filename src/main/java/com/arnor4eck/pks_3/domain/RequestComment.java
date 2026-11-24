package com.arnor4eck.pks_3.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor
@jakarta.persistence.Entity
@Table(name = "request_comments")
public class RequestComment {
    @Id private UUID id = UUID.randomUUID();
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "request_id") private Request request;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "author_id") private Employee author;
    @Column(nullable = false, length = 4000) private String content;
    @Column(nullable = false) private boolean pinned;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false) private Instant updatedAt = Instant.now();
    @Version @Column(nullable = false) private Long version;
    @PreUpdate void touch() { updatedAt = Instant.now(); }
}
