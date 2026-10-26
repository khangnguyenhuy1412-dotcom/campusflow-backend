package com.campusflow.user.dto;

import com.campusflow.user.entity.Role;
import com.campusflow.user.entity.User;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record UserResponse(
        Long id,
        String email,
        String fullName,
        String studentId,
        String university,
        String phoneNumber,
        String avatarUrl,
        BigDecimal reputationScore,
        int reviewCount,
        boolean verified,
        List<String> roles,
        Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getStudentId(),
                user.getUniversity(),
                user.getPhoneNumber(),
                user.getAvatarUrl(),
                user.getReputationScore(),
                user.getReviewCount(),
                user.isVerified(),
                user.getRoles().stream().map(Role::getName).sorted().toList(),
                user.getCreatedAt());
    }
}
