package com.arnor4eck.pks_3.service;

import com.arnor4eck.pks_3.data.entity.Entity;
import com.arnor4eck.pks_3.data.entity.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.UUID;

@Transactional(readOnly = true)
public abstract class CrudService<E extends Entity<E>, I, O> {
    protected final JpaRepository<E, UUID> repository;

    protected CrudService(JpaRepository<E, UUID> repository) {
        this.repository = repository;
    }

    protected abstract E newEntity();
    protected abstract void apply(E entity, I input);
    protected abstract O response(E entity);

    public List<O> getAll() {
        return repository.findAll().stream().map(this::response).toList();
    }

    public O get(UUID id) {
        return response(require(repository, id));
    }

    @Transactional
    public O create(I input) {
        E entity = newEntity();
        apply(entity, input);
        return response(repository.saveAndFlush(entity));
    }

    @Transactional
    public O update(UUID id, I input) {
        E entity = require(repository, id);
        apply(entity, input);
        return response(repository.saveAndFlush(entity));
    }

    @Transactional
    public void archive(UUID id) {
        require(repository, id).setStatus(Status.ARCHIVE);
        repository.flush();
    }

    @Transactional
    public O restore(UUID id) {
        E entity = archived(id);
        entity.setStatus(Status.ACTIVE);
        repository.flush();
        return response(entity);
    }

    @Transactional
    public void deletePermanently(UUID id) {
        repository.delete(archived(id));
        repository.flush();
    }

    private E archived(UUID id) {
        E entity = require(repository, id);
        if (entity.getStatus() != Status.ARCHIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Record must be archived first");
        }
        return entity;
    }

    protected static <T> T require(JpaRepository<T, UUID> repository, UUID id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found: " + id));
    }
}
