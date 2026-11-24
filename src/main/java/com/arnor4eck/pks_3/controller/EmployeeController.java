package com.arnor4eck.pks_3.controller;

import com.arnor4eck.pks_3.dto.EmployeeInput;
import com.arnor4eck.pks_3.dto.EmployeeResponse;
import com.arnor4eck.pks_3.service.EmployeeService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@Tag(name = "Сотрудники", description = "Учётные записи. Все операции доступны только ADMIN.")
@RestController
@RequestMapping({"/api/employees", "/api/admin/users"})
@RequiredArgsConstructor
public class EmployeeController {
    private final EmployeeService service;

    @PostMapping("/{id}/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Назначить временный пароль (ADMIN)", description = "Отзывает все сессии и требует сменить временный пароль при следующем входе.")
    public void resetPassword(@PathVariable("id") UUID id,
                              @Valid @RequestBody com.arnor4eck.pks_3.dto.AuthDtos.ResetPassword input) {
        service.resetPassword(id, input.temporaryPassword());
    }

    private final com.arnor4eck.pks_3.service.SearchService search;

    @GetMapping
    @Operation(summary = "Список сотрудников (ADMIN)", description = "С page возвращает объект страницы, без page — массив всех сотрудников. size от 1 до 100. Сортировка по login.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="200", content=@io.swagger.v3.oas.annotations.media.Content(schema=@io.swagger.v3.oas.annotations.media.Schema(oneOf={com.arnor4eck.pks_3.dto.EmployeePage.class, EmployeeResponse[].class})))
    public Object getAll(jakarta.servlet.http.HttpServletRequest request, @Valid @org.springdoc.core.annotations.ParameterObject com.arnor4eck.pks_3.dto.SearchQuery query) {
        return request.getParameterMap().isEmpty() ? service.getAll() : search.search(com.arnor4eck.pks_3.service.ResourceType.employees, query);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить запись по UUID", description = "Возвращает одну запись, включая архивную.")
    public EmployeeResponse get(@PathVariable("id") UUID id) { return service.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать запись", description = "ADMIN. Пароль обязателен, сохраняется BCrypt; при первом входе требуется его смена.")
    public EmployeeResponse create(@Valid @RequestBody EmployeeInput input) { return service.create(input); }

    @PutMapping("/{id}")
    @Operation(summary = "Изменить запись", description = "ADMIN. Пароль можно пропустить. Все сессии сотрудника отзываются.")
    public EmployeeResponse update(@PathVariable("id") UUID id, @Valid @RequestBody EmployeeInput input) {
        return service.update(id, input);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Архивировать запись", description = "Мягкое удаление: запись остаётся в базе со статусом ARCHIVE.")
    public void archive(@PathVariable("id") UUID id) { service.archive(id); }

    @PostMapping("/{id}/restore")
    @Operation(summary = "Восстановить из архива", description = "Только для архивной записи; иначе 409.")
    public EmployeeResponse restore(@PathVariable("id") UUID id) { return service.restore(id); }

    @DeleteMapping("/{id}/permanent")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить окончательно (ADMIN)", description = "Только ADMIN и только архивная запись. Внешние ссылки могут запрещать удаление (409).")
    public void deletePermanently(@PathVariable("id") UUID id) { service.deletePermanently(id); }
}
