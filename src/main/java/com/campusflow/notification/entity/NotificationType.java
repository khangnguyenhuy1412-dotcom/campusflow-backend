package com.campusflow.notification.entity;

public enum NotificationType {
    EXCHANGE_REQUESTED(true),
    EXCHANGE_ACCEPTED(true),
    EXCHANGE_REJECTED(false),
    /** Emailed: the other person may be on the way to a meeting. */
    EXCHANGE_CANCELLED(true),
    EXCHANGE_COMPLETED(false),
    REVIEW_RECEIVED(false),
    /** Tells a reporter that their report was handled. */
    REPORT_RESOLVED(false),
    /** Tells an owner that a moderator removed their listing. */
    ITEM_REMOVED(true),
    /** A new listing matches the user's "Cần tìm" post. */
    WANTED_MATCH(true),
    /** The reviewed person answered the user's review. */
    REVIEW_REPLIED(false),
    /** A moderator hid the user's review. */
    REVIEW_HIDDEN(false),
    /** The moderation team wrote in a support ticket. */
    SUPPORT_REPLY(true);

    private final boolean emailed;

    NotificationType(boolean emailed) {
        this.emailed = emailed;
    }

    /** Important types are also emailed, unless the user turned email notifications off. */
    public boolean isEmailed() {
        return emailed;
    }
}
