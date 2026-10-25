package com.arnor4eck.pks_3.dto;

import jakarta.validation.constraints.*;

public record CustomerInput(
        @NotBlank @Size(max = 150) String fullName,
        @NotBlank @Size(max = 20) String phone,
        @Email @Size(max = 100) String email
) {}
