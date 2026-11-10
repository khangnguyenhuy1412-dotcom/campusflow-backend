package com.campusflow.exchange.repository;

import com.campusflow.exchange.entity.ExchangeRequest;
import com.campusflow.exchange.entity.ExchangeStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExchangeRequestRepository extends JpaRepository<ExchangeRequest, Long> {

    @EntityGraph(attributePaths = {"item", "item.owner", "item.category", "requester", "offeredItem", "offeredItem.owner",
            "offeredItem.category"})
    Optional<ExchangeRequest> findWithDetailsById(Long id);

    /** Requests the user has sent. A null status means all statuses. */
    @EntityGraph(attributePaths = {"item", "item.owner", "item.category", "requester", "offeredItem", "offeredItem.owner",
            "offeredItem.category"})
    @Query("""
            select r from ExchangeRequest r
            where r.requester.id = :userId and (:status is null or r.status = :status)
            """)
    Page<ExchangeRequest> findSent(@Param("userId") Long userId, @Param("status") ExchangeStatus status,
            Pageable pageable);

    /** Requests for items the user owns. A null status means all statuses. */
    @EntityGraph(attributePaths = {"item", "item.owner", "item.category", "requester", "offeredItem", "offeredItem.owner",
            "offeredItem.category"})
    @Query("""
            select r from ExchangeRequest r
            where r.item.owner.id = :userId and (:status is null or r.status = :status)
            """)
    Page<ExchangeRequest> findReceived(@Param("userId") Long userId, @Param("status") ExchangeStatus status,
            Pageable pageable);

    boolean existsByItemIdAndRequesterIdAndStatusIn(Long itemId, Long requesterId, Collection<ExchangeStatus> statuses);

    List<ExchangeRequest> findByItemIdAndStatus(Long itemId, ExchangeStatus status);

    @EntityGraph(attributePaths = {"item", "item.owner", "requester"})
    List<ExchangeRequest> findByOfferedItemIdAndStatus(Long offeredItemId, ExchangeStatus status);

    @EntityGraph(attributePaths = {"item", "item.owner", "requester", "offeredItem"})
    List<ExchangeRequest> findByItemIdAndStatusIn(Long itemId, Collection<ExchangeStatus> statuses);

    @EntityGraph(attributePaths = {"item", "item.owner", "requester", "offeredItem"})
    List<ExchangeRequest> findByOfferedItemIdAndStatusIn(Long offeredItemId, Collection<ExchangeStatus> statuses);
}
