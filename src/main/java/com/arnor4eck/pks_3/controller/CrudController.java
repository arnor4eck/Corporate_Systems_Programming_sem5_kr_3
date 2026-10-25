package com.arnor4eck.pks_3.controller;

import com.arnor4eck.pks_3.service.CrudService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

public abstract class CrudController<I, O> {
    private final CrudService<?, I, O> service;

    protected CrudController(CrudService<?, I, O> service) {
        this.service = service;
    }

    @GetMapping
    public List<O> getAll() { return service.getAll(); }

    @GetMapping("/{id}")
    public O get(@PathVariable("id") UUID id) { return service.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public O create(@Valid @RequestBody I input) { return service.create(input); }

    @PutMapping("/{id}")
    public O update(@PathVariable("id") UUID id, @Valid @RequestBody I input) {
        return service.update(id, input);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable("id") UUID id) { service.archive(id); }

    @PostMapping("/{id}/restore")
    public O restore(@PathVariable("id") UUID id) { return service.restore(id); }

    @DeleteMapping("/{id}/permanent")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePermanently(@PathVariable("id") UUID id) { service.deletePermanently(id); }
}
