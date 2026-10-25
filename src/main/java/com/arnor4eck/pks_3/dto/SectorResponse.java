package com.arnor4eck.pks_3.dto;

import com.arnor4eck.pks_3.data.entity.*;
import java.util.UUID;
import java.time.LocalDateTime;

public record SectorResponse(
        UUID id,
        Status status,
        String name,
        LocalDateTime createdAt
) {}
