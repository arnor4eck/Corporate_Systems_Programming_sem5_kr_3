package com.arnor4eck.pks_3.repository;

import com.arnor4eck.pks_3.domain.Entity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.UUID;

@NoRepositoryBean
public interface EntityRepository<T extends Entity<T>> extends CrudRepository<T, UUID> {
}
