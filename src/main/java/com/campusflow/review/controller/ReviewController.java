package com.campusflow.review.controller;

import com.campusflow.common.web.CurrentUserId;
import com.campusflow.review.dto.ReviewRequest;
import com.campusflow.review.dto.ReviewResponse;
import com.campusflow.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Reviews of a user are listed under /api/users/{id}/reviews (UserController). */
@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/api/exchange-requests/{id}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse create(@CurrentUserId Long userId, @PathVariable Long id,
            @Valid @RequestBody ReviewRequest request) {
        return reviewService.create(userId, id, request);
    }
}
