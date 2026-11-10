package com.campusflow.moderation.event;

import com.campusflow.moderation.entity.Report;

/** A report was resolved or dismissed; the reporter is told. */
public record ReportHandledEvent(Report report) {
}
