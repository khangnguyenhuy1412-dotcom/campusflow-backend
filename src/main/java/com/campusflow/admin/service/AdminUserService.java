package com.campusflow.admin.service;

import com.campusflow.admin.dto.AdminUserResponse;
import com.campusflow.admin.dto.AdminUserStatus;
import com.campusflow.audit.entity.AuditAction;
import com.campusflow.audit.service.AuditService;
import com.campusflow.auth.repository.RefreshTokenRepository;
import com.campusflow.common.dto.PageQuery;
import com.campusflow.common.dto.PageResponse;
import com.campusflow.common.exception.ApiException;
import com.campusflow.common.util.SqlLike;
import com.campusflow.user.entity.Role;
import com.campusflow.user.entity.User;
import com.campusflow.user.repository.RoleRepository;
import com.campusflow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> search(String query, AdminUserStatus status, Integer page, Integer size) {
        String pattern = query == null || query.isBlank() ? null : SqlLike.contains(query);
        Boolean active = status == null ? null : status == AdminUserStatus.ACTIVE;
        return PageResponse.of(
                userRepository.search(pattern, active, PageQuery.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"))),
                AdminUserResponse::from);
    }

    /**
     * Grants or removes ROLE_MODERATOR. Roles are part of the access token, so the user's refresh
     * tokens are revoked: the change applies at their next login (old access tokens expire within 15 min).
     */
    @Transactional
    public AdminUserResponse setModerator(Long adminId, Long userId, boolean moderator) {
        User user = userRepository.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        Role role = roleRepository.findByName(Role.MODERATOR)
                .orElseThrow(() -> new IllegalStateException(Role.MODERATOR + " is missing; check the seed migration"));
        boolean changed = moderator ? user.getRoles().add(role) : user.getRoles().remove(role);
        if (changed) {
            refreshTokenRepository.revokeAllByUserId(userId);
            auditService.record(adminId, moderator ? AuditAction.MODERATOR_GRANTED : AuditAction.MODERATOR_REVOKED,
                    "USER", userId, null);
        }
        return AdminUserResponse.from(user);
    }
}
