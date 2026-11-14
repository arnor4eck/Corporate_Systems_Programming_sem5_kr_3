package com.arnor4eck.pks_3.dto;

import com.arnor4eck.pks_3.domain.*;
import java.util.UUID;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Данные для создания или изменения: места захоронения")
public record PlotInput(
        @Schema(description = "UUID существующей связанной записи: sectorId", example = "11111111-1111-4111-8111-111111111111")
        @NotNull UUID sectorId,
        @Schema(description = "Номер ряда", example = "1")
        @NotNull @Positive Integer rowNumber,
        @Schema(description = "Номер места", example = "2")
        @NotNull @Positive Integer plotNumber,
        @Schema(description = "Состояние места", example = "FREE")
        @NotNull PlotStatus plotStatus,
        @Schema(description = "Длина места, см", example = "200")
        @NotNull @Positive Integer lengthCm,
        @Schema(description = "Ширина места, см", example = "100")
        @NotNull @Positive Integer widthCm,
        @Schema(description = "Координаты места", example = "55.7500, 37.6100")
        @Size(max = 100) String coordinates ,
        @PositiveOrZero Long version
) {}
