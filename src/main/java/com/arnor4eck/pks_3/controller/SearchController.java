package com.arnor4eck.pks_3.controller;

import com.arnor4eck.pks_3.dto.*;
import com.arnor4eck.pks_3.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Поиск и фильтры", description = "customers, employees (ADMIN), sectors, plots, requests")
public class SearchController {
    private final SearchService service;
    @GetMapping({"/api/customers/search", "/api/employees/search", "/api/sectors/search", "/api/plots/search", "/api/requests/search"})
    @Operation(summary = "Поиск с фильтрами, сортировкой и пагинацией", description = "page от 0, size 1–100. Фильтры объединяются через AND, q ищет по текстовым полям. Старые списки без /search сохранены для совместимости.")
    public PageResponse<Object> search(jakarta.servlet.http.HttpServletRequest request, @Valid @ParameterObject SearchQuery query) {
        String resource = request.getRequestURI().substring(request.getContextPath().length()).split("/")[2];
        return service.search(ResourceType.parse(resource), query);
    }
}
