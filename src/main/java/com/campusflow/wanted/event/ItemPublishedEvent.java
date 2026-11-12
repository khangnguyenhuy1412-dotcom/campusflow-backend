package com.campusflow.wanted.event;

/**
 * Published when a new listing goes live. Want-list matching runs after the commit, in the
 * background, so creating an item never waits for (or fails because of) matching.
 */
public record ItemPublishedEvent(Long itemId) {
}
