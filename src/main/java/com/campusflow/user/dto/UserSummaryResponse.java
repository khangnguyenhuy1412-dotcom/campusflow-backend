package com.campusflow.user.dto;

import com.campusflow.user.entity.User;
import java.math.BigDecimal;

/** Public view of a user shown on listings and requests. Never contains email or phone number. */
public record UserSummaryResponse(
        Long id,
        String fullName,
        String avatarUrl,
        String university,
        BigDecimal reputationScore,
        int reviewCount,
        boolean verified) {

    public static UserSummaryResponse from(User user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getFullName(),
                user.getAvatarUrl(),
                user.getUniversity(),
                user.getReputationScore(),
                user.getReviewCount(),
                user.isVerified());
    }
}
