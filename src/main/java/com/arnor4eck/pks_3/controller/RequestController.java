package com.arnor4eck.pks_3.controller;

import com.arnor4eck.pks_3.dto.RequestInput;
import com.arnor4eck.pks_3.dto.RequestResponse;
import com.arnor4eck.pks_3.service.RequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
public class RequestController {
    private final RequestService service;

    @GetMapping
    public List<RequestResponse> getAll() { return service.getAll(); }

    @GetMapping("/{id}")
    public RequestResponse get(@PathVariable("id") UUID id) { return service.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RequestResponse create(@Valid @RequestBody RequestInput input) { return service.create(input); }

    @PutMapping("/{id}")
    public RequestResponse update(@PathVariable("id") UUID id, @Valid @RequestBody RequestInput input) {
        return service.update(id, input);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable("id") UUID id) { service.archive(id); }

    @PostMapping("/{id}/restore")
    public RequestResponse restore(@PathVariable("id") UUID id) { return service.restore(id); }

    @DeleteMapping("/{id}/permanent")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePermanently(@PathVariable("id") UUID id) { service.deletePermanently(id); }
}
