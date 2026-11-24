package com.arnor4eck.pks_3.controller;

import com.arnor4eck.pks_3.dto.PlotInput;
import com.arnor4eck.pks_3.dto.PlotResponse;
import com.arnor4eck.pks_3.service.PlotService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@Tag(name = "Места захоронения", description = "Размеры и состояние мест. Просмотр: все роли; изменение: MANAGER и ADMIN.")
@RestController
@RequestMapping("/api/plots")
@RequiredArgsConstructor
public class PlotController {
    private final PlotService service;
    private final com.arnor4eck.pks_3.service.SearchService search;

    @GetMapping
    @Operation(summary = "Список записей", description = "Все записи, включая архивные.")
    public Object getAll(jakarta.servlet.http.HttpServletRequest request, @Valid @org.springdoc.core.annotations.ParameterObject com.arnor4eck.pks_3.dto.SearchQuery query) {
        return request.getParameterMap().isEmpty() ? service.getAll() : search.search(com.arnor4eck.pks_3.service.ResourceType.plots, query);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить запись по UUID", description = "Возвращает одну запись, включая архивную.")
    public PlotResponse get(@PathVariable("id") UUID id) { return service.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать запись", description = "Связи передаются как UUID существующих записей.")
    public PlotResponse create(@Valid @RequestBody PlotInput input) { return service.create(input); }

    @PutMapping("/{id}")
    @Operation(summary = "Изменить запись", description = "Полная замена предметных полей. UUID, дата создания и статус архива не меняются.")
    public PlotResponse update(@PathVariable("id") UUID id, @Valid @RequestBody PlotInput input) {
        return service.update(id, input);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Архивировать запись", description = "Мягкое удаление: запись остаётся в базе со статусом ARCHIVE.")
    public void archive(@PathVariable("id") UUID id) { service.archive(id); }

    @PostMapping("/{id}/restore")
    @Operation(summary = "Восстановить из архива", description = "Только для архивной записи; иначе 409.")
    public PlotResponse restore(@PathVariable("id") UUID id) { return service.restore(id); }

    @DeleteMapping("/{id}/permanent")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить окончательно (ADMIN)", description = "Только ADMIN и только архивная запись. Внешние ссылки могут запрещать удаление (409).")
    public void deletePermanently(@PathVariable("id") UUID id) { service.deletePermanently(id); }
}
