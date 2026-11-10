package com.campusflow.item.entity;

/**
 * AVAILABLE and HIDDEN are set by the owner; IN_TRANSACTION and COMPLETED only by the exchange
 * flow; REMOVED only by a moderator (the owner cannot undo it).
 */
public enum ItemStatus {
    AVAILABLE,
    IN_TRANSACTION,
    COMPLETED,
    HIDDEN,
    REMOVED
}
