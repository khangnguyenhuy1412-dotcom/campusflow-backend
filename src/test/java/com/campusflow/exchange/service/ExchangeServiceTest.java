package com.campusflow.exchange.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusflow.category.entity.Category;
import com.campusflow.common.exception.ApiException;
import com.campusflow.exchange.dto.CreateExchangeRequest;
import com.campusflow.exchange.dto.ExchangeRequestResponse;
import com.campusflow.exchange.entity.ExchangeRequest;
import com.campusflow.exchange.entity.ExchangeStatus;
import com.campusflow.exchange.repository.ExchangeRequestRepository;
import com.campusflow.item.entity.Item;
import com.campusflow.item.entity.ItemCondition;
import com.campusflow.item.entity.ItemStatus;
import com.campusflow.item.entity.ItemType;
import com.campusflow.item.repository.ItemRepository;
import com.campusflow.user.entity.User;
import com.campusflow.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class ExchangeServiceTest {

    private static final Long OWNER = 1L;
    private static final Long REQUESTER = 2L;
    private static final Long STRANGER = 3L;

    @Mock
    private ExchangeRequestRepository exchangeRequestRepository;
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private ExchangeService service;

    private User owner;
    private User requester;
    private Category category;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(OWNER).email("owner@hcmus.edu.vn").fullName("Owner").phoneNumber("0901234567").build();
        requester = User.builder().id(REQUESTER).email("req@hcmus.edu.vn").fullName("Req").build();
        category = Category.builder().id(1L).name("Books").slug("books").build();
        lenient().when(userRepository.findById(REQUESTER)).thenReturn(Optional.of(requester));
        lenient().when(exchangeRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Item item(Long id, User itemOwner, ItemType type, ItemStatus status) {
        Item item = Item.builder().id(id).owner(itemOwner).category(category).title("Item " + id)
                .description("d").itemType(type).condition(ItemCondition.USED).status(status).build();
        lenient().when(itemRepository.findWithDetailsById(id)).thenReturn(Optional.of(item));
        return item;
    }

    private ExchangeRequest request(Long id, Item item, Item offered, ExchangeStatus status) {
        ExchangeRequest request = ExchangeRequest.builder().id(id).item(item).requester(requester)
                .offeredItem(offered).status(status).build();
        lenient().when(exchangeRequestRepository.findWithDetailsById(id)).thenReturn(Optional.of(request));
        return request;
    }

    private static HttpStatus statusOf(Throwable e) {
        return ((ApiException) e).getStatus();
    }

    // ------------------------------------------------------------------ create

    @Test
    void createGiveawayRequest() {
        item(10L, owner, ItemType.GIVEAWAY, ItemStatus.AVAILABLE);

        ExchangeRequestResponse response = service.create(REQUESTER, new CreateExchangeRequest(10L, null, " Hi "));

        assertThat(response.status()).isEqualTo(ExchangeStatus.PENDING);
        assertThat(response.message()).isEqualTo("Hi");
        assertThat(response.counterpartContact()).as("contact hidden while pending").isNull();
    }

    @Test
    void cannotRequestOwnItem() {
        item(10L, owner, ItemType.GIVEAWAY, ItemStatus.AVAILABLE);

        assertThatThrownBy(() -> service.create(OWNER, new CreateExchangeRequest(10L, null, null)))
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void cannotRequestHiddenItem() {
        item(10L, owner, ItemType.GIVEAWAY, ItemStatus.HIDDEN);

        assertThatThrownBy(() -> service.create(REQUESTER, new CreateExchangeRequest(10L, null, null)))
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void cannotRequestItemInTransaction() {
        item(10L, owner, ItemType.SALE, ItemStatus.IN_TRANSACTION);

        assertThatThrownBy(() -> service.create(REQUESTER, new CreateExchangeRequest(10L, null, null)))
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void cannotSendTwoOpenRequestsForSameItem() {
        item(10L, owner, ItemType.SALE, ItemStatus.AVAILABLE);
        when(exchangeRequestRepository.existsByItemIdAndRequesterIdAndStatusIn(any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> service.create(REQUESTER, new CreateExchangeRequest(10L, null, null)))
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void exchangeListingRequiresAnOfferedItem() {
        item(10L, owner, ItemType.EXCHANGE, ItemStatus.AVAILABLE);

        assertThatThrownBy(() -> service.create(REQUESTER, new CreateExchangeRequest(10L, null, null)))
                .hasMessageContaining("offer");
    }

    @Test
    void saleListingRejectsAnOfferedItem() {
        item(10L, owner, ItemType.SALE, ItemStatus.AVAILABLE);

        assertThatThrownBy(() -> service.create(REQUESTER, new CreateExchangeRequest(10L, 20L, null)))
                .hasMessageContaining("Only EXCHANGE");
    }

    @Test
    void cannotOfferSomeoneElsesItem() {
        item(10L, owner, ItemType.EXCHANGE, ItemStatus.AVAILABLE);
        item(20L, owner, ItemType.EXCHANGE, ItemStatus.AVAILABLE);

        assertThatThrownBy(() -> service.create(REQUESTER, new CreateExchangeRequest(10L, 20L, null)))
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.FORBIDDEN));
    }

    // ------------------------------------------------------------------ accept / reject

    @Test
    void acceptReservesBothItemsAndRejectsOtherPendingRequests() {
        Item wanted = item(10L, owner, ItemType.EXCHANGE, ItemStatus.AVAILABLE);
        Item offered = item(20L, requester, ItemType.EXCHANGE, ItemStatus.AVAILABLE);
        ExchangeRequest accepted = request(100L, wanted, offered, ExchangeStatus.PENDING);
        ExchangeRequest other = ExchangeRequest.builder().id(101L).item(wanted).requester(requester)
                .status(ExchangeStatus.PENDING).build();
        when(exchangeRequestRepository.findByItemIdAndStatus(10L, ExchangeStatus.PENDING))
                .thenReturn(List.of(accepted, other));

        ExchangeRequestResponse response = service.accept(OWNER, 100L);

        assertThat(accepted.getStatus()).isEqualTo(ExchangeStatus.ACCEPTED);
        assertThat(other.getStatus()).isEqualTo(ExchangeStatus.REJECTED);
        assertThat(wanted.getStatus()).isEqualTo(ItemStatus.IN_TRANSACTION);
        assertThat(offered.getStatus()).isEqualTo(ItemStatus.IN_TRANSACTION);
        assertThat(response.counterpartContact().email()).as("owner sees requester's contact").isEqualTo("req@hcmus.edu.vn");
    }

    @Test
    void onlyOwnerCanAccept() {
        request(100L, item(10L, owner, ItemType.GIVEAWAY, ItemStatus.AVAILABLE), null, ExchangeStatus.PENDING);

        assertThatThrownBy(() -> service.accept(REQUESTER, 100L))
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void strangerCannotSeeRequestAtAll() {
        request(100L, item(10L, owner, ItemType.GIVEAWAY, ItemStatus.AVAILABLE), null, ExchangeStatus.PENDING);

        assertThatThrownBy(() -> service.get(STRANGER, 100L))
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> service.accept(STRANGER, 100L))
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void cannotAcceptWhenOfferedItemIsGone() {
        Item wanted = item(10L, owner, ItemType.EXCHANGE, ItemStatus.AVAILABLE);
        Item offered = item(20L, requester, ItemType.EXCHANGE, ItemStatus.IN_TRANSACTION);
        request(100L, wanted, offered, ExchangeStatus.PENDING);

        assertThatThrownBy(() -> service.accept(OWNER, 100L)).hasMessageContaining("offered item");
        assertThat(wanted.getStatus()).isEqualTo(ItemStatus.AVAILABLE);
    }

    @Test
    void cannotAcceptTwice() {
        request(100L, item(10L, owner, ItemType.GIVEAWAY, ItemStatus.IN_TRANSACTION), null, ExchangeStatus.ACCEPTED);

        assertThatThrownBy(() -> service.accept(OWNER, 100L))
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void rejectOnlyFromPending() {
        ExchangeRequest request = request(100L, item(10L, owner, ItemType.GIVEAWAY, ItemStatus.AVAILABLE), null,
                ExchangeStatus.PENDING);

        service.reject(OWNER, 100L);

        assertThat(request.getStatus()).isEqualTo(ExchangeStatus.REJECTED);
    }

    // ------------------------------------------------------------------ cancel / complete

    @Test
    void requesterCancellingAcceptedDealReleasesItems() {
        Item wanted = item(10L, owner, ItemType.EXCHANGE, ItemStatus.IN_TRANSACTION);
        Item offered = item(20L, requester, ItemType.EXCHANGE, ItemStatus.IN_TRANSACTION);
        ExchangeRequest request = request(100L, wanted, offered, ExchangeStatus.ACCEPTED);

        service.cancel(REQUESTER, 100L);

        assertThat(request.getStatus()).isEqualTo(ExchangeStatus.CANCELLED);
        assertThat(wanted.getStatus()).isEqualTo(ItemStatus.AVAILABLE);
        assertThat(offered.getStatus()).isEqualTo(ItemStatus.AVAILABLE);
    }

    @Test
    void ownerMustRejectRatherThanCancelPendingRequest() {
        request(100L, item(10L, owner, ItemType.GIVEAWAY, ItemStatus.AVAILABLE), null, ExchangeStatus.PENDING);

        assertThatThrownBy(() -> service.cancel(OWNER, 100L))
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void completeMarksItemsTradedAndClosesOtherRequests() {
        Item wanted = item(10L, owner, ItemType.EXCHANGE, ItemStatus.IN_TRANSACTION);
        Item offered = item(20L, requester, ItemType.EXCHANGE, ItemStatus.IN_TRANSACTION);
        ExchangeRequest request = request(100L, wanted, offered, ExchangeStatus.ACCEPTED);

        service.complete(OWNER, 100L);

        assertThat(request.getStatus()).isEqualTo(ExchangeStatus.COMPLETED);
        assertThat(wanted.getStatus()).isEqualTo(ItemStatus.COMPLETED);
        assertThat(offered.getStatus()).isEqualTo(ItemStatus.COMPLETED);
        verify(exchangeRequestRepository).closePendingOffering(10L, ExchangeStatus.CANCELLED);
        verify(exchangeRequestRepository).closePendingOffering(20L, ExchangeStatus.CANCELLED);
    }

    @Test
    void requesterCannotComplete() {
        request(100L, item(10L, owner, ItemType.GIVEAWAY, ItemStatus.IN_TRANSACTION), null, ExchangeStatus.ACCEPTED);

        assertThatThrownBy(() -> service.complete(REQUESTER, 100L))
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.FORBIDDEN));
        verify(exchangeRequestRepository, never()).closePendingOffering(any(), any());
    }
}
