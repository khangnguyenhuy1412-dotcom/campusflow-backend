package com.campusflow.notification.entity;

public enum NotificationType {
    EXCHANGE_REQUESTED(true),
    EXCHANGE_ACCEPTED(true),
    EXCHANGE_REJECTED(false),
    /** Emailed: the other person may be on the way to a meeting. */
    EXCHANGE_CANCELLED(true),
    EXCHANGE_COMPLETED(false),
    REVIEW_RECEIVED(false);

    private final boolean emailed;

    NotificationType(boolean emailed) {
        this.emailed = emailed;
    }

    /** Important types are also emailed, unless the user turned email notifications off. */
    public boolean isEmailed() {
        return emailed;
    }
}
