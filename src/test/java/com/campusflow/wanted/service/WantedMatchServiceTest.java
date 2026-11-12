package com.campusflow.wanted.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusflow.category.entity.Category;
import com.campusflow.item.entity.Item;
import com.campusflow.item.entity.ItemType;
import com.campusflow.university.entity.University;
import com.campusflow.user.entity.User;
import com.campusflow.wanted.entity.WantedPost;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class WantedMatchServiceTest {

    private final Category books = Category.builder().id(1L).build();
    private final Category electronics = Category.builder().id(3L).build();
    private final University hcmus = University.builder().id(1L).build();
    private final University hcmut = University.builder().id(2L).build();
    private final User seeker = User.builder().id(1L).university(hcmus).build();

    private Item item(Category category, ItemType type, String price, User owner) {
        return Item.builder().id(10L).owner(owner).category(category).itemType(type)
                .price(new BigDecimal(price)).title("Giáo trình Giải tích 1").description("Còn mới 95%").build();
    }

    private WantedPost post(Category category, String maxPrice, boolean sameUniversity) {
        return WantedPost.builder().user(seeker).keywords("giai tich").category(category)
                .maxPrice(maxPrice == null ? null : new BigDecimal(maxPrice)).sameUniversityOnly(sameUniversity).build();
    }

    private static boolean matches(WantedPost post, Item item) {
        return WantedMatchService.matches(post, item, WantedMatchService.itemText(item));
    }

    @Test
    void keywordsMatchWithoutAccents() {
        User seller = User.builder().id(2L).build();
        assertThat(matches(post(null, null, false), item(books, ItemType.GIVEAWAY, "0", seller))).isTrue();
    }

    @Test
    void categoryMustMatchWhenSet() {
        User seller = User.builder().id(2L).build();
        assertThat(matches(post(books, null, false), item(electronics, ItemType.GIVEAWAY, "0", seller))).isFalse();
    }

    @Test
    void salePriceAboveMaxDoesNotMatchButFreeItemsDo() {
        User seller = User.builder().id(2L).build();
        WantedPost cheap = post(null, "50000", false);

        assertThat(matches(cheap, item(books, ItemType.SALE, "80000", seller))).isFalse();
        assertThat(matches(cheap, item(books, ItemType.SALE, "50000", seller))).isTrue();
        assertThat(matches(cheap, item(books, ItemType.EXCHANGE, "0", seller))).isTrue();
    }

    @Test
    void sameUniversityOnlyNeedsTheSameKnownUniversity() {
        WantedPost local = post(null, null, true);

        assertThat(matches(local, item(books, ItemType.GIVEAWAY, "0", User.builder().id(2L).university(hcmus).build())))
                .isTrue();
        assertThat(matches(local, item(books, ItemType.GIVEAWAY, "0", User.builder().id(3L).university(hcmut).build())))
                .isFalse();
        assertThat(matches(local, item(books, ItemType.GIVEAWAY, "0", User.builder().id(4L).build())))
                .as("unknown university never counts as the same").isFalse();
    }
}
