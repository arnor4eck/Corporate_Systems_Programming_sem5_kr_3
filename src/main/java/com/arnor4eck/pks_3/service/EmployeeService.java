package com.arnor4eck.pks_3.service;

import com.arnor4eck.pks_3.data.entity.Employee;
import com.arnor4eck.pks_3.data.repository.*;
import com.arnor4eck.pks_3.dto.EmployeeInput;
import com.arnor4eck.pks_3.dto.EmployeeResponse;
import org.springframework.stereotype.Service;

@Service
public class EmployeeService extends CrudService<Employee, EmployeeInput, EmployeeResponse> {

    public EmployeeService(EmployeeRepository repository) {
        super(repository);

    }

    @Override
    protected Employee newEntity() { return new Employee(); }

    @Override
    protected void apply(Employee entity, EmployeeInput input) {
        entity.setFullName(input.fullName());
        entity.setRole(input.role());
        entity.setLogin(input.login());
        entity.setPasswordHash(input.passwordHash());
        entity.setActive(input.active());
    }

    @Override
    protected EmployeeResponse response(Employee e) {
        return new EmployeeResponse(e.getId(), e.getStatus(), e.getFullName(), e.getRole(), e.getLogin(), e.isActive(), e.getCreatedAt());
    }
}
