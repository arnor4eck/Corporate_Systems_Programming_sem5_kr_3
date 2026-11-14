package com.arnor4eck.pks_3.repository;

import com.arnor4eck.pks_3.domain.Plot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface PlotRepository extends JpaRepository<Plot, UUID>, EntityRepository<Plot> {
}

