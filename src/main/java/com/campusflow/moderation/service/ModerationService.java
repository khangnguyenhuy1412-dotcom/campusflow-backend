package com.campusflow.moderation.service;

import com.campusflow.audit.entity.AuditAction;
import com.campusflow.audit.service.AuditService;
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
        }

        List<Report> closed = new ArrayList<>();
        closed.add(report);
        closed.addAll(reportRepository.findOpenForSameTarget(report.getId(),
                report.getTargetItem() == null ? null : report.getTargetItem().getId(),
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
