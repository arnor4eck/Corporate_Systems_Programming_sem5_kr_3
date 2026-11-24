package com.arnor4eck.pks_3.dto;

import com.arnor4eck.pks_3.domain.*;
import java.util.UUID;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Данные для создания или изменения: заявки")
public record RequestInput(
        @Schema(description = "UUID существующей связанной записи: customerId", example = "11111111-1111-4111-8111-111111111111")
        @NotNull UUID customerId,
        @Schema(description = "UUID существующей связанной записи: plotId", example = "11111111-1111-4111-8111-111111111111")
        @NotNull UUID plotId,
        @Schema(description = "UUID существующей связанной записи: employeeId", example = "11111111-1111-4111-8111-111111111111")
        UUID employeeId,
        @Schema(description = "ФИО умершего; при бронировании можно пропустить", example = "Пётр Иванов")
        @Size(max = 150) String deceasedFullName,
        @Schema(description = "Дата рождения умершего", example = "1950-01-02")
        LocalDate deceasedBirthday,
        @Schema(description = "Дата смерти умершего", example = "2026-10-20")
        LocalDate deceasedDeathday,
        @Schema(description = "Номер свидетельства о смерти", example = "IV-АБ №123456")
        @Size(max = 100) String deceasedCertificate,
        @Schema(description = "Статус обработки заявки", example = "NEW")
        @NotNull RequestStatus requestStatus,
        @Schema(description = "Дата и время подачи заявки", example = "2026-11-14T12:00:00")
        @NotNull LocalDateTime requestDate,
        @Schema(description = "Дата и время обработки; необязательно", example = "2026-11-14T13:00:00")
        LocalDateTime processedDate,
        @Schema(description = "Стоимость от 0 до 99999999.99", example = "12500.5")
        @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal totalCost,
        @Schema(description = "Примечание к заявке", example = "Бронирование места")
        String note,
        @Size(max = 50) @Pattern(regexp = "[A-Za-z0-9._-]+") String code,
        @Size(max = 150) @Pattern(regexp = "(?s).*\\S.*") String title,
        Boolean urgent,
        @PositiveOrZero Long version
) {}
