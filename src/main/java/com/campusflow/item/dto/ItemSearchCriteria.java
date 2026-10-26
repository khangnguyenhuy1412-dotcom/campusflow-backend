package com.campusflow.item.dto;

import com.campusflow.item.entity.ItemCondition;
import com.campusflow.item.entity.ItemType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Query parameters of GET /api/items. All filters are optional. */
public record ItemSearchCriteria(
        @Size(max = 100) String keyword,
        Long categoryId,
        ItemType type,
        ItemCondition condition,
        @DecimalMin("0") BigDecimal minPrice,
        @DecimalMin("0") BigDecimal maxPrice,
        @Size(max = 150) String university) {
}
