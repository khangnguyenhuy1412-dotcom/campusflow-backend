package com.campusflow.moderation.service;

import com.campusflow.audit.entity.AuditAction;
import com.campusflow.audit.service.AuditService;
import com.campusflow.auth.repository.RefreshTokenRepository;
import com.campusflow.common.exception.ApiException;
import com.campusflow.notification.mail.NotificationEmailEvent;
import com.campusflow.security.AccountStatusService;
import com.campusflow.user.entity.Role;
import com.campusflow.user.entity.User;
import com.campusflow.user.repository.UserRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bans and unbans accounts. A ban takes effect immediately: refresh tokens are revoked, and the
 * account-status cache is cleared so the next request with a still-valid access token is refused.
 */
@Service
@RequiredArgsConstructor
public class AccountBanService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AccountStatusService accountStatusService;
    private final AuditService auditService;
    private final ApplicationEventPublisher events;

    @Transactional
    public void ban(Long actorId, Long targetUserId, String reason) {
        User actor = getUser(actorId);
        User target = getUser(targetUserId);
        if (actor.getId().equals(target.getId())) {
            throw ApiException.badRequest("You cannot ban yourself");
        }
        if (hasRole(target, Role.ADMIN)) {
            throw ApiException.forbidden("Administrators cannot be banned");
        }
        if (hasRole(target, Role.MODERATOR) && !hasRole(actor, Role.ADMIN)) {
            throw ApiException.forbidden("Only administrators can ban moderators");
        }
        if (!target.isActive()) {
            return;
        }
        String cleanReason = reason == null || reason.isBlank() ? "Vi phạm quy định cộng đồng" : reason.strip();
        target.setActive(false);
        target.setBannedAt(Instant.now());
        target.setBanReason(cleanReason);
        refreshTokenRepository.revokeAllByUserId(target.getId());
        accountStatusService.evictAfterCommit(target.getId());
        auditService.record(actorId, AuditAction.USER_BANNED, "USER", target.getId(), cleanReason);
        // Sent regardless of notification settings: the user can no longer log in to see why
        events.publishEvent(new NotificationEmailEvent(target.getEmail(), target.getFullName(),
                "Tài khoản đã bị khóa",
                "Tài khoản CampusFlow của bạn đã bị khóa. Lý do: " + cleanReason
                        + ". Nếu bạn cho rằng đây là nhầm lẫn, hãy phản hồi email này."));
    }

    @Transactional
    public void unban(Long actorId, Long targetUserId) {
        User target = getUser(targetUserId);
        if (target.isActive()) {
            return;
        }
        target.setActive(true);
        target.setBannedAt(null);
        target.setBanReason(null);
        accountStatusService.evictAfterCommit(target.getId());
        auditService.record(actorId, AuditAction.USER_UNBANNED, "USER", target.getId(), null);
    }

    private User getUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> ApiException.notFound("User not found"));
    }

    static boolean hasRole(User user, String roleName) {
        return user.getRoles().stream().anyMatch(role -> role.getName().equals(roleName));
    }
}
