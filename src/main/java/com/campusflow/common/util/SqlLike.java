package com.campusflow.common.util;

import java.util.Locale;

/** Builds safe SQL LIKE patterns from user input. */
public final class SqlLike {

    /** Escape character to pass to LIKE (e.g. {@code like :pattern escape '\'}). */
    public static final char ESCAPE = '\\';

    private SqlLike() {
    }

    /**
     * Case-insensitive "contains" pattern. LIKE wildcards in the input are escaped, so a search
     * for "100%" matches the text "100%" instead of everything.
     */
    public static String contains(String text) {
        return "%" + escape(text.trim().toLowerCase(Locale.ROOT)) + "%";
    }

    static String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
