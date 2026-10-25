package com.arnor4eck.pks_3.data.entity;

import jakarta.persistence.*;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "employees")
public class Employee implements com.arnor4eck.pks_3.data.entity.Entity<Employee> {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Setter(AccessLevel.NONE)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "archive_status", nullable = false, length = 16)
    private Status status = Status.ACTIVE;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.MANAGER;

    @Column(nullable = false, unique = true, length = 50)
    private String login;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Override
    public void update(Employee other) {
        this.fullName = other.fullName;
        this.role = other.role;
        this.login = other.login;
        this.passwordHash = other.passwordHash;
        this.active = other.active;
    }
}

