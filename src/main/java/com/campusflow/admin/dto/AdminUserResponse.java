package com.campusflow.admin.dto;

import com.campusflow.user.entity.Role;
import com.campusflow.user.entity.User;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** A user as administrators see it, including account status. */
public record AdminUserResponse(
        Long id,
        String email,
        String fullName,
        String university,
        boolean verified,
        boolean active,
        Instant bannedAt,
        String banReason,
        List<String> roles,
        BigDecimal reputationScore,
        int reviewCount,
        Instant createdAt) {

    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getUniversityName(),
                user.isVerified(),
                user.isActive(),
                user.getBannedAt(),
                user.getBanReason(),
                user.getRoles().stream().map(Role::getName).sorted().toList(),
                user.getReputationScore(),
                user.getReviewCount(),
                user.getCreatedAt());
    }
}
