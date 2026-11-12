package com.campusflow.comment.dto;

import com.campusflow.comment.entity.ItemComment;
import com.campusflow.user.dto.UserSummaryResponse;
import java.time.Instant;
import java.util.List;

/**
 * @param seller  true when the author is the item owner ("Người bán" badge)
 * @param replies replies of a top-level comment, oldest first (always empty for a reply)
 */
public record CommentResponse(
        Long id,
        Long parentId,
        CommentState state,
        String content,
        UserSummaryResponse author,
        boolean seller,
        Instant createdAt,
        List<CommentResponse> replies) {

    public static CommentResponse from(ItemComment comment, List<CommentResponse> replies) {
        CommentState state = comment.isHidden() ? CommentState.HIDDEN
                : comment.isDeleted() ? CommentState.DELETED : CommentState.VISIBLE;
        boolean visible = state == CommentState.VISIBLE;
        return new CommentResponse(
                comment.getId(),
                comment.getParent() == null ? null : comment.getParent().getId(),
                state,
                visible ? comment.getContent() : null,
                visible ? UserSummaryResponse.from(comment.getAuthor()) : null,
                visible && comment.getItem().isOwnedBy(comment.getAuthor().getId()),
                comment.getCreatedAt(),
                replies);
    }
}
