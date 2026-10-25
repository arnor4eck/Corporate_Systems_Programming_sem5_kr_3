package com.arnor4eck.pks_3.controller;

import com.arnor4eck.pks_3.dto.EmployeeInput;
import com.arnor4eck.pks_3.dto.EmployeeResponse;
import com.arnor4eck.pks_3.service.EmployeeService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController extends CrudController<EmployeeInput, EmployeeResponse> {
    public EmployeeController(EmployeeService service) { super(service); }
}
