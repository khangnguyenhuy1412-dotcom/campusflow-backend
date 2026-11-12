package com.campusflow.item.entity;

/**
 * AVAILABLE and HIDDEN are set by the owner; IN_TRANSACTION and COMPLETED only by the exchange
 * flow; REMOVED only by a moderator (the owner cannot undo it). DELETED is a soft delete by the
 * owner: the row stays so exchange history and reviews keep their item, but it is shown nowhere.
 */
public enum ItemStatus {
    AVAILABLE,
    IN_TRANSACTION,
    COMPLETED,
    HIDDEN,
    REMOVED,
    DELETED
}
