package com.campusflow.item.service;

import com.campusflow.category.entity.Category;
import com.campusflow.category.service.CategoryService;
import com.campusflow.common.dto.PageQuery;
import com.campusflow.common.dto.PageResponse;
import com.campusflow.common.exception.ApiException;
import com.campusflow.exchange.entity.ExchangeStatus;
import com.campusflow.exchange.repository.ExchangeRequestRepository;
import com.campusflow.item.dto.ItemDetailResponse;
import com.campusflow.item.dto.ItemRequest;
import com.campusflow.item.dto.ItemSearchCriteria;
import com.campusflow.item.dto.ItemSort;
import com.campusflow.item.dto.ItemSummaryResponse;
import com.campusflow.item.entity.Item;
import com.campusflow.item.entity.ItemImage;
import com.campusflow.item.entity.ItemStatus;
import com.campusflow.item.entity.ItemType;
import com.campusflow.item.repository.ItemRepository;
import com.campusflow.item.repository.ItemSpecifications;
import com.campusflow.storage.StoredFileCleanup;
import com.campusflow.user.entity.User;
import com.campusflow.user.repository.UserRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ItemService {

    /** What other users can see on someone's profile. */
    private static final Set<ItemStatus> PUBLIC_PROFILE_STATUSES = EnumSet.of(ItemStatus.AVAILABLE, ItemStatus.IN_TRANSACTION);
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt", "id");

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final CategoryService categoryService;
    private final ExchangeRequestRepository exchangeRequestRepository;
    private final StoredFileCleanup storedFileCleanup;

    @Transactional(readOnly = true)
    public PageResponse<ItemSummaryResponse> search(ItemSearchCriteria criteria, ItemSort sort, Integer page, Integer size) {
        if (criteria.minPrice() != null && criteria.maxPrice() != null
                && criteria.minPrice().compareTo(criteria.maxPrice()) > 0) {
            throw ApiException.badRequest("minPrice must not be greater than maxPrice");
        }
        ItemSort safeSort = sort == null ? ItemSort.NEWEST : sort;
        return PageResponse.of(
                itemRepository.findAll(ItemSpecifications.publicSearch(criteria), PageQuery.of(page, size, safeSort.toSort())),
                ItemSummaryResponse::from);
    }

    /** Hidden items are only visible to their owner; for everyone else they do not exist. */
    @Transactional(readOnly = true)
    public ItemDetailResponse getDetail(Long itemId, Long currentUserId) {
        Item item = itemRepository.findWithDetailsById(itemId)
                .filter(found -> found.getStatus() != ItemStatus.HIDDEN || found.isOwnedBy(currentUserId))
                .orElseThrow(ItemService::itemNotFound);
        return ItemDetailResponse.from(item);
    }

    @Transactional(readOnly = true)
    public PageResponse<ItemSummaryResponse> listPublicByOwner(Long ownerId, Integer page, Integer size) {
        if (userRepository.findById(ownerId).filter(User::isActive).isEmpty()) {
            throw ApiException.notFound("User not found");
        }
        return PageResponse.of(
                itemRepository.findByOwnerIdAndStatusIn(ownerId, PUBLIC_PROFILE_STATUSES, PageQuery.of(page, size, NEWEST_FIRST)),
                ItemSummaryResponse::from);
    }

    /** All of the current user's own items, optionally filtered by status. */
    @Transactional(readOnly = true)
    public PageResponse<ItemSummaryResponse> listMine(Long userId, ItemStatus status, Integer page, Integer size) {
        Set<ItemStatus> statuses = status == null ? EnumSet.allOf(ItemStatus.class) : EnumSet.of(status);
        return PageResponse.of(
                itemRepository.findByOwnerIdAndStatusIn(userId, statuses, PageQuery.of(page, size, NEWEST_FIRST)),
                ItemSummaryResponse::from);
    }

    @Transactional
    public ItemDetailResponse create(Long userId, ItemRequest request) {
        User owner = userRepository.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        Item item = Item.builder().owner(owner).build();
        apply(item, request);
        return ItemDetailResponse.from(itemRepository.save(item));
    }

    @Transactional
    public ItemDetailResponse update(Long userId, Long itemId, ItemRequest request) {
        Item item = getOwnedItem(userId, itemId);
        requireEditable(item);
        apply(item, request);
        return ItemDetailResponse.from(item);
    }

    /** Owner hides a listing from search or shows it again. */
    @Transactional
    public ItemDetailResponse setHidden(Long userId, Long itemId, boolean hidden) {
        Item item = getOwnedItem(userId, itemId);
        requireEditable(item);
        item.setStatus(hidden ? ItemStatus.HIDDEN : ItemStatus.AVAILABLE);
        return ItemDetailResponse.from(item);
    }

    @Transactional
    public void delete(Long userId, Long itemId) {
        Item item = getOwnedItem(userId, itemId);
        if (!item.isEditable()) {
            throw ApiException.conflict("Items in an active or completed exchange cannot be deleted; hide them instead");
        }
        // Requests that offered this item can no longer be fulfilled
        exchangeRequestRepository.closePendingOffering(itemId, ExchangeStatus.CANCELLED);

        List<String> files = new ArrayList<>();
        for (ItemImage image : item.getImages()) {
            files.add(image.getImageUrl());
            files.add(image.getThumbnailUrl());
        }
        itemRepository.delete(item);
        storedFileCleanup.deleteAfterCommit(files);
    }

    /** Loads an item and checks that the user owns it. Used by every write operation, inside its transaction. */
    public Item getOwnedItem(Long userId, Long itemId) {
        Item item = itemRepository.findWithDetailsById(itemId).orElseThrow(ItemService::itemNotFound);
        if (!item.isOwnedBy(userId)) {
            throw ApiException.forbidden("You can only change your own items");
        }
        return item;
    }

    private void apply(Item item, ItemRequest request) {
        if (request.itemType() != ItemType.SALE && request.price() != null && request.price().signum() > 0) {
            throw ApiException.badRequest("Only SALE listings can have a price");
        }
        Category category = categoryService.getEntity(request.categoryId());
        item.setCategory(category);
        item.setTitle(request.title().trim());
        item.setDescription(request.description().trim());
        item.setItemType(request.itemType());
        item.setCondition(request.condition());
        item.setLocation(trimToNull(request.location()));

        // Price and exchange preference only make sense for some listing types
        switch (request.itemType()) {
            case SALE -> {
                if (request.price() == null || request.price().signum() <= 0) {
                    throw ApiException.badRequest("A SALE listing needs a price greater than 0");
                }
                item.setPrice(request.price());
                item.setExchangePreference(null);
            }
            case EXCHANGE -> {
                item.setPrice(BigDecimal.ZERO);
                item.setExchangePreference(trimToNull(request.exchangePreference()));
            }
            case GIVEAWAY -> {
                item.setPrice(BigDecimal.ZERO);
                item.setExchangePreference(null);
            }
        }
    }

    private static void requireEditable(Item item) {
        if (!item.isEditable()) {
            throw ApiException.conflict("Items in an active or completed exchange cannot be changed");
        }
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static ApiException itemNotFound() {
        return ApiException.notFound("Item not found");
    }
}
