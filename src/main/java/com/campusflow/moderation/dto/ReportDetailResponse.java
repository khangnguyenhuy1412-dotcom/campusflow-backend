package com.campusflow.moderation.dto;

import com.campusflow.moderation.entity.ModerationAction;
import com.campusflow.moderation.entity.Report;
import com.campusflow.moderation.entity.ReportReason;
import com.campusflow.moderation.entity.ReportStatus;
import com.campusflow.moderation.entity.ReportTargetType;
import com.campusflow.user.dto.UserSummaryResponse;
import java.time.Instant;

/** A report in the moderation queue. */
public record ReportDetailResponse(
        Long id,
        ReportTargetType targetType,
        Long targetItemId,
        String targetItemTitle,
        Long targetReviewId,
        Integer targetReviewRating,
        String targetReviewComment,
        Long targetCommentId,
        String targetCommentContent,
        UserSummaryResponse targetUser,
        UserSummaryResponse reporter,
        ReportReason reason,
        String details,
        ReportStatus status,
        ModerationAction action,
        String resolutionNote,
        UserSummaryResponse handledBy,
        Instant handledAt,
        Instant createdAt) {

    public static ReportDetailResponse from(Report report) {
        return new ReportDetailResponse(
                report.getId(),
                report.getTargetType(),
                report.getTargetItem() == null ? null : report.getTargetItem().getId(),
                report.getTargetItem() == null ? null : report.getTargetItem().getTitle(),
                report.getTargetReview() == null ? null : report.getTargetReview().getId(),
                report.getTargetReview() == null ? null : report.getTargetReview().getRating(),
                report.getTargetReview() == null ? null : report.getTargetReview().getComment(),
                report.getTargetComment() == null ? null : report.getTargetComment().getId(),
                report.getTargetComment() == null ? null : report.getTargetComment().getContent(),
                UserSummaryResponse.from(report.getTargetUser()),
                UserSummaryResponse.from(report.getReporter()),
                report.getReason(),
                report.getDetails(),
                report.getStatus(),
                report.getAction(),
                report.getResolutionNote(),
                report.getHandledBy() == null ? null : UserSummaryResponse.from(report.getHandledBy()),
                report.getHandledAt(),
                report.getCreatedAt());
    }
}
