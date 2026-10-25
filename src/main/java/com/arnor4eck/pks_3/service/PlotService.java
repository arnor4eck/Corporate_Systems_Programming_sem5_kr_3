package com.arnor4eck.pks_3.service;

import com.arnor4eck.pks_3.data.entity.Plot;
import com.arnor4eck.pks_3.data.entity.Sector;
import com.arnor4eck.pks_3.data.entity.Status;
import com.arnor4eck.pks_3.data.repository.PlotRepository;
import com.arnor4eck.pks_3.data.repository.SectorRepository;
import com.arnor4eck.pks_3.dto.PlotInput;
import com.arnor4eck.pks_3.dto.PlotResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlotService {
    private final PlotRepository repository;
    private final SectorRepository sectorRepository;

    public List<PlotResponse> getAll() {
        return repository.findAll().stream().map(this::response).toList();
    }

    public PlotResponse get(UUID id) {
        return response(find(id));
    }

    @Transactional
    public PlotResponse create(PlotInput input) {
        Plot entity = new Plot();
        apply(entity, input);
        return response(repository.saveAndFlush(entity));
    }

    @Transactional
    public PlotResponse update(UUID id, PlotInput input) {
        Plot entity = find(id);
        apply(entity, input);
        return response(repository.saveAndFlush(entity));
    }

    @Transactional
    public void archive(UUID id) {
        find(id).setStatus(Status.ARCHIVE);
        repository.flush();
    }

    @Transactional
    public PlotResponse restore(UUID id) {
        Plot entity = archived(id);
        entity.setStatus(Status.ACTIVE);
        repository.flush();
        return response(entity);
    }

    @Transactional
    public void deletePermanently(UUID id) {
        repository.delete(archived(id));
        repository.flush();
    }

    private Plot archived(UUID id) {
        Plot entity = find(id);
        if (entity.getStatus() != Status.ARCHIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Record must be archived first");
        }
        return entity;
    }

    private Plot find(UUID id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Plot not found: " + id));
    }

    private Sector findSector(UUID id) {
        return sectorRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Sector not found: " + id));
    }

    private void apply(Plot entity, PlotInput input) {
        entity.setSector(findSector(input.sectorId()));
        entity.setRowNumber(input.rowNumber());
        entity.setPlotNumber(input.plotNumber());
        entity.setPlotStatus(input.plotStatus());
        entity.setLengthCm(input.lengthCm());
        entity.setWidthCm(input.widthCm());
        entity.setCoordinates(input.coordinates());
    }

    private PlotResponse response(Plot e) {
        return new PlotResponse(e.getId(), e.getStatus(), e.getSector().getId(), e.getRowNumber(), e.getPlotNumber(), e.getPlotStatus(), e.getLengthCm(), e.getWidthCm(), e.getCoordinates());
    }
}
