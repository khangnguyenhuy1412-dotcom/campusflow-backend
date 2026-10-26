package com.campusflow.item.dto;

import com.campusflow.category.dto.CategoryResponse;
import com.campusflow.item.entity.Item;
import com.campusflow.item.entity.ItemCondition;
import com.campusflow.item.entity.ItemImage;
import com.campusflow.item.entity.ItemStatus;
import com.campusflow.item.entity.ItemType;
import com.campusflow.user.dto.UserSummaryResponse;
import java.math.BigDecimal;
import java.time.Instant;

/** Compact listing used in search results and lists. */
public record ItemSummaryResponse(
        Long id,
        String title,
        ItemType itemType,
        BigDecimal price,
        ItemCondition condition,
        ItemStatus status,
        String location,
        String thumbnailUrl,
        CategoryResponse category,
        UserSummaryResponse owner,
        Instant createdAt) {

    public static ItemSummaryResponse from(Item item) {
        return new ItemSummaryResponse(
                item.getId(),
                item.getTitle(),
                item.getItemType(),
                item.getPrice(),
                item.getCondition(),
                item.getStatus(),
                item.getLocation(),
                item.primaryImage().map(ItemImage::getThumbnailUrl).orElse(null),
                CategoryResponse.from(item.getCategory()),
                UserSummaryResponse.from(item.getOwner()),
                item.getCreatedAt());
    }
}
