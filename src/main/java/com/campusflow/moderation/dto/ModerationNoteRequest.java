package com.campusflow.moderation.dto;

import jakarta.validation.constraints.Size;

/** Optional reason or note for a moderation action. */
public record ModerationNoteRequest(@Size(max = 1000) String note) {
}
