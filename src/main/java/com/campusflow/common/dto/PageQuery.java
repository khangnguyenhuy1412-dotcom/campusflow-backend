package com.campusflow.common.dto;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Builds a safe {@link Pageable} from untrusted query parameters: page is at least 0
 * and size is capped, so a client cannot ask for millions of rows at once.
 */
public final class PageQuery {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 50;

    private PageQuery() {
    }

    public static Pageable of(Integer page, Integer size, Sort sort) {
        int safePage = page == null ? 0 : Math.max(page, 0);
        int safeSize = size == null ? DEFAULT_SIZE : Math.clamp(size, 1, MAX_SIZE);
        return PageRequest.of(safePage, safeSize, sort);
    }
}
