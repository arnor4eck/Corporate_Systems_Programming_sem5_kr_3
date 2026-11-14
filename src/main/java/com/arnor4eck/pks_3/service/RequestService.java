package com.arnor4eck.pks_3.service;

import com.arnor4eck.pks_3.domain.Request;
import com.arnor4eck.pks_3.domain.Customer;
import com.arnor4eck.pks_3.domain.Plot;
import com.arnor4eck.pks_3.domain.Employee;
import com.arnor4eck.pks_3.domain.Status;
import com.arnor4eck.pks_3.repository.RequestRepository;
import com.arnor4eck.pks_3.repository.CustomerRepository;
import com.arnor4eck.pks_3.repository.PlotRepository;
import com.arnor4eck.pks_3.repository.EmployeeRepository;
import com.arnor4eck.pks_3.dto.RequestInput;
import com.arnor4eck.pks_3.dto.RequestResponse;
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
public class RequestService {
    private final AuditService audit;
    private final RequestRepository repository;
    private final CustomerRepository customerRepository;
    private final PlotRepository plotRepository;
    private final EmployeeRepository employeeRepository;
    private final com.arnor4eck.pks_3.repository.TagRepository tags;

    @Transactional
    public RequestResponse bulk(com.arnor4eck.pks_3.dto.BulkDtos.Item item, com.arnor4eck.pks_3.dto.BulkDtos.Input input) {
        var entity = find(item.id());
        Versions.require(entity.getVersion(), item.version());
        switch (input.operation()) {
            case ARCHIVE -> entity.setStatus(Status.ARCHIVE);
            case RESTORE -> { archived(item.id()); entity.setStatus(Status.ACTIVE); }
            case SET_STATUS -> entity.setRequestStatus(input.requestStatus());
            case ADD_TAG, REMOVE_TAG -> {
                var tag = tags.findById(input.tagId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found"));
                if (input.operation() == com.arnor4eck.pks_3.dto.BulkDtos.Operation.ADD_TAG) entity.getTags().add(tag);
                else entity.getTags().removeIf(t -> t.getId().equals(tag.getId()));
            }
        }
        repository.flush();
        audit.record("BULK_" + input.operation(), "requests", item.id(), "Bulk operation on request");
        return response(entity);
    }

    public List<RequestResponse> getAll() {
        return repository.findAll().stream().map(this::response).toList();
    }

    public RequestResponse get(UUID id) {
        return response(find(id));
    }

    @Transactional
    public RequestResponse create(RequestInput input) {
        Request entity = new Request();
        apply(entity, input);
        repository.saveAndFlush(entity);
        audit.record("CREATE", "requests", entity.getId(), "CREATE Request");
        return response(entity);
    }

    @Transactional
    public RequestResponse update(UUID id, RequestInput input) {
        Request entity = find(id);
        Versions.require(entity.getVersion(), input.version());
        apply(entity, input);
        repository.saveAndFlush(entity);
        audit.record("UPDATE", "requests", entity.getId(), "UPDATE Request");
        return response(entity);
    }

    @Transactional
    public void archive(UUID id) {
        find(id).setStatus(Status.ARCHIVE);
        audit.record("ARCHIVE", "requests", id, "Archive Request");
        repository.flush();
    }

    @Transactional
    public RequestResponse restore(UUID id) {
        Request entity = archived(id);
        entity.setStatus(Status.ACTIVE);
        audit.record("RESTORE", "requests", id, "Restore Request");
        repository.flush();
        return response(entity);
    }

    @Transactional
    public void deletePermanently(UUID id) {
        repository.delete(archived(id));
        audit.record("DELETE", "requests", id, "Delete Request");
        repository.flush();
    }

    private Request archived(UUID id) {
        Request entity = find(id);
        if (entity.getStatus() != Status.ARCHIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Record must be archived first");
        }
        return entity;
    }

    private Request find(UUID id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found: " + id));
    }

    private Customer findCustomer(UUID id) {
        return customerRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found: " + id));
    }

    private Plot findPlot(UUID id) {
        return plotRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Plot not found: " + id));
    }

    private Employee findEmployee(UUID id) {
        return employeeRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found: " + id));
    }

    private void apply(Request entity, RequestInput input) {
        entity.setCustomer(findCustomer(input.customerId()));
        var plot = findPlot(input.plotId());
        boolean newAssignment = entity.getId() == null || entity.getPlot() == null || !entity.getPlot().getId().equals(plot.getId());
        if (newAssignment && (!plot.getSector().isEnabled() || plot.getSector().getStatus() != Status.ACTIVE))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sector is disabled or archived");
        entity.setPlot(plot);
        entity.setSector(plot.getSector());
        if (input.code() != null) entity.setCode(input.code());
        if (input.title() != null) {
            if (input.title().isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "title must not be blank");
            entity.setTitle(input.title());
        }
        if (input.urgent() != null) entity.setUrgent(input.urgent());
        entity.setEmployee(input.employeeId() == null ? null : findEmployee(input.employeeId()));
        entity.setDeceasedFullName(input.deceasedFullName());
        entity.setDeceasedBirthday(input.deceasedBirthday());
        entity.setDeceasedDeathday(input.deceasedDeathday());
        entity.setDeceasedCertificate(input.deceasedCertificate());
        entity.setRequestStatus(input.requestStatus());
        entity.setRequestDate(input.requestDate());
        entity.setProcessedDate(input.processedDate());
        entity.setTotalCost(input.totalCost());
        entity.setNote(input.note());
    }

    RequestResponse response(Request e) {
        return new RequestResponse(e.getId(), e.getStatus(), e.getCustomer().getId(), e.getPlot().getId(), e.getEmployee() == null ? null : e.getEmployee().getId(), e.getDeceasedFullName(), e.getDeceasedBirthday(), e.getDeceasedDeathday(), e.getDeceasedCertificate(), e.getRequestStatus(), e.getRequestDate(), e.getProcessedDate(), e.getTotalCost(), e.getNote(), e.getCreatedAt(), e.getCode(), e.getTitle(), e.isUrgent(), e.getPriority(), e.getSector().getId(), e.getTags().stream().map(t -> new com.arnor4eck.pks_3.dto.TagResponse(t.getId(), t.getName())).sorted(java.util.Comparator.comparing(com.arnor4eck.pks_3.dto.TagResponse::name)).toList(), e.getUpdatedAt(), e.getVersion());
    }
}
