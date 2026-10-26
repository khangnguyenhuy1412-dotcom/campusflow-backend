package com.campusflow.item.repository;

import com.campusflow.item.dto.ItemSearchCriteria;
import com.campusflow.item.entity.Item;
import com.campusflow.item.entity.ItemStatus;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/** Builds the dynamic WHERE clause for the public item search. */
public final class ItemSpecifications {

    private static final char LIKE_ESCAPE = '\\';

    private ItemSpecifications() {
    }

    /** Public search only ever returns AVAILABLE items of active (not banned) users. */
    public static Specification<Item> publicSearch(ItemSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), ItemStatus.AVAILABLE));
            predicates.add(cb.isTrue(root.get("owner").get("active")));

            if (hasText(criteria.keyword())) {
                String pattern = "%" + escapeLike(criteria.keyword().trim().toLowerCase(Locale.ROOT)) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern, LIKE_ESCAPE),
                        cb.like(cb.lower(root.get("description")), pattern, LIKE_ESCAPE)));
            }
            if (criteria.categoryId() != null) {
                predicates.add(cb.equal(root.get("category").get("id"), criteria.categoryId()));
            }
            if (criteria.type() != null) {
                predicates.add(cb.equal(root.get("itemType"), criteria.type()));
            }
            if (criteria.condition() != null) {
                predicates.add(cb.equal(root.get("condition"), criteria.condition()));
            }
            if (criteria.minPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), criteria.minPrice()));
            }
            if (criteria.maxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), criteria.maxPrice()));
            }
            if (hasText(criteria.university())) {
                predicates.add(cb.equal(
                        cb.lower(root.get("owner").get("university")),
                        criteria.university().trim().toLowerCase(Locale.ROOT)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    /** Escapes LIKE wildcards so a search for "100%" matches the text "100%", not everything. */
    static String escapeLike(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
