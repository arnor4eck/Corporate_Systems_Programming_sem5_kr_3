package com.arnor4eck.pks_3.domain;

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
public class Request implements com.arnor4eck.pks_3.domain.Entity<Request> {
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

    @Column(nullable = false, unique = true, length = 50)
    private String code = "REQ-" + UUID.randomUUID();

    @Column(nullable = false, length = 150)
    private String title = "Заявка на захоронение или бронирование";

    @Column(nullable = false)
    private boolean urgent;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sector_id", nullable = false)
    private Sector sector;

    @ManyToMany
    @JoinTable(name = "request_tags", joinColumns = @JoinColumn(name = "request_id"), inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private java.util.Set<Tag> tags = new java.util.HashSet<>();

    @Transient
    public Priority getPriority() {
        if (requestStatus == RequestStatus.COMPLETED || requestStatus == RequestStatus.REJECTED) return Priority.NORMAL;
        return urgent ? Priority.URGENT : requestStatus == RequestStatus.PROCESSING ? Priority.HIGH : Priority.NORMAL;
    }

    @PrePersist
    void initializeSector() { if (sector == null && plot != null) sector = plot.getSector(); }

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "updated_at", nullable = false)
    private java.time.LocalDateTime updatedAt = java.time.LocalDateTime.now();

    @PreUpdate
    void touch() { updatedAt = java.time.LocalDateTime.now(); }

    @Override
    public void update(Request other) {
        this.code = other.code;
        this.title = other.title;
        this.urgent = other.urgent;
        this.sector = other.sector;
        this.tags = new java.util.HashSet<>(other.tags);
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

