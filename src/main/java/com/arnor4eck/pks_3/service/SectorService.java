package com.arnor4eck.pks_3.service;

import com.arnor4eck.pks_3.data.entity.Sector;
import com.arnor4eck.pks_3.data.entity.Status;
import com.arnor4eck.pks_3.data.repository.SectorRepository;
import com.arnor4eck.pks_3.dto.SectorInput;
import com.arnor4eck.pks_3.dto.SectorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SectorService {
    private final SectorRepository repository;

    public List<SectorResponse> getAll() {
        return repository.findAll().stream().map(this::response).toList();
    }

    public SectorResponse get(UUID id) {
        return response(find(id));
    }

    @Transactional
    public SectorResponse create(SectorInput input) {
        Sector entity = new Sector();
        apply(entity, input);
        return response(repository.saveAndFlush(entity));
    }

    @Transactional
    public SectorResponse update(UUID id, SectorInput input) {
        Sector entity = find(id);
        apply(entity, input);
        return response(repository.saveAndFlush(entity));
    }

    @Transactional
    public void archive(UUID id) {
        find(id).setStatus(Status.ARCHIVE);
        repository.flush();
    }

    @Transactional
    public SectorResponse restore(UUID id) {
        Sector entity = archived(id);
        entity.setStatus(Status.ACTIVE);
        repository.flush();
        return response(entity);
    }

    @Transactional
    public void deletePermanently(UUID id) {
        repository.delete(archived(id));
        repository.flush();
    }

    private Sector archived(UUID id) {
        Sector entity = find(id);
        if (entity.getStatus() != Status.ARCHIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Record must be archived first");
        }
        return entity;
    }

    private Sector find(UUID id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Sector not found: " + id));
    }

    private void apply(Sector entity, SectorInput input) {
        entity.setName(input.name());
    }

    private SectorResponse response(Sector e) {
        return new SectorResponse(e.getId(), e.getStatus(), e.getName(), e.getCreatedAt());
    }
}
