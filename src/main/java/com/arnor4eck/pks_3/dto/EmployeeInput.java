package com.arnor4eck.pks_3.dto;

import com.arnor4eck.pks_3.domain.*;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Данные для создания или изменения: сотрудники")
public record EmployeeInput(
        @Schema(description = "ФИО", example = "Иван Иванов")
        @NotBlank @Size(max = 150) String fullName,
        @Schema(description = "WORKER — просмотр; MANAGER — редактирование; ADMIN — администрирование", example = "MANAGER")
        @NotNull Role role,
        @Schema(description = "Логин сотрудника", example = "manager")
        @NotBlank @Size(max = 50) String login,
        @Schema(description = "Временный пароль: от 12 символов до 72 байт UTF-8; при PUT можно пропустить", example = "TemporaryPass123!", format = "password", accessMode = Schema.AccessMode.WRITE_ONLY)
        @Size(min = 12, max = 72) String password,
        @Schema(description = "Разрешён ли вход сотруднику", example = "true")
        @NotNull Boolean active ,
        @PositiveOrZero Long version
) {}
