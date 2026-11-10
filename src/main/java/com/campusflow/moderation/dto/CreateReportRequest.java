package com.campusflow.moderation.dto;

import com.campusflow.moderation.entity.ReportReason;
import com.campusflow.moderation.entity.ReportTargetType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** @param targetId item id for ITEM reports, user id for USER reports */
public record CreateReportRequest(
        @NotNull ReportTargetType targetType,
        @NotNull Long targetId,
        @NotNull ReportReason reason,
        @Size(max = 1000) String details) {
}
