package com.arnor4eck.pks_3.data.repository;

import com.arnor4eck.pks_3.data.entity.Sector;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface SectorRepository extends JpaRepository<Sector, UUID>, EntityRepository<Sector> {
}

