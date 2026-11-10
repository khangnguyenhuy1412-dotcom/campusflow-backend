package com.campusflow.moderation.dto;

import com.campusflow.moderation.entity.Report;
import com.campusflow.moderation.entity.ReportReason;
import com.campusflow.moderation.entity.ReportStatus;
import com.campusflow.moderation.entity.ReportTargetType;
import java.time.Instant;

/** What a reporter sees about their own report (no moderator details). */
public record MyReportResponse(
        Long id, ReportTargetType targetType, ReportReason reason, ReportStatus status, Instant createdAt) {

    public static MyReportResponse from(Report report) {
        return new MyReportResponse(
                report.getId(), report.getTargetType(), report.getReason(), report.getStatus(), report.getCreatedAt());
    }
}
