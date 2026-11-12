package com.campusflow.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** @param parentId the top-level comment to reply to; null for a new top-level comment */
public record CommentRequest(@NotBlank @Size(max = 1000) String content, Long parentId) {
}
