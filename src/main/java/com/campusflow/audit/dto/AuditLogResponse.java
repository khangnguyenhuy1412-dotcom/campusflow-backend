package com.campusflow.audit.dto;

import com.campusflow.audit.entity.AuditAction;
import com.campusflow.audit.entity.AuditLog;
import com.campusflow.user.dto.UserSummaryResponse;
import java.time.Instant;

public record AuditLogResponse(
        Long id,
        UserSummaryResponse actor,
        AuditAction action,
        String targetType,
        Long targetId,
        String details,
        Instant createdAt) {

    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getActor() == null ? null : UserSummaryResponse.from(log.getActor()),
                log.getAction(),
                log.getTargetType(),
                log.getTargetId(),
                log.getDetails(),
                log.getCreatedAt());
    }
}
