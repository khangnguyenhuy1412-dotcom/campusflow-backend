package com.campusflow.exchange.event;

import com.campusflow.exchange.entity.ExchangeRequest;

/**
 * Published by ExchangeService after a request changes status, inside the same transaction.
 * Other features (notifications, later chat or statistics) react to it without the exchange
 * code knowing about them.
 *
 * @param actorId the user who caused the change, or null when the system did it automatically
 */
public record ExchangeEvent(ExchangeRequest request, ExchangeEventType type, Long actorId) {

    public boolean isAutomatic() {
        return actorId == null;
    }
}
