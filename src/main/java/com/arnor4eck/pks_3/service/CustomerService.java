package com.arnor4eck.pks_3.service;

import com.arnor4eck.pks_3.data.entity.Customer;
import com.arnor4eck.pks_3.data.repository.*;
import com.arnor4eck.pks_3.dto.CustomerInput;
import com.arnor4eck.pks_3.dto.CustomerResponse;
import org.springframework.stereotype.Service;

@Service
public class CustomerService extends CrudService<Customer, CustomerInput, CustomerResponse> {

    public CustomerService(CustomerRepository repository) {
        super(repository);

    }

    @Override
    protected Customer newEntity() { return new Customer(); }

    @Override
    protected void apply(Customer entity, CustomerInput input) {
        entity.setFullName(input.fullName());
        entity.setPhone(input.phone());
        entity.setEmail(input.email());
    }

    @Override
    protected CustomerResponse response(Customer e) {
        return new CustomerResponse(e.getId(), e.getStatus(), e.getFullName(), e.getPhone(), e.getEmail(), e.getCreatedAt());
    }
}
