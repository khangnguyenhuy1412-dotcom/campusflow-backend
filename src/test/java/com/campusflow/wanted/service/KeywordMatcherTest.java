package com.campusflow.wanted.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class KeywordMatcherTest {

    @Test
    void normalizeRemovesVietnameseAccentsAndPunctuation() {
        assertThat(KeywordMatcher.normalize("Giáo trình GIẢI TÍCH 1 (mới 99%)")).isEqualTo("giao trinh giai tich 1 moi 99");
        assertThat(KeywordMatcher.normalize("Đồ dùng phòng trọ")).isEqualTo("do dung phong tro");
    }

    @Test
    void keywordsAreNormalizedDistinctAndShortWordsDropped() {
        assertThat(KeywordMatcher.keywords("Giải tích, giải TÍCH a")).containsExactly("giai", "tich");
        assertThat(KeywordMatcher.keywords(" , ! ")).isEmpty();
    }

    @Test
    void textMustContainEveryKeyword() {
        List<String> keywords = KeywordMatcher.keywords("giai tich");
        String text = KeywordMatcher.normalize("Bán giáo trình Giải tích 1 còn mới");

        assertThat(KeywordMatcher.matches(keywords, text)).isTrue();
        assertThat(KeywordMatcher.matches(KeywordMatcher.keywords("giai tich 2"), text)).isFalse();
        assertThat(KeywordMatcher.matches(List.of(), text)).as("no keywords never matches").isFalse();
    }

    @Test
    void numbersAreKeptAndMatchedAsWholeWords() {
        assertThat(KeywordMatcher.keywords("giai tich 2")).containsExactly("giai", "tich", "2");
        assertThat(KeywordMatcher.matches(KeywordMatcher.keywords("tap 1"), KeywordMatcher.normalize("Tập 10")))
                .as("1 must not match 10").isFalse();
    }
}
