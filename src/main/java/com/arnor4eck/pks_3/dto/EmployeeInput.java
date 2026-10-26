package com.arnor4eck.pks_3.dto;

import com.arnor4eck.pks_3.data.entity.*;
import jakarta.validation.constraints.*;

public record EmployeeInput(
        @NotBlank @Size(max = 150) String fullName,
        @NotNull Role role,
        @NotBlank @Size(max = 50) String login,
        @Size(min = 1, max = 255) @Pattern(regexp = "(?s).*\\S.*") String passwordHash,
        @NotNull Boolean active
) {}
