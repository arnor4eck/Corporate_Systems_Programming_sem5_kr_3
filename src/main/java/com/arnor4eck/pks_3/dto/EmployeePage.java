package com.arnor4eck.pks_3.dto;

import java.util.List;

public record EmployeePage(List<EmployeeResponse> content, int page, int size, long totalElements, int totalPages) {}
