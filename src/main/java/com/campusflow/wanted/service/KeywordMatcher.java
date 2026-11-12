package com.campusflow.wanted.service;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Accent-insensitive keyword matching for Vietnamese text: "giai tich" matches "Giải tích 1",
 * "do dung" matches "Đồ dùng". A text matches when it contains every keyword as a whole word,
 * so "giai tich 2" does not match "Giải tích 1" and "1" does not match "10".
 */
public final class KeywordMatcher {

    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
    private static final Pattern DIGITS = Pattern.compile("[0-9]+");
    private static final int MIN_WORD_LENGTH = 2;
    public static final int MAX_KEYWORDS = 8;

    private KeywordMatcher() {
    }

    /** Lowercase, without accents, words separated by single spaces. */
    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String lower = text.toLowerCase(Locale.ROOT).replace('đ', 'd');
        String withoutAccents = COMBINING_MARKS.matcher(Normalizer.normalize(lower, Normalizer.Form.NFD)).replaceAll("");
        return NON_ALPHANUMERIC.matcher(withoutAccents).replaceAll(" ").strip();
    }

    /**
     * Distinct normalized keywords. Single letters are ignored because they match almost anything;
     * numbers are always kept because they matter ("Giải tích 1" vs "Giải tích 2").
     */
    public static List<String> keywords(String keywords) {
        String normalized = normalize(keywords);
        if (normalized.isEmpty()) {
            return List.of();
        }
        return Arrays.stream(normalized.split(" "))
                .filter(word -> word.length() >= MIN_WORD_LENGTH || DIGITS.matcher(word).matches())
                .distinct()
                .limit(MAX_KEYWORDS)
                .toList();
    }

    /** True when the (already normalized) text contains every keyword as a whole word. */
    public static boolean matches(List<String> keywords, String normalizedText) {
        if (keywords.isEmpty()) {
            return false;
        }
        String padded = " " + normalizedText + " ";
        return keywords.stream().allMatch(keyword -> padded.contains(" " + keyword + " "));
    }
}
