package com.campusflow.wanted.dto;

import com.campusflow.category.dto.CategoryResponse;
import com.campusflow.user.dto.UserSummaryResponse;
import com.campusflow.wanted.entity.WantedPost;
import com.campusflow.wanted.entity.WantedStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record WantedPostResponse(
        Long id,
        String title,
        String keywords,
        String description,
        CategoryResponse category,
        BigDecimal maxPrice,
        boolean sameUniversityOnly,
        WantedStatus status,
        Instant expiresAt,
        UserSummaryResponse user,
        Instant createdAt) {

    public static WantedPostResponse from(WantedPost post) {
        return new WantedPostResponse(
                post.getId(),
                post.getTitle(),
                post.getKeywords(),
                post.getDescription(),
                post.getCategory() == null ? null : CategoryResponse.from(post.getCategory()),
                post.getMaxPrice(),
                post.isSameUniversityOnly(),
                post.effectiveStatus(),
                post.getExpiresAt(),
                UserSummaryResponse.from(post.getUser()),
                post.getCreatedAt());
    }
}
