package com.arnor4eck.pks_3.dto;

import java.util.List;
import java.util.UUID;

public final class CsvDtos {
    private CsvDtos() {}
    public enum Mode { NEW_ONLY, UPSERT }
    public record RowError(long row, List<String> errors) {}
    public record Preview(int totalRows, int validRows, int invalidRows, List<RowError> errors, String previewToken) {}
    public record RowResult(long row, boolean success, UUID id, String error) {}
    public record Applied(int totalRows, int succeeded, int failed, List<RowResult> results) {}
}
