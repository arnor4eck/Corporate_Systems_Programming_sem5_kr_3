package com.arnor4eck.pks_3.dto;

import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Данные для создания или изменения: сектора")
public record SectorInput(
        @Schema(description = "Название сектора", example = "Центральный")
        @NotBlank @Size(max = 100) String name,
        Boolean enabled,
        @PositiveOrZero Long version
) {}
