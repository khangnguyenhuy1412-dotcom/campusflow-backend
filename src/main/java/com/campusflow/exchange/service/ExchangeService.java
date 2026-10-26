package com.campusflow.exchange.service;

import com.campusflow.common.dto.PageQuery;
import com.campusflow.common.dto.PageResponse;
import com.campusflow.common.exception.ApiException;
import com.campusflow.exchange.dto.CreateExchangeRequest;
import com.campusflow.exchange.dto.ExchangeRequestResponse;
import com.campusflow.exchange.entity.ExchangeRequest;
import com.campusflow.exchange.entity.ExchangeStatus;
import com.campusflow.exchange.repository.ExchangeRequestRepository;
import com.campusflow.item.entity.Item;
import com.campusflow.item.entity.ItemStatus;
import com.campusflow.item.entity.ItemType;
import com.campusflow.item.repository.ItemRepository;
import com.campusflow.user.entity.User;
import com.campusflow.user.repository.UserRepository;
import java.util.EnumSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The exchange state machine (see {@link ExchangeStatus}). Every transition checks who may do it
 * and from which status; item statuses are updated in the same transaction. Item and request rows
 * are versioned, so two conflicting actions at the same moment cannot both succeed.
 */
@Service
@RequiredArgsConstructor
public class ExchangeService {

    private static final Set<ExchangeStatus> ACTIVE = EnumSet.of(ExchangeStatus.PENDING, ExchangeStatus.ACCEPTED);
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt", "id");

    private final ExchangeRequestRepository exchangeRequestRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    @Transactional
    public ExchangeRequestResponse create(Long userId, CreateExchangeRequest request) {
        Item item = itemRepository.findWithDetailsById(request.itemId())
                .filter(found -> found.getStatus() != ItemStatus.HIDDEN)
                .orElseThrow(() -> ApiException.notFound("Item not found"));
        if (item.isOwnedBy(userId)) {
            throw ApiException.badRequest("You cannot send a request for your own item");
        }
        if (item.getStatus() != ItemStatus.AVAILABLE) {
            throw ApiException.conflict("This item is no longer available");
        }
        if (exchangeRequestRepository.existsByItemIdAndRequesterIdAndStatusIn(item.getId(), userId, ACTIVE)) {
            throw ApiException.conflict("You already have an open request for this item");
        }

        Item offeredItem = resolveOfferedItem(userId, item, request.offeredItemId());
        User requester = userRepository.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        ExchangeRequest saved = exchangeRequestRepository.save(ExchangeRequest.builder()
                .item(item)
                .requester(requester)
                .offeredItem(offeredItem)
                .message(trimToNull(request.message()))
                .build());
        return ExchangeRequestResponse.from(saved, userId);
    }

    /** Owner accepts: both items are reserved and all other pending requests for the item are rejected. */
    @Transactional
    public ExchangeRequestResponse accept(Long userId, Long requestId) {
        ExchangeRequest request = loadAsOwner(userId, requestId);
        requireStatus(request, ExchangeStatus.PENDING);
        Item item = request.getItem();
        Item offeredItem = request.getOfferedItem();
        if (item.getStatus() != ItemStatus.AVAILABLE) {
            throw ApiException.conflict("This item is no longer available");
        }
        if (offeredItem != null && offeredItem.getStatus() != ItemStatus.AVAILABLE) {
            throw ApiException.conflict("The offered item is no longer available");
        }

        request.setStatus(ExchangeStatus.ACCEPTED);
        item.setStatus(ItemStatus.IN_TRANSACTION);
        if (offeredItem != null) {
            offeredItem.setStatus(ItemStatus.IN_TRANSACTION);
        }
        exchangeRequestRepository.findByItemIdAndStatus(item.getId(), ExchangeStatus.PENDING).stream()
                .filter(other -> !other.getId().equals(request.getId()))
                .forEach(other -> other.setStatus(ExchangeStatus.REJECTED));
        return ExchangeRequestResponse.from(request, userId);
    }

    @Transactional
    public ExchangeRequestResponse reject(Long userId, Long requestId) {
        ExchangeRequest request = loadAsOwner(userId, requestId);
        requireStatus(request, ExchangeStatus.PENDING);
        request.setStatus(ExchangeStatus.REJECTED);
        return ExchangeRequestResponse.from(request, userId);
    }

    /**
     * The requester can withdraw a pending or accepted request; the owner can cancel an
     * accepted deal (a pending one is rejected instead). Reserved items become available again.
     */
    @Transactional
    public ExchangeRequestResponse cancel(Long userId, Long requestId) {
        ExchangeRequest request = loadAsParticipant(userId, requestId);
        boolean allowed = request.isRequester(userId)
                ? request.getStatus().isActive()
                : request.getStatus() == ExchangeStatus.ACCEPTED;
        if (!allowed) {
            throw ApiException.conflict("This request cannot be cancelled in status " + request.getStatus());
        }
        if (request.getStatus() == ExchangeStatus.ACCEPTED) {
            request.getItem().setStatus(ItemStatus.AVAILABLE);
            if (request.getOfferedItem() != null) {
                request.getOfferedItem().setStatus(ItemStatus.AVAILABLE);
            }
        }
        request.setStatus(ExchangeStatus.CANCELLED);
        return ExchangeRequestResponse.from(request, userId);
    }

    /** Owner confirms the hand-over. Both items are marked as completed and can be reviewed. */
    @Transactional
    public ExchangeRequestResponse complete(Long userId, Long requestId) {
        ExchangeRequest request = loadAsOwner(userId, requestId);
        requireStatus(request, ExchangeStatus.ACCEPTED);
        request.setStatus(ExchangeStatus.COMPLETED);
        markTraded(request.getItem());
        if (request.getOfferedItem() != null) {
            markTraded(request.getOfferedItem());
        }
        return ExchangeRequestResponse.from(request, userId);
    }

    /** Only the two participants can see a request; for anyone else it does not exist. */
    @Transactional(readOnly = true)
    public ExchangeRequestResponse get(Long userId, Long requestId) {
        return ExchangeRequestResponse.from(loadAsParticipant(userId, requestId), userId);
    }

    @Transactional(readOnly = true)
    public PageResponse<ExchangeRequestResponse> listSent(Long userId, ExchangeStatus status, Integer page, Integer size) {
        return PageResponse.of(
                exchangeRequestRepository.findSent(userId, status, PageQuery.of(page, size, NEWEST_FIRST)),
                request -> ExchangeRequestResponse.from(request, userId));
    }

    @Transactional(readOnly = true)
    public PageResponse<ExchangeRequestResponse> listReceived(Long userId, ExchangeStatus status, Integer page,
            Integer size) {
        return PageResponse.of(
                exchangeRequestRepository.findReceived(userId, status, PageQuery.of(page, size, NEWEST_FIRST)),
                request -> ExchangeRequestResponse.from(request, userId));
    }

    /** Only EXCHANGE listings take an item in return, and it must be one of the requester's available items. */
    private Item resolveOfferedItem(Long userId, Item wanted, Long offeredItemId) {
        if (wanted.getItemType() != ItemType.EXCHANGE) {
            if (offeredItemId != null) {
                throw ApiException.badRequest("Only EXCHANGE listings accept an item in return");
            }
            return null;
        }
        if (offeredItemId == null) {
            throw ApiException.badRequest("Choose one of your items to offer in exchange");
        }
        Item offered = itemRepository.findWithDetailsById(offeredItemId)
                .orElseThrow(() -> ApiException.notFound("Offered item not found"));
        if (!offered.isOwnedBy(userId)) {
            throw ApiException.forbidden("You can only offer your own items");
        }
        if (offered.getStatus() != ItemStatus.AVAILABLE) {
            throw ApiException.conflict("The offered item is not available");
        }
        return offered;
    }

    /** Marks an item as traded and closes every open request that still involves it. */
    private void markTraded(Item item) {
        item.setStatus(ItemStatus.COMPLETED);
        exchangeRequestRepository.findByItemIdAndStatus(item.getId(), ExchangeStatus.PENDING)
                .forEach(other -> other.setStatus(ExchangeStatus.REJECTED));
        exchangeRequestRepository.closePendingOffering(item.getId(), ExchangeStatus.CANCELLED);
    }

    private ExchangeRequest load(Long requestId) {
        return exchangeRequestRepository.findWithDetailsById(requestId)
                .orElseThrow(() -> ApiException.notFound("Exchange request not found"));
    }

    private ExchangeRequest loadAsParticipant(Long userId, Long requestId) {
        ExchangeRequest request = load(requestId);
        if (!request.isParticipant(userId)) {
            throw ApiException.notFound("Exchange request not found");
        }
        return request;
    }

    private ExchangeRequest loadAsOwner(Long userId, Long requestId) {
        ExchangeRequest request = loadAsParticipant(userId, requestId);
        if (!request.isOwner(userId)) {
            throw ApiException.forbidden("Only the item owner can do this");
        }
        return request;
    }

    private static void requireStatus(ExchangeRequest request, ExchangeStatus expected) {
        if (request.getStatus() != expected) {
            throw ApiException.conflict("Request must be " + expected + " but is " + request.getStatus());
        }
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
