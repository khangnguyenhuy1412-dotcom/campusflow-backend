package com.campusflow.admin.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Numbers for the admin dashboard. Dates are in Vietnam time (UTC+7). */
public record AdminStatsResponse(
        Totals totals,
        Map<String, Long> itemsByStatus,
        Map<String, Long> exchangesByStatus,
        List<DailyPoint> last30Days,
        List<NamedCount> topCategories,
        List<NamedCount> topUniversities) {

    public record Totals(long users, long verifiedUsers, long bannedUsers, long openReports,
            long completedExchangesLast7Days) {
    }

    public record DailyPoint(LocalDate date, long newUsers, long newItems, long completedExchanges) {
    }

    public record NamedCount(String name, long count) {
    }
}
