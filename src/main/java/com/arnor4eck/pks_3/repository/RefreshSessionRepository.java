package com.arnor4eck.pks_3.repository;

import com.arnor4eck.pks_3.domain.RefreshSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface RefreshSessionRepository extends JpaRepository<RefreshSession, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from RefreshSession s where s.tokenHash = :hash")
    Optional<RefreshSession> lockByTokenHash(@Param("hash") String hash);

    @Modifying
    @Query("update RefreshSession s set s.revoked = true where s.employee.id = :id")
    void revokeAll(@Param("id") UUID id);
}
