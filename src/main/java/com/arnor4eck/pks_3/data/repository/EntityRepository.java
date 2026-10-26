package com.arnor4eck.pks_3.data.repository;

import com.arnor4eck.pks_3.data.entity.Entity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.UUID;

@NoRepositoryBean
public interface EntityRepository<T extends Entity<T>> extends CrudRepository<T, UUID> {
}
