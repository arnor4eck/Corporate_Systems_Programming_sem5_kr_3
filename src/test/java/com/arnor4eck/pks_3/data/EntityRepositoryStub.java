package com.arnor4eck.pks_3.data;

import com.arnor4eck.pks_3.data.repository.EntityRepository;

import java.util.*;

import static java.util.Collections.singletonList;

public class EntityRepositoryStub implements EntityRepository<TestEntity> {

    private final Map<UUID, TestEntity> entities = new LinkedHashMap<>();

    @Override
    public <S extends TestEntity> S save(S entity) {
        entities.put(entity.getId(), entity);
        return entity;
    }

    @Override
    public <S extends TestEntity> Iterable<S> saveAll(Iterable<S> entities) {
        entities.forEach(this::save);
        return entities;
    }

    @Override
    public Optional<TestEntity> findById(UUID uuid) {
        return Optional.ofNullable(entities.get(uuid));
    }

    @Override
    public boolean existsById(UUID uuid) {
        return entities.containsKey(uuid);
    }

    @Override
    public Iterable<TestEntity> findAll() {
        return entities.values();
    }

    @Override
    public Iterable<TestEntity> findAllById(Iterable<UUID> uuids) {
        return singletonList(entities.get(uuids));
    }

    @Override
    public long count() {
        return entities.size();
    }

    @Override
    public void deleteById(UUID uuid) {
        entities.remove(uuid);
    }

    @Override
    public void delete(TestEntity entity) {
        entities.remove(entity.getId());
    }

    @Override
    public void deleteAllById(Iterable<? extends UUID> uuids) {
        uuids.forEach(this::deleteById);
    }

    @Override
    public void deleteAll(Iterable<? extends TestEntity> entities) {
        entities.forEach(this::delete);
    }

    @Override
    public void deleteAll() {
        entities.clear();
    }
}
