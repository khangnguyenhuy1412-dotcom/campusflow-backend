package com.campusflow.comment.event;

import com.campusflow.comment.entity.ItemComment;

/** A comment (or reply) was posted; the item owner and the replied-to author are told. */
public record CommentCreatedEvent(ItemComment comment) {
}
