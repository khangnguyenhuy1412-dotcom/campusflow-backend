package com.campusflow.review.controller;

import com.campusflow.common.web.CurrentUserId;
import com.campusflow.ratelimit.RateLimit;
import com.campusflow.ratelimit.RateLimitPolicy;
import com.campusflow.review.dto.ReviewRequest;
import com.campusflow.review.dto.ReviewResponse;
import com.campusflow.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Reviews of a user are listed under /api/users/{id}/reviews (UserController). */
@Tag(name = "Reviews", description = "Rate the other party after a completed exchange")
@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "Review the other participant of a completed exchange")
    @RateLimit(policy = RateLimitPolicy.CREATE_CONTENT, key = RateLimit.Key.USER)
    @PostMapping("/api/exchange-requests/{id}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse create(@CurrentUserId Long userId, @PathVariable Long id,
            @Valid @RequestBody ReviewRequest request) {
        return reviewService.create(userId, id, request);
    }
}
