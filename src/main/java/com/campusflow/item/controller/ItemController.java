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
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;
    private final ItemImageService itemImageService;

    @GetMapping
    public PageResponse<ItemSummaryResponse> search(
            @Valid @ModelAttribute ItemSearchCriteria criteria,
            @RequestParam(required = false) ItemSort sort,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return itemService.search(criteria, sort, page, size);
    }

    @GetMapping("/{id}")
    public ItemDetailResponse get(@PathVariable Long id, @CurrentUserId(required = false) Long userId) {
        return itemService.getDetail(id, userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemDetailResponse create(@CurrentUserId Long userId, @Valid @RequestBody ItemRequest request) {
        return itemService.create(userId, request);
    }

    @PutMapping("/{id}")
    public ItemDetailResponse update(@CurrentUserId Long userId, @PathVariable Long id,
            @Valid @RequestBody ItemRequest request) {
        return itemService.update(userId, id, request);
    }

    @PostMapping("/{id}/hide")
    public ItemDetailResponse hide(@CurrentUserId Long userId, @PathVariable Long id) {
        return itemService.setHidden(userId, id, true);
    }

    @PostMapping("/{id}/show")
    public ItemDetailResponse show(@CurrentUserId Long userId, @PathVariable Long id) {
        return itemService.setHidden(userId, id, false);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentUserId Long userId, @PathVariable Long id) {
        itemService.delete(userId, id);
    }

    @PostMapping(path = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public List<ItemImageResponse> uploadImages(@CurrentUserId Long userId, @PathVariable Long id,
            @RequestParam("files") List<MultipartFile> files) {
        return itemImageService.upload(userId, id, files);
    }

    @DeleteMapping("/{id}/images/{imageId}")
    public List<ItemImageResponse> deleteImage(@CurrentUserId Long userId, @PathVariable Long id,
            @PathVariable Long imageId) {
        return itemImageService.delete(userId, id, imageId);
    }

    @PutMapping("/{id}/images/{imageId}/primary")
    public List<ItemImageResponse> setPrimaryImage(@CurrentUserId Long userId, @PathVariable Long id,
            @PathVariable Long imageId) {
        return itemImageService.setPrimary(userId, id, imageId);
    }
}
