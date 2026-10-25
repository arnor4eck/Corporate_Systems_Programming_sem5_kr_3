package com.arnor4eck.pks_3.data.repository;

import com.arnor4eck.pks_3.data.entity.Request;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface RequestRepository extends JpaRepository<Request, UUID>, EntityRepository<Request> {
}

