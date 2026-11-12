package com.campusflow.wanted.entity;

/** ACTIVE until the student found the item (FULFILLED), gave up (CLOSED) or the post expired (CLOSED). */
public enum WantedStatus {
    ACTIVE,
    FULFILLED,
    CLOSED
}
