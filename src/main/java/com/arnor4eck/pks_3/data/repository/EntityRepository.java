package com.arnor4eck.pks_3.data.repository;

import com.arnor4eck.pks_3.data.entity.Entity;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface EntityRepository<T extends Entity> extends CrudRepository<T, UUID> {
}
