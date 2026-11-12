package com.campusflow.moderation.service;

import com.campusflow.common.exception.ApiException;
import com.campusflow.item.entity.Item;
import com.campusflow.item.entity.ItemStatus;
import com.campusflow.item.repository.ItemRepository;
import com.campusflow.moderation.dto.CreateReportRequest;
import com.campusflow.moderation.dto.MyReportResponse;
import com.campusflow.moderation.entity.Report;
import com.campusflow.moderation.entity.ReportTargetType;
import com.campusflow.moderation.repository.ReportRepository;
import com.campusflow.user.entity.User;
import com.campusflow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Students report listings or users; moderators handle them in {@link ModerationService}. */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    @Transactional
    public MyReportResponse create(Long userId, CreateReportRequest request) {
        Item item = null;
        User targetUser;
        if (request.targetType() == ReportTargetType.ITEM) {
            item = itemRepository.findWithDetailsById(request.targetId())
                    .filter(found -> found.getStatus() != ItemStatus.DELETED)
                    .orElseThrow(() -> ApiException.notFound("Item not found"));
            if (item.isOwnedBy(userId)) {
                throw ApiException.badRequest("You cannot report your own item");
            }
            targetUser = item.getOwner();
        } else {
            targetUser = userRepository.findById(request.targetId())
                    .orElseThrow(() -> ApiException.notFound("User not found"));
            if (targetUser.getId().equals(userId)) {
                throw ApiException.badRequest("You cannot report yourself");
            }
        }
        if (reportRepository.existsOpenDuplicate(userId, request.targetType(),
                item == null ? null : item.getId(), targetUser.getId())) {
            throw ApiException.conflict("You already reported this; a moderator will review it");
        }

        Report report = reportRepository.save(Report.builder()
                .reporter(userRepository.getReferenceById(userId))
                .targetType(request.targetType())
                .targetItem(item)
                .targetUser(targetUser)
                .reason(request.reason())
                .details(request.details() == null || request.details().isBlank() ? null : request.details().strip())
                .build());
        return MyReportResponse.from(report);
    }
}
