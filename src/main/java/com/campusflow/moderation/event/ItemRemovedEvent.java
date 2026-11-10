package com.campusflow.moderation.event;

import com.campusflow.item.entity.Item;

/** A moderator removed a listing; the owner is told why. */
public record ItemRemovedEvent(Item item, String reason) {
}
