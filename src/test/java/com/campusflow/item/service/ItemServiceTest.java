package com.campusflow.item.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.campusflow.category.entity.Category;
import com.campusflow.category.service.CategoryService;
import com.campusflow.common.exception.ApiException;
import com.campusflow.exchange.repository.ExchangeRequestRepository;
import com.campusflow.item.dto.ItemDetailResponse;
import com.campusflow.item.dto.ItemRequest;
import com.campusflow.item.dto.ItemSearchCriteria;
import com.campusflow.item.entity.Item;
import com.campusflow.item.entity.ItemCondition;
import com.campusflow.item.entity.ItemImage;
import com.campusflow.item.entity.ItemStatus;
import com.campusflow.item.entity.ItemType;
import com.campusflow.item.repository.ItemRepository;
import com.campusflow.storage.StoredFileCleanup;
import com.campusflow.user.entity.User;
import com.campusflow.user.repository.UserRepository;
import java.math.BigDecimal;
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
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CategoryService categoryService;
    @Mock
    private ExchangeRequestRepository exchangeRequestRepository;
    @Mock
    private StoredFileCleanup storedFileCleanup;
    @InjectMocks
    private ItemService service;

    private User owner;
    private Category category;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).fullName("Owner").build();
        category = Category.builder().id(3L).name("Books").slug("books").build();
        lenient().when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        lenient().when(categoryService.getEntity(3L)).thenReturn(category);
        lenient().when(itemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private static ItemRequest request(ItemType type, String price, String preference) {
        return new ItemRequest(3L, "  Giai tich 1  ", "Like new", type,
                price == null ? null : new BigDecimal(price), preference, ItemCondition.LIKE_NEW, null);
    }

    private Item existing(ItemStatus status) {
        Item item = Item.builder().id(10L).owner(owner).category(category).title("t").description("d")
                .itemType(ItemType.SALE).price(BigDecimal.TEN).condition(ItemCondition.USED).status(status).build();
        lenient().when(itemRepository.findWithDetailsById(10L)).thenReturn(Optional.of(item));
        return item;
    }

    @Test
    void saleNeedsPositivePrice() {
        assertThatThrownBy(() -> service.create(1L, request(ItemType.SALE, null, null)))
                .hasMessageContaining("price greater than 0");
        assertThatThrownBy(() -> service.create(1L, request(ItemType.SALE, "0", null)))
                .hasMessageContaining("price greater than 0");
    }

    @Test
    void onlySaleMayHaveAPrice() {
        assertThatThrownBy(() -> service.create(1L, request(ItemType.GIVEAWAY, "50000", null)))
                .hasMessageContaining("Only SALE");
    }

    @Test
    void createNormalizesFieldsByType() {
        ItemDetailResponse sale = service.create(1L, request(ItemType.SALE, "50000", "ignored"));
        assertThat(sale.title()).isEqualTo("Giai tich 1");
        assertThat(sale.price()).isEqualByComparingTo("50000");
        assertThat(sale.exchangePreference()).isNull();
        assertThat(sale.status()).isEqualTo(ItemStatus.AVAILABLE);

        ItemDetailResponse exchange = service.create(1L, request(ItemType.EXCHANGE, null, " a calculator "));
        assertThat(exchange.price()).isEqualByComparingTo("0");
        assertThat(exchange.exchangePreference()).isEqualTo("a calculator");
    }

    @Test
    void cannotUpdateSomeoneElsesItem() {
        existing(ItemStatus.AVAILABLE);

        assertThatThrownBy(() -> service.update(99L, 10L, request(ItemType.SALE, "1000", null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void cannotEditItemInTransaction() {
        existing(ItemStatus.IN_TRANSACTION);

        assertThatThrownBy(() -> service.update(1L, 10L, request(ItemType.SALE, "1000", null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void hiddenItemIsInvisibleToOthersButNotToOwner() {
        existing(ItemStatus.HIDDEN);

        assertThatThrownBy(() -> service.getDetail(10L, 99L))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> service.getDetail(10L, null)).isInstanceOf(ApiException.class);
        assertThat(service.getDetail(10L, 1L).id()).isEqualTo(10L);
    }

    @Test
    void deleteRemovesImagesAfterCommitAndClosesOffers() {
        Item item = existing(ItemStatus.AVAILABLE);
        item.getImages().add(ItemImage.builder().imageUrl("/uploads/a.jpg").thumbnailUrl("/uploads/a_t.jpg").build());

        service.delete(1L, 10L);

        verify(exchangeRequestRepository).closePendingOffering(any(), any());
        verify(itemRepository).delete(item);
        verify(storedFileCleanup).deleteAfterCommit(List.of("/uploads/a.jpg", "/uploads/a_t.jpg"));
    }

    @Test
    void cannotDeleteCompletedItem() {
        existing(ItemStatus.COMPLETED);

        assertThatThrownBy(() -> service.delete(1L, 10L)).isInstanceOf(ApiException.class);
        verify(itemRepository, never()).delete(any(Item.class));
    }

    @Test
    void searchRejectsInvertedPriceRange() {
        ItemSearchCriteria criteria = new ItemSearchCriteria(
                null, null, null, null, new BigDecimal("100"), new BigDecimal("10"), null);

        assertThatThrownBy(() -> service.search(criteria, null, 0, 20)).hasMessageContaining("minPrice");
    }
}
