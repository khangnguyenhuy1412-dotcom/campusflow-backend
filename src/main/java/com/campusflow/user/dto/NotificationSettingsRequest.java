package com.campusflow.user.dto;

import jakarta.validation.constraints.NotNull;

/** @param emailNotifications whether important notifications are also emailed */
public record NotificationSettingsRequest(@NotNull Boolean emailNotifications) {
}
