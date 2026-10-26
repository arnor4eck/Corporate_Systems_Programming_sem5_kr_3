package com.arnor4eck.pks_3.data.repository;

import com.arnor4eck.pks_3.data.entity.Plot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface PlotRepository extends JpaRepository<Plot, UUID>, EntityRepository<Plot> {
}

