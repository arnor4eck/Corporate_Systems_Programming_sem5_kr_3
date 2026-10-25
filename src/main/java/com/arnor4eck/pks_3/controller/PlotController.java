package com.arnor4eck.pks_3.controller;

import com.arnor4eck.pks_3.dto.PlotInput;
import com.arnor4eck.pks_3.dto.PlotResponse;
import com.arnor4eck.pks_3.service.PlotService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/plots")
public class PlotController extends CrudController<PlotInput, PlotResponse> {
    public PlotController(PlotService service) { super(service); }
}
