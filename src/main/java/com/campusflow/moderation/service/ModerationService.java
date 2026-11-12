package com.campusflow.moderation.service;

import com.campusflow.audit.entity.AuditAction;
import com.campusflow.audit.service.AuditService;
import com.campusflow.comment.entity.ItemComment;
import com.campusflow.comment.repository.ItemCommentRepository;
import com.campusflow.common.dto.PageQuery;
import com.campusflow.common.dto.PageResponse;
import com.campusflow.common.exception.ApiException;
import com.campusflow.exchange.service.ExchangeService;
import com.campusflow.item.entity.Item;
import com.campusflow.item.entity.ItemStatus;
import com.campusflow.item.repository.ItemRepository;
import com.campusflow.moderation.dto.ReportDetailResponse;
import com.campusflow.moderation.dto.ResolveReportRequest;
import com.campusflow.moderation.entity.ModerationAction;
import com.campusflow.moderation.entity.Report;
import com.campusflow.moderation.entity.ReportStatus;
import com.campusflow.moderation.entity.ReportTargetType;
import com.campusflow.moderation.event.ItemRemovedEvent;
import com.campusflow.moderation.event.ReportHandledEvent;
import com.campusflow.moderation.repository.ReportRepository;
import com.campusflow.review.entity.Review;
import com.campusflow.review.event.ReviewHiddenEvent;
import com.campusflow.review.repository.ReviewRepository;
import com.campusflow.review.service.ReviewService;
import com.campusflow.user.entity.User;
import com.campusflow.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Work of moderators (ROLE_MODERATOR or ROLE_ADMIN): the report queue and actions on listings.
 * Every action is written to the audit log in the same transaction.
 */
@Service
@RequiredArgsConstructor
public class ModerationService {

    private final ReportRepository reportRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final ExchangeService exchangeService;
    private final AccountBanService accountBanService;
    private final AuditService auditService;
    private final ApplicationEventPublisher events;
    private final ReviewRepository reviewRepository;
    private final ReviewService reviewService;
    private final ItemCommentRepository commentRepository;

    @Transactional(readOnly = true)
    public PageResponse<ReportDetailResponse> queue(ReportStatus status, Integer page, Integer size) {
        // Oldest open reports first, so nothing waits forever
        Sort sort = status == ReportStatus.OPEN
                ? Sort.by(Sort.Direction.ASC, "createdAt", "id")
                : Sort.by(Sort.Direction.DESC, "createdAt", "id");
        return PageResponse.of(reportRepository.findQueue(status, PageQuery.of(page, size, sort)),
                ReportDetailResponse::from);
    }

    @Transactional(readOnly = true)
    public ReportDetailResponse get(Long reportId) {
        return ReportDetailResponse.from(getReport(reportId));
    }

    /**
     * Takes the chosen action and closes this report together with every other open report about
     * the same listing (or user), so the same problem is not handled twice.
     */
    @Transactional
    public ReportDetailResponse resolve(Long moderatorId, Long reportId, ResolveReportRequest request) {
        Report report = getOpenReport(reportId);
        switch (request.action()) {
            case NONE -> { }
            case REMOVE_ITEM -> {
                if (report.getTargetType() != ReportTargetType.ITEM) {
                    throw ApiException.badRequest("REMOVE_ITEM is only possible for reports about a listing");
                }
                removeItem(moderatorId, report.getTargetItem().getId(), request.note());
            }
            case BAN_USER -> accountBanService.ban(moderatorId, report.getTargetUser().getId(), request.note());
            case HIDE_COMMENT -> {
                if (report.getTargetType() != ReportTargetType.COMMENT) {
                    throw ApiException.badRequest("HIDE_COMMENT is only possible for reports about a comment");
                }
                hideComment(moderatorId, report.getTargetComment().getId(), request.note());
            }
            case HIDE_REVIEW -> {
                if (report.getTargetType() != ReportTargetType.REVIEW) {
                    throw ApiException.badRequest("HIDE_REVIEW is only possible for reports about a review");
                }
                hideReview(moderatorId, report.getTargetReview().getId(), request.note());
            }
        }

        List<Report> closed = new ArrayList<>();
        closed.add(report);
        closed.addAll(reportRepository.findOpenForSameTarget(report.getId(),
                report.getTargetItem() == null ? null : report.getTargetItem().getId(),
                report.getTargetReview() == null ? null : report.getTargetReview().getId(),
                report.getTargetComment() == null ? null : report.getTargetComment().getId(),
                report.getTargetUser().getId()));
        User moderator = userRepository.getReferenceById(moderatorId);
        for (Report each : closed) {
            each.close(ReportStatus.RESOLVED, request.action(), request.note(), moderator);
            events.publishEvent(new ReportHandledEvent(each));
        }
        auditService.record(moderatorId, AuditAction.REPORT_RESOLVED, "REPORT", report.getId(),
                "action=" + request.action() + ", closed " + closed.size() + " report(s)"
                        + (request.note() == null ? "" : ", note: " + request.note()));
        return ReportDetailResponse.from(report);
    }

    @Transactional
    public ReportDetailResponse dismiss(Long moderatorId, Long reportId, String note) {
        Report report = getOpenReport(reportId);
        report.close(ReportStatus.DISMISSED, ModerationAction.NONE, note, userRepository.getReferenceById(moderatorId));
        events.publishEvent(new ReportHandledEvent(report));
        auditService.record(moderatorId, AuditAction.REPORT_DISMISSED, "REPORT", report.getId(), note);
        return ReportDetailResponse.from(report);
    }

    /** Takes a listing off the marketplace, ends its open deals and tells the owner why. */
    @Transactional
    public void removeItem(Long moderatorId, Long itemId, String reason) {
        Item item = itemRepository.findWithDetailsById(itemId).orElseThrow(() -> ApiException.notFound("Item not found"));
        if (item.getStatus() == ItemStatus.REMOVED) {
            return;
        }
        exchangeService.closeAllFor(item);
        item.setStatus(ItemStatus.REMOVED);
        events.publishEvent(new ItemRemovedEvent(item, reason));
        auditService.record(moderatorId, AuditAction.ITEM_REMOVED, "ITEM", item.getId(), reason);
    }

    /** Puts a removed listing back as HIDDEN; the owner decides when to show it again. */
    @Transactional
    public void restoreItem(Long moderatorId, Long itemId) {
        Item item = itemRepository.findWithDetailsById(itemId).orElseThrow(() -> ApiException.notFound("Item not found"));
        if (item.getStatus() != ItemStatus.REMOVED) {
            throw ApiException.conflict("Only removed items can be restored");
        }
        item.setStatus(ItemStatus.HIDDEN);
        auditService.record(moderatorId, AuditAction.ITEM_RESTORED, "ITEM", item.getId(), null);
    }

    /**
     * Hides a review (e.g. it is false or abusive): it disappears from the profile and the reviewed
     * person's reputation is recalculated without it. The author is told why.
     */
    @Transactional
    public void hideReview(Long moderatorId, Long reviewId, String reason) {
        Review review = reviewRepository.findWithDetailsById(reviewId)
                .orElseThrow(() -> ApiException.notFound("Review not found"));
        if (review.isHidden()) {
            return;
        }
        review.setHidden(true);
        reviewRepository.flush();
        reviewService.recalculateReputation(review.getReviewee());
        events.publishEvent(new ReviewHiddenEvent(review, reason));
        auditService.record(moderatorId, AuditAction.REVIEW_HIDDEN, "REVIEW", review.getId(), reason);
    }

    @Transactional
    public void restoreReview(Long moderatorId, Long reviewId) {
        Review review = reviewRepository.findWithDetailsById(reviewId)
                .orElseThrow(() -> ApiException.notFound("Review not found"));
        if (!review.isHidden()) {
            throw ApiException.conflict("Only hidden reviews can be restored");
        }
        review.setHidden(false);
        reviewRepository.flush();
        reviewService.recalculateReputation(review.getReviewee());
        auditService.record(moderatorId, AuditAction.REVIEW_RESTORED, "REVIEW", review.getId(), null);
    }

    @Transactional
    public void hideComment(Long moderatorId, Long commentId, String reason) {
        ItemComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> ApiException.notFound("Comment not found"));
        if (!comment.isHidden()) {
            comment.setHidden(true);
            auditService.record(moderatorId, AuditAction.COMMENT_HIDDEN, "COMMENT", commentId, reason);
        }
    }

    @Transactional
    public void restoreComment(Long moderatorId, Long commentId) {
        ItemComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> ApiException.notFound("Comment not found"));
        if (!comment.isHidden()) {
            throw ApiException.conflict("Only hidden comments can be restored");
        }
        comment.setHidden(false);
        auditService.record(moderatorId, AuditAction.COMMENT_RESTORED, "COMMENT", commentId, null);
    }

    private Report getReport(Long reportId) {
        return reportRepository.findWithDetailsById(reportId)
                .orElseThrow(() -> ApiException.notFound("Report not found"));
    }

    private Report getOpenReport(Long reportId) {
        Report report = getReport(reportId);
        if (!report.isOpen()) {
            throw ApiException.conflict("This report was already handled");
        }
        return report;
    }
}
