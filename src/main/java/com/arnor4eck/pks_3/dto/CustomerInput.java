package com.arnor4eck.pks_3.dto;

import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Данные для создания или изменения: клиенты")
public record CustomerInput(
        @Schema(description = "ФИО", example = "Иван Иванов")
        @NotBlank @Size(max = 150) String fullName,
        @Schema(description = "Контактный телефон", example = "+79991234567")
        @NotBlank @Size(max = 20) String phone,
        @Schema(description = "Электронная почта", example = "ivan@example.com")
        @Email @Size(max = 100) String email ,
        @PositiveOrZero Long version
) {}
