package com.arnor4eck.pks_3.service;

import com.arnor4eck.pks_3.domain.Employee;
import com.arnor4eck.pks_3.domain.Status;
import com.arnor4eck.pks_3.repository.EmployeeRepository;
import com.arnor4eck.pks_3.repository.RefreshSessionRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.arnor4eck.pks_3.dto.EmployeeInput;
import com.arnor4eck.pks_3.dto.EmployeeResponse;
import com.arnor4eck.pks_3.dto.EmployeePage;
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
    private final AuditService audit;
    private final EmployeeRepository repository;
    private final RefreshSessionRepository sessions;
    private final PasswordEncoder passwords;

    public List<EmployeeResponse> getAll() {
        return repository.findAll().stream().map(this::response).toList();
    }

    public EmployeePage getPage(int page, int size) {
        var result = repository.findAll(org.springframework.data.domain.PageRequest.of(page, size,
                org.springframework.data.domain.Sort.by("login")));
        return new EmployeePage(result.getContent().stream().map(this::response).toList(),
                page, size, result.getTotalElements(), result.getTotalPages());
    }

    public EmployeeResponse get(UUID id) {
        return response(find(id));
    }

    @Transactional
    public EmployeeResponse create(EmployeeInput input) {
        Employee entity = new Employee();
        if (input.password() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Temporary password is required");
        apply(entity, input);
        repository.saveAndFlush(entity);
        audit.record("CREATE", "employees", entity.getId(), "CREATE Employee");
        return response(entity);
    }

    @Transactional
    public EmployeeResponse update(UUID id, EmployeeInput input) {
        Employee entity = find(id);
        Versions.require(entity.getVersion(), input.version());
        apply(entity, input);
        sessions.revokeAll(id);
        repository.saveAndFlush(entity);
        audit.record("UPDATE", "employees", entity.getId(), "UPDATE Employee");
        return response(entity);
    }

    @Transactional
    public void archive(UUID id) {
        find(id).setStatus(Status.ARCHIVE);
        audit.record("ARCHIVE", "employees", id, "Archive Employee");
        sessions.revokeAll(id);
        repository.flush();
    }

    @Transactional
    public EmployeeResponse restore(UUID id) {
        Employee entity = archived(id);
        entity.setStatus(Status.ACTIVE);
        audit.record("RESTORE", "employees", id, "Restore Employee");
        repository.flush();
        return response(entity);
    }

    @Transactional
    public void deletePermanently(UUID id) {
        repository.delete(archived(id));
        audit.record("DELETE", "employees", id, "Delete Employee");
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
        if (input.password() != null) setPassword(entity, input.password());
        entity.setActive(input.active());
    }

    @Transactional
    public void resetPassword(UUID id, String password) {
        setPassword(find(id), password);
        audit.record("RESET_PASSWORD", "employees", id, "Temporary password assigned");
        sessions.revokeAll(id);
    }

    private void setPassword(Employee entity, String password) {
        if (password.isBlank() || password.length() < 12 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password requires at least 12 characters and at most 72 UTF-8 bytes");
        entity.setPasswordHash(passwords.encode(password));
        entity.setPasswordChangeRequired(true);
        entity.setFailedLogins(0);
        entity.setLockedUntil(null);
    }

    EmployeeResponse response(Employee e) {
        return new EmployeeResponse(e.getId(), e.getStatus(), e.getFullName(), e.getRole(), e.getLogin(), e.isActive(), e.getCreatedAt(), e.getUpdatedAt(), e.getVersion());
    }
}
