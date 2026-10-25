package com.arnor4eck.pks_3.service;

import com.arnor4eck.pks_3.data.entity.Request;
import com.arnor4eck.pks_3.data.repository.*;
import com.arnor4eck.pks_3.dto.RequestInput;
import com.arnor4eck.pks_3.dto.RequestResponse;
import org.springframework.stereotype.Service;

@Service
public class RequestService extends CrudService<Request, RequestInput, RequestResponse> {
    private final CustomerRepository customerRepository;
    private final PlotRepository plotRepository;
    private final EmployeeRepository employeeRepository;
    public RequestService(RequestRepository repository, CustomerRepository customerRepository, PlotRepository plotRepository, EmployeeRepository employeeRepository) {
        super(repository);
        this.customerRepository = customerRepository;
        this.plotRepository = plotRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    protected Request newEntity() { return new Request(); }

    @Override
    protected void apply(Request entity, RequestInput input) {
        entity.setCustomer(require(customerRepository, input.customerId()));
        entity.setPlot(require(plotRepository, input.plotId()));
        entity.setEmployee(input.employeeId() == null ? null : require(employeeRepository, input.employeeId()));
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

    @Override
    protected RequestResponse response(Request e) {
        return new RequestResponse(e.getId(), e.getStatus(), e.getCustomer().getId(), e.getPlot().getId(), e.getEmployee() == null ? null : e.getEmployee().getId(), e.getDeceasedFullName(), e.getDeceasedBirthday(), e.getDeceasedDeathday(), e.getDeceasedCertificate(), e.getRequestStatus(), e.getRequestDate(), e.getProcessedDate(), e.getTotalCost(), e.getNote(), e.getCreatedAt());
    }
}
