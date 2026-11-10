package com.campusflow.admin.dto;

import jakarta.validation.constraints.NotNull;

/** @param moderator true grants ROLE_MODERATOR, false removes it */
public record ModeratorRoleRequest(@NotNull Boolean moderator) {
}
