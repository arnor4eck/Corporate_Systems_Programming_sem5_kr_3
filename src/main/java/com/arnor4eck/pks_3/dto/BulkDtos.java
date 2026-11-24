package com.arnor4eck.pks_3.dto;

import com.arnor4eck.pks_3.domain.RequestStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;

public final class BulkDtos {
    private BulkDtos() {}
    public enum Operation { ARCHIVE, RESTORE, SET_STATUS, ADD_TAG, REMOVE_TAG }
    public record Item(@NotNull UUID id, @NotNull @PositiveOrZero Long version) {}
    public record Input(@NotNull Operation operation, @NotEmpty @Size(max=100) List<@Valid Item> items, RequestStatus requestStatus, UUID tagId) {}
    public record Result(UUID id, boolean success, int status, String error, RequestResponse record) {}
    public record Response(int succeeded, int failed, List<Result> results) {}
}
