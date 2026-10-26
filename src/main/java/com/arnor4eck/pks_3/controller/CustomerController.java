package com.arnor4eck.pks_3.controller;

import com.arnor4eck.pks_3.dto.CustomerInput;
import com.arnor4eck.pks_3.dto.CustomerResponse;
import com.arnor4eck.pks_3.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {
    private final CustomerService service;

    @GetMapping
    public List<CustomerResponse> getAll() { return service.getAll(); }

    @GetMapping("/{id}")
    public CustomerResponse get(@PathVariable("id") UUID id) { return service.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse create(@Valid @RequestBody CustomerInput input) { return service.create(input); }

    @PutMapping("/{id}")
    public CustomerResponse update(@PathVariable("id") UUID id, @Valid @RequestBody CustomerInput input) {
        return service.update(id, input);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable("id") UUID id) { service.archive(id); }

    @PostMapping("/{id}/restore")
    public CustomerResponse restore(@PathVariable("id") UUID id) { return service.restore(id); }

    @DeleteMapping("/{id}/permanent")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePermanently(@PathVariable("id") UUID id) { service.deletePermanently(id); }
}
