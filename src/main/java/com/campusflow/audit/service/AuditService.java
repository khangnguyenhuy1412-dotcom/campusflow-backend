package com.campusflow.audit.service;

import com.campusflow.audit.dto.AuditLogResponse;
import com.campusflow.audit.entity.AuditAction;
import com.campusflow.audit.entity.AuditLog;
import com.campusflow.audit.repository.AuditLogRepository;
import com.campusflow.common.dto.PageQuery;
import com.campusflow.common.dto.PageResponse;
import com.campusflow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditService {

    private static final int MAX_DETAILS = 1000;

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    /** Records an action in the caller's transaction, so it is only logged if the action is committed. */
    @Transactional
    public void record(Long actorId, AuditAction action, String targetType, Long targetId, String details) {
        auditLogRepository.save(AuditLog.builder()
                .actor(actorId == null ? null : userRepository.getReferenceById(actorId))
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .details(details == null || details.length() <= MAX_DETAILS ? details : details.substring(0, MAX_DETAILS))
                .build());
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> list(Integer page, Integer size) {
        return PageResponse.of(
                auditLogRepository.findAll(PageQuery.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"))),
                AuditLogResponse::from);
    }
}
