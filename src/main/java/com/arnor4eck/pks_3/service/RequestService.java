package com.arnor4eck.pks_3.service;

import com.arnor4eck.pks_3.data.entity.Request;
import com.arnor4eck.pks_3.data.entity.Customer;
import com.arnor4eck.pks_3.data.entity.Plot;
import com.arnor4eck.pks_3.data.entity.Employee;
import com.arnor4eck.pks_3.data.entity.Status;
import com.arnor4eck.pks_3.data.entity.PlotStatus;
import com.arnor4eck.pks_3.data.entity.RequestStatus;
import java.util.stream.Stream;
import com.arnor4eck.pks_3.data.repository.RequestRepository;
import com.arnor4eck.pks_3.data.repository.CustomerRepository;
import com.arnor4eck.pks_3.data.repository.PlotRepository;
import com.arnor4eck.pks_3.data.repository.EmployeeRepository;
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
    private final RequestRepository repository;
    private final CustomerRepository customerRepository;
    private final PlotRepository plotRepository;
    private final EmployeeRepository employeeRepository;

    public List<RequestResponse> getAll() {
        return repository.findAll().stream().map(this::response).toList();
    }

    public RequestResponse get(UUID id) {
        return response(find(id));
    }

    @Transactional
    public RequestResponse create(RequestInput input) {
        lockPlots(input.plotId());
        Request entity = new Request();
        apply(entity, input);
        repository.saveAndFlush(entity);
        syncPlot(entity.getPlot());
        return response(entity);
    }

    @Transactional
    public RequestResponse update(UUID id, RequestInput input) {
        Request entity = mutable(id);
        Plot previous = entity.getPlot();
        lockPlots(previous.getId(), input.plotId());
        apply(entity, input);
        repository.flush();
        syncPlot(previous);
        if (!previous.getId().equals(entity.getPlot().getId())) syncPlot(entity.getPlot());
        return response(entity);
    }

    @Transactional
    public void archive(UUID id) {
        Request entity = mutable(id);
        lockPlots(entity.getPlot().getId());
        entity.setStatus(Status.ARCHIVE);
        repository.flush();
        syncPlot(entity.getPlot());
    }

    @Transactional
    public RequestResponse restore(UUID id) {
        Request entity = archived(id);
        lockPlots(entity.getPlot().getId());
        entity.setStatus(Status.ACTIVE);
        repository.flush();
        syncPlot(entity.getPlot());
        return response(entity);
    }

    @Transactional
    public void deletePermanently(UUID id) {
        Request entity = archived(id);
        Plot plot = entity.getPlot();
        lockPlots(plot.getId());
        repository.delete(entity);
        repository.flush();
        syncPlot(plot);
    }

    private Request archived(UUID id) {
        Request entity = mutable(id);
        if (entity.getStatus() != Status.ARCHIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Record must be archived first");
        }
        return entity;
    }

    private Request mutable(UUID id) {
        return repository.findForUpdate(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found: " + id));
    }

    private void lockPlots(UUID... ids) {
        Stream.of(ids).distinct().sorted().forEach(id -> plotRepository.findForUpdate(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Plot not found: " + id)));
    }

    private void syncPlot(Plot plot) {
        var requests = repository.findAllByPlotIdAndStatus(plot.getId(), Status.ACTIVE);
        PlotStatus target = requests.stream().anyMatch(r -> r.getRequestStatus() == RequestStatus.COMPLETED)
                ? PlotStatus.OCCUPIED
                : requests.stream().anyMatch(r -> r.getRequestStatus() != RequestStatus.REJECTED)
                ? PlotStatus.RESERVED : PlotStatus.FREE;
        plot.setPlotStatus(target);
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
        entity.setPlot(findPlot(input.plotId()));
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

    private RequestResponse response(Request e) {
        return new RequestResponse(e.getId(), e.getStatus(), e.getCustomer().getId(), e.getPlot().getId(), e.getEmployee() == null ? null : e.getEmployee().getId(), e.getDeceasedFullName(), e.getDeceasedBirthday(), e.getDeceasedDeathday(), e.getDeceasedCertificate(), e.getRequestStatus(), e.getRequestDate(), e.getProcessedDate(), e.getTotalCost(), e.getNote(), e.getCreatedAt());
    }
}
