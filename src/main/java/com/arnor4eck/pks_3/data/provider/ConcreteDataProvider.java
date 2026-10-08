package com.arnor4eck.pks_3.data.provider;

import com.arnor4eck.pks_3.data.entity.Entity;
import com.arnor4eck.pks_3.exception.entity.NotArchivedException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.repository.CrudRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.arnor4eck.pks_3.data.entity.Status.ACTIVE;
import static com.arnor4eck.pks_3.data.entity.Status.ARCHIVE;

public class ConcreteDataProvider<T extends Entity> implements DataProvider<T> {

    private final CrudRepository<T, UUID> repository;

    public ConcreteDataProvider(
            CrudRepository<T, UUID> repository
    ) {
        this.repository = repository;
    }

    @Override
    public Collection<T> getAll() {
        return StreamSupport.stream(repository.findAll().spliterator(), false)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<T> get(UUID id) {
        return repository.findById(id);
    }

    @Override
    public void save(T val) {
        repository.save(val);
    }

    @Override
    public void update(T val) {
        T found = findOrThrow(val.getId());

        found.update(val);

        this.save(found);
    }

    private T findOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(EntityNotFoundException::new);
    }

    @Override
    public void archive(UUID id) {
        T found = findOrThrow(id);

        found.setStatus(ARCHIVE);

        repository.save(found);
    }

    @Override
    public void restore(UUID id) {
        T found = findArchivedOrThrow(id);

        found.setStatus(ACTIVE);
        repository.save(found);
    }

    private T findArchivedOrThrow(UUID id) {
        T found = findOrThrow(id);

        if (found.getStatus() != ARCHIVE) {
            throw new NotArchivedException("Сущность с id %s не в архиве.".formatted(found.getId()));
        }

        return found;
    }

    @Override
    public void delete(UUID id) {
        T found = findArchivedOrThrow(id);

        repository.delete(found);
    }
}
