package com.campusflow.item.controller;

import com.campusflow.common.dto.PageResponse;
import com.campusflow.common.web.CurrentUserId;
import com.campusflow.item.dto.ItemDetailResponse;
import com.campusflow.item.dto.ItemImageResponse;
import com.campusflow.item.dto.ItemRequest;
import com.campusflow.item.dto.ItemSearchCriteria;
import com.campusflow.item.dto.ItemSort;
import com.campusflow.item.dto.ItemSummaryResponse;
import com.campusflow.item.service.ItemImageService;
import com.campusflow.item.service.ItemService;
import com.campusflow.ratelimit.RateLimit;
import com.campusflow.ratelimit.RateLimitPolicy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * GET endpoints are public. Every write requires a verified student email (see SecurityConfig)
 * and ownership of the item (checked in the services).
 */
@Tag(name = "Items", description = "Listings: search, details, create/edit (verified students), images")
@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;
    private final ItemImageService itemImageService;

    @Operation(summary = "Search available items (public)")
    @GetMapping
    public PageResponse<ItemSummaryResponse> search(
            @Valid @ParameterObject @ModelAttribute ItemSearchCriteria criteria,
            @RequestParam(required = false) ItemSort sort,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return itemService.search(criteria, sort, page, size);
    }

    @Operation(summary = "Item details; hidden items only for their owner")
    @GetMapping("/{id}")
    public ItemDetailResponse get(@PathVariable Long id, @CurrentUserId(required = false) Long userId) {
        return itemService.getDetail(id, userId);
    }

    @Operation(summary = "Create a listing (verified email required)")
    @RateLimit(policy = RateLimitPolicy.CREATE_CONTENT, key = RateLimit.Key.USER)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemDetailResponse create(@CurrentUserId Long userId, @Valid @RequestBody ItemRequest request) {
        return itemService.create(userId, request);
    }

    @Operation(summary = "Update your listing")
    @PutMapping("/{id}")
    public ItemDetailResponse update(@CurrentUserId Long userId, @PathVariable Long id,
            @Valid @RequestBody ItemRequest request) {
        return itemService.update(userId, id, request);
    }

    @Operation(summary = "Hide your listing from search")
    @PostMapping("/{id}/hide")
    public ItemDetailResponse hide(@CurrentUserId Long userId, @PathVariable Long id) {
        return itemService.setHidden(userId, id, true);
    }

    @Operation(summary = "Show a hidden listing again")
    @PostMapping("/{id}/show")
    public ItemDetailResponse show(@CurrentUserId Long userId, @PathVariable Long id) {
        return itemService.setHidden(userId, id, false);
    }

    @Operation(summary = "Delete your listing (not while in an exchange)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentUserId Long userId, @PathVariable Long id) {
        itemService.delete(userId, id);
    }

    @Operation(summary = "Upload up to 5 JPEG/PNG images (max 5 MB each)")
    @RateLimit(policy = RateLimitPolicy.UPLOAD, key = RateLimit.Key.USER)
    @PostMapping(path = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public List<ItemImageResponse> uploadImages(@CurrentUserId Long userId, @PathVariable Long id,
            @RequestParam("files") List<MultipartFile> files) {
        return itemImageService.upload(userId, id, files);
    }

    @Operation(summary = "Delete an image")
    @DeleteMapping("/{id}/images/{imageId}")
    public List<ItemImageResponse> deleteImage(@CurrentUserId Long userId, @PathVariable Long id,
            @PathVariable Long imageId) {
        return itemImageService.delete(userId, id, imageId);
    }

    @Operation(summary = "Set the main image")
    @PutMapping("/{id}/images/{imageId}/primary")
    public List<ItemImageResponse> setPrimaryImage(@CurrentUserId Long userId, @PathVariable Long id,
            @PathVariable Long imageId) {
        return itemImageService.setPrimary(userId, id, imageId);
    }
}
