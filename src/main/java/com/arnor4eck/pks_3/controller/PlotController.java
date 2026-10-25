package com.arnor4eck.pks_3.controller;

import com.arnor4eck.pks_3.dto.PlotInput;
import com.arnor4eck.pks_3.dto.PlotResponse;
import com.arnor4eck.pks_3.service.PlotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/plots")
@RequiredArgsConstructor
public class PlotController {
    private final PlotService service;

    @GetMapping
    public List<PlotResponse> getAll() { return service.getAll(); }

    @GetMapping("/{id}")
    public PlotResponse get(@PathVariable("id") UUID id) { return service.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlotResponse create(@Valid @RequestBody PlotInput input) { return service.create(input); }

    @PutMapping("/{id}")
    public PlotResponse update(@PathVariable("id") UUID id, @Valid @RequestBody PlotInput input) {
        return service.update(id, input);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable("id") UUID id) { service.archive(id); }

    @PostMapping("/{id}/restore")
    public PlotResponse restore(@PathVariable("id") UUID id) { return service.restore(id); }

    @DeleteMapping("/{id}/permanent")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePermanently(@PathVariable("id") UUID id) { service.deletePermanently(id); }
}
