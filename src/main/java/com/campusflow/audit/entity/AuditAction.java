package com.campusflow.audit.entity;

/** Moderation and administration actions that are recorded in the audit log. */
public enum AuditAction {
    REPORT_RESOLVED,
    REPORT_DISMISSED,
    ITEM_REMOVED,
    ITEM_RESTORED,
    REVIEW_HIDDEN,
    REVIEW_RESTORED,
    USER_BANNED,
    USER_UNBANNED,
    MODERATOR_GRANTED,
    MODERATOR_REVOKED,
    CATEGORY_CREATED,
    CATEGORY_UPDATED,
    CATEGORY_DELETED,
    UNIVERSITY_CREATED,
    UNIVERSITY_UPDATED,
    UNIVERSITY_DELETED
}
