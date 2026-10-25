package com.arnor4eck.pks_3.data.entity;

import jakarta.persistence.*;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "plots", uniqueConstraints = @UniqueConstraint(name = "uk_plot_location", columnNames = {"sector_id", "row_number", "plot_number"}), indexes = @Index(name = "idx_plots_status_sector", columnList = "sector_id,status"))
public class Plot implements com.arnor4eck.pks_3.data.entity.Entity<Plot> {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Setter(AccessLevel.NONE)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "archive_status", nullable = false, length = 16)
    private Status status = Status.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sector_id", nullable = false)
    private Sector sector;

    @Column(name = "row_number", nullable = false)
    private int rowNumber;

    @Column(name = "plot_number", nullable = false)
    private int plotNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PlotStatus plotStatus = PlotStatus.FREE;

    @Column(name = "length_cm", nullable = false)
    private int lengthCm = 200;

    @Column(name = "width_cm", nullable = false)
    private int widthCm = 100;

    @Column(length = 100)
    private String coordinates;

    @Override
    public void update(Plot other) {
        this.sector = other.sector;
        this.rowNumber = other.rowNumber;
        this.plotNumber = other.plotNumber;
        this.plotStatus = other.plotStatus;
        this.lengthCm = other.lengthCm;
        this.widthCm = other.widthCm;
        this.coordinates = other.coordinates;
    }
}

