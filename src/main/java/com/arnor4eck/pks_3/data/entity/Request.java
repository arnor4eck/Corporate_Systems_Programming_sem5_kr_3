package com.arnor4eck.pks_3.data.entity;

import jakarta.persistence.*;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "requests", indexes = {@Index(name = "idx_requests_status", columnList = "status"), @Index(name = "idx_requests_customer", columnList = "customer_id")})
public class Request implements com.arnor4eck.pks_3.data.entity.Entity<Request> {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Setter(AccessLevel.NONE)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "archive_status", nullable = false, length = 16)
    private Status status = Status.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plot_id", nullable = false)
    private Plot plot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Column(name = "deceased_full_name", length = 150)
    private String deceasedFullName;

    @Column(name = "deceased_birth_date")
    private LocalDate deceasedBirthday;

    @Column(name = "deceased_death_date")
    private LocalDate deceasedDeathday;

    @Column(name = "deceased_certificate", length = 100)
    private String deceasedCertificate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RequestStatus requestStatus = RequestStatus.NEW;

    @Column(name = "request_date", nullable = false)
    private LocalDateTime requestDate = LocalDateTime.now();

    @Column(name = "processed_date")
    private LocalDateTime processedDate;

    @Column(name = "total_cost", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalCost = BigDecimal.ZERO;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Override
    public void update(Request other) {
        this.customer = other.customer;
        this.plot = other.plot;
        this.employee = other.employee;
        this.deceasedFullName = other.deceasedFullName;
        this.deceasedBirthday = other.deceasedBirthday;
        this.deceasedDeathday = other.deceasedDeathday;
        this.deceasedCertificate = other.deceasedCertificate;
        this.requestStatus = other.requestStatus;
        this.requestDate = other.requestDate;
        this.processedDate = other.processedDate;
        this.totalCost = other.totalCost;
        this.note = other.note;
    }
}

