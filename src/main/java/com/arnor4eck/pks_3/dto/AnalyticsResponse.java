package com.arnor4eck.pks_3.dto;

import java.math.BigDecimal;
import java.util.List;

public record AnalyticsResponse(
        Counts customers, Counts employees, Counts sectors, Counts plots, Counts requests,
        long enabledEmployees, long disabledEmployees,
        BigDecimal totalRequestCost, BigDecimal averageRequestCost, BigDecimal occupiedPlotPercent,
        List<Group> requestsByStatus, List<Group> plotsByStatus, List<Group> requestsBySector,
        long urgentRequests, long ordinaryRequests, List<Group> requestsByPriority) {
    public record Counts(long total, long active, long archived) {}
    public record Group(String key, String label, long count) {}
}
