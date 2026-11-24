package com.arnor4eck.pks_3.service;

import com.arnor4eck.pks_3.domain.Sector;
import com.arnor4eck.pks_3.domain.Status;
import com.arnor4eck.pks_3.repository.SectorRepository;
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
    private final AuditService audit;
    private final SectorRepository repository;

    @Transactional
    public SectorResponse enabled(UUID id, com.arnor4eck.pks_3.dto.RequestExtras.Enabled input) {
        var entity = find(id); Versions.require(entity.getVersion(), input.version());
        entity.setEnabled(input.enabled()); repository.flush();
        audit.record(input.enabled() ? "ENABLE" : "DISABLE", "sectors", id, "Sector availability changed");
        return response(entity);
    }

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
        repository.saveAndFlush(entity);
        audit.record("CREATE", "sectors", entity.getId(), "CREATE Sector");
        return response(entity);
    }

    @Transactional
    public SectorResponse update(UUID id, SectorInput input) {
        Sector entity = find(id);
        Versions.require(entity.getVersion(), input.version());
        apply(entity, input);
        repository.saveAndFlush(entity);
        audit.record("UPDATE", "sectors", entity.getId(), "UPDATE Sector");
        return response(entity);
    }

    @Transactional
    public void archive(UUID id) {
        find(id).setStatus(Status.ARCHIVE);
        audit.record("ARCHIVE", "sectors", id, "Archive Sector");
        repository.flush();
    }

    @Transactional
    public SectorResponse restore(UUID id) {
        Sector entity = archived(id);
        entity.setStatus(Status.ACTIVE);
        audit.record("RESTORE", "sectors", id, "Restore Sector");
        repository.flush();
        return response(entity);
    }

    @Transactional
    public void deletePermanently(UUID id) {
        repository.delete(archived(id));
        audit.record("DELETE", "sectors", id, "Delete Sector");
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
        if (input.enabled() != null) entity.setEnabled(input.enabled());
    }

    SectorResponse response(Sector e) {
        return new SectorResponse(e.getId(), e.getStatus(), e.getName(), e.getCreatedAt(), e.isEnabled(), e.getUpdatedAt(), e.getVersion());
    }
}
