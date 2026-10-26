package com.campusflow.item.dto;

import com.campusflow.item.entity.ItemImage;

public record ItemImageResponse(Long id, String url, String thumbnailUrl, boolean primary, int sortOrder) {

    public static ItemImageResponse from(ItemImage image) {
        return new ItemImageResponse(
                image.getId(), image.getImageUrl(), image.getThumbnailUrl(), image.isPrimary(), image.getSortOrder());
    }
}
