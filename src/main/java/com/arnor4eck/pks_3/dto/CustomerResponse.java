package com.arnor4eck.pks_3.dto;

import com.arnor4eck.pks_3.domain.*;
import java.util.UUID;
import java.time.LocalDateTime;

public record CustomerResponse(
        UUID id,
        Status status,
        String fullName,
        String phone,
        String email,
        LocalDateTime createdAt,
        java.time.LocalDateTime updatedAt, Long version
) {}
