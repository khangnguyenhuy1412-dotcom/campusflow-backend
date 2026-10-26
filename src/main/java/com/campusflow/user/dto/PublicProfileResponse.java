package com.campusflow.user.dto;

import com.campusflow.user.entity.User;
import java.math.BigDecimal;
import java.time.Instant;

/** Profile page of another user. Contact details are only shared inside an accepted exchange. */
public record PublicProfileResponse(
        Long id,
        String fullName,
        String avatarUrl,
        String university,
        BigDecimal reputationScore,
        int reviewCount,
        boolean verified,
        Instant joinedAt) {

    public static PublicProfileResponse from(User user) {
        return new PublicProfileResponse(
                user.getId(),
                user.getFullName(),
                user.getAvatarUrl(),
                user.getUniversity(),
                user.getReputationScore(),
                user.getReviewCount(),
                user.isVerified(),
                user.getCreatedAt());
    }
}
