package com.campusflow.notification.dto;

import com.campusflow.notification.entity.Notification;
import com.campusflow.notification.entity.NotificationType;
import com.campusflow.user.dto.UserSummaryResponse;
import java.time.Instant;

/**
 * @param itemId            item to open when the notification is clicked (null if deleted)
 * @param exchangeRequestId exchange request to open when the notification is clicked
 * @param supportTicketId   support ticket to open when the notification is clicked
 */
public record NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String message,
        UserSummaryResponse actor,
        Long itemId,
        Long exchangeRequestId,
        Long supportTicketId,
        boolean read,
        Instant createdAt) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getActor() == null ? null : UserSummaryResponse.from(notification.getActor()),
                notification.getItem() == null ? null : notification.getItem().getId(),
                notification.getExchangeRequest() == null ? null : notification.getExchangeRequest().getId(),
                notification.getSupportTicket() == null ? null : notification.getSupportTicket().getId(),
                notification.isRead(),
                notification.getCreatedAt());
    }
}
