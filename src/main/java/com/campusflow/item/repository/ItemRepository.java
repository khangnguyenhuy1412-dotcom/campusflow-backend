package com.campusflow.item.repository;

import com.campusflow.item.entity.Item;
import com.campusflow.item.entity.ItemStatus;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ItemRepository extends JpaRepository<Item, Long>, JpaSpecificationExecutor<Item> {

    /** Loads owner and category in the same query to avoid one extra query per item (N+1). */
    @Override
    @EntityGraph(attributePaths = {"owner", "category"})
    Page<Item> findAll(Specification<Item> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"owner", "category"})
    Optional<Item> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"owner", "category"})
    Page<Item> findByOwnerIdAndStatusIn(Long ownerId, Collection<ItemStatus> statuses, Pageable pageable);

    boolean existsByCategoryId(Long categoryId);
}
