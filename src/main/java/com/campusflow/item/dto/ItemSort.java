package com.campusflow.item.dto;

import org.springframework.data.domain.Sort;

/** Whitelisted sort options, so clients cannot sort by arbitrary (or non-indexed) columns. */
public enum ItemSort {
    NEWEST(Sort.by(Sort.Direction.DESC, "createdAt")),
    OLDEST(Sort.by(Sort.Direction.ASC, "createdAt")),
    PRICE_ASC(Sort.by(Sort.Direction.ASC, "price").and(Sort.by(Sort.Direction.DESC, "createdAt"))),
    PRICE_DESC(Sort.by(Sort.Direction.DESC, "price").and(Sort.by(Sort.Direction.DESC, "createdAt")));

    private final Sort sort;

    ItemSort(Sort sort) {
        this.sort = sort;
    }

    public Sort toSort() {
        return sort.and(Sort.by(Sort.Direction.DESC, "id"));
    }
}
