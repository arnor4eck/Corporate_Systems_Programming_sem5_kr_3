package com.arnor4eck.pks_3.controller;

import com.arnor4eck.pks_3.dto.*;
import com.arnor4eck.pks_3.dto.CsvDtos.*;
import com.arnor4eck.pks_3.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@RestController
@RequiredArgsConstructor
@Tag(name = "CSV", description = "UTF-8, разделитель запятая, RFC 4180. До 2 MiB и 5000 строк. customers, employees (ADMIN), sectors (импорт ADMIN), plots, requests.")
public class CsvController {
    private final CsvService service;
    @GetMapping(value = {"/api/customers/export", "/api/employees/export", "/api/sectors/export", "/api/plots/export", "/api/requests/export"}, produces = "text/csv;charset=UTF-8")
    @Operation(summary = "Экспорт CSV с учётом поиска, фильтров и сортировки", description = "Экспортирует весь результат фильтра, независимо от page/size. Формулы экранируются апострофом. Пароли и хеши никогда не экспортируются.")
    public ResponseEntity<byte[]> export(jakarta.servlet.http.HttpServletRequest request, @Valid @ParameterObject SearchQuery query) {
        var type = resource(request);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + type.name() + ".csv\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store").body(service.export(type, query));
    }
    @PostMapping(value = {"/api/customers/import/preview", "/api/employees/import/preview", "/api/sectors/import/preview", "/api/plots/import/preview", "/api/requests/import/preview"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Проверить CSV без изменения предметных данных", description = "NEW_ONLY: новая запись. UPSERT: заявки обновляются по внешнему code (либо id); остальные сущности по id. Для обновления обязателен version. Новый код создаёт заявку. Возвращает ошибки строк и previewToken на 15 минут.")
    public Preview preview(jakarta.servlet.http.HttpServletRequest request, @RequestPart("file") MultipartFile file,
                           @RequestParam(defaultValue = "NEW_ONLY") Mode mode) throws IOException {
        return service.preview(resource(request), file.getBytes(), mode);
    }
    @PostMapping(value = {"/api/customers/import/apply", "/api/employees/import/apply", "/api/sectors/import/apply", "/api/plots/import/apply", "/api/requests/import/apply"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Применить проверенный CSV", description = "Передайте тот же file, mode и одноразовый previewToken. Каждая корректная строка применяется в отдельной транзакции; ошибочные строки не мешают остальным. Проверки выполняются повторно. Повторное применение токена запрещено.")
    public Applied apply(jakarta.servlet.http.HttpServletRequest request, @RequestPart("file") MultipartFile file,
                         @RequestParam(defaultValue = "NEW_ONLY") Mode mode, @RequestParam String previewToken) throws IOException {
        return service.apply(resource(request), file.getBytes(), mode, previewToken);
    }
    private ResourceType resource(jakarta.servlet.http.HttpServletRequest request) {
        return ResourceType.parse(request.getRequestURI().substring(request.getContextPath().length()).split("/")[2]);
    }
}
