package com.campusflow.notification.mail;

/** Request to email a notification; handled after the transaction that created it commits. */
public record NotificationEmailEvent(String email, String fullName, String title, String message) {
}
