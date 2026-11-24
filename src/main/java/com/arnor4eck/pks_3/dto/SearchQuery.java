package com.arnor4eck.pks_3.dto;

import com.arnor4eck.pks_3.domain.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Поиск и фильтры по полям выбранной сущности. Неприменимые фильтры дают 400.")
public record SearchQuery(
        @Size(max = 200) String q,
        Status archiveStatus, RequestStatus requestStatus, PlotStatus plotStatus, Role role, Boolean active,
        UUID customerId, UUID employeeId, UUID sectorId, UUID plotId,
        @DecimalMin("0") BigDecimal minCost, @DecimalMin("0") BigDecimal maxCost,
        LocalDateTime fromDate, LocalDateTime toDate,
        @Positive Integer minLengthCm, @Positive Integer maxLengthCm,
        @Positive Integer rowNumber, @Positive Integer plotNumber,
        @Min(0) @Max(1000000) Integer page, @Min(1) @Max(100) Integer size,
        @Schema(description = "Повторяемый параметр: sort=totalCost,desc&sort=requestDate,asc. По умолчанию id,asc.")
        List<String> sort,
        @Size(max=50) String code, Boolean urgent, Priority priority, java.util.UUID tagId, Boolean enabled) {
    public int pageNumber() { return page == null ? 0 : page; }
    public int pageSize() { return size == null ? 20 : size; }
}
