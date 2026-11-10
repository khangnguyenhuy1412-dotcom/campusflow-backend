package com.campusflow.admin.service;

import com.campusflow.admin.dto.AdminStatsResponse;
import com.campusflow.admin.dto.AdminStatsResponse.DailyPoint;
import com.campusflow.admin.dto.AdminStatsResponse.NamedCount;
import com.campusflow.admin.dto.AdminStatsResponse.Totals;
import com.campusflow.moderation.entity.ReportStatus;
import com.campusflow.moderation.repository.ReportRepository;
import jakarta.persistence.EntityManager;
import java.sql.Date;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dashboard numbers computed with a few aggregate SQL queries (no entities are loaded). Dates are
 * grouped in Vietnam time: timestamps are stored in UTC, so they are shifted by +7 hours first.
 */
@Service
@RequiredArgsConstructor
public class AdminStatsService {

    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int DAYS = 30;
    private static final int TOP = 5;
    private static final String LOCAL_DATE = "DATE(CONVERT_TZ(%s, '+00:00', '+07:00'))";

    private final EntityManager entityManager;
    private final ReportRepository reportRepository;

    @Transactional(readOnly = true)
    public AdminStatsResponse stats() {
        Object[] users = (Object[]) entityManager.createNativeQuery(
                "SELECT COUNT(*), COALESCE(SUM(is_verified), 0), COALESCE(SUM(NOT is_active), 0) FROM users")
                .getSingleResult();
        Instant weekAgo = Instant.now().minus(Duration.ofDays(7));
        long completedLastWeek = number(entityManager.createNativeQuery(
                        "SELECT COUNT(*) FROM exchange_requests WHERE status = 'COMPLETED' AND updated_at >= :since")
                .setParameter("since", weekAgo)
                .getSingleResult());

        Totals totals = new Totals(number(users[0]), number(users[1]), number(users[2]),
                reportRepository.countByStatus(ReportStatus.OPEN), completedLastWeek);
        return new AdminStatsResponse(
                totals,
                countByStatus("items"),
                countByStatus("exchange_requests"),
                last30Days(),
                namedCounts("""
                        SELECT c.name, COUNT(*) FROM items i JOIN categories c ON c.id = i.category_id
                        WHERE i.status = 'AVAILABLE' GROUP BY c.name ORDER BY COUNT(*) DESC, c.name LIMIT %d
                        """.formatted(TOP)),
                namedCounts("""
                        SELECT university, COUNT(*) FROM users WHERE university IS NOT NULL
                        GROUP BY university ORDER BY COUNT(*) DESC, university LIMIT %d
                        """.formatted(TOP)));
    }

    /** Table names are constants from this class, never user input. */
    private Map<String, Long> countByStatus(String table) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (Object row : entityManager.createNativeQuery(
                "SELECT status, COUNT(*) FROM " + table + " GROUP BY status ORDER BY status").getResultList()) {
            Object[] columns = (Object[]) row;
            result.put((String) columns[0], number(columns[1]));
        }
        return result;
    }

    private List<DailyPoint> last30Days() {
        LocalDate today = LocalDate.now(VIETNAM);
        LocalDate firstDay = today.minusDays(DAYS - 1L);
        Instant since = firstDay.atStartOfDay(VIETNAM).toInstant();
        Map<LocalDate, Long> users = dailyCounts("users", "created_at", "", since);
        Map<LocalDate, Long> items = dailyCounts("items", "created_at", "", since);
        Map<LocalDate, Long> completed = dailyCounts("exchange_requests", "updated_at", "AND status = 'COMPLETED'", since);

        List<DailyPoint> points = new ArrayList<>(DAYS);
        for (LocalDate day = firstDay; !day.isAfter(today); day = day.plusDays(1)) {
            points.add(new DailyPoint(day, users.getOrDefault(day, 0L), items.getOrDefault(day, 0L),
                    completed.getOrDefault(day, 0L)));
        }
        return points;
    }

    private Map<LocalDate, Long> dailyCounts(String table, String column, String extraCondition, Instant since) {
        String day = LOCAL_DATE.formatted(column);
        String sql = "SELECT %s AS day, COUNT(*) FROM %s WHERE %s >= :since %s GROUP BY day"
                .formatted(day, table, column, extraCondition);
        Map<LocalDate, Long> counts = new HashMap<>();
        for (Object row : entityManager.createNativeQuery(sql).setParameter("since", since).getResultList()) {
            Object[] columns = (Object[]) row;
            counts.put(toLocalDate(columns[0]), number(columns[1]));
        }
        return counts;
    }

    private List<NamedCount> namedCounts(String sql) {
        List<NamedCount> result = new ArrayList<>();
        for (Object row : entityManager.createNativeQuery(sql).getResultList()) {
            Object[] columns = (Object[]) row;
            result.add(new NamedCount((String) columns[0], number(columns[1])));
        }
        return result;
    }

    private static LocalDate toLocalDate(Object value) {
        return value instanceof LocalDate date ? date : ((Date) value).toLocalDate();
    }

    private static long number(Object value) {
        return value == null ? 0 : ((Number) value).longValue();
    }
}
