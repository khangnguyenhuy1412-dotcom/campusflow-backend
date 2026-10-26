package com.campusflow.item.dto;

import com.campusflow.category.dto.CategoryResponse;
import com.campusflow.item.entity.Item;
import com.campusflow.item.entity.ItemCondition;
import com.campusflow.item.entity.ItemStatus;
import com.campusflow.item.entity.ItemType;
import com.campusflow.user.dto.UserSummaryResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ItemDetailResponse(
        Long id,
        String title,
        String description,
        ItemType itemType,
        BigDecimal price,
        String exchangePreference,
        ItemCondition condition,
        ItemStatus status,
        String location,
        List<ItemImageResponse> images,
        CategoryResponse category,
        UserSummaryResponse owner,
        Instant createdAt,
        Instant updatedAt) {

    public static ItemDetailResponse from(Item item) {
        return new ItemDetailResponse(
                item.getId(),
                item.getTitle(),
                item.getDescription(),
                item.getItemType(),
                item.getPrice(),
                item.getExchangePreference(),
                item.getCondition(),
                item.getStatus(),
                item.getLocation(),
                item.getImages().stream().map(ItemImageResponse::from).toList(),
                CategoryResponse.from(item.getCategory()),
                UserSummaryResponse.from(item.getOwner()),
                item.getCreatedAt(),
                item.getUpdatedAt());
    }
}
