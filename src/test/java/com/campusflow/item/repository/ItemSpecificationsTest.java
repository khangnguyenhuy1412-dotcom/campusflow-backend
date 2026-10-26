package com.campusflow.item.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ItemSpecificationsTest {

    @Test
    void likeWildcardsAreEscaped() {
        assertThat(ItemSpecifications.escapeLike("100%_off\\")).isEqualTo("100\\%\\_off\\\\");
        assertThat(ItemSpecifications.escapeLike("giao trinh")).isEqualTo("giao trinh");
    }
}
