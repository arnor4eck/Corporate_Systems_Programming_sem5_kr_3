package com.arnor4eck.pks_3.repository;
import com.arnor4eck.pks_3.domain.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface TagRepository extends JpaRepository<Tag, UUID> {}
