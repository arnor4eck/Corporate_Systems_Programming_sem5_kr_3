package com.arnor4eck.pks_3.dto;

import com.arnor4eck.pks_3.data.entity.*;
import jakarta.validation.constraints.*;

public record EmployeeInput(
        @NotBlank @Size(max = 150) String fullName,
        @NotNull Role role,
        @NotBlank @Size(max = 50) String login,
        @NotBlank @Size(max = 255) String passwordHash,
        @NotNull Boolean active
) {}
