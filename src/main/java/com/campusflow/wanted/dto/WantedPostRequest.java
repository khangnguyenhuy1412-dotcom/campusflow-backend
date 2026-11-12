package com.campusflow.wanted.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * @param keywords           words that must all appear in a matching item, e.g. "giai tich 1"
 * @param categoryId         optional: only items in this category match
 * @param maxPrice           optional: SALE items above this price (VND) do not match
 * @param sameUniversityOnly only items from students of the same university match
 */
public record WantedPostRequest(
        @NotBlank @Size(min = 3, max = 150) String title,
        @NotBlank @Size(max = 200) String keywords,
        @Size(max = 1000) String description,
        Long categoryId,
        @DecimalMin("0") @DecimalMax("999999999") @Digits(integer = 9, fraction = 0) BigDecimal maxPrice,
        boolean sameUniversityOnly) {
}
