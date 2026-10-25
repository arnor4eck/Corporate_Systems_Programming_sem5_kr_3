package com.arnor4eck.pks_3.dto;

import jakarta.validation.constraints.*;

public record SectorInput(
        @NotBlank @Size(max = 100) String name
) {}
