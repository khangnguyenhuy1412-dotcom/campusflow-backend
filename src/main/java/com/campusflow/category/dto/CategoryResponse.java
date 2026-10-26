package com.campusflow.category.dto;

import com.campusflow.category.entity.Category;

public record CategoryResponse(Long id, String name, String slug, String description, String iconUrl) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(), category.getName(), category.getSlug(), category.getDescription(), category.getIconUrl());
    }
}
