package com.arnor4eck.pks_3.dto;

import com.arnor4eck.pks_3.data.entity.*;
import java.util.UUID;

public record PlotResponse(
        UUID id,
        Status status,
        UUID sectorId,
        int rowNumber,
        int plotNumber,
        PlotStatus plotStatus,
        int lengthCm,
        int widthCm,
        String coordinates
) {}
