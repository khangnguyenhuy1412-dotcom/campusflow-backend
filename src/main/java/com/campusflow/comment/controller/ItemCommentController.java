package com.campusflow.comment.controller;

import com.campusflow.comment.dto.CommentRequest;
import com.campusflow.comment.dto.CommentResponse;
import com.campusflow.comment.service.ItemCommentService;
import com.campusflow.common.dto.PageResponse;
import com.campusflow.common.web.CurrentUserId;
import com.campusflow.ratelimit.RateLimit;
import com.campusflow.ratelimit.RateLimitPolicy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Reading is public like the item itself; writing needs a verified email (SecurityConfig). */
@Tag(name = "Item comments", description = "Public questions and answers under a listing")
@RestController
@RequestMapping("/api/items/{itemId}/comments")
@RequiredArgsConstructor
public class ItemCommentController {

    private final ItemCommentService commentService;

    @Operation(summary = "Comments of a listing, newest first, each with its replies")
    @GetMapping
    public PageResponse<CommentResponse> list(@PathVariable Long itemId,
            @CurrentUserId(required = false) Long userId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return commentService.list(itemId, userId, page, size);
    }

    @Operation(summary = "Comment on a listing, or reply to a comment with parentId")
    @RateLimit(policy = RateLimitPolicy.COMMENT, key = RateLimit.Key.USER)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse create(@CurrentUserId Long userId, @PathVariable Long itemId,
            @Valid @RequestBody CommentRequest request) {
        return commentService.create(userId, itemId, request);
    }

    @Operation(summary = "Delete a comment (its author or the item owner)")
    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentUserId Long userId, @PathVariable Long itemId, @PathVariable Long commentId) {
        commentService.delete(userId, itemId, commentId);
    }
}
