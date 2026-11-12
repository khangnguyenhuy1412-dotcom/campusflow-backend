package com.campusflow.wanted.controller;

import com.campusflow.common.dto.PageResponse;
import com.campusflow.common.web.CurrentUserId;
import com.campusflow.item.dto.ItemSummaryResponse;
import com.campusflow.ratelimit.RateLimit;
import com.campusflow.ratelimit.RateLimitPolicy;
import com.campusflow.wanted.dto.CloseWantedPostRequest;
import com.campusflow.wanted.dto.WantedPostRequest;
import com.campusflow.wanted.dto.WantedPostResponse;
import com.campusflow.wanted.service.WantedPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The board and single posts are public; writes need a verified email (SecurityConfig). */
@Tag(name = "Wanted (Cần tìm)", description = "Post what you need; you are notified when a matching item is listed")
@RestController
@RequiredArgsConstructor
public class WantedPostController {

    private final WantedPostService wantedPostService;

    @Operation(summary = "Public board of open posts, newest first")
    @GetMapping("/api/wanted-posts")
    public PageResponse<WantedPostResponse> board(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long universityId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return wantedPostService.board(categoryId, universityId, page, size);
    }

    @Operation(summary = "A single post")
    @GetMapping("/api/wanted-posts/{id}")
    public WantedPostResponse get(@PathVariable Long id) {
        return wantedPostService.get(id);
    }

    @Operation(summary = "Post what you are looking for (open for 30 days, at most 10 open posts)")
    @RateLimit(policy = RateLimitPolicy.CREATE_CONTENT, key = RateLimit.Key.USER)
    @PostMapping("/api/wanted-posts")
    @ResponseStatus(HttpStatus.CREATED)
    public WantedPostResponse create(@CurrentUserId Long userId, @Valid @RequestBody WantedPostRequest request) {
        return wantedPostService.create(userId, request);
    }

    @Operation(summary = "Update your open post")
    @PutMapping("/api/wanted-posts/{id}")
    public WantedPostResponse update(@CurrentUserId Long userId, @PathVariable Long id,
            @Valid @RequestBody WantedPostRequest request) {
        return wantedPostService.update(userId, id, request);
    }

    @Operation(summary = "Close your post (fulfilled = you found it)")
    @PostMapping("/api/wanted-posts/{id}/close")
    public WantedPostResponse close(@CurrentUserId Long userId, @PathVariable Long id,
            @Valid @RequestBody CloseWantedPostRequest request) {
        return wantedPostService.close(userId, id, request.fulfilled());
    }

    @Operation(summary = "Listings that match your post right now (owner only)")
    @GetMapping("/api/wanted-posts/{id}/matches")
    public List<ItemSummaryResponse> matches(@CurrentUserId Long userId, @PathVariable Long id) {
        return wantedPostService.currentMatches(userId, id);
    }

    @Operation(summary = "Your posts, any status")
    @GetMapping("/api/users/me/wanted-posts")
    public PageResponse<WantedPostResponse> mine(@CurrentUserId Long userId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return wantedPostService.mine(userId, page, size);
    }
}
