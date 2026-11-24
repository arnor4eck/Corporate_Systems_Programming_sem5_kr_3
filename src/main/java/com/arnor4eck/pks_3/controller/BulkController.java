package com.arnor4eck.pks_3.controller;
import com.arnor4eck.pks_3.dto.BulkDtos.*;
import com.arnor4eck.pks_3.service.BulkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController @RequiredArgsConstructor @Tag(name = "Массовые операции заявок")
public class BulkController {
    private final BulkService service;
    @PostMapping("/api/requests/bulk")
    @Operation(summary = "Массовый архив, восстановление, статус или метки", description = "До 100 UUID с version. Каждая запись применяется в отдельной транзакции; ошибки 404/409 возвращаются отдельно и не отменяют успехи. Только MANAGER/ADMIN.")
    public Response apply(@Valid @RequestBody Input input) { return service.apply(input); }
}
