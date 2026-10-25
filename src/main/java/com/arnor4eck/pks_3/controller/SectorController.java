package com.arnor4eck.pks_3.controller;

import com.arnor4eck.pks_3.dto.SectorInput;
import com.arnor4eck.pks_3.dto.SectorResponse;
import com.arnor4eck.pks_3.service.SectorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sectors")
@RequiredArgsConstructor
public class SectorController {
    private final SectorService service;

    @GetMapping
    public List<SectorResponse> getAll() { return service.getAll(); }

    @GetMapping("/{id}")
    public SectorResponse get(@PathVariable("id") UUID id) { return service.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SectorResponse create(@Valid @RequestBody SectorInput input) { return service.create(input); }

    @PutMapping("/{id}")
    public SectorResponse update(@PathVariable("id") UUID id, @Valid @RequestBody SectorInput input) {
        return service.update(id, input);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable("id") UUID id) { service.archive(id); }

    @PostMapping("/{id}/restore")
    public SectorResponse restore(@PathVariable("id") UUID id) { return service.restore(id); }

    @DeleteMapping("/{id}/permanent")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePermanently(@PathVariable("id") UUID id) { service.deletePermanently(id); }
}
