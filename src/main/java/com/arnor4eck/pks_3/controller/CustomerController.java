package com.arnor4eck.pks_3.controller;

import com.arnor4eck.pks_3.dto.CustomerInput;
import com.arnor4eck.pks_3.dto.CustomerResponse;
import com.arnor4eck.pks_3.service.CustomerService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
public class CustomerController extends CrudController<CustomerInput, CustomerResponse> {
    public CustomerController(CustomerService service) { super(service); }
}
