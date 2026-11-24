package com.arnor4eck.pks_3.controller;

import com.arnor4eck.pks_3.dto.AnalyticsResponse;
import com.arnor4eck.pks_3.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Аналитика")
public class AnalyticsController {
    private final AnalyticsService service;
    @GetMapping("/api/analytics/summary")
    @Operation(summary = "Сводка по сущностям кладбища", description = "Количество всех/активных/архивных записей; суммы и средняя стоимость активных заявок; занятость активных мест; группировки активных заявок и мест по статусам, заявок по секторам. Отключённые сотрудники считаются отдельно.")
    public AnalyticsResponse summary() { return service.summary(); }
}
