package com.campusflow.notification.service;

import com.campusflow.exchange.event.ExchangeEvent;
import com.campusflow.moderation.event.ItemRemovedEvent;
import com.campusflow.moderation.event.ReportHandledEvent;
import com.campusflow.review.event.ReviewCreatedEvent;
import com.campusflow.wanted.event.WantedMatchFoundEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Turns domain events into notifications. Runs synchronously inside the publisher's transaction:
 * if the exchange action rolls back, its notification is rolled back with it.
 */
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    @EventListener
    public void onExchangeEvent(ExchangeEvent event) {
        notificationService.create(NotificationMessages.forExchange(event));
    }

    @EventListener
    public void onReviewCreated(ReviewCreatedEvent event) {
        notificationService.create(NotificationMessages.forReview(event.review()));
    }

    @EventListener
    public void onReportHandled(ReportHandledEvent event) {
        notificationService.create(NotificationMessages.forReportHandled(event.report()));
    }

    @EventListener
    public void onItemRemoved(ItemRemovedEvent event) {
        notificationService.create(NotificationMessages.forItemRemoved(event.item(), event.reason()));
    }

    @EventListener
    public void onWantedMatch(WantedMatchFoundEvent event) {
        notificationService.create(NotificationMessages.forWantedMatch(event.post(), event.item()));
    }
}
