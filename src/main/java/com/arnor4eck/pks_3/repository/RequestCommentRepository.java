package com.arnor4eck.pks_3.repository;
import com.arnor4eck.pks_3.domain.RequestComment;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface RequestCommentRepository extends JpaRepository<RequestComment, UUID> {
    Page<RequestComment> findByRequestId(UUID requestId, Pageable pageable);
}
