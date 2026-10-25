package com.arnor4eck.pks_3.controller;

import com.arnor4eck.pks_3.dto.SectorInput;
import com.arnor4eck.pks_3.dto.SectorResponse;
import com.arnor4eck.pks_3.service.SectorService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sectors")
public class SectorController extends CrudController<SectorInput, SectorResponse> {
    public SectorController(SectorService service) { super(service); }
}
