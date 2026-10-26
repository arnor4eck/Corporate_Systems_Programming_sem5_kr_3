package com.arnor4eck.pks_3.dto;

import com.arnor4eck.pks_3.data.entity.*;
import java.util.UUID;
import java.time.LocalDateTime;

public record EmployeeResponse(
        UUID id,
        Status status,
        String fullName,
        Role role,
        String login,
        boolean active,
        LocalDateTime createdAt
) {}
