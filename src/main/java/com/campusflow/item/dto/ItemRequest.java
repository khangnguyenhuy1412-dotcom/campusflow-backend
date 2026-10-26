package com.campusflow.item.dto;

import com.campusflow.item.entity.ItemCondition;
import com.campusflow.item.entity.ItemType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Body for creating or updating a listing. Rules that depend on the type
 * (e.g. a SALE needs a price) are checked in ItemService.
 */
public record ItemRequest(
        @NotNull Long categoryId,
        @NotBlank @Size(min = 5, max = 200) String title,
        @NotBlank @Size(max = 5000) String description,
        @NotNull ItemType itemType,
        // VND, whole numbers only
        @DecimalMin("0") @DecimalMax("999999999") @Digits(integer = 9, fraction = 0) BigDecimal price,
        @Size(max = 255) String exchangePreference,
        @NotNull ItemCondition condition,
        @Size(max = 255) String location) {
}
