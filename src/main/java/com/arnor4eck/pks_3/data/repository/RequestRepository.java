package com.arnor4eck.pks_3.data.repository;

import com.arnor4eck.pks_3.data.entity.Request;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;
import java.util.Optional;
import com.arnor4eck.pks_3.data.entity.Status;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface RequestRepository extends JpaRepository<Request, UUID>, EntityRepository<Request> {
    List<Request> findAllByPlotIdAndStatus(UUID plotId, Status status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Request r where r.id = :id")
    Optional<Request> findForUpdate(@Param("id") UUID id);
}

