package com.arnor4eck.pks_3.controller;

import com.arnor4eck.pks_3.dto.RequestInput;
import com.arnor4eck.pks_3.dto.RequestResponse;
import com.arnor4eck.pks_3.service.RequestService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/requests")
public class RequestController extends CrudController<RequestInput, RequestResponse> {
    public RequestController(RequestService service) { super(service); }
}
