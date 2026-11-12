package com.campusflow.wanted.event;

import com.campusflow.item.entity.Item;
import com.campusflow.wanted.entity.WantedPost;

/** A new item matches someone's "Cần tìm" post; the post owner is notified. */
public record WantedMatchFoundEvent(WantedPost post, Item item) {
}
