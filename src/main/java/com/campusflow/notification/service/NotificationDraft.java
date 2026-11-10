package com.campusflow.notification.service;

import com.campusflow.exchange.entity.ExchangeRequest;
import com.campusflow.item.entity.Item;
import com.campusflow.notification.entity.NotificationType;
import com.campusflow.user.entity.User;

/** Everything needed to create one notification; built by {@link NotificationMessages}. */
public record NotificationDraft(
        User recipient,
        NotificationType type,
        String title,
        String message,
        User actor,
        Item item,
        ExchangeRequest exchangeRequest) {
}
