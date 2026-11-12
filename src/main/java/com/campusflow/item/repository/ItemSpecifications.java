package com.campusflow.item.repository;

import com.campusflow.common.util.SqlLike;
import com.campusflow.item.dto.ItemSearchCriteria;
import com.campusflow.item.entity.Item;
import com.campusflow.item.entity.ItemStatus;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/** Builds the dynamic WHERE clause for the public item search. */
public final class ItemSpecifications {

    private ItemSpecifications() {
    }

    /** Public search only ever returns AVAILABLE items of active (not banned) users. */
    public static Specification<Item> publicSearch(ItemSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), ItemStatus.AVAILABLE));
            predicates.add(cb.isTrue(root.get("owner").get("active")));

            if (hasText(criteria.keyword())) {
                String pattern = SqlLike.contains(criteria.keyword());
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern, SqlLike.ESCAPE),
                        cb.like(cb.lower(root.get("description")), pattern, SqlLike.ESCAPE)));
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
            if (criteria.universityId() != null) {
                predicates.add(cb.equal(root.get("owner").get("university").get("id"), criteria.universityId()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
