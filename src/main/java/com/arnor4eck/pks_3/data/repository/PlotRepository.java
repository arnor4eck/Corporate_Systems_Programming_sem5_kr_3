package com.arnor4eck.pks_3.data.repository;

import com.arnor4eck.pks_3.data.entity.Plot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface PlotRepository extends JpaRepository<Plot, UUID>, EntityRepository<Plot> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Plot p where p.id = :id")
    Optional<Plot> findForUpdate(@Param("id") UUID id);
}

