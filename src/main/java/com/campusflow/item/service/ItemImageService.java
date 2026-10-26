package com.campusflow.item.service;

import com.campusflow.common.exception.ApiException;
import com.campusflow.item.dto.ItemImageResponse;
import com.campusflow.item.entity.Item;
import com.campusflow.item.entity.ItemImage;
import com.campusflow.storage.FileStorage;
import com.campusflow.storage.ImageProcessor;
import com.campusflow.storage.ImageProcessor.ProcessedImageSet;
import com.campusflow.storage.StoredFileCleanup;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ItemImageService {

    public static final int MAX_IMAGES_PER_ITEM = 5;
    private static final int FULL_SIZE = 1600;
    private static final int THUMBNAIL_SIZE = 400;
    private static final String FOLDER = "items";

    private final ItemService itemService;
    private final ImageProcessor imageProcessor;
    private final FileStorage fileStorage;
    private final StoredFileCleanup storedFileCleanup;
    private final EntityManager entityManager;

    @Transactional
    public List<ItemImageResponse> upload(Long userId, Long itemId, List<MultipartFile> files) {
        Item item = lockOwnedItem(userId, itemId);
        if (files == null || files.isEmpty()) {
            throw ApiException.badRequest("No files were uploaded");
        }
        if (item.getImages().size() + files.size() > MAX_IMAGES_PER_ITEM) {
            throw ApiException.badRequest("An item can have at most " + MAX_IMAGES_PER_ITEM + " images");
        }

        // Validate every file first, so nothing is stored if one of them is invalid
        List<ProcessedImageSet> processed = files.stream()
                .map(file -> imageProcessor.process(file, FULL_SIZE, THUMBNAIL_SIZE))
                .toList();

        List<String> storedUrls = new ArrayList<>();
        storedFileCleanup.deleteOnRollback(storedUrls);
        int nextOrder = item.getImages().stream().mapToInt(ItemImage::getSortOrder).max().orElse(-1) + 1;
        boolean hasPrimary = item.getImages().stream().anyMatch(ItemImage::isPrimary);

        for (ProcessedImageSet image : processed) {
            String url = fileStorage.store(FOLDER, image.full().content(), image.full().extension());
            storedUrls.add(url);
            String thumbnailUrl = fileStorage.store(FOLDER, image.thumbnail().content(), image.thumbnail().extension());
            storedUrls.add(thumbnailUrl);

            item.getImages().add(ItemImage.builder()
                    .item(item)
                    .imageUrl(url)
                    .thumbnailUrl(thumbnailUrl)
                    .primary(!hasPrimary)
                    .sortOrder(nextOrder++)
                    .build());
            hasPrimary = true;
        }
        entityManager.flush(); // assigns ids to the new images
        return toResponses(item);
    }

    @Transactional
    public List<ItemImageResponse> delete(Long userId, Long itemId, Long imageId) {
        Item item = lockOwnedItem(userId, itemId);
        ItemImage image = findImage(item, imageId);

        item.getImages().remove(image);
        if (image.isPrimary() && !item.getImages().isEmpty()) {
            item.getImages().getFirst().setPrimary(true);
        }
        storedFileCleanup.deleteAfterCommit(List.of(image.getImageUrl(), image.getThumbnailUrl()));
        return toResponses(item);
    }

    @Transactional
    public List<ItemImageResponse> setPrimary(Long userId, Long itemId, Long imageId) {
        Item item = lockOwnedItem(userId, itemId);
        ItemImage primary = findImage(item, imageId);
        item.getImages().forEach(image -> image.setPrimary(image == primary));
        return toResponses(item);
    }

    /**
     * Image changes do not touch the item row, so the version is bumped explicitly:
     * two uploads at the same moment then cannot both pass the image limit.
     */
    private Item lockOwnedItem(Long userId, Long itemId) {
        Item item = itemService.getOwnedItem(userId, itemId);
        requireEditable(item);
        entityManager.lock(item, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        return item;
    }

    private static ItemImage findImage(Item item, Long imageId) {
        return item.getImages().stream()
                .filter(image -> image.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("Image not found"));
    }

    private static void requireEditable(Item item) {
        if (!item.isEditable()) {
            throw ApiException.conflict("Items in an active or completed exchange cannot be changed");
        }
    }

    private static List<ItemImageResponse> toResponses(Item item) {
        return item.getImages().stream().map(ItemImageResponse::from).toList();
    }
}
