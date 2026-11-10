package com.campusflow.moderation.entity;

/** OPEN until a moderator resolves (action taken or not) or dismisses (not a violation). */
public enum ReportStatus {
    OPEN,
    RESOLVED,
    DISMISSED
}
