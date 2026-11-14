package com.arnor4eck.pks_3.controller;

import com.arnor4eck.pks_3.dto.*;
import com.arnor4eck.pks_3.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Аудит", description = "Журнал входов и изменений, доступ только ADMIN")
public class AuditController {
    private final AuditService service;
    @GetMapping("/api/admin/audit")
    @Operation(summary = "Журнал аудита (ADMIN)", description = "Поиск, фильтры по сотруднику, действию, сущности, UUID, времени и Correlation ID; пагинация и сортировка.")
    public PageResponse<AuditDtos.Event> search(@Valid @ParameterObject AuditDtos.Query query) { return service.search(query); }
}
