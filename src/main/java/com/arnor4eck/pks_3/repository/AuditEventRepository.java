package com.arnor4eck.pks_3.repository;

import com.arnor4eck.pks_3.domain.AuditEvent;
import org.springframework.data.jpa.repository.*;
import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID>, JpaSpecificationExecutor<AuditEvent> {}
