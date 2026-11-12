package com.campusflow.review.dto;

import com.campusflow.review.entity.Review;
import com.campusflow.user.dto.UserSummaryResponse;
import java.time.Instant;

public record ReviewResponse(
        Long id,
        int rating,
        String comment,
        UserSummaryResponse reviewer,
        Long exchangeRequestId,
        String itemTitle,
        String reply,
        Instant repliedAt,
        Instant createdAt) {

    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getRating(),
                review.getComment(),
                UserSummaryResponse.from(review.getReviewer()),
                review.getExchangeRequest().getId(),
                review.getExchangeRequest().getItem().getTitle(),
                review.getReply(),
                review.getRepliedAt(),
                review.getCreatedAt());
    }
}
