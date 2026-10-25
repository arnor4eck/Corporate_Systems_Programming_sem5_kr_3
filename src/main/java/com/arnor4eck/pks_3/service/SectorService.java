package com.arnor4eck.pks_3.service;

import com.arnor4eck.pks_3.data.entity.Sector;
import com.arnor4eck.pks_3.data.repository.*;
import com.arnor4eck.pks_3.dto.SectorInput;
import com.arnor4eck.pks_3.dto.SectorResponse;
import org.springframework.stereotype.Service;

@Service
public class SectorService extends CrudService<Sector, SectorInput, SectorResponse> {

    public SectorService(SectorRepository repository) {
        super(repository);

    }

    @Override
    protected Sector newEntity() { return new Sector(); }

    @Override
    protected void apply(Sector entity, SectorInput input) {
        entity.setName(input.name());
    }

    @Override
    protected SectorResponse response(Sector e) {
        return new SectorResponse(e.getId(), e.getStatus(), e.getName(), e.getCreatedAt());
    }
}
