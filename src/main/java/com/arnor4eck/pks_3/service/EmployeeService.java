package com.arnor4eck.pks_3.service;

import com.arnor4eck.pks_3.data.entity.Employee;
import com.arnor4eck.pks_3.data.entity.Status;
import com.arnor4eck.pks_3.data.repository.EmployeeRepository;
import com.arnor4eck.pks_3.dto.EmployeeInput;
import com.arnor4eck.pks_3.dto.EmployeeResponse;
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
public class EmployeeService {
    private final EmployeeRepository repository;

    public List<EmployeeResponse> getAll() {
        return repository.findAll().stream().map(this::response).toList();
    }

    public EmployeeResponse get(UUID id) {
        return response(find(id));
    }

    @Transactional
    public EmployeeResponse create(EmployeeInput input) {
        if (input.passwordHash() == null || input.passwordHash().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password hash is required when creating an employee");
        Employee entity = new Employee();
        apply(entity, input);
        return response(repository.saveAndFlush(entity));
    }

    @Transactional
    public EmployeeResponse update(UUID id, EmployeeInput input) {
        Employee entity = find(id);
        apply(entity, input);
        return response(repository.saveAndFlush(entity));
    }

    @Transactional
    public void archive(UUID id) {
        find(id).setStatus(Status.ARCHIVE);
        repository.flush();
    }

    @Transactional
    public EmployeeResponse restore(UUID id) {
        Employee entity = archived(id);
        entity.setStatus(Status.ACTIVE);
        repository.flush();
        return response(entity);
    }

    @Transactional
    public void deletePermanently(UUID id) {
        repository.delete(archived(id));
        repository.flush();
    }

    private Employee archived(UUID id) {
        Employee entity = find(id);
        if (entity.getStatus() != Status.ARCHIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Record must be archived first");
        }
        return entity;
    }

    private Employee find(UUID id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found: " + id));
    }

    private void apply(Employee entity, EmployeeInput input) {
        entity.setFullName(input.fullName());
        entity.setRole(input.role());
        entity.setLogin(input.login());
        if (input.passwordHash() != null) entity.setPasswordHash(input.passwordHash());
        entity.setActive(input.active());
    }

    private EmployeeResponse response(Employee e) {
        return new EmployeeResponse(e.getId(), e.getStatus(), e.getFullName(), e.getRole(), e.getLogin(), e.isActive(), e.getCreatedAt());
    }
}
