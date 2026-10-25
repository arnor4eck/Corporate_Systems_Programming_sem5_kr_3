package com.arnor4eck.pks_3.dto;

import com.arnor4eck.pks_3.data.entity.*;
import java.util.UUID;
import jakarta.validation.constraints.*;

public record PlotInput(
        @NotNull UUID sectorId,
        @NotNull @Positive Integer rowNumber,
        @NotNull @Positive Integer plotNumber,
        @NotNull PlotStatus plotStatus,
        @NotNull @Positive Integer lengthCm,
        @NotNull @Positive Integer widthCm,
        @Size(max = 100) String coordinates
) {}
