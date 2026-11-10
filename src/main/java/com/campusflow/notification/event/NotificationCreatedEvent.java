package com.campusflow.notification.event;

import com.campusflow.notification.dto.NotificationResponse;

/** Published when a notification is stored; pushed to the recipient over WebSocket after commit. */
public record NotificationCreatedEvent(Long recipientId, NotificationResponse notification) {
}
