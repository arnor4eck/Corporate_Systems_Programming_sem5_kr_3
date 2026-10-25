package com.arnor4eck.pks_3.controller;

import com.arnor4eck.pks_3.dto.EmployeeInput;
import com.arnor4eck.pks_3.dto.EmployeeResponse;
import com.arnor4eck.pks_3.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {
    private final EmployeeService service;

    @GetMapping
    public List<EmployeeResponse> getAll() { return service.getAll(); }

    @GetMapping("/{id}")
    public EmployeeResponse get(@PathVariable("id") UUID id) { return service.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeResponse create(@Valid @RequestBody EmployeeInput input) { return service.create(input); }

    @PutMapping("/{id}")
    public EmployeeResponse update(@PathVariable("id") UUID id, @Valid @RequestBody EmployeeInput input) {
        return service.update(id, input);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable("id") UUID id) { service.archive(id); }

    @PostMapping("/{id}/restore")
    public EmployeeResponse restore(@PathVariable("id") UUID id) { return service.restore(id); }

    @DeleteMapping("/{id}/permanent")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePermanently(@PathVariable("id") UUID id) { service.deletePermanently(id); }
}
