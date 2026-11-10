package com.campusflow.exchange.event;

public enum ExchangeEventType {
    /** A user sent a request for an item. */
    REQUESTED,
    ACCEPTED,
    /** The owner rejected the request. */
    REJECTED,
    /** Rejected by the system because the item went to someone else. */
    AUTO_REJECTED,
    /** The requester or owner cancelled. */
    CANCELLED,
    /** Cancelled by the system because the offered item is no longer available. */
    AUTO_CANCELLED,
    /** Cancelled because the wanted item was removed by a moderator (or deleted). */
    ITEM_REMOVED,
    COMPLETED
}
