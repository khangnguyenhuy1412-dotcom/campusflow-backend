package com.campusflow.moderation.dto;

import com.campusflow.moderation.entity.ModerationAction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** @param note shown to the item owner (REMOVE_ITEM) or banned user (BAN_USER) as the reason */
public record ResolveReportRequest(@NotNull ModerationAction action, @Size(max = 1000) String note) {
}
