package com.arnor4eck.pks_3.service;

import com.arnor4eck.pks_3.domain.Customer;
import com.arnor4eck.pks_3.domain.Status;
import com.arnor4eck.pks_3.repository.CustomerRepository;
import com.arnor4eck.pks_3.dto.CustomerInput;
import com.arnor4eck.pks_3.dto.CustomerResponse;
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
public class CustomerService {
    private final AuditService audit;
    private final CustomerRepository repository;

    public List<CustomerResponse> getAll() {
        return repository.findAll().stream().map(this::response).toList();
    }

    public CustomerResponse get(UUID id) {
        return response(find(id));
    }

    @Transactional
    public CustomerResponse create(CustomerInput input) {
        Customer entity = new Customer();
        apply(entity, input);
        repository.saveAndFlush(entity);
        audit.record("CREATE", "customers", entity.getId(), "CREATE Customer");
        return response(entity);
    }

    @Transactional
    public CustomerResponse update(UUID id, CustomerInput input) {
        Customer entity = find(id);
        Versions.require(entity.getVersion(), input.version());
        apply(entity, input);
        repository.saveAndFlush(entity);
        audit.record("UPDATE", "customers", entity.getId(), "UPDATE Customer");
        return response(entity);
    }

    @Transactional
    public void archive(UUID id) {
        find(id).setStatus(Status.ARCHIVE);
        audit.record("ARCHIVE", "customers", id, "Archive Customer");
        repository.flush();
    }

    @Transactional
    public CustomerResponse restore(UUID id) {
        Customer entity = archived(id);
        entity.setStatus(Status.ACTIVE);
        audit.record("RESTORE", "customers", id, "Restore Customer");
        repository.flush();
        return response(entity);
    }

    @Transactional
    public void deletePermanently(UUID id) {
        repository.delete(archived(id));
        audit.record("DELETE", "customers", id, "Delete Customer");
        repository.flush();
    }

    private Customer archived(UUID id) {
        Customer entity = find(id);
        if (entity.getStatus() != Status.ARCHIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Record must be archived first");
        }
        return entity;
    }

    private Customer find(UUID id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found: " + id));
    }

    private void apply(Customer entity, CustomerInput input) {
        entity.setFullName(input.fullName());
        entity.setPhone(input.phone());
        entity.setEmail(input.email());
    }

    CustomerResponse response(Customer e) {
        return new CustomerResponse(e.getId(), e.getStatus(), e.getFullName(), e.getPhone(), e.getEmail(), e.getCreatedAt(), e.getUpdatedAt(), e.getVersion());
    }
}
