package com.arnor4eck.pks_3.domain;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor
@jakarta.persistence.Entity
@Table(name = "tags")
public class Tag {
    @Id private UUID id = UUID.randomUUID();
    @Column(nullable = false, unique = true, length = 50) private String name;
}
