package com.campusflow.chat.dto;

import com.campusflow.item.entity.Item;
import com.campusflow.item.entity.ItemImage;
import com.campusflow.item.entity.ItemStatus;

/** The item a conversation is about, shown in the chat header. */
public record ChatItemResponse(Long id, String title, String thumbnailUrl, ItemStatus status) {

    public static ChatItemResponse from(Item item) {
        return new ChatItemResponse(
                item.getId(),
                item.getTitle(),
                item.primaryImage().map(ItemImage::getThumbnailUrl).orElse(null),
                item.getStatus());
    }
}
