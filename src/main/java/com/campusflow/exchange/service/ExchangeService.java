package com.campusflow.exchange.service;

import com.campusflow.block.service.BlockService;
import com.campusflow.common.dto.PageQuery;
import com.campusflow.common.dto.PageResponse;
import com.campusflow.common.exception.ApiException;
import com.campusflow.exchange.dto.CreateExchangeRequest;
import com.campusflow.exchange.dto.ExchangeRequestResponse;
import com.campusflow.exchange.entity.ExchangeRequest;
import com.campusflow.exchange.entity.ExchangeStatus;
import com.campusflow.exchange.event.ExchangeEvent;
import com.campusflow.exchange.event.ExchangeEventType;
import com.campusflow.exchange.repository.ExchangeRequestRepository;
import com.campusflow.item.entity.Item;
import com.campusflow.item.entity.ItemStatus;
import com.campusflow.item.entity.ItemType;
import com.campusflow.item.repository.ItemRepository;
import com.campusflow.user.entity.User;
import com.campusflow.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The exchange state machine (see {@link ExchangeStatus}). Every transition checks who may do it
 * and from which status; item statuses are updated in the same transaction. Changes lock the
 * affected item rows first (see {@link #lock}), and rows are versioned as a second safety net,
 * so two conflicting actions at the same moment can never both succeed.
 */
@Service
@RequiredArgsConstructor
public class ExchangeService {

    private static final Set<ExchangeStatus> ACTIVE = EnumSet.of(ExchangeStatus.PENDING, ExchangeStatus.ACCEPTED);
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt", "id");

    private final ExchangeRequestRepository exchangeRequestRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher events;
    private final EntityManager entityManager;
    private final BlockService blockService;

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
        blockService.requireNoBlock(userId, item.getOwner().getId());
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
        events.publishEvent(new ExchangeEvent(saved, ExchangeEventType.REQUESTED, userId));
        return ExchangeRequestResponse.from(saved, userId);
    }

    /** Owner accepts: both items are reserved and all other pending requests for the item are rejected. */
    @Transactional
    public ExchangeRequestResponse accept(Long userId, Long requestId) {
        ExchangeRequest request = lockAsOwner(userId, requestId);
        requireStatus(request, ExchangeStatus.PENDING);
        Item item = request.getItem();
        Item offeredItem = request.getOfferedItem();
        if (item.getStatus() != ItemStatus.AVAILABLE) {
            throw ApiException.conflict("This item is no longer available");
        }
        if (offeredItem != null && offeredItem.getStatus() != ItemStatus.AVAILABLE) {
            throw ApiException.conflict("The offered item is no longer available");
        }

        item.setStatus(ItemStatus.IN_TRANSACTION);
        if (offeredItem != null) {
            offeredItem.setStatus(ItemStatus.IN_TRANSACTION);
        }
        transition(request, ExchangeStatus.ACCEPTED, ExchangeEventType.ACCEPTED, userId);
        exchangeRequestRepository.findByItemIdAndStatus(item.getId(), ExchangeStatus.PENDING).stream()
                .filter(other -> !other.getId().equals(request.getId()))
                .forEach(other -> transition(other, ExchangeStatus.REJECTED, ExchangeEventType.AUTO_REJECTED, null));
        return ExchangeRequestResponse.from(request, userId);
    }

    @Transactional
    public ExchangeRequestResponse reject(Long userId, Long requestId) {
        ExchangeRequest request = lockAsOwner(userId, requestId);
        requireStatus(request, ExchangeStatus.PENDING);
        transition(request, ExchangeStatus.REJECTED, ExchangeEventType.REJECTED, userId);
        return ExchangeRequestResponse.from(request, userId);
    }

    /**
     * The requester can withdraw a pending or accepted request; the owner can cancel an
     * accepted deal (a pending one is rejected instead). Reserved items become available again.
     */
    @Transactional
    public ExchangeRequestResponse cancel(Long userId, Long requestId) {
        ExchangeRequest request = lock(loadAsParticipant(userId, requestId));
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
        transition(request, ExchangeStatus.CANCELLED, ExchangeEventType.CANCELLED, userId);
        return ExchangeRequestResponse.from(request, userId);
    }

    /** Owner confirms the hand-over. Both items are marked as completed and can be reviewed. */
    @Transactional
    public ExchangeRequestResponse complete(Long userId, Long requestId) {
        ExchangeRequest request = lockAsOwner(userId, requestId);
        requireStatus(request, ExchangeStatus.ACCEPTED);
        transition(request, ExchangeStatus.COMPLETED, ExchangeEventType.COMPLETED, userId);
        markTraded(request.getItem());
        if (request.getOfferedItem() != null) {
            markTraded(request.getOfferedItem());
        }
        return ExchangeRequestResponse.from(request, userId);
    }

    /**
     * Cancels pending requests that offer this item, because it can no longer be given
     * (it was traded or deleted). The owners of the wanted items are notified.
     *
     * @return the cancelled requests
     */
    @Transactional
    public List<ExchangeRequest> cancelOpenOffersOf(Item item) {
        List<ExchangeRequest> requests =
                exchangeRequestRepository.findByOfferedItemIdAndStatus(item.getId(), ExchangeStatus.PENDING);
        requests.forEach(request -> transition(request, ExchangeStatus.CANCELLED, ExchangeEventType.AUTO_CANCELLED, null));
        return requests;
    }

    /**
     * Ends every open deal around an item that left the marketplace (removed by a moderator or
     * deleted by its owner) and notifies the people involved. Partner items that were reserved
     * for those deals become available again. The caller sets the item's own new status.
     */
    @Transactional
    public void closeAllFor(Item item) {
        entityManager.refresh(item, LockModeType.PESSIMISTIC_WRITE);
        for (ExchangeRequest request : exchangeRequestRepository.findByItemIdAndStatusIn(item.getId(), ACTIVE)) {
            if (request.getStatus() == ExchangeStatus.ACCEPTED && request.getOfferedItem() != null) {
                request.getOfferedItem().setStatus(ItemStatus.AVAILABLE);
            }
            transition(request, ExchangeStatus.CANCELLED, ExchangeEventType.ITEM_REMOVED, null);
        }
        for (ExchangeRequest request : exchangeRequestRepository.findByOfferedItemIdAndStatusIn(item.getId(), ACTIVE)) {
            if (request.getStatus() == ExchangeStatus.ACCEPTED) {
                request.getItem().setStatus(ItemStatus.AVAILABLE);
            }
            transition(request, ExchangeStatus.CANCELLED, ExchangeEventType.AUTO_CANCELLED, null);
        }
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
                .forEach(other -> transition(other, ExchangeStatus.REJECTED, ExchangeEventType.AUTO_REJECTED, null));
        cancelOpenOffersOf(item);
    }

    /** The only place a request changes status, so every change is announced as an event. */
    private void transition(ExchangeRequest request, ExchangeStatus status, ExchangeEventType type, Long actorId) {
        request.setStatus(status);
        events.publishEvent(new ExchangeEvent(request, type, actorId));
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

    private ExchangeRequest lockAsOwner(Long userId, Long requestId) {
        return lock(loadAsOwner(userId, requestId));
    }

    /**
     * Locks the item rows of a request (SELECT ... FOR UPDATE) before changing it, then reloads the
     * request. Every change to a request goes through here, so actions on the same item run one
     * after another and see each other's result: no lost updates and no deadlocks. Items are
     * always locked in id order, so two deals with crossed offers cannot wait on each other.
     */
    private ExchangeRequest lock(ExchangeRequest request) {
        Stream.of(request.getItem(), request.getOfferedItem())
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Item::getId))
                .forEach(item -> entityManager.refresh(item, LockModeType.PESSIMISTIC_WRITE));
        entityManager.refresh(request);
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
