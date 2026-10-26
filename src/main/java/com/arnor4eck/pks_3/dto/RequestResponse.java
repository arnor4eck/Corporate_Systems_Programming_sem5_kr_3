package com.arnor4eck.pks_3.dto;

import com.arnor4eck.pks_3.data.entity.*;
import java.util.UUID;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

public record RequestResponse(
        UUID id,
        Status status,
        UUID customerId,
        UUID plotId,
        UUID employeeId,
        String deceasedFullName,
        LocalDate deceasedBirthday,
        LocalDate deceasedDeathday,
        String deceasedCertificate,
        RequestStatus requestStatus,
        LocalDateTime requestDate,
        LocalDateTime processedDate,
        BigDecimal totalCost,
        String note,
        LocalDateTime createdAt
) {}
