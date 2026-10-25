package com.arnor4eck.pks_3.service;

import com.arnor4eck.pks_3.data.entity.Plot;
import com.arnor4eck.pks_3.data.repository.*;
import com.arnor4eck.pks_3.dto.PlotInput;
import com.arnor4eck.pks_3.dto.PlotResponse;
import org.springframework.stereotype.Service;

@Service
public class PlotService extends CrudService<Plot, PlotInput, PlotResponse> {
    private final SectorRepository sectorRepository;
    public PlotService(PlotRepository repository, SectorRepository sectorRepository) {
        super(repository);
        this.sectorRepository = sectorRepository;
    }

    @Override
    protected Plot newEntity() { return new Plot(); }

    @Override
    protected void apply(Plot entity, PlotInput input) {
        entity.setSector(require(sectorRepository, input.sectorId()));
        entity.setRowNumber(input.rowNumber());
        entity.setPlotNumber(input.plotNumber());
        entity.setPlotStatus(input.plotStatus());
        entity.setLengthCm(input.lengthCm());
        entity.setWidthCm(input.widthCm());
        entity.setCoordinates(input.coordinates());
    }

    @Override
    protected PlotResponse response(Plot e) {
        return new PlotResponse(e.getId(), e.getStatus(), e.getSector().getId(), e.getRowNumber(), e.getPlotNumber(), e.getPlotStatus(), e.getLengthCm(), e.getWidthCm(), e.getCoordinates());
    }
}
