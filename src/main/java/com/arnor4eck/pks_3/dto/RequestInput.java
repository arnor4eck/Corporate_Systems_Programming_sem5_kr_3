package com.arnor4eck.pks_3.dto;

import com.arnor4eck.pks_3.data.entity.*;
import java.util.UUID;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;

public record RequestInput(
        @NotNull UUID customerId,
        @NotNull UUID plotId,
        UUID employeeId,
        @Size(max = 150) String deceasedFullName,
        LocalDate deceasedBirthday,
        LocalDate deceasedDeathday,
        @Size(max = 100) String deceasedCertificate,
        @NotNull RequestStatus requestStatus,
        @NotNull LocalDateTime requestDate,
        LocalDateTime processedDate,
        @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal totalCost,
        String note
) {}
