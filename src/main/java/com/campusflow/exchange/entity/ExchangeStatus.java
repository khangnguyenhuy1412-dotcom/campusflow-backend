package com.campusflow.exchange.entity;

/**
 * Lifecycle of an exchange request.
 * <pre>
 * PENDING --accept--> ACCEPTED --complete--> COMPLETED
 *    |                   |
 *    +--reject--> REJECTED
 *    +--cancel--> CANCELLED <--cancel--+
 * </pre>
 */
public enum ExchangeStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    CANCELLED,
    COMPLETED;

    public boolean isActive() {
        return this == PENDING || this == ACCEPTED;
    }
}
