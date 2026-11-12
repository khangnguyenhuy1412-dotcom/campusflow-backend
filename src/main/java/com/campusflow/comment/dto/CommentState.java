package com.campusflow.comment.dto;

/** DELETED and HIDDEN comments keep their place in the thread, without content or author. */
public enum CommentState {
    VISIBLE,
    DELETED,
    HIDDEN
}
